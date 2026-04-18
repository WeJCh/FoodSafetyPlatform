package com.mortal.regulation.operation.support;

import com.mortal.platform.common.PageResult;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class SamplingPublicCacheService {

    private static final String DOMAIN = "public";
    private static final String SAMPLING = "sampling";
    private static final String LIST = "list";
    private static final String DETAIL = "detail";
    private static final String VERSION = "ver";

    private final OperationPublicCacheSupport cacheSupport;

    public SamplingPublicCacheService(OperationPublicCacheSupport cacheSupport) {
        this.cacheSupport = cacheSupport;
    }

    public PageResult<SamplingResultVO> getList(String queryHash, Supplier<PageResult<SamplingResultVO>> loader) {
        return cacheSupport.getOrLoadList(listKey(queryHash), loader);
    }

    public SamplingResultVO getDetail(Long resultId, Supplier<SamplingResultVO> loader) {
        return cacheSupport.getOrLoadDetail(detailKey(resultId), loader);
    }

    public void evict(Long resultId) {
        if (resultId != null) {
            cacheSupport.delete(detailKey(resultId));
        }
        cacheSupport.bumpVersion(listVersionKey());
    }

    private String listKey(String queryHash) {
        long version = cacheSupport.getVersion(listVersionKey());
        return cacheSupport.buildKey(DOMAIN, SAMPLING, LIST, "v" + version, queryHash);
    }

    private String detailKey(Long resultId) {
        return cacheSupport.buildKey(DOMAIN, SAMPLING, DETAIL, String.valueOf(resultId));
    }

    private String listVersionKey() {
        return cacheSupport.buildKey(DOMAIN, SAMPLING, LIST, VERSION);
    }
}
