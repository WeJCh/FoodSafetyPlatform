package com.mortal.regulation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.regulation.common.constants.ProductCategoryCatalog;
import com.mortal.regulation.dto.ProductSaveDTO;
import com.mortal.regulation.entity.AddrRegion;
import com.mortal.regulation.entity.FoodEnterprise;
import com.mortal.regulation.entity.FoodProduct;
import com.mortal.regulation.entity.FoodRegulator;
import com.mortal.regulation.entity.FoodRegulatorRegion;
import com.mortal.regulation.mapper.AddrRegionMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.mapper.FoodProductMapper;
import com.mortal.regulation.mapper.FoodRegulatorMapper;
import com.mortal.regulation.mapper.FoodRegulatorRegionMapper;
import com.mortal.regulation.service.AuditLogService;
import com.mortal.regulation.service.ProductService;
import com.mortal.regulation.support.AuditOperatorNameResolver;
import com.mortal.regulation.support.ProductMasterCacheService;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.vo.ProductVO;
import com.mortal.regulation.vo.internal.InternalProductDetailVO;
import com.mortal.regulation.vo.internal.InternalProductSummaryVO;
import java.util.ArrayDeque;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String APPROVAL_APPROVED = "APPROVED";

    private final FoodProductMapper foodProductMapper;
    private final FoodEnterpriseMapper foodEnterpriseMapper;
    private final FoodRegulatorMapper foodRegulatorMapper;
    private final FoodRegulatorRegionMapper foodRegulatorRegionMapper;
    private final AddrRegionMapper addrRegionMapper;
    private final AuditLogService auditLogService;
    private final AuditOperatorNameResolver auditOperatorNameResolver;
    private final ProductMasterCacheService productMasterCacheService;

    public ProductServiceImpl(FoodProductMapper foodProductMapper,
                              FoodEnterpriseMapper foodEnterpriseMapper,
                              FoodRegulatorMapper foodRegulatorMapper,
                              FoodRegulatorRegionMapper foodRegulatorRegionMapper,
                              AddrRegionMapper addrRegionMapper,
                              AuditLogService auditLogService,
                              AuditOperatorNameResolver auditOperatorNameResolver,
                              ProductMasterCacheService productMasterCacheService) {
        this.foodProductMapper = foodProductMapper;
        this.foodEnterpriseMapper = foodEnterpriseMapper;
        this.foodRegulatorMapper = foodRegulatorMapper;
        this.foodRegulatorRegionMapper = foodRegulatorRegionMapper;
        this.addrRegionMapper = addrRegionMapper;
        this.auditLogService = auditLogService;
        this.auditOperatorNameResolver = auditOperatorNameResolver;
        this.productMasterCacheService = productMasterCacheService;
    }

    @Override
    public List<ProductVO> listMyProducts(Long userId) {
        FoodEnterprise enterprise = requireEnterpriseByUserId(userId);
        return listByEnterpriseId(enterprise.getId());
    }

    @Override
    public ProductVO createMyProduct(Long userId, String username, ProductSaveDTO dto) {
        FoodEnterprise enterprise = requireApprovedEnterpriseByUserId(userId);
        ensureUniqueProductName(enterprise.getId(), normalizeText(dto.getProductName()), null);
        FoodProduct product = new FoodProduct();
        product.setEnterpriseId(enterprise.getId());
        product.setProductName(normalizeText(dto.getProductName()));
        product.setCategory(normalizeCategory(dto.getCategory()));
        product.setSpecification(normalizeText(dto.getSpecification()));
        product.setStatus(resolveStatus(dto.getStatus(), STATUS_ACTIVE));
        product.setRemark(normalizeText(dto.getRemark()));
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        product.setDeleted(0);
        foodProductMapper.insert(product);
        productMasterCacheService.evict(product.getId());
        auditLogService.recordProductAudit(
            userId,
            "ENTERPRISE",
            auditOperatorNameResolver.resolveEnterpriseOperatorName(enterprise, username),
            "PRODUCT_CREATE",
            "企业新增产品档案",
            null,
            copyProduct(product),
            null
        );
        return toVO(product);
    }

    @Override
    public ProductVO updateMyProduct(Long userId, String username, Long productId, ProductSaveDTO dto) {
        FoodEnterprise enterprise = requireApprovedEnterpriseByUserId(userId);
        FoodProduct product = requireOwnedProduct(enterprise.getId(), productId);
        FoodProduct before = copyProduct(product);
        String normalizedName = normalizeText(dto.getProductName());
        ensureUniqueProductName(enterprise.getId(), normalizedName, product.getId());
        product.setProductName(normalizedName);
        product.setCategory(normalizeCategory(dto.getCategory()));
        product.setSpecification(normalizeText(dto.getSpecification()));
        product.setStatus(resolveStatus(dto.getStatus(), product.getStatus()));
        product.setRemark(normalizeText(dto.getRemark()));
        product.setUpdateTime(LocalDateTime.now());
        foodProductMapper.updateById(product);
        productMasterCacheService.evict(product.getId());
        auditLogService.recordProductAudit(
            userId,
            "ENTERPRISE",
            auditOperatorNameResolver.resolveEnterpriseOperatorName(enterprise, username),
            "PRODUCT_UPDATE",
            "企业更新产品档案",
            before,
            copyProduct(product),
            null
        );
        return toVO(product);
    }

    @Override
    public List<AuditLogVO> listMyProductLogs(Long userId, Long productId, Integer limit) {
        FoodEnterprise enterprise = requireEnterpriseByUserId(userId);
        FoodProduct product = requireOwnedProduct(enterprise.getId(), productId);
        return auditLogService.listProductLogs(product.getId(), limit == null ? 10 : limit);
    }

    @Override
    public List<ProductVO> listByEnterpriseId(Long enterpriseId) {
        requireEnterpriseById(enterpriseId);
        return foodProductMapper.selectList(new LambdaQueryWrapper<FoodProduct>()
                .eq(FoodProduct::getEnterpriseId, enterpriseId)
                .eq(FoodProduct::getDeleted, 0)
                .orderByDesc(FoodProduct::getUpdateTime, FoodProduct::getId))
            .stream()
            .filter(Objects::nonNull)
            .map(this::toVO)
            .toList();
    }

    @Override
    public List<ProductVO> listByEnterpriseIdForRegulator(Long operatorUserId, Long enterpriseId) {
        requireEnterpriseInRegulatorScope(operatorUserId, enterpriseId);
        return listByEnterpriseId(enterpriseId);
    }

    @Override
    public List<AuditLogVO> listProductLogsForRegulator(Long operatorUserId,
                                                        Long enterpriseId,
                                                        Long productId,
                                                        Integer limit) {
        requireEnterpriseInRegulatorScope(operatorUserId, enterpriseId);
        FoodProduct product = requireProduct(productId);
        if (!Objects.equals(product.getEnterpriseId(), enterpriseId)) {
            throw new IllegalArgumentException("product not found");
        }
        return auditLogService.listProductLogs(product.getId(), limit == null ? 10 : limit);
    }

    @Override
    public InternalProductDetailVO getInternalById(Long productId) {
        FoodProduct product = requireProduct(productId);
        return toInternalDetailVO(product);
    }

    @Override
    public List<InternalProductSummaryVO> listInternalByEnterpriseId(Long enterpriseId) {
        requireEnterpriseById(enterpriseId);
        return foodProductMapper.selectList(new LambdaQueryWrapper<FoodProduct>()
                .eq(FoodProduct::getEnterpriseId, enterpriseId)
                .eq(FoodProduct::getDeleted, 0)
                .orderByDesc(FoodProduct::getUpdateTime, FoodProduct::getId))
            .stream()
            .filter(Objects::nonNull)
            .map(this::toInternalSummaryVO)
            .toList();
    }

    @Override
    public List<Long> queryInternalProductIdsByNameKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return foodProductMapper.selectList(new LambdaQueryWrapper<FoodProduct>()
                .eq(FoodProduct::getDeleted, 0)
                .like(FoodProduct::getProductName, keyword.trim())
                .select(FoodProduct::getId)
                .orderByAsc(FoodProduct::getId)
                .last("limit 2000"))
            .stream()
            .map(FoodProduct::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public List<InternalProductSummaryVO> getInternalSummaries(List<Long> ids) {
        List<Long> cleanedIds = sanitizeIds(ids);
        if (cleanedIds.isEmpty()) {
            return List.of();
        }
        List<FoodProduct> products = foodProductMapper.selectBatchIds(cleanedIds)
            .stream()
            .filter(Objects::nonNull)
            .filter(product -> !isDeleted(product.getDeleted()))
            .toList();
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, FoodProduct> productMap = products.stream()
            .collect(Collectors.toMap(FoodProduct::getId, Function.identity(), (a, b) -> a));
        return cleanedIds.stream()
            .map(productMap::get)
            .filter(Objects::nonNull)
            .map(this::toInternalSummaryVO)
            .toList();
    }

    private FoodEnterprise requireEnterpriseByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodEnterprise enterprise = foodEnterpriseMapper.selectOne(new LambdaQueryWrapper<FoodEnterprise>()
            .eq(FoodEnterprise::getUserId, userId)
            .eq(FoodEnterprise::getDeleted, 0)
            .last("limit 1"));
        if (enterprise == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private FoodEnterprise requireApprovedEnterpriseByUserId(Long userId) {
        FoodEnterprise enterprise = requireEnterpriseByUserId(userId);
        if (!APPROVAL_APPROVED.equalsIgnoreCase(enterprise.getApprovalStatus())) {
            throw new IllegalArgumentException("enterprise profile not approved");
        }
        return enterprise;
    }

    private FoodEnterprise requireEnterpriseById(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new IllegalArgumentException("enterprise not found");
        }
        FoodEnterprise enterprise = foodEnterpriseMapper.selectById(enterpriseId);
        if (enterprise == null || isDeleted(enterprise.getDeleted())) {
            throw new IllegalArgumentException("enterprise not found");
        }
        return enterprise;
    }

    private void requireEnterpriseInRegulatorScope(Long operatorUserId, Long enterpriseId) {
        if (operatorUserId == null) {
            throw new IllegalArgumentException("unauthorized");
        }
        FoodEnterprise enterprise = requireEnterpriseById(enterpriseId);
        List<Long> scopeRegionIds = resolveRegulatorRegionIds(operatorUserId);
        if (scopeRegionIds.isEmpty() || !scopeRegionIds.contains(enterprise.getRegionId())) {
            throw new IllegalArgumentException("unauthorized");
        }
    }

    private List<Long> resolveRegulatorRegionIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        FoodRegulator regulator = foodRegulatorMapper.selectOne(new LambdaQueryWrapper<FoodRegulator>()
            .eq(FoodRegulator::getUserId, userId)
            .eq(FoodRegulator::getDeleted, 0)
            .last("limit 1"));
        if (regulator == null) {
            return List.of();
        }
        List<Long> directRegionIds = foodRegulatorRegionMapper.selectList(new LambdaQueryWrapper<FoodRegulatorRegion>()
                .eq(FoodRegulatorRegion::getRegulatorId, regulator.getId())
                .eq(FoodRegulatorRegion::getDeleted, 0))
            .stream()
            .map(FoodRegulatorRegion::getRegionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (directRegionIds.isEmpty()) {
            return List.of();
        }
        return collectRegionIds(directRegionIds);
    }

    private List<Long> collectRegionIds(List<Long> rootIds) {
        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(rootIds);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || result.contains(current)) {
                continue;
            }
            result.add(current);
            List<AddrRegion> children = addrRegionMapper.selectList(new LambdaQueryWrapper<AddrRegion>()
                .eq(AddrRegion::getParentId, current)
                .eq(AddrRegion::getDeleted, 0));
            for (AddrRegion child : children) {
                if (child != null && child.getId() != null) {
                    queue.add(child.getId());
                }
            }
        }
        return result.stream().toList();
    }

    private FoodProduct requireProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("product not found");
        }
        FoodProduct product = foodProductMapper.selectById(productId);
        if (product == null || isDeleted(product.getDeleted())) {
            throw new IllegalArgumentException("product not found");
        }
        return product;
    }

    private FoodProduct requireOwnedProduct(Long enterpriseId, Long productId) {
        FoodProduct product = requireProduct(productId);
        if (!Objects.equals(product.getEnterpriseId(), enterpriseId)) {
            throw new IllegalArgumentException("product not found");
        }
        return product;
    }

    private void ensureUniqueProductName(Long enterpriseId, String productName, Long excludeId) {
        if (!StringUtils.hasText(productName)) {
            throw new IllegalArgumentException("productName required");
        }
        LambdaQueryWrapper<FoodProduct> wrapper = new LambdaQueryWrapper<FoodProduct>()
            .eq(FoodProduct::getEnterpriseId, enterpriseId)
            .eq(FoodProduct::getDeleted, 0)
            .eq(FoodProduct::getProductName, productName)
            .last("limit 1");
        if (excludeId != null) {
            wrapper.ne(FoodProduct::getId, excludeId);
        }
        FoodProduct duplicate = foodProductMapper.selectOne(wrapper);
        if (duplicate != null) {
            throw new IllegalArgumentException("product name already exists");
        }
    }

    private ProductVO toVO(FoodProduct product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setEnterpriseId(product.getEnterpriseId());
        vo.setProductName(product.getProductName());
        vo.setCategory(product.getCategory());
        vo.setSpecification(product.getSpecification());
        vo.setStatus(product.getStatus());
        vo.setRemark(product.getRemark());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());
        return vo;
    }

    private InternalProductDetailVO toInternalDetailVO(FoodProduct product) {
        InternalProductDetailVO vo = new InternalProductDetailVO();
        vo.setId(product.getId());
        vo.setEnterpriseId(product.getEnterpriseId());
        vo.setProductName(product.getProductName());
        vo.setCategory(product.getCategory());
        vo.setSpecification(product.getSpecification());
        vo.setStatus(product.getStatus());
        vo.setRemark(product.getRemark());
        return vo;
    }

    private InternalProductSummaryVO toInternalSummaryVO(FoodProduct product) {
        InternalProductSummaryVO vo = new InternalProductSummaryVO();
        vo.setId(product.getId());
        vo.setEnterpriseId(product.getEnterpriseId());
        vo.setProductName(product.getProductName());
        vo.setCategory(product.getCategory());
        vo.setSpecification(product.getSpecification());
        vo.setStatus(product.getStatus());
        return vo;
    }

    private String resolveStatus(String value, String fallback) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return fallback;
        }
        if (STATUS_ACTIVE.equals(normalized) || STATUS_INACTIVE.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("invalid product status");
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCategory(String value) {
        String normalized = ProductCategoryCatalog.normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("invalid product category");
        }
        return normalized;
    }

    private String buildEnterpriseOperatorName(FoodEnterprise enterprise, String username) {
        String enterpriseName = enterprise == null ? null : normalizeText(enterprise.getEnterpriseName());
        String normalizedUsername = normalizeText(username);
        if (StringUtils.hasText(enterpriseName) && StringUtils.hasText(normalizedUsername)) {
            return enterpriseName + "（" + normalizedUsername + "）";
        }
        if (StringUtils.hasText(enterpriseName)) {
            return enterpriseName;
        }
        return normalizedUsername;
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> cleaned = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                cleaned.add(id);
            }
        }
        return cleaned.stream().toList();
    }

    private boolean isDeleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private FoodProduct copyProduct(FoodProduct source) {
        if (source == null) {
            return null;
        }
        FoodProduct copy = new FoodProduct();
        copy.setId(source.getId());
        copy.setEnterpriseId(source.getEnterpriseId());
        copy.setProductName(source.getProductName());
        copy.setCategory(source.getCategory());
        copy.setSpecification(source.getSpecification());
        copy.setStatus(source.getStatus());
        copy.setRemark(source.getRemark());
        copy.setCreateTime(source.getCreateTime());
        copy.setUpdateTime(source.getUpdateTime());
        copy.setDeleted(source.getDeleted());
        return copy;
    }
}
