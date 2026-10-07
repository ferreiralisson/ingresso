package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.CreateEventRequest;
import org.example.ingresso.ingresso.dto.EventResponse;
import org.example.ingresso.ingresso.dto.EventSummaryResponse;
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
import org.example.ingresso.ingresso.security.UserSS;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EventService {

    public static final ZoneId EVENT_TIME_ZONE = ZoneId.of("America/Sao_Paulo");

    private final EventRepository eventRepository;
    private final UsuarioRepository usuarioRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final TicketRefundService ticketRefundService;

    public EventService(
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
    public Page<EventSummaryResponse> listPublic(Pageable pageable) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public EventResponse getPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (event.getStatus() == EventStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> listProducerEvents(UserSS producer, Pageable pageable) {
        return eventRepository.findByProducer_Id(producer.getId(), pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> listAllForAdmin(Pageable pageable) {
        return eventRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public EventResponse getProducerEvent(Long eventId, UserSS producer) {
        return toResponse(findOwnedEvent(eventId, producer.getId()));
    }

    @Transactional(readOnly = true)
    public EventResponse getAdminEvent(Long eventId) {
        return toResponse(eventRepository.findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Transactional
    public EventResponse create(CreateEventRequest request, UserSS principal) {
        validateEventRequest(request);
        Usuario producer = findProducer(principal.getId());
        Event event = new Event(
            producer,
            request.title().trim(),
            request.description().trim(),
            validateImageUrl(request.imageUrl()),
            request.startsAt().atZone(EVENT_TIME_ZONE).toInstant(),
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
    public EventResponse update(Long eventId, CreateEventRequest request, UserSS principal) {
        validateEventRequest(request);
        Event event = findOwnedEvent(eventId, principal.getId());
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Evento cancelado nao pode ser alterado");
        }

        Map<String, CreateEventRequest.TicketCategoryInput> categoryInputs = categoryInputMap(request.categories());
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
            CreateEventRequest.TicketCategoryInput update = categoryInputs.get(existing.getCode());
            if (update == null) {
                if (existing.getSoldQuantity() > 0 || existing.getReservedQuantity() > 0) {
                    throw new IllegalArgumentException("Categoria com ingressos vendidos ou reservados nao pode ser removida");
                }
                event.removeCategory(existing);
                continue;
            }
            validateCategory(update);
            if (existing.getAdmissionMode() != update.admissionMode()
                && (existing.getSoldQuantity() > 0 || existing.getReservedQuantity() > 0)) {
                throw new IllegalArgumentException("Modalidade de categoria vendida ou reservada nao pode ser alterada");
            }
            if (update.admissionMode() == AdmissionMode.GENERAL_ADMISSION
                && update.quantity() < existing.getSoldQuantity() + existing.getReservedQuantity()) {
                throw new IllegalArgumentException("Quantidade nao pode ficar abaixo dos ingressos vendidos ou reservados");
            }
            existing.update(update.name().trim(), update.priceInCents(), update.admissionMode(), update.quantity());
        }

        Map<String, TicketCategory> categories = new LinkedHashMap<>();
        for (CreateEventRequest.TicketCategoryInput input : request.categories()) {
            TicketCategory category = existingCategories.get(input.code());
            if (category == null) {
                category = new TicketCategory(
                    input.code(), input.name().trim(), input.priceInCents(), input.admissionMode(), input.quantity()
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
            request.startsAt().atZone(EVENT_TIME_ZONE).toInstant(),
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
    public void cancel(Long eventId, UserSS principal) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario actor = usuarioRepository.findWithPerfisById(principal.getId())
            .orElseThrow(() -> new AccessDeniedException("Usuario autenticado nao encontrado"));
        boolean admin = actor.getPerfis().contains(UsuarioPerfil.ADMIN);
        boolean owner = actor.getPerfis().contains(UsuarioPerfil.PRODUCER)
            && event.getProducer().getId().equals(actor.getId());
        if (!admin && !owner) {
            throw new AccessDeniedException("Somente o produtor dono ou ADMIN pode cancelar o evento");
        }
        event.cancel();
        purchaseOrderService.cancelPendingForEvent(eventId);
        ticketRefundService.refundUnusedTicketsForEvent(event, actor);
    }

    @Transactional
    public EventResponse reactivate(Long eventId, UserSS producer) {
        Event event = findOwnedEvent(eventId, producer.getId());
        event.reactivate();
        return toResponse(event);
    }

    private Usuario findProducer(Long producerId) {
        Usuario producer = usuarioRepository.findWithPerfisById(producerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!producer.getPerfis().contains(UsuarioPerfil.PRODUCER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return producer;
    }

    private Event findOwnedEvent(Long eventId, Long producerId) {
        return eventRepository.findByIdAndProducer_Id(eventId, producerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateEventRequest(CreateEventRequest request) {
        validateImageUrl(request.imageUrl());
        Map<String, CreateEventRequest.TicketCategoryInput> categories = categoryInputMap(request.categories());
        Map<String, Integer> assignedSeatCounts = new HashMap<>();
        Set<String> sectorNames = new HashSet<>();
        boolean hasAssignedCategory = false;

        for (CreateEventRequest.TicketCategoryInput category : categories.values()) {
            validateCategory(category);
            if (category.admissionMode() == AdmissionMode.ASSIGNED_SEAT) {
                hasAssignedCategory = true;
            }
        }

        List<CreateEventRequest.SeatSectorInput> sectors = request.sectors() == null ? List.of() : request.sectors();
        for (CreateEventRequest.SeatSectorInput sector : sectors) {
            if (!sectorNames.add(sector.name().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Nome de setor duplicado");
            }
            Set<String> rowLabels = new HashSet<>();
            for (CreateEventRequest.SeatRowInput row : sector.rows()) {
                if (!rowLabels.add(row.label().trim().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Fileira duplicada no setor");
                }
                Set<String> seatLabels = new HashSet<>();
                for (CreateEventRequest.SeatInput seat : row.seats()) {
                    if (!seatLabels.add(seat.label().trim().toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("Assento duplicado na fileira");
                    }
                    CreateEventRequest.TicketCategoryInput category = categories.get(seat.categoryCode());
                    if (category == null || category.admissionMode() != AdmissionMode.ASSIGNED_SEAT) {
                        throw new IllegalArgumentException("Assento deve pertencer a uma categoria com lugar marcado");
                    }
                    assignedSeatCounts.merge(category.code(), 1, Integer::sum);
                }
            }
        }

        if (hasAssignedCategory != !sectors.isEmpty()) {
            throw new IllegalArgumentException("Categorias com lugar marcado precisam de mapa; categorias gerais nao usam mapa");
        }
        for (CreateEventRequest.TicketCategoryInput category : categories.values()) {
            if (category.admissionMode() == AdmissionMode.ASSIGNED_SEAT
                && assignedSeatCounts.getOrDefault(category.code(), 0) == 0) {
                throw new IllegalArgumentException("Cada categoria com lugar marcado precisa ter assentos");
            }
        }
    }

    private Map<String, CreateEventRequest.TicketCategoryInput> categoryInputMap(
        List<CreateEventRequest.TicketCategoryInput> inputs
    ) {
        Map<String, CreateEventRequest.TicketCategoryInput> categories = new LinkedHashMap<>();
        for (CreateEventRequest.TicketCategoryInput input : inputs) {
            if (categories.putIfAbsent(input.code(), input) != null) {
                throw new IllegalArgumentException("Codigo de categoria duplicado");
            }
        }
        return categories;
    }

    private void validateCategory(CreateEventRequest.TicketCategoryInput category) {
        if (category.priceInCents() < 0) {
            throw new IllegalArgumentException("Preco nao pode ser negativo");
        }
        if (category.admissionMode() == AdmissionMode.GENERAL_ADMISSION
            && (category.quantity() == null || category.quantity() < 1)) {
            throw new IllegalArgumentException("Categoria geral precisa de quantidade positiva");
        }
        if (category.admissionMode() == AdmissionMode.ASSIGNED_SEAT && category.quantity() != null) {
            throw new IllegalArgumentException("Quantidade de lugares marcados e derivada do mapa");
        }
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
        List<CreateEventRequest.TicketCategoryInput> inputs
    ) {
        Map<String, TicketCategory> categories = new LinkedHashMap<>();
        for (CreateEventRequest.TicketCategoryInput input : inputs) {
            TicketCategory category = new TicketCategory(
                input.code(), input.name().trim(), input.priceInCents(), input.admissionMode(), input.quantity()
            );
            event.addCategory(category);
            categories.put(input.code(), category);
        }
        return categories;
    }

    private void addSeatMap(
        Event event,
        List<CreateEventRequest.SeatSectorInput> inputs,
        Map<String, TicketCategory> categories
    ) {
        if (inputs == null) {
            return;
        }
        for (CreateEventRequest.SeatSectorInput sectorInput : inputs) {
            SeatSector sector = new SeatSector(sectorInput.name().trim(), sectorInput.positionIndex());
            for (CreateEventRequest.SeatRowInput rowInput : sectorInput.rows()) {
                SeatRow row = new SeatRow(rowInput.label().trim(), rowInput.positionIndex());
                for (CreateEventRequest.SeatInput seatInput : rowInput.seats()) {
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

    private boolean hasSameSeatMap(Event event, List<CreateEventRequest.SeatSectorInput> inputs) {
        List<CreateEventRequest.SeatSectorInput> requested = inputs == null ? List.of() : inputs;
        List<SeatSector> existing = event.getSectors();
        if (existing.size() != requested.size()) {
            return false;
        }
        for (int sectorIndex = 0; sectorIndex < existing.size(); sectorIndex++) {
            SeatSector oldSector = existing.get(sectorIndex);
            CreateEventRequest.SeatSectorInput newSector = requested.get(sectorIndex);
            if (!oldSector.getName().equals(newSector.name())
                || oldSector.getPositionIndex() != newSector.positionIndex()
                || oldSector.getRows().size() != newSector.rows().size()) {
                return false;
            }
            for (int rowIndex = 0; rowIndex < oldSector.getRows().size(); rowIndex++) {
                SeatRow oldRow = oldSector.getRows().get(rowIndex);
                CreateEventRequest.SeatRowInput newRow = newSector.rows().get(rowIndex);
                if (!oldRow.getLabel().equals(newRow.label())
                    || oldRow.getPositionIndex() != newRow.positionIndex()
                    || oldRow.getSeats().size() != newRow.seats().size()) {
                    return false;
                }
                for (int seatIndex = 0; seatIndex < oldRow.getSeats().size(); seatIndex++) {
                    EventSeat oldSeat = oldRow.getSeats().get(seatIndex);
                    CreateEventRequest.SeatInput newSeat = newRow.seats().get(seatIndex);
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

    private EventSummaryResponse toSummary(Event event) {
        long lowestPrice = event.getCategories().stream()
            .filter(category -> getConfiguredQuantity(event, category) > 0)
            .mapToLong(TicketCategory::getPriceInCents)
            .min()
            .orElse(0);
        return new EventSummaryResponse(
            event.getId(),
            event.getTitle(),
            event.getImageUrl(),
            toOffsetDateTime(event.getStartsAt()),
            event.getCity(),
            event.getStateCode(),
            lowestPrice,
            event.getStatus()
        );
    }

    private EventResponse toResponse(Event event) {
        boolean available = event.getStatus() == EventStatus.PUBLISHED;
        List<EventResponse.Category> categories = event.getCategories().stream()
            .map(category -> {
                int configured = getConfiguredQuantity(event, category);
                int availableQuantity = available
                    ? category.getAdmissionMode() == AdmissionMode.GENERAL_ADMISSION
                        ? category.getAvailableQuantity()
                        : availableSeatCount(event, category)
                    : 0;
                return new EventResponse.Category(
                    category.getId(),
                    category.getCode(),
                    category.getName(),
                    category.getPriceInCents(),
                    category.getAdmissionMode(),
                    configured,
                    availableQuantity,
                    category.getSoldQuantity()
                );
            })
            .toList();
        List<EventResponse.Sector> sectors = event.getSectors().stream()
            .sorted(java.util.Comparator.comparingInt(SeatSector::getPositionIndex))
            .map(sector -> new EventResponse.Sector(
                sector.getId(),
                sector.getName(),
                sector.getPositionIndex(),
                sector.getRows().stream()
                    .sorted(java.util.Comparator.comparingInt(SeatRow::getPositionIndex))
                    .map(row -> new EventResponse.Row(
                        row.getId(),
                        row.getLabel(),
                        row.getPositionIndex(),
                        row.getSeats().stream()
                            .sorted(java.util.Comparator.comparingInt(EventSeat::getPositionIndex))
                            .map(seat -> new EventResponse.Seat(
                                seat.getId(),
                                seat.getLabel(),
                                seat.getPositionIndex(),
                                seat.getCategory().getCode(),
                                seat.getCategory().getName(),
                                seat.getCategory().getPriceInCents(),
                                available ? seat.getStatus() : SeatStatus.UNAVAILABLE
                            ))
                            .toList()
                    ))
                    .toList()
            ))
            .toList();

        return new EventResponse(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getImageUrl(),
            toOffsetDateTime(event.getStartsAt()),
            EVENT_TIME_ZONE.getId(),
            new EventResponse.Venue(
                event.getVenueName(), event.getStreetAddress(), event.getCity(), event.getStateCode()
            ),
            event.getAgeClassification(),
            event.getOrganizer(),
            event.getStatus(),
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
        return instant.atZone(EVENT_TIME_ZONE).toOffsetDateTime();
    }
}