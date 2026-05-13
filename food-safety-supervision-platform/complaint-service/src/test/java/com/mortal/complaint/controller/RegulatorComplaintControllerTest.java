package com.mortal.complaint.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.complaint.application.ComplaintCommandService;
import com.mortal.complaint.application.ComplaintQueryService;
import com.mortal.complaint.application.ComplaintStatsService;
import com.mortal.complaint.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.complaint.dto.InternalStatsQueryDTO;
import com.mortal.complaint.support.RequestIdentityResolver;
import com.mortal.complaint.support.RequestIdentityResolver.RequestIdentity;
import com.mortal.complaint.vo.InternalComplaintStatsOverviewVO;
import com.mortal.platform.common.ApiResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegulatorComplaintControllerTest {

    @Test
    void statsOverview_shouldUseRegionScopeForAdmin() {
        ComplaintStatsService complaintStatsService = mock(ComplaintStatsService.class);
        ComplaintQueryService complaintQueryService = mock(ComplaintQueryService.class);
        RequestIdentityResolver requestIdentityResolver = mock(RequestIdentityResolver.class);
        RegulatorComplaintController controller = new RegulatorComplaintController(
            mock(ComplaintCommandService.class),
            complaintQueryService,
            complaintStatsService,
            requestIdentityResolver
        );

        when(requestIdentityResolver.resolve("101", "REGULATOR", "REGULATOR_ADMIN"))
            .thenReturn(new RequestIdentity(101L, "REGULATOR", Set.of("REGULATOR_ADMIN")));
        InternalRegulatorIdentityVO regulator = new InternalRegulatorIdentityVO();
        regulator.setId(9L);
        regulator.setUserId(101L);
        regulator.setRoleType("REGULATOR_ADMIN");
        regulator.setRegionIds(List.of(1L, 4L, 3L));
        when(complaintQueryService.requireRegulator(101L)).thenReturn(regulator);

        InternalComplaintStatsOverviewVO overview = new InternalComplaintStatsOverviewVO();
        overview.setTotalCount(12L);
        when(complaintStatsService.getOverview(any())).thenReturn(overview);

        ApiResponse<InternalComplaintStatsOverviewVO> response =
            controller.statsOverview("101", "REGULATOR", "REGULATOR_ADMIN");

        ArgumentCaptor<InternalStatsQueryDTO> captor = ArgumentCaptor.forClass(InternalStatsQueryDTO.class);
        verify(complaintStatsService).getOverview(captor.capture());
        assertEquals(9L, captor.getValue().getScopeRegulatorId());
        assertNull(captor.getValue().getOwnerRegulatorId());
        assertEquals(0, response.getCode());
        assertEquals(12L, response.getData().getTotalCount());
    }

    @Test
    void statsOverview_shouldUseOwnerScopeForEnforcer() {
        ComplaintStatsService complaintStatsService = mock(ComplaintStatsService.class);
        ComplaintQueryService complaintQueryService = mock(ComplaintQueryService.class);
        RequestIdentityResolver requestIdentityResolver = mock(RequestIdentityResolver.class);
        RegulatorComplaintController controller = new RegulatorComplaintController(
            mock(ComplaintCommandService.class),
            complaintQueryService,
            complaintStatsService,
            requestIdentityResolver
        );

        when(requestIdentityResolver.resolve("202", "REGULATOR", "REGULATOR_ENFORCER"))
            .thenReturn(new RequestIdentity(202L, "REGULATOR", Set.of("REGULATOR_ENFORCER")));
        InternalRegulatorIdentityVO regulator = new InternalRegulatorIdentityVO();
        regulator.setId(6L);
        regulator.setUserId(202L);
        regulator.setRoleType("REGULATOR_ENFORCER");
        regulator.setRegionIds(List.of(8L));
        when(complaintQueryService.requireRegulator(202L)).thenReturn(regulator);

        when(complaintStatsService.getOverview(any())).thenReturn(new InternalComplaintStatsOverviewVO());

        controller.statsOverview("202", "REGULATOR", "REGULATOR_ENFORCER");

        ArgumentCaptor<InternalStatsQueryDTO> captor = ArgumentCaptor.forClass(InternalStatsQueryDTO.class);
        verify(complaintStatsService).getOverview(captor.capture());
        assertEquals(6L, captor.getValue().getOwnerRegulatorId());
        assertNull(captor.getValue().getRegionIds());
        assertNull(captor.getValue().getScopeRegulatorId());
    }
}
