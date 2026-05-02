package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.RegulatorRegionAdjustDTO;
import com.mortal.regulation.service.RegulatorProfileService;
import com.mortal.regulation.vo.AuditLogVO;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.RegulatorProfileVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegulatorProfileControllerTest {

    @Test
    void listEligible_shouldUseCurrentAdminScope() {
        RegulatorProfileService regulatorProfileService = mock(RegulatorProfileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        RegulatorProfileController controller = new RegulatorProfileController(regulatorProfileService, jwtUserResolver);

        RegulatorProfileVO currentProfile = new RegulatorProfileVO();
        currentProfile.setUserId(2L);
        currentProfile.setRoleType("REGULATOR_ADMIN");

        RegulatorProfileVO enforcer = new RegulatorProfileVO();
        enforcer.setId(4L);
        enforcer.setRoleType("REGULATOR_ENFORCER");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(2L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("REGULATOR");
        when(regulatorProfileService.getByUserId(2L)).thenReturn(currentProfile);
        when(regulatorProfileService.listEligibleEnforcers(2L, 9L)).thenReturn(List.of(enforcer));

        ApiResponse<List<RegulatorProfileVO>> response = controller.listEligible("Bearer token", 9L);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals(4L, response.getData().get(0).getId());
        verify(regulatorProfileService).listEligibleEnforcers(2L, 9L);
    }

    @Test
    void listEligible_shouldRejectEnforcerRole() {
        RegulatorProfileService regulatorProfileService = mock(RegulatorProfileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        RegulatorProfileController controller = new RegulatorProfileController(regulatorProfileService, jwtUserResolver);

        RegulatorProfileVO currentProfile = new RegulatorProfileVO();
        currentProfile.setUserId(5L);
        currentProfile.setRoleType("REGULATOR_ENFORCER");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(5L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("REGULATOR");
        when(regulatorProfileService.getByUserId(5L)).thenReturn(currentProfile);

        ApiResponse<List<RegulatorProfileVO>> response = controller.listEligible("Bearer token", 9L);

        assertEquals(403, response.getCode());
        assertEquals("regulator admin only", response.getMessage());
    }

    @Test
    void adjustRegions_shouldAllowAdmin() {
        RegulatorProfileService regulatorProfileService = mock(RegulatorProfileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        RegulatorProfileController controller = new RegulatorProfileController(regulatorProfileService, jwtUserResolver);

        RegulatorProfileVO profile = new RegulatorProfileVO();
        profile.setId(8L);
        profile.setRegionIds(List.of(9L));

        RegulatorRegionAdjustDTO dto = new RegulatorRegionAdjustDTO();
        dto.setRegionIds(List.of(9L));
        dto.setRemark("move");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(1L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("ADMIN");
        when(jwtUserResolver.resolveUsername("Bearer token")).thenReturn("admin");
        when(regulatorProfileService.adjustRegions(1L, "admin", 8L, List.of(9L), "move")).thenReturn(profile);

        ApiResponse<RegulatorProfileVO> response = controller.adjustRegions("Bearer token", 8L, dto);

        assertEquals(0, response.getCode());
        assertEquals(8L, response.getData().getId());
        verify(regulatorProfileService).adjustRegions(1L, "admin", 8L, List.of(9L), "move");
    }

    @Test
    void listAuditLogs_shouldAllowAdmin() {
        RegulatorProfileService regulatorProfileService = mock(RegulatorProfileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        RegulatorProfileController controller = new RegulatorProfileController(regulatorProfileService, jwtUserResolver);

        AuditLogVO log = new AuditLogVO();
        log.setId(100L);
        log.setActionType("REGULATOR_REGION_ADJUST");

        when(jwtUserResolver.resolveUserId("Bearer token")).thenReturn(1L);
        when(jwtUserResolver.resolveUserType("Bearer token")).thenReturn("ADMIN");
        when(jwtUserResolver.resolveUsername("Bearer token")).thenReturn("admin");
        when(regulatorProfileService.listAuditLogs(8L, 5)).thenReturn(List.of(log));

        ApiResponse<List<AuditLogVO>> response = controller.listAuditLogs("Bearer token", 8L, 5);

        assertEquals(0, response.getCode());
        assertEquals(100L, response.getData().get(0).getId());
        verify(regulatorProfileService).listAuditLogs(8L, 5);
    }
}
