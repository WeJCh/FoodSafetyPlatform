package com.mortal.complaint.client.regulation;

import com.mortal.complaint.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.complaint.client.regulation.vo.InternalEnterpriseSummaryVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorSummaryVO;
import com.mortal.platform.common.ApiResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 内部监管系统客户端
 */
@FeignClient("regulation-service")
public interface RegulationInternalClient {

    /**
     * 获取企业详情
     * @param id 企业ID
     * @param internalToken 内部令牌
     * @return 企业详情
     */
    @GetMapping("/api/internal/regulation/enterprises/{id}")
    ApiResponse<InternalEnterpriseDetailVO> getEnterpriseById(@PathVariable("id") Long id,
                                                              @RequestHeader("X-Internal-Token")
                                                              String internalToken);

    /**
     * 获取企业摘要列表
     * @param ids 企业ID列表
     * @param internalToken 内部令牌
     * @return 企业摘要列表
     */
    @PostMapping("/api/internal/regulation/enterprises/summaries")
    ApiResponse<List<InternalEnterpriseSummaryVO>> getEnterpriseSummaries(@RequestBody List<Long> ids,
                                                                          @RequestHeader("X-Internal-Token")
                                                                          String internalToken);

    /**
     * 根据企业名称查询企业ID列表
     * @param keyword 企业名称关键字
     * @param internalToken 内部令牌
     * @return 企业ID列表
     */
    @GetMapping("/api/internal/regulation/enterprises/query-ids-by-name")
    ApiResponse<List<Long>> queryEnterpriseIdsByName(@RequestParam("keyword") String keyword,
                                                     @RequestHeader("X-Internal-Token")
                                                     String internalToken);

    /**
     * 根据用户ID获取监管者身份
     * @param userId 用户ID
     * @param internalToken 内部令牌
     * @return 监管者身份
     */
    @GetMapping("/api/internal/regulation/regulators/by-user/{userId}")
    ApiResponse<InternalRegulatorIdentityVO> getRegulatorByUserId(@PathVariable("userId") Long userId,
                                                                  @RequestHeader("X-Internal-Token")
                                                                  String internalToken);

    /**
     * 获取监管者摘要
     * @param id 监管者ID
     * @param internalToken 内部令牌
     * @return 监管者摘要
     */
    @GetMapping("/api/internal/regulation/regulators/{id}")
    ApiResponse<InternalRegulatorSummaryVO> getRegulatorById(@PathVariable("id") Long id,
                                                             @RequestHeader("X-Internal-Token")
                                                             String internalToken);

    /**
     * 获取监管者摘要列表
     * @param ids 监管者ID列表
     * @param internalToken 内部令牌
     * @return 监管者摘要列表
     */
    @PostMapping("/api/internal/regulation/regulators/summaries")
    ApiResponse<List<InternalRegulatorSummaryVO>> getRegulatorSummaries(@RequestBody List<Long> ids,
                                                                        @RequestHeader("X-Internal-Token")
                                                                        String internalToken);

    /**
     * 根据监管者名称查询监管者ID列表
     * @param keyword 监管者名称关键字
     * @param internalToken 内部令牌
     * @return 监管者ID列表
     */
    @GetMapping("/api/internal/regulation/regulators/query-ids-by-name")
    ApiResponse<List<Long>> queryRegulatorIdsByName(@RequestParam("keyword") String keyword,
                                                    @RequestHeader("X-Internal-Token")
                                                    String internalToken);

    /**
     * 获取监管者管辖的企业ID列表
     * @param id 监管者ID
     * @param internalToken 内部令牌
     * @return 企业ID列表
     */
    @GetMapping("/api/internal/regulation/regulators/{id}/scope-enterprise-ids")
    ApiResponse<List<Long>> getScopeEnterpriseIds(@PathVariable("id") Long id,
                                                  @RequestHeader("X-Internal-Token")
                                                  String internalToken);
}

