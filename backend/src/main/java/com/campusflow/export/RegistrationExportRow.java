package com.campusflow.export;

import com.alibaba.excel.annotation.ExcelProperty;

public class RegistrationExportRow {

    @ExcelProperty("学号")
    private String username;

    @ExcelProperty("姓名")
    private String displayName;

    @ExcelProperty("报名状态")
    private String status;

    @ExcelProperty("报名时间")
    private String registeredAt;

    @ExcelProperty("候补时间")
    private String waitlistTime;

    @ExcelProperty("确认截止时间")
    private String confirmDeadline;

    @ExcelProperty("签到时间")
    private String checkedInAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(String registeredAt) {
        this.registeredAt = registeredAt;
    }

    public String getWaitlistTime() {
        return waitlistTime;
    }

    public void setWaitlistTime(String waitlistTime) {
        this.waitlistTime = waitlistTime;
    }

    public String getConfirmDeadline() {
        return confirmDeadline;
    }

    public void setConfirmDeadline(String confirmDeadline) {
        this.confirmDeadline = confirmDeadline;
    }

    public String getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(String checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
