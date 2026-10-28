package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.EventService;
import org.example.ingresso.ingresso.service.PurchaseOrderService;
import org.example.ingresso.ingresso.service.TicketRefundService;

import org.example.ingresso.ingresso.service.dto.EventContracts.CreateEventCommand;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventSummary;
import org.example.ingresso.ingresso.service.dto.EventContracts.AdmissionModeCode;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventStatusCode;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatStatusCode;
import org.example.ingresso.ingresso.service.dto.EventContracts.EventView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatRowInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.SeatSectorInput;
import org.example.ingresso.ingresso.service.dto.EventContracts.TicketCategoryInput;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.EventSeat;
import org.example.ingresso.ingresso.model.SeatRow;
import org.example.ingresso.ingresso.model.SeatSector;
import org.example.ingresso.ingresso.model.TicketCategory;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.AdmissionMode;
import org.example.ingresso.ingresso.model.enums.EventStatus;
import org.example.ingresso.ingresso.model.enums.SeatStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UsuarioRepository usuarioRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final TicketRefundService ticketRefundService;

    public EventServiceImpl(
        EventRepository eventRepository,
        UsuarioRepository usuarioRepository,
        PurchaseOrderService purchaseOrderService,
        TicketRefundService ticketRefundService
    ) {
        this.eventRepository = eventRepository;
        this.usuarioRepository = usuarioRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.ticketRefundService = ticketRefundService;
    }

    @Transactional(readOnly = true)
    public PageResult<EventSummary> listPublic(PageQuery query) {
        return toPageResult(eventRepository.findByStatus(EventStatus.PUBLISHED, toPageable(query)).map(this::toSummary), query);
    }

    @Transactional(readOnly = true)
    public EventView getPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.NOT_FOUND, "Evento nao encontrado"
            ));
        if (event.getStatus() == EventStatus.SUSPENDED) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.NOT_FOUND, "Evento nao encontrado"
            );
        }
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public PageResult<EventSummary> listProducerEvents(Long producerId, PageQuery query) {
        return toPageResult(eventRepository.findByProducer_Id(producerId, toPageable(query)).map(this::toSummary), query);
    }

    @Transactional(readOnly = true)
    public PageResult<EventSummary> listAllForAdmin(PageQuery query) {
        return toPageResult(eventRepository.findAll(toPageable(query)).map(this::toSummary), query);
    }

    @Transactional(readOnly = true)
    public EventView getProducerEvent(Long eventId, Long producerId) {
        return toResponse(findOwnedEvent(eventId, producerId));
    }

    @Transactional(readOnly = true)
    public EventView getAdminEvent(Long eventId) {
        return toResponse(eventRepository.findById(eventId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.NOT_FOUND, "Evento nao encontrado"
            )));
    }

    @Transactional
    public EventView create(CreateEventCommand request, Long producerId) {
        validateEventRequest(request);
        Usuario producer = findProducer(producerId);
        Event event = new Event(
            producer,
            request.title().trim(),
            request.description().trim(),
            validateImageUrl(request.imageUrl()),
            request.startsAt().atZone(EventService.EVENT_TIME_ZONE).toInstant(),
            request.venueName().trim(),
            request.streetAddress().trim(),
            request.city().trim(),
            request.stateCode().trim().toUpperCase(Locale.ROOT),
            request.ageClassification().trim(),
            request.organizer().trim()
        );

        Map<String, TicketCategory> categories = addCategories(event, request.categories());
        addSeatMap(event, request.sectors(), categories);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventView update(Long eventId, CreateEventCommand request, Long producerId) {
        validateEventRequest(request);
        Event event = findOwnedEvent(eventId, producerId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Evento cancelado nao pode ser alterado");
        }

        Map<String, TicketCategoryInput> categoryInputs = categoryInputMap(request.categories());
        Map<String, TicketCategory> existingCategories = new LinkedHashMap<>();
        event.getCategories().forEach(category -> existingCategories.put(category.getCode(), category));
        boolean seatMapChanged = !hasSameSeatMap(event, request.sectors());

        if (seatMapChanged && containsCommittedSeat(event)) {
            throw new IllegalArgumentException("O mapa nao pode ser alterado com assentos vendidos ou reservados");
        }
        if (seatMapChanged) {
            event.clearSeatMap();
        }

        for (TicketCategory existing : new ArrayList<>(event.getCategories())) {
            TicketCategoryInput update = categoryInputs.get(existing.getCode());
            if (update == null) {
                if (existing.getSoldQuantity() > 0 || existing.getReservedQuantity() > 0) {
                    throw new IllegalArgumentException("Categoria com ingressos vendidos ou reservados nao pode ser removida");
                }
                event.removeCategory(existing);
                continue;
            }
            validateCategory(update);
            if (existing.getAdmissionMode() != toAdmissionMode(update.admissionMode())
                && (existing.getSoldQuantity() > 0 || existing.getReservedQuantity() > 0)) {
                throw new IllegalArgumentException("Modalidade de categoria vendida ou reservada nao pode ser alterada");
            }
            if (toAdmissionMode(update.admissionMode()) == AdmissionMode.GENERAL_ADMISSION
                && update.quantity() < existing.getSoldQuantity() + existing.getReservedQuantity()) {
                throw new IllegalArgumentException("Quantidade nao pode ficar abaixo dos ingressos vendidos ou reservados");
            }
            existing.update(
                update.name().trim(), update.priceInCents(), toAdmissionMode(update.admissionMode()), update.quantity()
            );
        }

        Map<String, TicketCategory> categories = new LinkedHashMap<>();
        for (TicketCategoryInput input : request.categories()) {
            TicketCategory category = existingCategories.get(input.code());
            if (category == null) {
                category = new TicketCategory(
                    input.code(), input.name().trim(), input.priceInCents(), toAdmissionMode(input.admissionMode()), input.quantity()
                );
                event.addCategory(category);
            }
            categories.put(input.code(), category);
        }

        if (seatMapChanged) {
            addSeatMap(event, request.sectors(), categories);
        }
        event.updateDetails(
            request.title().trim(),
            request.description().trim(),
            validateImageUrl(request.imageUrl()),
            request.startsAt().atZone(EventService.EVENT_TIME_ZONE).toInstant(),
            request.venueName().trim(),
            request.streetAddress().trim(),
            request.city().trim(),
            request.stateCode().trim().toUpperCase(Locale.ROOT),
            request.ageClassification().trim(),
            request.organizer().trim()
        );
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void suspend(Long eventId) {
        eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"))
            .suspend();
        purchaseOrderService.cancelPendingForEvent(eventId);
    }

    @Transactional
    public void cancel(Long eventId, Long actorId) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario actor = usuarioRepository.findWithPerfisById(actorId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Usuario autenticado nao encontrado"
            ));
        boolean admin = actor.getPerfis().contains(UsuarioPerfil.ADMIN);
        boolean owner = actor.getPerfis().contains(UsuarioPerfil.PRODUCER)
            && event.getProducer().getId().equals(actor.getId());
        if (!admin && !owner) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Somente o produtor dono ou ADMIN pode cancelar o evento"
            );
        }
        event.cancel();
        purchaseOrderService.cancelPendingForEvent(eventId);
        ticketRefundService.refundUnusedTicketsForEvent(event.getId(), actor.getId());
    }

    @Transactional
    public EventView reactivate(Long eventId, Long producerId) {
        Event event = findOwnedEvent(eventId, producerId);
        event.reactivate();
        return toResponse(event);
    }

    private Usuario findProducer(Long producerId) {
        Usuario producer = usuarioRepository.findWithPerfisById(producerId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Usuario autenticado nao encontrado"
            ));
        if (!producer.getPerfis().contains(UsuarioPerfil.PRODUCER)) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Usuario nao possui perfil de produtor"
            );
        }
        return producer;
    }

    private Event findOwnedEvent(Long eventId, Long producerId) {
        return eventRepository.findByIdAndProducer_Id(eventId, producerId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.NOT_FOUND, "Evento nao encontrado"
            ));
    }

    private void validateEventRequest(CreateEventCommand request) {
        validateImageUrl(request.imageUrl());
        Map<String, TicketCategoryInput> categories = categoryInputMap(request.categories());
        Map<String, Integer> assignedSeatCounts = new HashMap<>();
        Set<String> sectorNames = new HashSet<>();
        boolean hasAssignedCategory = false;

        for (TicketCategoryInput category : categories.values()) {
            validateCategory(category);
            if (toAdmissionMode(category.admissionMode()) == AdmissionMode.ASSIGNED_SEAT) {
                hasAssignedCategory = true;
            }
        }

        List<SeatSectorInput> sectors = request.sectors() == null ? List.of() : request.sectors();
        for (SeatSectorInput sector : sectors) {
            if (!sectorNames.add(sector.name().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Nome de setor duplicado");
            }
            Set<String> rowLabels = new HashSet<>();
            for (SeatRowInput row : sector.rows()) {
                if (!rowLabels.add(row.label().trim().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Fileira duplicada no setor");
                }
                Set<String> seatLabels = new HashSet<>();
                for (SeatInput seat : row.seats()) {
                    if (!seatLabels.add(seat.label().trim().toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("Assento duplicado na fileira");
                    }
                    TicketCategoryInput category = categories.get(seat.categoryCode());
                    if (category == null || toAdmissionMode(category.admissionMode()) != AdmissionMode.ASSIGNED_SEAT) {
                        throw new IllegalArgumentException("Assento deve pertencer a uma categoria com lugar marcado");
                    }
                    assignedSeatCounts.merge(category.code(), 1, Integer::sum);
                }
            }
        }

        if (hasAssignedCategory != !sectors.isEmpty()) {
            throw new IllegalArgumentException("Categorias com lugar marcado precisam de mapa; categorias gerais nao usam mapa");
        }
        for (TicketCategoryInput category : categories.values()) {
            if (toAdmissionMode(category.admissionMode()) == AdmissionMode.ASSIGNED_SEAT
                && assignedSeatCounts.getOrDefault(category.code(), 0) == 0) {
                throw new IllegalArgumentException("Cada categoria com lugar marcado precisa ter assentos");
            }
        }
    }

    private Map<String, TicketCategoryInput> categoryInputMap(
        List<TicketCategoryInput> inputs
    ) {
        Map<String, TicketCategoryInput> categories = new LinkedHashMap<>();
        for (TicketCategoryInput input : inputs) {
            if (categories.putIfAbsent(input.code(), input) != null) {
                throw new IllegalArgumentException("Codigo de categoria duplicado");
            }
        }
        return categories;
    }

    private void validateCategory(TicketCategoryInput category) {
        if (category.priceInCents() < 0) {
            throw new IllegalArgumentException("Preco nao pode ser negativo");
        }
        if (toAdmissionMode(category.admissionMode()) == AdmissionMode.GENERAL_ADMISSION
            && (category.quantity() == null || category.quantity() < 1)) {
            throw new IllegalArgumentException("Categoria geral precisa de quantidade positiva");
        }
        if (toAdmissionMode(category.admissionMode()) == AdmissionMode.ASSIGNED_SEAT && category.quantity() != null) {
            throw new IllegalArgumentException("Quantidade de lugares marcados e derivada do mapa");
        }
    }

    private AdmissionMode toAdmissionMode(AdmissionModeCode admissionMode) {
        return AdmissionMode.valueOf(admissionMode.name());
    }

    private String validateImageUrl(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException("Imagem precisa usar uma URL HTTPS valida");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Imagem precisa usar uma URL HTTPS valida");
        }
    }

    private Map<String, TicketCategory> addCategories(
        Event event,
        List<TicketCategoryInput> inputs
    ) {
        Map<String, TicketCategory> categories = new LinkedHashMap<>();
        for (TicketCategoryInput input : inputs) {
            TicketCategory category = new TicketCategory(
                input.code(), input.name().trim(), input.priceInCents(), toAdmissionMode(input.admissionMode()), input.quantity()
            );
            event.addCategory(category);
            categories.put(input.code(), category);
        }
        return categories;
    }

    private void addSeatMap(
        Event event,
        List<SeatSectorInput> inputs,
        Map<String, TicketCategory> categories
    ) {
        if (inputs == null) {
            return;
        }
        for (SeatSectorInput sectorInput : inputs) {
            SeatSector sector = new SeatSector(sectorInput.name().trim(), sectorInput.positionIndex());
            for (SeatRowInput rowInput : sectorInput.rows()) {
                SeatRow row = new SeatRow(rowInput.label().trim(), rowInput.positionIndex());
                for (SeatInput seatInput : rowInput.seats()) {
                    row.addSeat(new EventSeat(
                        seatInput.label().trim(),
                        seatInput.positionIndex(),
                        categories.get(seatInput.categoryCode())
                    ));
                }
                sector.addRow(row);
            }
            event.addSector(sector);
        }
    }

    private boolean hasSameSeatMap(Event event, List<SeatSectorInput> inputs) {
        List<SeatSectorInput> requested = inputs == null ? List.of() : inputs;
        List<SeatSector> existing = event.getSectors();
        if (existing.size() != requested.size()) {
            return false;
        }
        for (int sectorIndex = 0; sectorIndex < existing.size(); sectorIndex++) {
            SeatSector oldSector = existing.get(sectorIndex);
            SeatSectorInput newSector = requested.get(sectorIndex);
            if (!oldSector.getName().equals(newSector.name())
                || oldSector.getPositionIndex() != newSector.positionIndex()
                || oldSector.getRows().size() != newSector.rows().size()) {
                return false;
            }
            for (int rowIndex = 0; rowIndex < oldSector.getRows().size(); rowIndex++) {
                SeatRow oldRow = oldSector.getRows().get(rowIndex);
                SeatRowInput newRow = newSector.rows().get(rowIndex);
                if (!oldRow.getLabel().equals(newRow.label())
                    || oldRow.getPositionIndex() != newRow.positionIndex()
                    || oldRow.getSeats().size() != newRow.seats().size()) {
                    return false;
                }
                for (int seatIndex = 0; seatIndex < oldRow.getSeats().size(); seatIndex++) {
                    EventSeat oldSeat = oldRow.getSeats().get(seatIndex);
                    SeatInput newSeat = newRow.seats().get(seatIndex);
                    if (!oldSeat.getLabel().equals(newSeat.label())
                        || oldSeat.getPositionIndex() != newSeat.positionIndex()
                        || !oldSeat.getCategory().getCode().equals(newSeat.categoryCode())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean containsCommittedSeat(Event event) {
        return event.getSectors().stream()
            .flatMap(sector -> sector.getRows().stream())
            .flatMap(row -> row.getSeats().stream())
            .anyMatch(seat -> seat.getStatus() != SeatStatus.AVAILABLE);
    }

    private EventSummary toSummary(Event event) {
        long lowestPrice = event.getCategories().stream()
            .filter(category -> getConfiguredQuantity(event, category) > 0)
            .mapToLong(TicketCategory::getPriceInCents)
            .min()
            .orElse(0);
        return new EventSummary(
            event.getId(),
            event.getTitle(),
            event.getImageUrl(),
            toOffsetDateTime(event.getStartsAt()),
            event.getCity(),
            event.getStateCode(),
            lowestPrice,
            EventStatusCode.valueOf(event.getStatus().name())
        );
    }

    private EventView toResponse(Event event) {
        boolean available = event.getStatus() == EventStatus.PUBLISHED;
        List<EventView.Category> categories = event.getCategories().stream()
            .map(category -> {
                int configured = getConfiguredQuantity(event, category);
                int availableQuantity = available
                    ? category.getAdmissionMode() == AdmissionMode.GENERAL_ADMISSION
                        ? category.getAvailableQuantity()
                        : availableSeatCount(event, category)
                    : 0;
                return new EventView.Category(
                    category.getId(),
                    category.getCode(),
                    category.getName(),
                    category.getPriceInCents(),
                    AdmissionModeCode.valueOf(category.getAdmissionMode().name()),
                    configured,
                    availableQuantity,
                    category.getSoldQuantity()
                );
            })
            .toList();
        List<EventView.Sector> sectors = event.getSectors().stream()
            .sorted(java.util.Comparator.comparingInt(SeatSector::getPositionIndex))
            .map(sector -> new EventView.Sector(
                sector.getId(),
                sector.getName(),
                sector.getPositionIndex(),
                sector.getRows().stream()
                    .sorted(java.util.Comparator.comparingInt(SeatRow::getPositionIndex))
                    .map(row -> new EventView.Row(
                        row.getId(),
                        row.getLabel(),
                        row.getPositionIndex(),
                        row.getSeats().stream()
                            .sorted(java.util.Comparator.comparingInt(EventSeat::getPositionIndex))
                            .map(seat -> new EventView.Seat(
                                seat.getId(),
                                seat.getLabel(),
                                seat.getPositionIndex(),
                                seat.getCategory().getCode(),
                                seat.getCategory().getName(),
                                seat.getCategory().getPriceInCents(),
                                SeatStatusCode.valueOf((available ? seat.getStatus() : SeatStatus.UNAVAILABLE).name())
                            ))
                            .toList()
                    ))
                    .toList()
            ))
            .toList();

        return new EventView(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getImageUrl(),
            toOffsetDateTime(event.getStartsAt()),
            EventService.EVENT_TIME_ZONE.getId(),
            new EventView.Venue(
                event.getVenueName(), event.getStreetAddress(), event.getCity(), event.getStateCode()
            ),
            event.getAgeClassification(),
            event.getOrganizer(),
            EventStatusCode.valueOf(event.getStatus().name()),
            categories,
            sectors
        );
    }

    private int getConfiguredQuantity(Event event, TicketCategory category) {
        if (category.getAdmissionMode() == AdmissionMode.GENERAL_ADMISSION) {
            return category.getConfiguredQuantity() == null ? 0 : category.getConfiguredQuantity();
        }
        return (int) event.getSectors().stream()
            .flatMap(sector -> sector.getRows().stream())
            .flatMap(row -> row.getSeats().stream())
            .filter(seat -> seat.getCategory().getId().equals(category.getId()))
            .count();
    }

    private int availableSeatCount(Event event, TicketCategory category) {
        return (int) event.getSectors().stream()
            .flatMap(sector -> sector.getRows().stream())
            .flatMap(row -> row.getSeats().stream())
            .filter(seat -> seat.getCategory().getId().equals(category.getId()))
            .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
            .count();
    }

    private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
        return instant.atZone(EventService.EVENT_TIME_ZONE).toOffsetDateTime();
    }

    private Pageable toPageable(PageQuery query) {
        Sort sort = Sort.by(query.sort().stream()
            .map(order -> new Sort.Order(
                order.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                order.property()
            ))
            .toList());
        return PageRequest.of(query.pageNumber(), query.pageSize(), sort);
    }

    private PageResult<EventSummary> toPageResult(Page<EventSummary> page, PageQuery query) {
        return new PageResult<>(
            page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), query.sort()
        );
    }
}