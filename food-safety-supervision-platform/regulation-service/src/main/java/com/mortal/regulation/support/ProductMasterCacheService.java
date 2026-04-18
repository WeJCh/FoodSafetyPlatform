package com.mortal.regulation.support;

import com.mortal.regulation.vo.internal.InternalProductDetailVO;
import com.mortal.regulation.vo.internal.InternalProductSummaryVO;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductMasterCacheService {

    private static final String DOMAIN = "reg";
    private static final String PRODUCT = "product";
    private static final String DETAIL = "detail";
    private static final String SUMMARY = "summary";

    private final RegulationMasterCacheSupport cacheSupport;

    public ProductMasterCacheService(RegulationMasterCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public InternalProductDetailVO getDetail(Long productId, Supplier<InternalProductDetailVO> loader) {
        return cacheSupport.getOrLoad(detailKey(productId), loader);
    }

    public List<InternalProductSummaryVO> getSummaries(List<Long> ids,
                                                       Function<List<Long>, List<InternalProductSummaryVO>> batchLoader) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> dedup = new LinkedHashSet<>(ids);
        List<Long> missingIds = new ArrayList<>();
        for (Long id : dedup) {
            if (id != null && cacheSupport.<InternalProductSummaryVO>read(summaryKey(id)) == null) {
                missingIds.add(id);
            }
        }
        if (!missingIds.isEmpty()) {
            List<InternalProductSummaryVO> loaded = batchLoader.apply(missingIds);
            if (loaded != null) {
                for (InternalProductSummaryVO summary : loaded) {
                    if (summary != null && summary.getId() != null) {
                        cacheSupport.write(summaryKey(summary.getId()), summary);
                    }
                }
            }
        }
        Map<Long, InternalProductSummaryVO> summaryMap = dedup.stream()
            .filter(Objects::nonNull)
            .map(id -> cacheSupport.<InternalProductSummaryVO>read(summaryKey(id)))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalProductSummaryVO::getId, Function.identity(), (a, b) -> a));
        return ids.stream()
            .filter(Objects::nonNull)
            .map(summaryMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public void evict(Long productId) {
        if (productId == null) {
            return;
        }
        cacheSupport.delete(detailKey(productId));
        cacheSupport.delete(summaryKey(productId));
    }

    private String detailKey(Long productId) {
        return cacheSupport.buildKey(DOMAIN, PRODUCT, DETAIL, String.valueOf(productId));
    }

    private String summaryKey(Long productId) {
        return cacheSupport.buildKey(DOMAIN, PRODUCT, SUMMARY, String.valueOf(productId));
    }
}
