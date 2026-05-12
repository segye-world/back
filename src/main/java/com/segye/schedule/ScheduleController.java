package com.segye.schedule;

import com.segye.common.ApiResponse;
import com.segye.schedule.dto.ScheduleDtos;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService service;

    public ScheduleController(ScheduleService service) {
        this.service = service;
    }

    private Long memberId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public ApiResponse<ScheduleDtos.ScheduleResponse> create(
            Authentication auth,
            @RequestBody @Valid ScheduleDtos.CreateRequest req
    ) {
        return ApiResponse.ok(service.create(memberId(auth), req));
    }

    @GetMapping
    public ApiResponse<List<ScheduleDtos.ScheduleResponse>> listByDate(
            Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok(service.listByDate(memberId(auth), date));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleDtos.ScheduleResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody @Valid ScheduleDtos.UpdateRequest req
    ) {
        return ApiResponse.ok(service.update(memberId(auth), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        service.delete(memberId(auth), id);
        return ApiResponse.ok();
    }
}
