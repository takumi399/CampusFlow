package com.campusflow.controller;

import com.campusflow.common.ApiResponse;
import com.campusflow.dto.AdminRegistrationResponse;
import com.campusflow.export.ExportFile;
import com.campusflow.export.RegistrationExportService;
import com.campusflow.service.RegistrationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminRegistrationController {

    private final RegistrationService registrationService;
    private final RegistrationExportService exportService;

    public AdminRegistrationController(
            RegistrationService registrationService,
            RegistrationExportService exportService
    ) {
        this.registrationService = registrationService;
        this.exportService = exportService;
    }

    @GetMapping("/events/{eventId}/registrations")
    public ApiResponse<List<AdminRegistrationResponse>> list(@PathVariable Long eventId) {
        return ApiResponse.ok(registrationService.adminRegistrations(eventId));
    }

    @PostMapping("/registrations/{registrationId}/check-in")
    public ApiResponse<AdminRegistrationResponse> checkIn(@PathVariable Long registrationId) {
        return ApiResponse.ok(registrationService.adminCheckIn(registrationId), "签到成功");
    }

    @GetMapping("/events/{eventId}/export")
    public ResponseEntity<ByteArrayResource> export(@PathVariable Long eventId) {
        ExportFile file = exportService.export(eventId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .contentLength(file.content().length)
                .body(new ByteArrayResource(file.content()));
    }
}
