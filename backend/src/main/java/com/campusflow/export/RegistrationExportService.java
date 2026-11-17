package com.campusflow.export;

import com.alibaba.excel.EasyExcel;
import com.campusflow.common.ApiException;
import com.campusflow.domain.AdminRegistrationView;
import com.campusflow.domain.Event;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.mapper.EventMapper;
import com.campusflow.mapper.RegistrationMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RegistrationExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;

    public RegistrationExportService(EventMapper eventMapper, RegistrationMapper registrationMapper) {
        this.eventMapper = eventMapper;
        this.registrationMapper = registrationMapper;
    }

    public ExportFile export(Long eventId) {
        Event event = eventMapper.findById(eventId);
        if (event == null) {
            throw ApiException.notFound("EVENT_NOT_FOUND", "活动不存在");
        }

        List<RegistrationExportRow> rows = registrationMapper.findAdminViewsByEventId(eventId).stream()
                .map(this::toRow)
                .toList();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, RegistrationExportRow.class)
                .sheet("报名名单")
                .doWrite(rows);

        return new ExportFile(
                "campus-event-" + eventId + "-registrations.xlsx",
                output.toByteArray()
        );
    }

    private RegistrationExportRow toRow(AdminRegistrationView view) {
        RegistrationExportRow row = new RegistrationExportRow();
        row.setUsername(view.getUsername());
        row.setDisplayName(view.getDisplayName());
        row.setStatus(statusLabel(view.getStatus()));
        row.setRegisteredAt(format(view.getCreatedAt()));
        row.setWaitlistTime(format(view.getWaitlistTime()));
        row.setConfirmDeadline(format(view.getConfirmDeadline()));
        row.setCheckedInAt(format(view.getCheckedInAt()));
        return row;
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String statusLabel(RegistrationStatus status) {
        return switch (status) {
            case CONFIRMED -> "已确认";
            case WAITLISTED -> "候补中";
            case PROMOTED -> "待确认";
            case CANCELED -> "已取消";
            case EXPIRED -> "已过期";
            case CHECKED_IN -> "已签到";
        };
    }
}
