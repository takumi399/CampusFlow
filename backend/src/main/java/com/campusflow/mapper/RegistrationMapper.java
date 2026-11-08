package com.campusflow.mapper;

import com.campusflow.domain.AdminRegistrationView;
import com.campusflow.domain.Registration;
import com.campusflow.domain.RegistrationStatus;
import com.campusflow.domain.RegistrationView;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistrationMapper {

    Registration findByEventAndUser(@Param("eventId") Long eventId, @Param("userId") Long userId);

    Registration findByEventAndUserForUpdate(@Param("eventId") Long eventId, @Param("userId") Long userId);

    RegistrationView findViewByEventAndUser(@Param("eventId") Long eventId, @Param("userId") Long userId);

    Registration findById(@Param("id") Long id);

    Registration findByIdForUpdate(@Param("id") Long id);

    int insert(Registration registration);

    int markConfirmedOnRegister(@Param("id") Long id);

    int markWaitlistedOnRegister(@Param("id") Long id, @Param("waitlistTime") LocalDateTime waitlistTime);

    int markCanceled(@Param("id") Long id, @Param("canceledAt") LocalDateTime canceledAt);

    int markPromoted(@Param("id") Long id, @Param("confirmDeadline") LocalDateTime confirmDeadline);

    int markConfirmedAfterPromotion(@Param("id") Long id);

    int markExpired(@Param("id") Long id);

    int markCheckedIn(@Param("id") Long id, @Param("checkedInAt") LocalDateTime checkedInAt);

    Registration findFirstWaitlistedForUpdate(@Param("eventId") Long eventId);

    List<Long> findExpiredPromotionIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int countWaitlistedBefore(
            @Param("eventId") Long eventId,
            @Param("waitlistTime") LocalDateTime waitlistTime,
            @Param("id") Long id
    );

    long countByEventAndStatus(
            @Param("eventId") Long eventId,
            @Param("status") RegistrationStatus status
    );

    List<RegistrationView> findViewsByUserId(@Param("userId") Long userId);

    List<AdminRegistrationView> findAdminViewsByEventId(@Param("eventId") Long eventId);

    AdminRegistrationView findAdminViewById(@Param("id") Long id);

    int cancelAllActiveByEvent(
            @Param("eventId") Long eventId,
            @Param("canceledAt") LocalDateTime canceledAt
    );
}
