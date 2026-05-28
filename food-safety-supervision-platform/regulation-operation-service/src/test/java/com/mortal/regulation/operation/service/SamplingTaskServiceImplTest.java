package com.mortal.regulation.operation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mortal.regulation.operation.client.regulation.vo.InternalEnterpriseDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductDetailVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalProductSummaryVO;
import com.mortal.regulation.operation.client.regulation.vo.InternalRegulatorIdentityVO;
import com.mortal.regulation.operation.dto.SamplingResultSubmitDTO;
import com.mortal.regulation.operation.dto.SamplingTaskCreateDTO;
import com.mortal.regulation.operation.entity.SamplingResult;
import com.mortal.regulation.operation.entity.SamplingTask;
import com.mortal.regulation.operation.mapper.SamplingResultMapper;
import com.mortal.regulation.operation.mapper.SamplingTaskMapper;
import com.mortal.regulation.operation.service.impl.SamplingTaskServiceImpl;
import com.mortal.regulation.operation.support.OperationAuditOperatorNameResolver;
import com.mortal.regulation.operation.support.OperationLockSupport;
import com.mortal.regulation.operation.support.OperationMasterDataSupport;
import com.mortal.regulation.operation.support.SamplingPublicCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.regulation.operation.vo.SamplingResultVO;
import com.mortal.regulation.operation.vo.SamplingTaskVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class SamplingTaskServiceImplTest {

    @Test
    void createTask_shouldPersistEnterpriseProductSamplingTask() {
        SamplingTaskMapper taskMapper = mock(SamplingTaskMapper.class);
        SamplingResultMapper resultMapper = mock(SamplingResultMapper.class);
        OperationMasterDataSupport masterDataSupport = mock(OperationMasterDataSupport.class);
        OperationLockSupport operationLockSupport = mock(OperationLockSupport.class);
        SamplingPublicCacheService samplingPublicCacheService = mock(SamplingPublicCacheService.class);
        WarningEventOutboxService warningEventOutboxService = mock(WarningEventOutboxService.class);
        SamplingTaskServiceImpl service = new SamplingTaskServiceImpl(
            taskMapper,
            resultMapper,
            masterDataSupport,
            operationLockSupport,
            samplingPublicCacheService,
            warningEventOutboxService,
            mock(AuditLogService.class),
            mock(OperationAuditOperatorNameResolver.class),
            new ObjectMapper()
        );

        InternalRegulatorIdentityVO admin = new InternalRegulatorIdentityVO();
        admin.setId(10L);
        InternalEnterpriseDetailVO enterprise = new InternalEnterpriseDetailVO();
        enterprise.setId(101L);
        enterprise.setEnterpriseName("示例企业");
        enterprise.setRegionId(301L);
        InternalProductDetailVO product = new InternalProductDetailVO();
        product.setId(501L);
        product.setEnterpriseId(101L);
        product.setProductName("巴氏鲜奶");
        product.setCategory("乳制品");
        product.setSpecification("250ml/盒");
        product.setStatus("ACTIVE");

        when(masterDataSupport.requireAdmin(1L)).thenReturn(admin);
        when(masterDataSupport.requireApprovedEnterprise(101L)).thenReturn(enterprise);
        when(masterDataSupport.requireProduct(501L)).thenReturn(product);
        when(masterDataSupport.loadRegulatorNames(any())).thenReturn(Map.of(10L, "区域管理员"));
        doAnswer(invocation -> {
            SamplingTask task = invocation.getArgument(0);
            task.setId(900L);
            return null;
        }).when(taskMapper).insert(any(SamplingTask.class));

        SamplingTaskCreateDTO dto = new SamplingTaskCreateDTO();
        dto.setEnterpriseId(101L);
        dto.setProductId(501L);
        dto.setTaskTitle("乳制品例行抽检");
        dto.setTaskDesc("检查冷链和批次留样");
        dto.setPriority("HIGH");
        dto.setDeadline(LocalDateTime.now().plusDays(2));

        SamplingTaskVO result = service.createTask(1L, dto);

        assertNotNull(result);
        assertEquals(900L, result.getId());
        assertEquals(101L, result.getEnterpriseId());
        assertEquals(501L, result.getProductId());
        assertEquals("巴氏鲜奶", result.getProductName());
        assertEquals("CREATED", result.getStatus());
        verify(masterDataSupport).requireEnterpriseInScope(10L, 101L);
    }

    @Test
    void submitResult_shouldInsertDraftSamplingResultAndCompleteTask() {
        SamplingTaskMapper taskMapper = mock(SamplingTaskMapper.class);
        SamplingResultMapper resultMapper = mock(SamplingResultMapper.class);
        OperationMasterDataSupport masterDataSupport = mock(OperationMasterDataSupport.class);
        OperationLockSupport operationLockSupport = mock(OperationLockSupport.class);
        SamplingPublicCacheService samplingPublicCacheService = mock(SamplingPublicCacheService.class);
        WarningEventOutboxService warningEventOutboxService = mock(WarningEventOutboxService.class);
        when(operationLockSupport.executeWithLock(any(), any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(2);
            return callable.call();
        });
        SamplingTaskServiceImpl service = new SamplingTaskServiceImpl(
            taskMapper,
            resultMapper,
            masterDataSupport,
            operationLockSupport,
            samplingPublicCacheService,
            warningEventOutboxService,
            mock(AuditLogService.class),
            mock(OperationAuditOperatorNameResolver.class),
            new ObjectMapper()
        );

        InternalRegulatorIdentityVO enforcer = new InternalRegulatorIdentityVO();
        enforcer.setId(20L);
        SamplingTask task = new SamplingTask();
        task.setId(1000L);
        task.setTaskNo("SMP001");
        task.setEnterpriseId(101L);
        task.setProductId(501L);
        task.setAssignedTo(20L);
        task.setStatus("ASSIGNED");
        task.setDeadline(LocalDateTime.now().plusDays(1));

        InternalProductSummaryVO product = new InternalProductSummaryVO();
        product.setId(501L);
        product.setProductName("巴氏鲜奶");
        product.setCategory("乳制品");
        product.setSpecification("250ml/盒");

        when(masterDataSupport.requireEnforcer(2L)).thenReturn(enforcer);
        when(taskMapper.selectById(1000L)).thenReturn(task);
        when(resultMapper.selectOne(any())).thenReturn(null);
        when(masterDataSupport.loadEnterpriseNames(any())).thenReturn(Map.of(101L, "示例企业"));
        when(masterDataSupport.loadProductSummaries(any())).thenReturn(Map.of(501L, product));
        when(masterDataSupport.loadRegulatorNames(any())).thenReturn(Map.of(20L, "执法人员"));
        doAnswer(invocation -> {
            SamplingResult result = invocation.getArgument(0);
            result.setId(700L);
            return null;
        }).when(resultMapper).insert(any(SamplingResult.class));

        SamplingResultSubmitDTO dto = new SamplingResultSubmitDTO();
        dto.setSampledTime(LocalDateTime.now());
        dto.setResult("FAIL");
        dto.setConclusion("检出指标异常");
        dto.setDisposalSuggestion("建议启动复查");

        SamplingResultVO result = service.submitResult(2L, 1000L, dto);

        assertNotNull(result);
        assertEquals(700L, result.getId());
        assertEquals("FAIL", result.getResult());
        assertEquals("DRAFT", result.getPublicStatus());
        assertEquals("COMPLETED", task.getStatus());
        verify(masterDataSupport).markEnterpriseAsKey(
            eq(101L),
            eq("SAMPLING_FAIL"),
            any(),
            eq("ROUTINE"),
            eq(700L),
            eq(20L)
        );
        verify(warningEventOutboxService).ensurePendingEvent(
            eq("SAMPLING:700:SAMPLING_FAIL"),
            any(),
            any()
        );
        verify(warningEventOutboxService).dispatchByEventKey("SAMPLING:700:SAMPLING_FAIL");
        verify(taskMapper).updateById(task);
    }
}
