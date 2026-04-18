package com.mortal.regulation.support;

import com.mortal.regulation.vo.internal.InternalRegulatorIdentityVO;
import com.mortal.regulation.vo.internal.InternalRegulatorSummaryVO;
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
public class RegulatorMasterCacheService {

    private static final String DOMAIN = "reg";
    private static final String REGULATOR = "regulator";
    private static final String IDENTITY = "identity";
    private static final String BY_USER = "by-user";
    private static final String SUMMARY = "summary";
    private static final String SCOPE_REGION = "scope-region";
    private static final String SCOPE_ENTERPRISE = "scope-enterprise";
    private static final String VERSION = "ver";

    private final RegulationMasterCacheSupport cacheSupport;

    public RegulatorMasterCacheService(RegulationMasterCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public InternalRegulatorIdentityVO getIdentity(Long regulatorId, Supplier<InternalRegulatorIdentityVO> loader) {
        return cacheSupport.getOrLoad(identityKey(regulatorId), loader);
    }

    public InternalRegulatorIdentityVO getByUser(Long userId, Supplier<InternalRegulatorIdentityVO> loader) {
        return cacheSupport.getOrLoad(byUserKey(userId), loader);
    }

    public InternalRegulatorSummaryVO getSummary(Long regulatorId, Supplier<InternalRegulatorSummaryVO> loader) {
        return cacheSupport.getOrLoad(summaryKey(regulatorId), loader);
    }

    public List<InternalRegulatorSummaryVO> getSummaries(List<Long> ids,
                                                         Function<List<Long>, List<InternalRegulatorSummaryVO>> batchLoader) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> dedup = new LinkedHashSet<>(ids);
        List<Long> missingIds = new ArrayList<>();
        for (Long id : dedup) {
            if (id != null && cacheSupport.<InternalRegulatorSummaryVO>read(summaryKey(id)) == null) {
                missingIds.add(id);
            }
        }
        if (!missingIds.isEmpty()) {
            List<InternalRegulatorSummaryVO> loaded = batchLoader.apply(missingIds);
            if (loaded != null) {
                for (InternalRegulatorSummaryVO summary : loaded) {
                    if (summary != null && summary.getId() != null) {
                        cacheSupport.write(summaryKey(summary.getId()), summary);
                    }
                }
            }
        }
        Map<Long, InternalRegulatorSummaryVO> summaryMap = dedup.stream()
            .filter(Objects::nonNull)
            .map(id -> cacheSupport.<InternalRegulatorSummaryVO>read(summaryKey(id)))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(InternalRegulatorSummaryVO::getId, Function.identity(), (a, b) -> a));
        return ids.stream()
            .filter(Objects::nonNull)
            .map(summaryMap::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public List<Long> getScopeRegionIds(Long regulatorId, Supplier<List<Long>> loader) {
        return cacheSupport.getOrLoad(scopeRegionKey(regulatorId), loader);
    }

    public List<Long> getScopeEnterpriseIds(Long regulatorId, Supplier<List<Long>> loader) {
        return cacheSupport.getOrLoad(scopeEnterpriseKey(regulatorId), loader);
    }

    public void evict(Long regulatorId, Long userId) {
        if (regulatorId != null) {
            cacheSupport.delete(identityKey(regulatorId));
            cacheSupport.delete(summaryKey(regulatorId));
            cacheSupport.delete(scopeRegionKey(regulatorId));
            cacheSupport.delete(scopeEnterpriseKey(regulatorId));
        }
        if (userId != null) {
            cacheSupport.delete(byUserKey(userId));
        }
    }

    public void bumpScopeEnterpriseVersion() {
        cacheSupport.increment(scopeEnterpriseVersionKey());
    }

    private String identityKey(Long regulatorId) {
        return cacheSupport.buildKey(DOMAIN, REGULATOR, IDENTITY, String.valueOf(regulatorId));
    }

    private String byUserKey(Long userId) {
        return cacheSupport.buildKey(DOMAIN, REGULATOR, BY_USER, String.valueOf(userId));
    }

    private String summaryKey(Long regulatorId) {
        return cacheSupport.buildKey(DOMAIN, REGULATOR, SUMMARY, String.valueOf(regulatorId));
    }

    private String scopeRegionKey(Long regulatorId) {
        return cacheSupport.buildKey(DOMAIN, REGULATOR, SCOPE_REGION, String.valueOf(regulatorId));
    }

    private String scopeEnterpriseKey(Long regulatorId) {
        long version = cacheSupport.getLong(scopeEnterpriseVersionKey(), 1L);
        return cacheSupport.buildKey(DOMAIN, REGULATOR, SCOPE_ENTERPRISE, "v" + version, String.valueOf(regulatorId));
    }

    private String scopeEnterpriseVersionKey() {
        return cacheSupport.buildKey(DOMAIN, REGULATOR, SCOPE_ENTERPRISE, VERSION);
    }
}
