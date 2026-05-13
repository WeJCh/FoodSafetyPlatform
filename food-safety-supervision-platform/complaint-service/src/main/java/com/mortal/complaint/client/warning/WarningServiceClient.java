package com.mortal.complaint.client.warning;

import com.mortal.complaint.client.warning.dto.WarningEventUpsertDTO;
import com.mortal.platform.common.ApiResponse;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient("warning-service")
public interface WarningServiceClient {

    @PostMapping("/api/warning/internal/events/upsert")
    ApiResponse<Map<String, Object>> upsertInternalEvent(@RequestBody WarningEventUpsertDTO dto,
                                                         @RequestHeader("X-Internal-Token") String internalToken);
}
