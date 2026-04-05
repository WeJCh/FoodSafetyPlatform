package com.mortal.regulation.operation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.dto.InspectionItemDTO;
import com.mortal.regulation.operation.dto.InspectionSubmitDTO;
import com.mortal.regulation.operation.entity.InspectionRecord;
import com.mortal.regulation.operation.entity.InspectionTask;
import com.mortal.regulation.operation.mapper.InspectionItemMapper;
import com.mortal.regulation.operation.mapper.InspectionRecordMapper;
import com.mortal.regulation.operation.mapper.InspectionTaskMapper;
import com.mortal.regulation.operation.service.impl.InspectionTaskServiceImpl;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.vo.InspectionTaskVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InspectionTaskServiceImplTest {

    @Test
    void submitTask_shouldMarkEnterpriseAsKeyAndSendWarningWhenConsecutiveFailTriggered() {
        InspectionTaskMapper inspectionTaskMapper = mock(InspectionTaskMapper.class);
        InspectionRecordMapper inspectionRecordMapper = mock(InspectionRecordMapper.class);
        InspectionItemMapper inspectionItemMapper = mock(InspectionItemMapper.class);
        OperationMasterDataSupport masterDataSupport = mock(OperationMasterDataSupport.class);
        RectificationService rectificationService = mock(RectificationService.class);
        WarningEventOutboxService warningEventOutboxService = mock(WarningEventOutboxService.class);
        InspectionTaskServiceImpl service = new InspectionTaskServiceImpl(
            inspectionTaskMapper,
            inspectionRecordMapper,
            inspectionItemMapper,
            masterDataSupport,
            rectificationService,
            warningEventOutboxService
        );

        InternalRegulatorIdentityVO enforcer = new InternalRegulatorIdentityVO();
        enforcer.setId(20L);
        InspectionTask task = new InspectionTask();
        task.setId(100L);
        task.setEnterpriseId(300L);
        task.setRegionId(400L);
        task.setAssignedTo(20L);
        task.setAssignedBy(11L);
        task.setCreatedBy(11L);
        task.setTaskNo("TSK001");
        task.setTaskTitle("现场检查");
        task.setPriority("HIGH");
        task.setStatus("IN_PROGRESS");
        task.setDeadline(LocalDateTime.now().plusDays(1));

        when(masterDataSupport.requireEnforcer(7L)).thenReturn(enforcer);
        when(inspectionTaskMapper.selectById(100L)).thenReturn(task);
        when(masterDataSupport.loadEnterpriseNames(any())).thenReturn(Map.of(300L, "示例企业"));
        when(masterDataSupport.loadRegulatorNames(any())).thenReturn(Map.of(11L, "管理员", 20L, "执法员"));
        doAnswer(invocation -> {
            InspectionRecord record = invocation.getArgument(0);
            record.setId(501L);
            return null;
        }).when(inspectionRecordMapper).insert(any(InspectionRecord.class));
        when(inspectionRecordMapper.selectList(any())).thenReturn(List.of(
            buildFailRecord(501L, 300L, 20L),
            buildFailRecord(499L, 300L, 19L)
        ));

        InspectionSubmitDTO dto = new InspectionSubmitDTO();
        dto.setInspectionDate(LocalDate.now());
        dto.setResult("FAIL");
        dto.setProblemDesc("冷藏温度不达标");
        InspectionItemDTO item = new InspectionItemDTO();
        item.setItemName("冷藏设备");
        item.setItemResult("FAIL");
        item.setProblemDesc("温度记录异常");
        dto.setItems(List.of(item));

        InspectionTaskVO result = service.submitTask(7L, 100L, dto);

        assertNotNull(result);
        assertEquals("COMPLETED", task.getStatus());
        verify(masterDataSupport).markEnterpriseAsKey(
            300L,
            "CONSECUTIVE_FAIL",
            "企业最近2次检查均为不合格，已自动纳入重点监管",
            "ROUTINE",
            501L,
            20L
        );
        verify(warningEventOutboxService).ensurePendingEvent(any(), any(), any());
        verify(warningEventOutboxService).dispatchByEventKey("INSPECTION:501:INSPECTION_CONSECUTIVE_FAIL");
        verify(rectificationService).createFromInspection(501L, 300L, "请针对本次检查不合格项完成整改并提交整改说明。\n问题概述：冷藏温度不达标\n不合格项：冷藏设备（温度记录异常）");
    }

    private InspectionRecord buildFailRecord(Long id, Long enterpriseId, Long inspectorId) {
        InspectionRecord record = new InspectionRecord();
        record.setId(id);
        record.setEnterpriseId(enterpriseId);
        record.setInspectorId(inspectorId);
        record.setInspectionDate(LocalDate.now());
        record.setResult("FAIL");
        record.setDeleted(0);
        return record;
    }
}
