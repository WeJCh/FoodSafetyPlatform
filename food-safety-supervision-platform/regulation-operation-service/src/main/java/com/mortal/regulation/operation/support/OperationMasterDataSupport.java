package com.mortal.regulation.operation.support;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.RegulationEnterpriseInternalClient;
import com.mortal.regulation.operation.client.regulation.RegulationProductInternalClient;
import com.mortal.regulation.operation.client.regulation.RegulationRegulatorInternalClient;
import com.mortal.regulation.operation.client.regulation.dto.EnterpriseKeyReasonUpsertDTO;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseSummaryVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductSummaryVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.regulation.operation.common.OperationErrorMessages;
import java.util.Collection;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationMasterDataSupport {

    public static final String ROLE_ADMIN = "REGULATOR_ADMIN";
    public static final String ROLE_ENFORCER = "REGULATOR_ENFORCER";

    private final RegulationEnterpriseInternalClient enterpriseClient;
    private final RegulationProductInternalClient productClient;
    private final RegulationRegulatorInternalClient regulatorClient;
    private final OperationAuditOperatorNameResolver operationAuditOperatorNameResolver;
    private final String regulationInternalToken;

    public OperationMasterDataSupport(RegulationEnterpriseInternalClient enterpriseClient,
                                      RegulationProductInternalClient productClient,
                                      RegulationRegulatorInternalClient regulatorClient,
                                      OperationAuditOperatorNameResolver operationAuditOperatorNameResolver,
                                      @Value("${regulation.internal.token:regulation-internal-token}")
                                      String regulationInternalToken) {
        this.enterpriseClient = enterpriseClient;
        this.productClient = productClient;
        this.regulatorClient = regulatorClient;
        this.operationAuditOperatorNameResolver = operationAuditOperatorNameResolver;
        this.regulationInternalToken = regulationInternalToken;
    }

    public InternalEnterpriseDetailVO requireEnterprise(Long enterpriseId) {
        if (enterpriseId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.ENTERPRISE_NOT_FOUND);
        }
        ApiResponse<InternalEnterpriseDetailVO> response =
            enterpriseClient.getEnterpriseById(enterpriseId, regulationInternalToken);
        return requireData(response, OperationErrorMessages.ENTERPRISE_NOT_FOUND);
    }

    public InternalEnterpriseDetailVO requireApprovedEnterprise(Long enterpriseId) {
        InternalEnterpriseDetailVO enterprise = requireEnterprise(enterpriseId);
        if (!"APPROVED".equalsIgnoreCase(enterprise.getApprovalStatus())) {
            throw new IllegalArgumentException("enterprise not approved");
        }
        return enterprise;
    }

    public Long resolveEnterpriseOwnerRegulatorId(Long enterpriseId) {
        InternalEnterpriseDetailVO enterprise = requireEnterprise(enterpriseId);
        return enterprise == null ? null : enterprise.getRegulatorId();
    }

    public InternalEnterpriseDetailVO requireEnterpriseByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.UNAUTHORIZED);
        }
        ApiResponse<InternalEnterpriseDetailVO> response =
            enterpriseClient.getEnterpriseByUserId(userId, regulationInternalToken);
        return requireData(response, OperationErrorMessages.ENTERPRISE_NOT_FOUND);
    }

    /**
     * 获取产品详情
     * 
     * @param productId 产品ID
     * @return 产品详情
     */
    public InternalProductDetailVO requireProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("product not found");
        }
        ApiResponse<InternalProductDetailVO> response =
            productClient.getProductById(productId, regulationInternalToken);
        return requireData(response, "product not found");
    }

    /**
     * 获取企业的产品列表
     * 
     * @param enterpriseId 企业ID
     * @return 产品列表
     */
    public List<InternalProductSummaryVO> listProductsByEnterprise(Long enterpriseId) {
        if (enterpriseId == null) {
            return List.of();
        }
        ApiResponse<List<InternalProductSummaryVO>> response =
            productClient.listByEnterprise(enterpriseId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .toList();
    }

    public InternalRegulatorIdentityVO requireRegulatorByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.UNAUTHORIZED);
        }
        ApiResponse<InternalRegulatorIdentityVO> response =
            regulatorClient.getRegulatorByUserId(userId, regulationInternalToken);
        InternalRegulatorIdentityVO regulator = requireData(response, OperationErrorMessages.REGULATOR_NOT_FOUND);
        ensureEnabled(regulator);
        return regulator;
    }

    public InternalRegulatorIdentityVO requireRegulatorById(Long regulatorId) {
        if (regulatorId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        ApiResponse<InternalRegulatorIdentityVO> response =
            regulatorClient.getRegulatorIdentityById(regulatorId, regulationInternalToken);
        InternalRegulatorIdentityVO regulator = requireData(response, OperationErrorMessages.REGULATOR_NOT_FOUND);
        ensureEnabled(regulator);
        return regulator;
    }

    public InternalRegulatorIdentityVO requireAdmin(Long userId) {
        InternalRegulatorIdentityVO regulator = requireRegulatorByUserId(userId);
        requireRole(regulator, ROLE_ADMIN);
        return regulator;
    }

    public InternalRegulatorIdentityVO requireEnforcer(Long userId) {
        InternalRegulatorIdentityVO regulator = requireRegulatorByUserId(userId);
        requireRole(regulator, ROLE_ENFORCER);
        return regulator;
    }

    public void requireRole(InternalRegulatorIdentityVO regulator, String roleType) {
        if (regulator == null || !roleType.equalsIgnoreCase(regulator.getRoleType())) {
            throw new IllegalArgumentException(OperationErrorMessages.INVALID_REGULATOR_ROLE);
        }
    }

    public Map<Long, String> loadEnterpriseNames(Collection<Long> enterpriseIds) {
        List<Long> cleanedIds = sanitizeIds(enterpriseIds);
        if (cleanedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<InternalEnterpriseSummaryVO>> response =
            enterpriseClient.getEnterpriseSummaries(cleanedIds, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyMap();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalEnterpriseSummaryVO::getId,
                InternalEnterpriseSummaryVO::getEnterpriseName,
                (a, b) -> a));
    }

    public Map<Long, String> loadRegulatorNames(Collection<Long> regulatorIds) {
        List<Long> cleanedIds = sanitizeIds(regulatorIds);
        if (cleanedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<InternalRegulatorSummaryVO>> response =
            regulatorClient.getRegulatorSummaries(cleanedIds, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyMap();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalRegulatorSummaryVO::getId,
                InternalRegulatorSummaryVO::getName,
                (a, b) -> a));
    }

    /**
     * 获取产品名称列表
     * 
     * @param productIds 产品ID列表
     * @return 产品名称列表
     */
    public Map<Long, String> loadProductNames(Collection<Long> productIds) {
        return loadProductSummaries(productIds).values().stream()
            .collect(Collectors.toMap(InternalProductSummaryVO::getId,
                InternalProductSummaryVO::getProductName,
                (a, b) -> a));
    }

    /**
     * 获取产品摘要列表
     * 
     * @param productIds 产品ID列表
     * @return 产品摘要列表
     */
    public Map<Long, InternalProductSummaryVO> loadProductSummaries(Collection<Long> productIds) {
        List<Long> cleanedIds = sanitizeIds(productIds);
        if (cleanedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ApiResponse<List<InternalProductSummaryVO>> response =
            productClient.getProductSummaries(cleanedIds, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return Collections.emptyMap();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalProductSummaryVO::getId,
                item -> item,
                (a, b) -> a));
    }

    public Map<Long, String> loadOperatorNamesByUserIds(Collection<Long> userIds) {
        List<Long> cleanedIds = sanitizeIds(userIds);
        if (cleanedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (Long userId : cleanedIds) {
            ApiResponse<InternalRegulatorIdentityVO> regulatorResponse =
                regulatorClient.getRegulatorByUserId(userId, regulationInternalToken);
            if (regulatorResponse != null && regulatorResponse.isSuccess() && regulatorResponse.getData() != null) {
                InternalRegulatorIdentityVO regulator = regulatorResponse.getData();
                names.put(userId, operationAuditOperatorNameResolver.resolveRegulatorOperatorName(
                    regulator.getName(),
                    regulator.getUsername()
                ));
                continue;
            }
            ApiResponse<InternalEnterpriseDetailVO> enterpriseResponse =
                enterpriseClient.getEnterpriseByUserId(userId, regulationInternalToken);
            if (enterpriseResponse != null && enterpriseResponse.isSuccess() && enterpriseResponse.getData() != null) {
                names.put(
                    userId,
                    operationAuditOperatorNameResolver.resolveEnterpriseOperatorName(
                        enterpriseResponse.getData().getEnterpriseName()
                    )
                );
            }
        }
        return names;
    }

    public boolean isEnterpriseInRegulatorScope(Long regulatorId, Long enterpriseId) {
        if (enterpriseId == null) {
            return false;
        }
        return resolveScopeEnterpriseIds(regulatorId).contains(enterpriseId);
    }

    public void requireEnterpriseInScope(Long regulatorId, Long enterpriseId) {
        if (!isEnterpriseInRegulatorScope(regulatorId, enterpriseId)) {
            throw new IllegalArgumentException(OperationErrorMessages.NOT_IN_SCOPE);
        }
    }

    public List<Long> resolveScopeRegionIds(Long regulatorId) {
        if (regulatorId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        ApiResponse<List<Long>> response =
            regulatorClient.getScopeRegionIds(regulatorId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public boolean coversRegion(Long regulatorId, Long regionId) {
        if (regionId == null) {
            return false;
        }
        return resolveScopeRegionIds(regulatorId).contains(regionId);
    }

    public void requireRegionInScope(Long regulatorId, Long regionId) {
        if (!coversRegion(regulatorId, regionId)) {
            throw new IllegalArgumentException(OperationErrorMessages.NOT_IN_SCOPE);
        }
    }

    public boolean isRegulatorAssignableToRegion(Long regulatorId, Long regionId) {
        if (regulatorId == null || regionId == null) {
            return false;
        }
        ApiResponse<Boolean> response =
            regulatorClient.isAssignableToRegion(regulatorId, regionId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        return Boolean.TRUE.equals(response.getData());
    }

    public List<Long> resolveScopeEnterpriseIds(Long regulatorId) {
        if (regulatorId == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        ApiResponse<List<Long>> response =
            regulatorClient.getScopeEnterpriseIds(regulatorId, regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_NOT_FOUND);
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<Long> resolveScopedEnterpriseIds(Long regulatorId, String enterpriseName) {
        List<Long> scopeEnterpriseIds = resolveScopeEnterpriseIds(regulatorId);
        if (scopeEnterpriseIds.isEmpty()) {
            return List.of();
        }
        if (!StringUtils.hasText(enterpriseName)) {
            return scopeEnterpriseIds;
        }
        List<Long> matchedEnterpriseIds = queryEnterpriseIdsByName(enterpriseName);
        if (matchedEnterpriseIds == null || matchedEnterpriseIds.isEmpty()) {
            return List.of();
        }
        Set<Long> scopeSet = new LinkedHashSet<>(scopeEnterpriseIds);
        return matchedEnterpriseIds.stream()
            .filter(scopeSet::contains)
            .distinct()
            .toList();
    }

    public List<Long> queryEnterpriseIdsByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        ApiResponse<List<Long>> response =
            enterpriseClient.queryEnterpriseIdsByName(keyword.trim(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<Long> queryProductIdsByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        ApiResponse<List<Long>> response =
            productClient.queryProductIdsByName(keyword.trim(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<Long> queryRegulatorIdsByName(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        ApiResponse<List<Long>> response =
            regulatorClient.queryRegulatorIdsByName(keyword.trim(), regulationInternalToken);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    /**
     * 标记企业为关键企业
     * @param enterpriseId 企业ID
     * @param reasonType 原因类型
     * @param reasonDetail 原因详情
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param operatorId 操作员ID
     */
    public void markEnterpriseAsKey(Long enterpriseId,
                                    String reasonType,
                                    String reasonDetail,
                                    String sourceType,
                                    Long sourceId,
                                    Long operatorId) {
        EnterpriseKeyReasonUpsertDTO dto = new EnterpriseKeyReasonUpsertDTO();
        dto.setReasonType(reasonType);
        dto.setReasonDetail(reasonDetail);
        dto.setSourceType(sourceType);
        dto.setSourceId(sourceId);
        dto.setOperatorId(operatorId);
        ApiResponse<Void> response =
            enterpriseClient.markEnterpriseAsKey(enterpriseId, dto, regulationInternalToken);
        if (response == null || !response.isSuccess()) {
            throw new IllegalArgumentException("mark enterprise as key failed");
        }
    }

    private void ensureEnabled(InternalRegulatorIdentityVO regulator) {
        if (regulator.getStatus() != null && regulator.getStatus() != 1) {
            throw new IllegalArgumentException(OperationErrorMessages.REGULATOR_DISABLED);
        }
    }

    private <T> T requireData(ApiResponse<T> response, String notFoundMessage) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalArgumentException(notFoundMessage);
        }
        return response.getData();
    }

    private List<Long> sanitizeIds(Collection<Long> ids) {
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
}
