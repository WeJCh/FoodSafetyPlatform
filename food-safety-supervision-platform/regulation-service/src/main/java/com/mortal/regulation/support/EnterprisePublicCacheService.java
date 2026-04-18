package com.mortal.regulation.support;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.vo.PublicEnterpriseDetailVO;
import com.mortal.regulation.vo.PublicEnterpriseVO;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class EnterprisePublicCacheService {

    private static final String DOMAIN = "public";
    private static final String ENTERPRISE = "enterprise";
    private static final String LIST = "list";
    private static final String DETAIL = "detail";
    private static final String VERSION = "ver";

    private final RegulationPublicCacheSupport cacheSupport;

    public EnterprisePublicCacheService(RegulationPublicCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public PageResult<PublicEnterpriseVO> getList(String queryHash, Supplier<PageResult<PublicEnterpriseVO>> loader) {
        return cacheSupport.getOrLoadList(listKey(queryHash), loader);
    }

    public PublicEnterpriseDetailVO getDetail(Long enterpriseId, Supplier<PublicEnterpriseDetailVO> loader) {
        return cacheSupport.getOrLoadDetail(detailKey(enterpriseId), loader);
    }

    public void evict(Long enterpriseId) {
        if (enterpriseId != null) {
            cacheSupport.delete(detailKey(enterpriseId));
        }
        cacheSupport.bumpVersion(listVersionKey());
    }

    private String listKey(String queryHash) {
        long version = cacheSupport.getVersion(listVersionKey());
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, LIST, "v" + version, queryHash);
    }

    private String detailKey(Long enterpriseId) {
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, DETAIL, String.valueOf(enterpriseId));
    }

    private String listVersionKey() {
        return cacheSupport.buildKey(DOMAIN, ENTERPRISE, LIST, VERSION);
    }
}
