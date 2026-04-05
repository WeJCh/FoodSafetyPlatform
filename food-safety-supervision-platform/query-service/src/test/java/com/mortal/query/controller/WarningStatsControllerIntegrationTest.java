package com.mortal.query.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mortal.query.client.RegulatorProfileClient;
import com.mortal.query.client.RegulationRegionClient;
import com.mortal.query.client.WarningStatsClient;
import com.mortal.platform.common.ApiResponse;
import com.mortal.query.dto.WarningStatsQueryDTO;
import com.mortal.query.vo.RegulatorProfileVO;
import com.mortal.query.vo.RegionVO;
import com.mortal.query.vo.WarningStatsOverviewVO;
import com.mortal.query.vo.WarningTrendPointVO;
import java.time.LocalDateTime;
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
        "warning.internal.token=test-internal-token",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.sentinel.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    }
)
@AutoConfigureMockMvc
class WarningStatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarningStatsClient warningStatsClient;

    @MockBean
    private RegulatorProfileClient regulatorProfileClient;

    @MockBean
    private RegulationRegionClient regulationRegionClient;

    @Test
    void overview_shouldPassInternalTokenAndScopedParams() throws Exception {
        mockAdminProfile(List.of(1001L, 1002L));

        ArgumentCaptor<WarningStatsQueryDTO> queryCaptor = ArgumentCaptor.forClass(WarningStatsQueryDTO.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        WarningStatsOverviewVO overview = new WarningStatsOverviewVO();
        overview.setTotalCount(3L);
        when(warningStatsClient.fetchOverview(queryCaptor.capture(), tokenCaptor.capture()))
            .thenReturn(ApiResponse.success(overview));

        mockMvc.perform(
                get("/api/query/warnings/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
                    .param("startTime", "2026-03-10T00:00:00")
                    .param("endTime", "2026-03-13T23:59:59")
                    .param("warningType", "SLA_OVERDUE_SUBMIT")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.totalCount").value(3));

        WarningStatsQueryDTO forwarded = queryCaptor.getValue();
        assertEquals("test-internal-token", tokenCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 3, 10, 0, 0), forwarded.getStartTime());
        assertEquals(LocalDateTime.of(2026, 3, 13, 23, 59, 59), forwarded.getEndTime());
        assertEquals("SLA_OVERDUE_SUBMIT", forwarded.getWarningType());
        assertEquals("1001,1002", forwarded.getRegionIds());
    }

    @Test
    void overview_shouldExpandAdminRegionScopeWithChildren() throws Exception {
        mockAdminProfile(List.of(1001L));
        when(regulationRegionClient.listRegions(anyString(), eq(1001L)))
            .thenReturn(ApiResponse.success(List.of(region(100101L, 1001L))));
        when(regulationRegionClient.listRegions(anyString(), eq(100101L)))
            .thenReturn(ApiResponse.success(List.of()));

        ArgumentCaptor<WarningStatsQueryDTO> queryCaptor = ArgumentCaptor.forClass(WarningStatsQueryDTO.class);
        when(warningStatsClient.fetchOverview(queryCaptor.capture(), anyString()))
            .thenReturn(ApiResponse.success(new WarningStatsOverviewVO()));

        mockMvc.perform(
                get("/api/query/warnings/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        assertEquals("1001,100101", queryCaptor.getValue().getRegionIds());
    }

    @Test
    void overview_shouldReturn400WhenWarningRejectsToken() throws Exception {
        mockAdminProfile(List.of(1001L));
        when(warningStatsClient.fetchOverview(any(), anyString()))
            .thenReturn(ApiResponse.failure(403, "forbidden"));

        mockMvc.perform(
                get("/api/query/warnings/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("forbidden"));
    }

    @Test
    void trend_shouldPassThroughRequestParams() throws Exception {
        mockAdminProfile(List.of(1001L, 1002L));

        ArgumentCaptor<WarningStatsQueryDTO> queryCaptor = ArgumentCaptor.forClass(WarningStatsQueryDTO.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        when(warningStatsClient.fetchTrend(queryCaptor.capture(), tokenCaptor.capture()))
            .thenReturn(ApiResponse.success(List.of(new WarningTrendPointVO())));

        mockMvc.perform(
                get("/api/query/warnings/trend")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
                    .param("regionId", "1001")
                    .param("bizType", "RECTIFICATION")
                    .param("status", "OPEN")
                    .param("trendDays", "14")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        WarningStatsQueryDTO forwarded = queryCaptor.getValue();
        assertEquals("test-internal-token", tokenCaptor.getValue());
        assertEquals(1001L, forwarded.getRegionId());
        assertNull(forwarded.getRegionIds());
        assertEquals("RECTIFICATION", forwarded.getBizType());
        assertEquals("OPEN", forwarded.getStatus());
        assertEquals(14, forwarded.getTrendDays());
    }

    @Test
    void overview_shouldReturn400WhenStartTimeAfterEndTime() throws Exception {
        mockAdminProfile(List.of(1001L));

        mockMvc.perform(
                get("/api/query/warnings/overview")
                    .header("X-User-Id", "18")
                    .header("X-User-Type", "REGULATOR_ADMIN")
                    .header("Authorization", "Bearer test-token")
                    .param("startTime", "2026-03-14T00:00:00")
                    .param("endTime", "2026-03-13T00:00:00")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("startTime must be before endTime"));

        verifyNoInteractions(warningStatsClient);
    }

    private void mockAdminProfile(List<Long> regionIds) {
        RegulatorProfileVO profile = new RegulatorProfileVO();
        profile.setId(12L);
        profile.setUserId(18L);
        profile.setRoleType("REGULATOR_ADMIN");
        profile.setRegionIds(regionIds);
        when(regulatorProfileClient.getMyProfile(anyString())).thenReturn(ApiResponse.success(profile));
        // 默认不返回下级辖区，便于当前用例验证参数透传。
        for (Long regionId : regionIds) {
            when(regulationRegionClient.listRegions(anyString(), eq(regionId))).thenReturn(ApiResponse.success(List.of()));
        }
    }

    private RegionVO region(Long id, Long parentId) {
        RegionVO region = new RegionVO();
        region.setId(id);
        region.setParentId(parentId);
        return region;
    }
}
