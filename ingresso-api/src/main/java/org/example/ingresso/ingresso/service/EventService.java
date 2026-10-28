package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.EventContracts.CreateEventCommand;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventSummary;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;

import java.time.ZoneId;

public interface EventService {

    ZoneId EVENT_TIME_ZONE = ZoneId.of("America/Sao_Paulo");

    PageResult<EventSummary> listPublic(PageQuery query);

    EventView getPublic(Long eventId);

    PageResult<EventSummary> listProducerEvents(Long producerId, PageQuery query);

    PageResult<EventSummary> listAllForAdmin(PageQuery query);

    EventView getProducerEvent(Long eventId, Long producerId);

    EventView getAdminEvent(Long eventId);

    EventView create(CreateEventCommand request, Long producerId);

    EventView update(Long eventId, CreateEventCommand request, Long producerId);

    void suspend(Long eventId);

    void cancel(Long eventId, Long actorId);

    EventView reactivate(Long eventId, Long producerId);
}