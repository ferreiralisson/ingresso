package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.EventService;
import org.example.ingresso.ingresso.service.EntryValidationService;
import org.example.ingresso.ingresso.service.EventStaffService;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryCorrectionCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineManifestView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineScanCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.example.ingresso.ingresso.model.EntryCheckIn;
import org.example.ingresso.ingresso.model.EntryCheckInCorrection;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.IssuedTicket;
import org.example.ingresso.ingresso.model.OfflineEntryManifest;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.EntryCheckInOutcome;
import org.example.ingresso.ingresso.model.enums.EntryCheckInSource;
import org.example.ingresso.ingresso.model.enums.EventStatus;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EntryCheckInCorrectionRepository;
import org.example.ingresso.ingresso.repository.EntryCheckInRepository;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.OfflineEntryManifestRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HexFormat;

@Service
public class EntryValidationServiceImpl implements EntryValidationService {

    private static final ZoneId EVENT_ZONE = EventService.EVENT_TIME_ZONE;
    private static final Duration OFFLINE_MANIFEST_LIFETIME = Duration.ofHours(24);

    private final EventRepository eventRepository;
    private final IssuedTicketRepository ticketRepository;
    private final EntryCheckInRepository checkInRepository;
    private final EntryCheckInCorrectionRepository correctionRepository;
    private final UsuarioRepository usuarioRepository;
    private final OfflineEntryManifestRepository manifestRepository;
    private final EventStaffService eventStaffService;

    public EntryValidationServiceImpl(
        EventRepository eventRepository,
        IssuedTicketRepository ticketRepository,
        EntryCheckInRepository checkInRepository,
        EntryCheckInCorrectionRepository correctionRepository,
        UsuarioRepository usuarioRepository,
        OfflineEntryManifestRepository manifestRepository,
        EventStaffService eventStaffService
    ) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.checkInRepository = checkInRepository;
        this.correctionRepository = correctionRepository;
        this.usuarioRepository = usuarioRepository;
        this.manifestRepository = manifestRepository;
        this.eventStaffService = eventStaffService;
    }

    @Transactional
    public EntryValidationView validateOnline(Long eventId, EntryValidationCommand request, Long operatorId) {
        Usuario operator = findUser(operatorId);
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        authorizeEventOperator(event, operator);
        var repeated = checkInRepository.findByOperator_IdAndClientScanId(operator.getId(), request.scanId());
        if (repeated.isPresent()) {
            if (!repeated.get().getEvent().getId().equals(eventId)) {
                throw new IllegalArgumentException("scanId ja utilizado em outro evento");
            }
            return response(repeated.get());
        }

        Instant now = Instant.now();
        String qrHash = hash(request.qrCodeValue());
        var ticketOptional = ticketRepository.findWithLockByQrToken(request.qrCodeValue());
        if (ticketOptional.isEmpty()) {
            return record(event, null, operator, request.scanId(), qrHash, EntryCheckInOutcome.INVALID, now);
        }
        IssuedTicket ticket = ticketOptional.get();
        if (!ticket.getOrder().getEvent().getId().equals(eventId)) {
            return record(event, null, operator, request.scanId(), qrHash, EntryCheckInOutcome.WRONG_EVENT, now);
        }
        if (ticket.getOrder().getStatus() != OrderStatus.PAID) {
            return record(event, ticket, operator, request.scanId(), qrHash, EntryCheckInOutcome.INVALID, now);
        }
        if (ticket.isRefunded()) {
            return record(event, ticket, operator, request.scanId(), qrHash, EntryCheckInOutcome.REFUNDED, now);
        }
        EntryCheckInOutcome availability = eventAvailability(event, now);
        if (availability != null) {
            return record(event, ticket, operator, request.scanId(), qrHash, availability, now);
        }
        if (ticket.isUsed()) {
            EntryCheckIn duplicate = checkInRepository.save(new EntryCheckIn(
                event, ticket, operator, request.scanId(), qrHash,
                EntryCheckInSource.ONLINE, EntryCheckInOutcome.ALREADY_USED, null, null, now
            ));
            return response(duplicate);
        }

        ticket.consume(operator, now);
        EntryCheckIn accepted = checkInRepository.save(new EntryCheckIn(
            event, ticket, operator, request.scanId(), qrHash,
            EntryCheckInSource.ONLINE, EntryCheckInOutcome.ACCEPTED, null, null, now
        ));
        return response(accepted);
    }

    @Transactional(readOnly = true)
    public PageResult<EntryValidationView> history(Long eventId, Long operatorId, PageQuery query) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("Evento nao encontrado");
        }
        Usuario operator = findUser(operatorId);
        if (!operator.getPerfis().contains(UsuarioPerfil.ADMIN)) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Somente ADMIN pode consultar a auditoria de entrada"
            );
        }
        Pageable pageable = toPageable(query);
        Page<EntryValidationView> page = checkInRepository
            .findByEvent_IdOrderByProcessedAtDesc(eventId, pageable).map(this::response);
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), query.sort());
    }

    @Transactional
    public OfflineManifestView provisionOffline(Long eventId, Long operatorId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario operator = findUser(operatorId);
        authorizeEventOperator(event, operator);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("Somente eventos publicados podem ser preparados offline");
        }

        Instant now = Instant.now();
        List<IssuedTicket> tickets = ticketRepository.findByOrder_Event_IdOrderByIdAsc(eventId);
        Set<String> hashes = tickets.stream().map(ticket -> hash(ticket.getQrToken())).collect(Collectors.toSet());
        OfflineEntryManifest manifest = manifestRepository.save(new OfflineEntryManifest(
            UUID.randomUUID().toString(), event, operator, now, now.plus(OFFLINE_MANIFEST_LIFETIME), hashes
        ));
        return manifestResponse(manifest, tickets);
    }

    @Transactional
    public OfflineSyncView synchronizeOffline(Long eventId, OfflineSyncCommand request, Long operatorId) {
        Usuario operator = findUser(operatorId);
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        OfflineEntryManifest manifest = manifestRepository.findById(request.manifestId())
            .orElseThrow(() -> new IllegalArgumentException("Manifesto offline nao encontrado"));
        if (!manifest.getEvent().getId().equals(eventId) || !manifest.getOperator().getId().equals(operator.getId())) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Manifesto offline nao pertence ao operador ou evento"
            );
        }

        Instant synchronizedAt = Instant.now();
        boolean operatorAuthorized = canOperateEvent(event, operator);
        List<EntryValidationView> results = request.scans().stream()
            .map(scan -> operatorAuthorized
                ? synchronizeScan(event, operator, manifest, request.deviceId(), scan, synchronizedAt)
                : recordOffline(event, null, operator, manifest, request.deviceId(), scan, scan.qrTokenHash(),
                    EntryCheckInOutcome.OPERATOR_UNAUTHORIZED, synchronizedAt))
            .toList();
        return new OfflineSyncView(manifest.getId(), synchronizedAt, results);
    }

    private EntryValidationView synchronizeScan(
        Event event,
        Usuario operator,
        OfflineEntryManifest manifest,
        String deviceId,
        OfflineScanCommand scan,
        Instant synchronizedAt
    ) {
        var repeated = checkInRepository.findByOperator_IdAndClientScanId(operator.getId(), scan.scanId());
        if (repeated.isPresent()) {
            if (!repeated.get().getEvent().getId().equals(event.getId())
                || repeated.get().getSource() != EntryCheckInSource.OFFLINE) {
                throw new IllegalArgumentException("scanId ja utilizado em outro fluxo");
            }
            return response(repeated.get());
        }

        String qrHash = scan.qrTokenHash();
        if (scan.scannedAt().isBefore(manifest.getCreatedAt()) || scan.scannedAt().isAfter(manifest.getExpiresAt())) {
            return recordOffline(event, null, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.MANIFEST_EXPIRED, synchronizedAt);
        }
        if (!manifest.getTicketHashes().contains(qrHash)) {
            return recordOffline(event, null, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.NOT_IN_OFFLINE_MANIFEST, synchronizedAt);
        }
        var ticketOptional = ticketRepository.findWithLockByQrTokenHash(scan.qrTokenHash());
        if (ticketOptional.isEmpty()) {
            return recordOffline(event, null, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.INVALID, synchronizedAt);
        }
        IssuedTicket ticket = ticketOptional.get();
        if (!ticket.getOrder().getEvent().getId().equals(event.getId())) {
            return recordOffline(event, null, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.WRONG_EVENT, synchronizedAt);
        }
        if (ticket.getOrder().getStatus() != OrderStatus.PAID) {
            return recordOffline(event, ticket, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.INVALID, synchronizedAt);
        }
        if (ticket.isRefunded()) {
            return recordOffline(event, ticket, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.REFUNDED, synchronizedAt);
        }
        EntryCheckInOutcome availability = eventAvailability(event, scan.scannedAt());
        if (availability != null) {
            return recordOffline(event, ticket, operator, manifest, deviceId, scan, qrHash,
                availability, synchronizedAt);
        }
        if (ticket.isUsed()) {
            return recordOffline(event, ticket, operator, manifest, deviceId, scan, qrHash,
                EntryCheckInOutcome.OFFLINE_CONFLICT, synchronizedAt);
        }

        ticket.consume(operator, synchronizedAt);
        return recordOffline(event, ticket, operator, manifest, deviceId, scan, qrHash,
            EntryCheckInOutcome.ACCEPTED, synchronizedAt);
    }

    private EntryValidationView recordOffline(
        Event event,
        IssuedTicket ticket,
        Usuario operator,
        OfflineEntryManifest manifest,
        String deviceId,
        OfflineScanCommand scan,
        String qrHash,
        EntryCheckInOutcome outcome,
        Instant synchronizedAt
    ) {
        return response(checkInRepository.save(new EntryCheckIn(
            event, ticket, operator, scan.scanId(), qrHash, EntryCheckInSource.OFFLINE, outcome,
            scan.scannedAt(), synchronizedAt, synchronizedAt, deviceId, manifest
        )));
    }

    private OfflineManifestView manifestResponse(OfflineEntryManifest manifest, List<IssuedTicket> tickets) {
        Event event = manifest.getEvent();
        return new OfflineManifestView(
            manifest.getId(), event.getId(), manifest.getOperator().getEmail(), event.getTitle(), event.getStartsAt(),
            manifest.getCreatedAt(), manifest.getExpiresAt(),
            tickets.stream().map(ticket -> new OfflineManifestView.Ticket(
                ticket.getId(), ticket.getQrTokenHash(), ticket.isUsed(), ticket.getOrderItem().getCategoryNameSnapshot(),
                ticket.getUnitNumber(), ticket.getSector(), ticket.getRow(), ticket.getSeatLabel()
            )).toList()
        );
    }

    @Transactional
    public void correct(Long checkInId, EntryCorrectionCommand request, Long administratorId) {
        Usuario admin = findUser(administratorId);
        if (!admin.getPerfis().contains(UsuarioPerfil.ADMIN)) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Somente ADMIN pode corrigir check-ins"
            );
        }
        EntryCheckIn checkIn = checkInRepository.findById(checkInId)
            .orElseThrow(() -> new IllegalArgumentException("Registro de entrada nao encontrado"));
        IssuedTicket ticket = checkIn.getTicket();
        if (checkIn.getOutcome() != EntryCheckInOutcome.ACCEPTED || ticket == null || !ticket.isUsed()) {
            throw new IllegalArgumentException("Somente um check-in aceito e ativo pode ser corrigido");
        }
        ticket = ticketRepository.findWithLockById(ticket.getId())
            .orElseThrow(() -> new IllegalArgumentException("Ingresso nao encontrado"));
        if (correctionRepository.existsByTicket_Id(ticket.getId()) || ticket.isCorrectionUsed()) {
            throw new IllegalArgumentException("O ingresso ja teve uma correcao");
        }
        ticket.correctUsage();
        correctionRepository.save(new EntryCheckInCorrection(
            checkIn, ticket, admin, request.reason().trim(), Instant.now()
        ));
    }

    private EntryValidationView record(
        Event event,
        IssuedTicket ticket,
        Usuario operator,
        String scanId,
        String qrHash,
        EntryCheckInOutcome outcome,
        Instant now
    ) {
        return response(checkInRepository.save(new EntryCheckIn(
            event, ticket, operator, scanId, qrHash, EntryCheckInSource.ONLINE, outcome, null, null, now
        )));
    }

    private EntryValidationView response(EntryCheckIn checkIn) {
        IssuedTicket ticket = checkIn.getTicket();
        boolean alreadyUsed = checkIn.getOutcome() == EntryCheckInOutcome.ALREADY_USED;
        var correction = ticket == null ? null : correctionRepository.findByTicket_Id(ticket.getId()).orElse(null);
        return new EntryValidationView(
            checkIn.getOutcome().name(),
            message(checkIn.getOutcome()),
            ticket == null ? null : ticket.getId(),
            checkIn.getId(),
            checkIn.getProcessedAt(),
            alreadyUsed ? ticket.getUsedAt() : null,
            alreadyUsed && ticket.getUsedBy() != null ? ticket.getUsedBy().getEmail() : null,
            checkIn.getSource().name(),
            checkIn.getDeviceScannedAt(),
            checkIn.getSynchronizedAt(),
            checkIn.getOperator().getEmail(),
            correction == null ? null : correction.getCorrectedAt(),
            correction == null ? null : correction.getReason(),
            correction == null ? null : correction.getAdministrator().getEmail()
        );
    }

    private EntryCheckInOutcome eventAvailability(Event event, Instant now) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return EntryCheckInOutcome.EVENT_UNAVAILABLE;
        }
        LocalDate eventDate = event.getStartsAt().atZone(EVENT_ZONE).toLocalDate();
        if (!eventDate.equals(now.atZone(EVENT_ZONE).toLocalDate())) {
            return EntryCheckInOutcome.OUTSIDE_EVENT_DAY;
        }
        return null;
    }

    private String message(EntryCheckInOutcome outcome) {
        return switch (outcome) {
            case ACCEPTED -> "Entrada validada";
            case ALREADY_USED -> "Ingresso ja utilizado";
            case WRONG_EVENT -> "Ingresso pertence a outro evento";
            case EVENT_UNAVAILABLE -> "Evento suspenso ou cancelado";
            case OUTSIDE_EVENT_DAY -> "Validacao permitida somente no dia do evento";
            case OFFLINE_CONFLICT -> "Conflito de validacao offline";
            case NOT_IN_OFFLINE_MANIFEST -> "Ingresso nao consta nos dados offline do evento";
            case MANIFEST_EXPIRED -> "Dados offline expirados; sincronize o evento novamente";
            case REFUNDED -> "Ingresso reembolsado";
            case OPERATOR_UNAUTHORIZED -> "Operador sem permissao atual para este evento";
            case INVALID -> "Ingresso invalido";
        };
    }

    private Usuario findUser(Long userId) {
        return usuarioRepository.findWithPerfisById(userId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Operador autenticado nao encontrado"
            ));
    }

    private void authorizeEventOperator(Event event, Usuario operator) {
        if (!canOperateEvent(event, operator)) {
            throw new ApplicationServiceException(
                ApplicationServiceException.Code.FORBIDDEN, "Operador sem permissao para este evento"
            );
        }
    }

    private boolean canOperateEvent(Event event, Usuario operator) {
        if (operator.getPerfis().contains(UsuarioPerfil.ADMIN)) {
            return true;
        }
        if (operator.getPerfis().contains(UsuarioPerfil.PRODUCER)
            && event.getProducer().getId().equals(operator.getId())) {
            return true;
        }
        return eventStaffService.hasAccess(event.getId(), operator.getId());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
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
}