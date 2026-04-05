package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.regulation.RegulationEnterpriseInternalClient;
import com.mortal.regulation.operation.dto.InternalStatsQueryDTO;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.entity.SamplingResult;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.mapper.SamplingResultMapper;
import com.mortal.regulation.operation.service.InternalStatsService;
import com.mortal.regulation.operation.vo.InternalOperationStatsOverviewVO;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InternalStatsServiceImpl implements InternalStatsService {

    private static final String RESULT_FAIL = "FAIL";

    private final InspectionRecordMapper inspectionRecordMapper;
    private final SamplingResultMapper samplingResultMapper;
    private final RegulationEnterpriseInternalClient regulationEnterpriseInternalClient;
    private final String regulationInternalToken;

    public InternalStatsServiceImpl(InspectionRecordMapper inspectionRecordMapper,
                                    SamplingResultMapper samplingResultMapper,
                                    RegulationEnterpriseInternalClient regulationEnterpriseInternalClient,
                                    @Value("${regulation.internal.token:regulation-internal-token}")
                                    String regulationInternalToken) {
        this.inspectionRecordMapper = inspectionRecordMapper;
        this.samplingResultMapper = samplingResultMapper;
        this.regulationEnterpriseInternalClient = regulationEnterpriseInternalClient;
        this.regulationInternalToken = regulationInternalToken;
    }

    /**
     * 获取执行域统计概览。
     * 
     * @param queryDTO 查询条件
     * @return 执行域统计概览
     */
    @Override
    public InternalOperationStatsOverviewVO getOverview(InternalStatsQueryDTO queryDTO) {
        List<Long> enterpriseIds = resolveEnterpriseIds(queryDTO);
        InternalOperationStatsOverviewVO overview = new InternalOperationStatsOverviewVO();
        overview.setInspectionTotalCount(countInspections(queryDTO, enterpriseIds, null));
        overview.setInspectionFailCount(countInspections(queryDTO, enterpriseIds, RESULT_FAIL));
        overview.setSamplingTotalCount(countSamplingResults(queryDTO, enterpriseIds, null));
        overview.setSamplingFailCount(countSamplingResults(queryDTO, enterpriseIds, RESULT_FAIL));
        return overview;
    }
    /**
     * 统计检查记录数量。
     * 
     * @param queryDTO 查询条件
     * @param enterpriseIds 企业ID列表
     * @param result 结果
     * @return 检查记录数量
     */
    private long countInspections(InternalStatsQueryDTO queryDTO, List<Long> enterpriseIds, String result) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
            .eq(InspectionRecord::getDeleted, 0);
        if (queryDTO != null && queryDTO.getOwnerRegulatorId() != null) {
            wrapper.eq(InspectionRecord::getInspectorId, queryDTO.getOwnerRegulatorId());
        } else if (enterpriseIds != null) {
            if (enterpriseIds.isEmpty()) {
                return 0L;
            }
            wrapper.in(InspectionRecord::getEnterpriseId, enterpriseIds);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(InspectionRecord::getResult, result);
        }
        return inspectionRecordMapper.selectCount(wrapper);
    }

    /**
     * 统计抽样结果数量。
     * 
     * @param queryDTO 查询条件
     * @param enterpriseIds 企业ID列表
     * @param result 结果
     * @return 抽样结果数量
     */
    private long countSamplingResults(InternalStatsQueryDTO queryDTO, List<Long> enterpriseIds, String result) {
        LambdaQueryWrapper<SamplingResult> wrapper = new LambdaQueryWrapper<SamplingResult>()
            .eq(SamplingResult::getDeleted, 0);
        if (queryDTO != null && queryDTO.getOwnerRegulatorId() != null) {
            wrapper.eq(SamplingResult::getSampledBy, queryDTO.getOwnerRegulatorId());
        } else if (enterpriseIds != null) {
            if (enterpriseIds.isEmpty()) {
                return 0L;
            }
            wrapper.in(SamplingResult::getEnterpriseId, enterpriseIds);
        }
        if (StringUtils.hasText(result)) {
            wrapper.eq(SamplingResult::getResult, result);
        }
        return samplingResultMapper.selectCount(wrapper);
    }

    /**
     * 解析企业ID列表。
     * 
     * @param queryDTO 查询条件
     * @return 企业ID列表
     */
    private List<Long> resolveEnterpriseIds(InternalStatsQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getOwnerRegulatorId() != null) {
            return null;
        }
        if (queryDTO.getRegionId() == null && !StringUtils.hasText(queryDTO.getRegionIds())) {
            return null;
        }
        ApiResponse<List<Long>> response = regulationEnterpriseInternalClient.getEnterpriseIdsByScope(
            queryDTO.getRegionId(),
            queryDTO.getRegionIds(),
            regulationInternalToken
        );
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }
}
