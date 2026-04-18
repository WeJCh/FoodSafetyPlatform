package com.mortal.regulation.support;

import com.mortal.regulation.vo.internal.InternalEnterpriseDetailVO;
import com.mortal.regulation.vo.internal.InternalEnterpriseSummaryVO;
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
public class EnterpriseMasterCacheService {

    private static final String DOMAIN = "reg";
    private static final String ENTERPRISE = "enterprise";
    private static final String DETAIL = "detail";
    private static final String BY_USER = "by-user";
    private static final String SUMMARY = "summary";

    private final RegulationMasterCacheSupport cacheSupport;

    public EnterpriseMasterCacheService(RegulationMasterCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public InternalEnterpriseDetailVO getDetail(Long enterpriseId, Supplier<InternalEnterpriseDetailVO> loader) {
        return cacheSupport.getOrLoad(detailKey(enterpriseId), loader);
    }

    public InternalEnterpriseDetailVO getByUser(Long userId, Supplier<InternalEnterpriseDetailVO> loader) {
        return cacheSupport.getOrLoad(byUserKey(userId), loader);
    }

    public List<InternalEnterpriseSummaryVO> getSummaries(List<Long> ids,
                                                          Function<List<Long>, List<InternalEnterpriseSummaryVO>> batchLoader) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> dedup = new LinkedHashSet<>(ids);
        List<Long> missingIds = new ArrayList<>();
        for (Long id : dedup) {
            if (id != null && cacheSupport.<InternalEnterpriseSummaryVO>read(summaryKey(id)) == null) {
                missingIds.add(id);
            }
        }
        if (!missingIds.isEmpty()) {
            List<InternalEnterpriseSummaryVO> loaded = batchLoader.apply(missingIds);
            if (loaded != null) {
                for (InternalEnterpriseSummaryVO summary : loaded) {
                    if (summary != null && summary.getId() != null) {
                        cacheSupport.write(summaryKey(summary.getId()), summary);
                    }
                }
            }
        }
        Map<Long, InternalEnterpriseSummaryVO> summaryMap = dedup.stream()
            .filter(Objects::nonNull)
            .map(id -> cacheSupport.<InternalEnterpriseSummaryVO>read(summaryKey(id)))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalEnterpriseSummaryVO::getId, Function.identity(), (a, b) -> a));
        return ids.stream()
            .filter(Objects::nonNull)
            .map(summaryMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public void evict(Long enterpriseId, Long userId) {
        if (enterpriseId != null) {
            cacheSupport.delete(detailKey(enterpriseId));
            cacheSupport.delete(summaryKey(enterpriseId));
        }
        if (userId != null) {
            cacheSupport.delete(byUserKey(userId));
        }
    }

    private String detailKey(Long enterpriseId) {
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, DETAIL, String.valueOf(enterpriseId));
    }

    private String byUserKey(Long userId) {
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, BY_USER, String.valueOf(userId));
    }

    private String summaryKey(Long enterpriseId) {
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, SUMMARY, String.valueOf(enterpriseId));
    }
}
