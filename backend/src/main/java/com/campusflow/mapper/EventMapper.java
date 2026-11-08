package com.campusflow.mapper;

import com.campusflow.domain.Event;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EventMapper {

    Event findById(@Param("id") Long id);

    Event findByIdForUpdate(@Param("id") Long id);

    List<Event> findPublished();

    List<Event> findAll();

    int insert(Event event);

    int updateDraft(Event event);

    int publish(@Param("id") Long id);

    int cancel(@Param("id") Long id);

    int finish(@Param("id") Long id);

    int incrementOccupied(@Param("id") Long id);

    int decrementOccupied(@Param("id") Long id);

    int resetOccupied(@Param("id") Long id);

    long count();
}
