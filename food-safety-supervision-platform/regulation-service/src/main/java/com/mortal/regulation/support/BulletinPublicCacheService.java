package com.mortal.regulation.support;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.vo.BulletinDetailVO;
import com.mortal.regulation.vo.BulletinVO;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class BulletinPublicCacheService {

    private static final String DOMAIN = "public";
    private static final String BULLETIN = "bulletin";
    private static final String LIST = "list";
    private static final String DETAIL = "detail";
    private static final String VERSION = "ver";

    private final RegulationPublicCacheSupport cacheSupport;

    public BulletinPublicCacheService(RegulationPublicCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public PageResult<BulletinVO> getList(String queryHash, Supplier<PageResult<BulletinVO>> loader) {
        return cacheSupport.getOrLoadList(listKey(queryHash), loader);
    }

    public BulletinDetailVO getDetail(Long bulletinId, Supplier<BulletinDetailVO> loader) {
        return cacheSupport.getOrLoadDetail(detailKey(bulletinId), loader);
    }

    public void evict(Long bulletinId) {
        if (bulletinId != null) {
            cacheSupport.delete(detailKey(bulletinId));
        }
        cacheSupport.bumpVersion(listVersionKey());
    }

    private String listKey(String queryHash) {
        long version = cacheSupport.getVersion(listVersionKey());
        return cacheSupport.buildKey(DOMAIN, BULLETIN, LIST, "v" + version, queryHash);
    }

    private String detailKey(Long bulletinId) {
        return cacheSupport.buildKey(DOMAIN, BULLETIN, DETAIL, String.valueOf(bulletinId));
    }

    private String listVersionKey() {
        return cacheSupport.buildKey(DOMAIN, BULLETIN, LIST, VERSION);
    }
}
