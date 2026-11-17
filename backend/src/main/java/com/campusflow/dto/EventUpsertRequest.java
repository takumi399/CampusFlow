package com.campusflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventUpsertRequest(
        @NotBlank(message = "活动标题不能为空")
        @Size(max = 100, message = "活动标题不能超过 100 个字符")
        String title,

        @Size(max = 2000, message = "活动描述不能超过 2000 个字符")
        String description,

        @NotBlank(message = "活动地点不能为空")
        @Size(max = 100, message = "活动地点不能超过 100 个字符")
        String location,

        @NotBlank(message = "活动分类不能为空")
        @Size(max = 30, message = "活动分类不能超过 30 个字符")
        String category,

        @NotBlank(message = "主办方不能为空")
        @Size(max = 100, message = "主办方不能超过 100 个字符")
        String organizer,

        @NotNull(message = "活动开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "活动结束时间不能为空")
        LocalDateTime endTime,

        @NotNull(message = "报名开始时间不能为空")
        LocalDateTime registrationStart,

        @NotNull(message = "报名结束时间不能为空")
        LocalDateTime registrationEnd,

        @NotNull(message = "活动名额不能为空")
        @Min(value = 1, message = "活动名额至少为 1")
        @Max(value = 10000, message = "活动名额不能超过 10000")
        Integer capacity
) {
}
