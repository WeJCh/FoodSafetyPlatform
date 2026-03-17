package com.mortal.regulation.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.operation.client.WarningServiceClient;
import com.mortal.regulation.operation.dto.WarningEventUpsertDTO;
import com.mortal.regulation.operation.entity.WarningEventOutbox;
import com.mortal.regulation.operation.mapper.WarningEventOutboxMapper;
import com.mortal.regulation.operation.service.WarningEventOutboxService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WarningEventOutboxServiceImpl implements WarningEventOutboxService {

    private static final Logger log = LoggerFactory.getLogger(WarningEventOutboxServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_DEAD = "DEAD";

    private final WarningEventOutboxMapper warningEventOutboxMapper;
    private final WarningServiceClient warningServiceClient;
    private final ObjectMapper objectMapper;
    private final String warningInternalToken;
    private final int maxRetry;
    private final long backoffBaseSeconds;
    private final long backoffMaxSeconds;

    public WarningEventOutboxServiceImpl(
        WarningEventOutboxMapper warningEventOutboxMapper,
        WarningServiceClient warningServiceClient,
        ObjectMapper objectMapper,
        @Value("${warning.internal.token:warning-internal-token}") String warningInternalToken,
        @Value("${regulation.warning.outbox.max-retry:8}") int maxRetry,
        @Value("${regulation.warning.outbox.backoff-base-seconds:30}") long backoffBaseSeconds,
        @Value("${regulation.warning.outbox.backoff-max-seconds:1800}") long backoffMaxSeconds
    ) {
        this.warningEventOutboxMapper = warningEventOutboxMapper;
        this.warningServiceClient = warningServiceClient;
        this.objectMapper = objectMapper;
        this.warningInternalToken = warningInternalToken;
        this.maxRetry = Math.max(1, maxRetry);
        this.backoffBaseSeconds = Math.max(1L, backoffBaseSeconds);
        this.backoffMaxSeconds = Math.max(this.backoffBaseSeconds, backoffMaxSeconds);
    }

    @Override
    public void ensurePendingEvent(String eventKey, WarningEventUpsertDTO dto, LocalDateTime now) {
        if (!StringUtils.hasText(eventKey) || dto == null) {
            return;
        }
        String normalizedEventKey = eventKey.trim();
        LocalDateTime current = now == null ? LocalDateTime.now() : now;
        String payloadJson = toJson(dto);
        WarningEventOutbox existing = selectByEventKey(normalizedEventKey);
        if (existing == null) {
            WarningEventOutbox created = new WarningEventOutbox();
            created.setEventKey(normalizedEventKey);
            created.setEventType(normalizeText(dto.getEventType()));
            created.setPayloadJson(payloadJson);
            created.setStatus(STATUS_PENDING);
            created.setRetryCount(0);
            created.setNextRetryTime(current);
            created.setLastAttemptTime(null);
            created.setLastError(null);
            created.setCreateTime(current);
            created.setUpdateTime(current);
            created.setDeleted(0);
            try {
                warningEventOutboxMapper.insert(created);
                return;
            } catch (DuplicateKeyException ignored) {
                // idempotent insert fallback to update branch
            }
            existing = selectByEventKey(normalizedEventKey);
            if (existing == null) {
                return;
            }
        }
        if (STATUS_SENT.equalsIgnoreCase(existing.getStatus()) || STATUS_DEAD.equalsIgnoreCase(existing.getStatus())) {
            return;
        }
        warningEventOutboxMapper.update(null, new LambdaUpdateWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getId, existing.getId())
            .eq(WarningEventOutbox::getDeleted, 0)
            .set(WarningEventOutbox::getEventType, normalizeText(dto.getEventType()))
            .set(WarningEventOutbox::getPayloadJson, payloadJson)
            .set(WarningEventOutbox::getStatus, STATUS_PENDING)
            .set(WarningEventOutbox::getNextRetryTime, current)
            .set(WarningEventOutbox::getUpdateTime, current));
    }

    @Override
    public boolean dispatchByEventKey(String eventKey) {
        if (!StringUtils.hasText(eventKey)) {
            return true;
        }
        WarningEventOutbox record = selectByEventKey(eventKey.trim());
        if (record == null) {
            return true;
        }
        if (STATUS_SENT.equalsIgnoreCase(record.getStatus())) {
            return true;
        }
        if (STATUS_DEAD.equalsIgnoreCase(record.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (record.getNextRetryTime() != null && record.getNextRetryTime().isAfter(now)) {
            return false;
        }
        return doDispatch(record, now);
    }

    @Override
    public int dispatchDueEvents(int batchSize) {
        int safeBatchSize = Math.max(1, batchSize);
        LocalDateTime now = LocalDateTime.now();
        List<WarningEventOutbox> pending = warningEventOutboxMapper.selectList(new LambdaQueryWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getDeleted, 0)
            .eq(WarningEventOutbox::getStatus, STATUS_PENDING)
            .and(wrapper -> wrapper.isNull(WarningEventOutbox::getNextRetryTime)
                .or()
                .le(WarningEventOutbox::getNextRetryTime, now))
            .orderByAsc(WarningEventOutbox::getNextRetryTime, WarningEventOutbox::getId)
            .last("limit " + safeBatchSize));
        int successCount = 0;
        for (WarningEventOutbox record : pending) {
            if (doDispatch(record, LocalDateTime.now())) {
                successCount++;
            }
        }
        return successCount;
    }

    private boolean doDispatch(WarningEventOutbox record, LocalDateTime now) {
        if (record == null || record.getId() == null) {
            return false;
        }
        WarningEventUpsertDTO dto;
        try {
            dto = objectMapper.readValue(record.getPayloadJson(), WarningEventUpsertDTO.class);
        } catch (Exception ex) {
            markDead(record, now, "invalid payload_json: " + ex.getMessage());
            return false;
        }
        try {
            ApiResponse<Map<String, Object>> response = warningServiceClient.upsertInternalEvent(dto, warningInternalToken);
            if (response != null && response.getCode() == 0) {
                markSent(record, now);
                return true;
            }
            markRetry(record, now, buildResponseError(response));
            return false;
        } catch (Exception ex) {
            markRetry(record, now, "exception: " + ex.getMessage());
            return false;
        }
    }

    private WarningEventOutbox selectByEventKey(String eventKey) {
        return warningEventOutboxMapper.selectOne(new LambdaQueryWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getEventKey, eventKey)
            .eq(WarningEventOutbox::getDeleted, 0)
            .last("limit 1"));
    }

    private void markSent(WarningEventOutbox record, LocalDateTime now) {
        warningEventOutboxMapper.update(null, new LambdaUpdateWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getId, record.getId())
            .eq(WarningEventOutbox::getDeleted, 0)
            .set(WarningEventOutbox::getStatus, STATUS_SENT)
            .set(WarningEventOutbox::getLastAttemptTime, now)
            .set(WarningEventOutbox::getLastError, null)
            .set(WarningEventOutbox::getUpdateTime, now));
    }

    private void markDead(WarningEventOutbox record, LocalDateTime now, String error) {
        warningEventOutboxMapper.update(null, new LambdaUpdateWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getId, record.getId())
            .eq(WarningEventOutbox::getDeleted, 0)
            .set(WarningEventOutbox::getStatus, STATUS_DEAD)
            .set(WarningEventOutbox::getRetryCount, maxRetry)
            .set(WarningEventOutbox::getLastAttemptTime, now)
            .set(WarningEventOutbox::getLastError, truncateError(error))
            .set(WarningEventOutbox::getUpdateTime, now));
        log.error("Warning outbox marked dead. id={}, eventKey={}, reason={}",
            record.getId(), record.getEventKey(), truncateError(error));
    }

    private void markRetry(WarningEventOutbox record, LocalDateTime now, String error) {
        int currentRetry = record.getRetryCount() == null ? 0 : record.getRetryCount();
        int nextRetryCount = currentRetry + 1;
        boolean dead = nextRetryCount >= maxRetry;
        LocalDateTime nextRetryTime = dead ? null : now.plusSeconds(resolveBackoffSeconds(nextRetryCount));
        warningEventOutboxMapper.update(null, new LambdaUpdateWrapper<WarningEventOutbox>()
            .eq(WarningEventOutbox::getId, record.getId())
            .eq(WarningEventOutbox::getDeleted, 0)
            .set(WarningEventOutbox::getStatus, dead ? STATUS_DEAD : STATUS_PENDING)
            .set(WarningEventOutbox::getRetryCount, nextRetryCount)
            .set(WarningEventOutbox::getNextRetryTime, nextRetryTime)
            .set(WarningEventOutbox::getLastAttemptTime, now)
            .set(WarningEventOutbox::getLastError, truncateError(error))
            .set(WarningEventOutbox::getUpdateTime, now));
        if (dead) {
            log.error("Warning outbox reached max retry and marked dead. id={}, eventKey={}, error={}",
                record.getId(), record.getEventKey(), truncateError(error));
        } else {
            log.warn("Warning outbox dispatch failed, will retry. id={}, eventKey={}, retryCount={}, error={}",
                record.getId(), record.getEventKey(), nextRetryCount, truncateError(error));
        }
    }

    private String buildResponseError(ApiResponse<Map<String, Object>> response) {
        if (response == null) {
            return "response is null";
        }
        return "code=" + response.getCode() + ", message=" + response.getMessage();
    }

    private long resolveBackoffSeconds(int retryCount) {
        if (retryCount <= 1) {
            return backoffBaseSeconds;
        }
        long multiplier = 1L;
        int shift = Math.min(retryCount - 1, 16);
        multiplier = multiplier << shift;
        long seconds;
        try {
            seconds = Math.multiplyExact(backoffBaseSeconds, multiplier);
        } catch (ArithmeticException ex) {
            seconds = backoffMaxSeconds;
        }
        return Math.min(seconds, backoffMaxSeconds);
    }

    private String truncateError(String error) {
        if (!StringUtils.hasText(error)) {
            return null;
        }
        String text = error.trim();
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private String toJson(WarningEventUpsertDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("serialize warning event failed", ex);
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
