package com.mortal.query.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mortal.platform.common.ApiResponse;
import com.mortal.query.client.ComplaintStatsClient;
import com.mortal.query.client.RegulationEnterpriseStatsClient;
import com.mortal.query.client.RegulationOperationStatsClient;
import com.mortal.query.client.RegulationRegionClient;
import com.mortal.query.client.RegulatorProfileClient;
import com.mortal.query.client.WarningStatsClient;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.ComplaintStatsOverviewVO;
import com.mortal.query.vo.EnterpriseStatsOverviewVO;
import com.mortal.query.vo.OperationStatsOverviewVO;
import com.mortal.query.vo.RegulatorProfileVO;
import com.mortal.query.vo.RegionVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "regulation.internal.token=test-regulation-token",
        "regulation-operation.internal.token=test-operation-token",
        "complaint.internal.token=test-complaint-token",
        "warning.internal.token=test-warning-token",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.sentinel.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    }
)
@AutoConfigureMockMvc
class SupervisionOverviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegulationEnterpriseStatsClient regulationEnterpriseStatsClient;

    @MockBean
    private RegulationOperationStatsClient regulationOperationStatsClient;

    @MockBean
    private ComplaintStatsClient complaintStatsClient;

    @MockBean
    private WarningStatsClient warningStatsClient;

    @MockBean
    private RegulatorProfileClient regulatorProfileClient;

    @MockBean
    private RegulationRegionClient regulationRegionClient;

    @Test
    void overview_shouldAggregateScopedStats() throws Exception {
        mockAdminProfile(List.of(330100L, 330101L));

        ArgumentCaptor<WarningStatsQueryDTO> enterpriseQueryCaptor = ArgumentCaptor.forClass(WarningStatsQueryDTO.class);
        ArgumentCaptor<String> enterpriseTokenCaptor = ArgumentCaptor.forClass(String.class);
        when(regulationEnterpriseStatsClient.fetchOverview(enterpriseQueryCaptor.capture(), enterpriseTokenCaptor.capture()))
            .thenReturn(ApiResponse.success(enterpriseOverview(18L, 4L, 12L)));
        when(regulationOperationStatsClient.fetchOverview(any(), eq("test-operation-token")))
            .thenReturn(ApiResponse.success(operationOverview(26L, 3L, 11L, 2L)));
        when(complaintStatsClient.fetchOverview(any(), eq("test-complaint-token")))
            .thenReturn(ApiResponse.success(complaintOverview(9L, 6L, 1L)));
        when(warningStatsClient.fetchOverview(any(), eq("test-warning-token")))
            .thenReturn(ApiResponse.success(warningOverview(5L)));

        mockMvc.perform(
                get("/api/query/supervision/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.enterpriseTotalCount").value(18))
            .andExpect(jsonPath("$.data.keyEnterpriseCount").value(4))
            .andExpect(jsonPath("$.data.approvedEnterpriseCount").value(12))
            .andExpect(jsonPath("$.data.inspectionTotalCount").value(26))
            .andExpect(jsonPath("$.data.inspectionFailCount").value(3))
            .andExpect(jsonPath("$.data.samplingTotalCount").value(11))
            .andExpect(jsonPath("$.data.samplingFailCount").value(2))
            .andExpect(jsonPath("$.data.complaintTotalCount").value(9))
            .andExpect(jsonPath("$.data.complaintFeedbackedCount").value(6))
            .andExpect(jsonPath("$.data.complaintOverdueCount").value(1))
            .andExpect(jsonPath("$.data.openWarningCount").value(5));

        assertEquals("test-regulation-token", enterpriseTokenCaptor.getValue());
        assertEquals("330100,330101", enterpriseQueryCaptor.getValue().getRegionIds());
    }

    @Test
    void overview_shouldExpandAdminChildren() throws Exception {
        mockAdminProfile(List.of(330100L));
        when(regulationRegionClient.listRegions(anyString(), eq(330100L)))
            .thenReturn(ApiResponse.success(List.of(region(330101L, 330100L))));
        when(regulationRegionClient.listRegions(anyString(), eq(330101L)))
            .thenReturn(ApiResponse.success(List.of()));

        ArgumentCaptor<WarningStatsQueryDTO> enterpriseQueryCaptor = ArgumentCaptor.forClass(WarningStatsQueryDTO.class);
        when(regulationEnterpriseStatsClient.fetchOverview(enterpriseQueryCaptor.capture(), anyString()))
            .thenReturn(ApiResponse.success(enterpriseOverview(1L, 0L, 1L)));
        when(regulationOperationStatsClient.fetchOverview(any(), anyString()))
            .thenReturn(ApiResponse.success(operationOverview(0L, 0L, 0L, 0L)));
        when(complaintStatsClient.fetchOverview(any(), anyString()))
            .thenReturn(ApiResponse.success(complaintOverview(0L, 0L, 0L)));
        when(warningStatsClient.fetchOverview(any(), anyString()))
            .thenReturn(ApiResponse.success(warningOverview(0L)));

        mockMvc.perform(
                get("/api/query/supervision/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        assertEquals("330100,330101", enterpriseQueryCaptor.getValue().getRegionIds());
    }

    private void mockAdminProfile(List<Long> regionIds) {
        RegulatorProfileVO profile = new RegulatorProfileVO();
        profile.setId(12L);
        profile.setUserId(18L);
        profile.setRoleType("REGULATOR_ADMIN");
        profile.setRegionIds(regionIds);
        when(regulatorProfileClient.getMyProfile(anyString())).thenReturn(ApiResponse.success(profile));
        for (Long regionId : regionIds) {
            when(regulationRegionClient.listRegions(anyString(), eq(regionId))).thenReturn(ApiResponse.success(List.of()));
        }
    }

    private EnterpriseStatsOverviewVO enterpriseOverview(Long totalCount, Long keyCount, Long approvedCount) {
        EnterpriseStatsOverviewVO overview = new EnterpriseStatsOverviewVO();
        overview.setTotalCount(totalCount);
        overview.setKeyEnterpriseCount(keyCount);
        overview.setApprovedEnterpriseCount(approvedCount);
        return overview;
    }

    private OperationStatsOverviewVO operationOverview(Long inspectionTotal,
                                                       Long inspectionFail,
                                                       Long samplingTotal,
                                                       Long samplingFail) {
        OperationStatsOverviewVO overview = new OperationStatsOverviewVO();
        overview.setInspectionTotalCount(inspectionTotal);
        overview.setInspectionFailCount(inspectionFail);
        overview.setSamplingTotalCount(samplingTotal);
        overview.setSamplingFailCount(samplingFail);
        return overview;
    }

    private ComplaintStatsOverviewVO complaintOverview(Long totalCount, Long feedbackedCount, Long overdueCount) {
        ComplaintStatsOverviewVO overview = new ComplaintStatsOverviewVO();
        overview.setTotalCount(totalCount);
        overview.setFeedbackedCount(feedbackedCount);
        overview.setOverdueCount(overdueCount);
        return overview;
    }

    private WarningStatsOverviewVO warningOverview(Long openCount) {
        WarningStatsOverviewVO overview = new WarningStatsOverviewVO();
        overview.setOpenCount(openCount);
        return overview;
    }

    private RegionVO region(Long id, Long parentId) {
        RegionVO region = new RegionVO();
        region.setId(id);
        region.setParentId(parentId);
        return region;
    }
}
