package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.controller.internal.InternalEnterpriseController;
import com.mortal.regulation.dto.EnterpriseKeyReasonUpsertDTO;
import com.mortal.regulation.mapper.AddrLocationMapper;
import com.mortal.regulation.mapper.FoodEnterpriseMapper;
import com.mortal.regulation.service.EnterpriseKeyReasonService;
import org.junit.jupiter.api.Test;

class InternalEnterpriseControllerTest {

    @Test
    void markAsKey_shouldDelegateToService() {
        FoodEnterpriseMapper enterpriseMapper = mock(FoodEnterpriseMapper.class);
        AddrLocationMapper addrLocationMapper = mock(AddrLocationMapper.class);
        EnterpriseKeyReasonService enterpriseKeyReasonService = mock(EnterpriseKeyReasonService.class);
        InternalEnterpriseController controller = new InternalEnterpriseController(
            enterpriseMapper,
            addrLocationMapper,
            enterpriseKeyReasonService
        );
        EnterpriseKeyReasonUpsertDTO dto = new EnterpriseKeyReasonUpsertDTO();
        dto.setReasonType("CONSECUTIVE_INSPECTION_FAIL");
        dto.setReasonDetail("企业最近2次检查均为不合格，已自动纳入重点监管");
        dto.setSourceType("ROUTINE");
        dto.setSourceId(101L);
        dto.setOperatorId(20L);

        ApiResponse<Void> response = controller.markAsKey(8L, dto);

        assertEquals(0, response.getCode());
        verify(enterpriseKeyReasonService).markEnterpriseAsKey(
            8L,
            "CONSECUTIVE_INSPECTION_FAIL",
            "企业最近2次检查均为不合格，已自动纳入重点监管",
            "ROUTINE",
            101L,
            20L
        );
    }
}
