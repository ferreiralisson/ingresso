package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.CreateOrderRequest;
import org.example.ingresso.ingresso.dto.OrderResponse;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.EventSeat;
import org.example.ingresso.ingresso.model.OrderItem;
import org.example.ingresso.ingresso.model.PaymentAttempt;
import org.example.ingresso.ingresso.model.PurchaseOrder;
import org.example.ingresso.ingresso.model.TicketCategory;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.AdmissionMode;
import org.example.ingresso.ingresso.model.enums.EventStatus;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
import org.example.ingresso.ingresso.model.enums.SeatStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.EventSeatRepository;
import org.example.ingresso.ingresso.repository.PurchaseOrderRepository;
import org.example.ingresso.ingresso.repository.TicketCategoryRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.UserSS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private static final Duration MAX_RESERVATION = Duration.ofMinutes(15);
    private static final int MAX_TICKETS_PER_CATEGORY = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EventRepository eventRepository;
    private final UsuarioRepository usuarioRepository;
    private final TicketCategoryRepository categoryRepository;
    private final EventSeatRepository seatRepository;
    private final PurchaseOrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final TicketDeliveryService ticketDeliveryService;

    public PurchaseOrderService(
        EventRepository eventRepository,
        UsuarioRepository usuarioRepository,
        TicketCategoryRepository categoryRepository,
        EventSeatRepository seatRepository,
        PurchaseOrderRepository orderRepository,
        PaymentGateway paymentGateway,
        TicketDeliveryService ticketDeliveryService
    ) {
        this.eventRepository = eventRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoryRepository = categoryRepository;
        this.seatRepository = seatRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.ticketDeliveryService = ticketDeliveryService;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String idempotencyKey, UserSS principal) {
        validateIdempotencyKey(idempotencyKey);
        validateRequestShape(request);
        expireDueOrders(Instant.now());

        Usuario buyer = usuarioRepository.findWithLockById(principal.getId())
            .orElseThrow(() -> new AccessDeniedException("Comprador autenticado nao encontrado"));
        if (!buyer.getPerfis().contains(UsuarioPerfil.USER)) {
            throw new AccessDeniedException("Apenas compradores podem criar pedidos");
        }

        String payloadHash = hashRequest(request);
        var existing = orderRepository.findByBuyer_IdAndIdempotencyKey(buyer.getId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestPayloadHash().equals(payloadHash)) {
                throw new IllegalArgumentException("Idempotency-Key ja utilizada com outro conteudo");
            }
            return toResponse(existing.get());
        }

        Instant now = Instant.now();
        Event event = eventRepository.findWithLockById(request.eventId())
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("Evento nao esta disponivel para venda");
        }
        if (!event.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("Vendas encerradas para este evento");
        }

        List<Long> categoryIds = request.items().stream()
            .map(CreateOrderRequest.Item::categoryId)
            .sorted()
            .toList();
        List<TicketCategory> lockedCategories = categoryRepository
            .findWithLockByEvent_IdAndIdIn(event.getId(), categoryIds);
        if (lockedCategories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("Uma ou mais categorias nao pertencem ao evento");
        }
        Map<Long, TicketCategory> categories = lockedCategories.stream()
            .collect(Collectors.toMap(TicketCategory::getId, category -> category));

        Set<Long> requestedSeatIds = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            TicketCategory category = categories.get(item.categoryId());
            validateLine(category, item);
            if (item.seatIds() != null) {
                for (Long seatId : item.seatIds()) {
                    if (!requestedSeatIds.add(seatId)) {
                        throw new IllegalArgumentException("Um assento nao pode aparecer mais de uma vez no pedido");
                    }
                }
            }
        }

        List<EventSeat> lockedSeats = requestedSeatIds.isEmpty()
            ? List.of()
            : seatRepository.findWithLockByIdIn(requestedSeatIds.stream().sorted().toList());
        if (lockedSeats.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException("Um ou mais assentos nao existem");
        }
        Map<Long, EventSeat> seats = lockedSeats.stream()
            .collect(Collectors.toMap(EventSeat::getId, seat -> seat));
        validateRequestedSeats(event, request.items(), categories, seats);

        Instant reservationExpiry = now.plus(MAX_RESERVATION).isBefore(event.getStartsAt())
            ? now.plus(MAX_RESERVATION)
            : event.getStartsAt();
        PurchaseOrder order = new PurchaseOrder(
            buyer,
            event,
            idempotencyKey,
            payloadHash,
            now,
            reservationExpiry
        );
        for (CreateOrderRequest.Item item : request.items()) {
            TicketCategory category = categories.get(item.categoryId());
            OrderItem orderItem = new OrderItem(order, category, item.quantity());
            for (Long seatId : item.seatIds() == null ? List.<Long>of() : item.seatIds()) {
                orderItem.addSeatSnapshot(seats.get(seatId));
            }
            order.addItem(orderItem);
        }
        orderRepository.saveAndFlush(order);

        for (int i = 0; i < request.items().size(); i++) {
            CreateOrderRequest.Item input = request.items().get(i);
            TicketCategory category = categories.get(input.categoryId());
            if (category.getAdmissionMode() == AdmissionMode.GENERAL_ADMISSION) {
                category.reserve(input.quantity());
            } else {
                category.reserveAssignedSeats(input.quantity());
                for (Long seatId : input.seatIds()) {
                    seats.get(seatId).reserve(order.getId());
                }
            }
        }

        return toResponse(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse startPayment(Long orderId, UserSS principal) {
        Instant now = Instant.now();
        PurchaseOrder order = orderRepository.findWithLockByIdAndBuyer_Id(orderId, principal.getId())
            .orElseThrow(() -> new IllegalArgumentException("Pedido nao encontrado"));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT && !order.getReservationExpiresAt().isAfter(now)) {
            expireOrder(order, now);
            return toResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            return toResponse(order);
        }
        if (!order.getPaymentAttempts().isEmpty()) {
            return toResponse(order);
        }

        PaymentOutcome outcome = paymentGateway.start(order);
        Instant completedAt = outcome == PaymentOutcome.PENDING ? null : now;
        String providerReference = generateAttemptKey();
        PaymentAttempt attempt = new PaymentAttempt(
            providerReference,
            outcome,
            paymentGateway.providerName(),
            now,
            completedAt
        );
        order.addPaymentAttempt(attempt);
        order.applyPaymentOutcome(outcome, now, providerReference);
        if (outcome != PaymentOutcome.PENDING && order.getStatus() == OrderStatus.PAID) {
            commitInventory(order);
            ticketDeliveryService.issueForPaidOrder(order);
        } else if (outcome == PaymentOutcome.DECLINED || order.getStatus() == OrderStatus.EXPIRED) {
            releaseInventory(order);
        }
        orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public Page<OrderResponse> listMine(UserSS principal, Pageable pageable) {
        expireDueOrders(Instant.now());
        return orderRepository.findByBuyer_Id(principal.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse getMine(Long orderId, UserSS principal) {
        PurchaseOrder order = orderRepository.findWithLockByIdAndBuyer_Id(orderId, principal.getId())
            .orElseThrow(() -> new IllegalArgumentException("Pedido nao encontrado"));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT
            && !order.getReservationExpiresAt().isAfter(Instant.now())) {
            expireOrder(order, Instant.now());
        }
        return toResponse(order);
    }

    @Transactional
    public void cancelPendingForEvent(Long eventId) {
        for (PurchaseOrder order : orderRepository.findByEvent_IdAndStatus(eventId, OrderStatus.PENDING_PAYMENT)) {
            PurchaseOrder locked = orderRepository.findWithLockById(order.getId()).orElseThrow();
            if (locked.getStatus() == OrderStatus.PENDING_PAYMENT) {
                locked.cancelForEvent(Instant.now());
                releaseInventory(locked);
            }
        }
    }

    @Transactional
    public OrderResponse processPaymentResult(Long orderId, String attemptKey, PaymentOutcome outcome) {
        Instant now = Instant.now();
        PurchaseOrder order = orderRepository.findWithLockById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Pedido nao encontrado"));
        PaymentAttempt attempt = order.getPaymentAttempts().stream()
            .filter(current -> current.getAttemptKey().equals(attemptKey))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Tentativa de pagamento nao encontrada"));

        if (attempt.getCompletedAt() != null) {
            if (attempt.getOutcome() != outcome) {
                throw new IllegalArgumentException("Resultado conflitante para pagamento ja finalizado");
            }
            return toResponse(order);
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || !order.getReservationExpiresAt().isAfter(now)) {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                expireOrder(order, now);
            }
            attempt.complete(outcome, now);
            return toResponse(order);
        }

        if (outcome == PaymentOutcome.PENDING) {
            return toResponse(order);
        }
        order.applyPaymentOutcome(outcome, now, attemptKey);
        attempt.complete(outcome, now);
        if (outcome == PaymentOutcome.APPROVED && order.getStatus() == OrderStatus.PAID) {
            commitInventory(order);
            ticketDeliveryService.issueForPaidOrder(order);
        } else {
            releaseInventory(order);
        }
        return toResponse(order);
    }

    @Scheduled(fixedDelayString = "${ingresso.orders.expiration-scan-ms:5000}")
    @Transactional
    public void expireScheduledOrders() {
        expireDueOrders(Instant.now());
    }

    private void expireDueOrders(Instant now) {
        List<PurchaseOrder> candidates = orderRepository
            .findTop100ByStatusAndReservationExpiresAtLessThanEqualOrderByIdAsc(OrderStatus.PENDING_PAYMENT, now);
        for (PurchaseOrder candidate : candidates) {
            PurchaseOrder order = orderRepository.findWithLockById(candidate.getId()).orElse(null);
            if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT
                && !order.getReservationExpiresAt().isAfter(now)) {
                expireOrder(order, now);
            }
        }
    }

    private void expireOrder(PurchaseOrder order, Instant now) {
        order.expire(now);
        releaseInventory(order);
        orderRepository.save(order);
    }

    private void releaseInventory(PurchaseOrder order) {
        List<Long> categoryIds = order.getItems().stream()
            .map(item -> item.getCategory().getId())
            .distinct()
            .sorted()
            .toList();
        Map<Long, TicketCategory> categories = categoryRepository
            .findWithLockByEvent_IdAndIdIn(order.getEvent().getId(), categoryIds).stream()
            .collect(Collectors.toMap(TicketCategory::getId, category -> category));
        List<Long> seatIds = order.getItems().stream()
            .flatMap(item -> item.getSeats().stream())
            .map(seat -> seat.getEventSeatId())
            .sorted()
            .toList();
        Map<Long, EventSeat> seats = seatIds.isEmpty()
            ? Map.of()
            : seatRepository.findWithLockByIdIn(seatIds).stream()
                .collect(Collectors.toMap(EventSeat::getId, seat -> seat));

        for (OrderItem item : order.getItems()) {
            TicketCategory category = categories.get(item.getCategory().getId());
            category.releaseReservation(item.getQuantity());
            for (var seatSnapshot : item.getSeats()) {
                EventSeat seat = seats.get(seatSnapshot.getEventSeatId());
                if (seat != null && seat.getStatus() == SeatStatus.RESERVED
                    && java.util.Objects.equals(seat.getReservedOrderId(), order.getId())) {
                    seat.release(order.getId());
                }
            }
        }
    }

    private void commitInventory(PurchaseOrder order) {
        List<Long> categoryIds = order.getItems().stream()
            .map(item -> item.getCategory().getId())
            .distinct()
            .sorted()
            .toList();
        Map<Long, TicketCategory> categories = categoryRepository
            .findWithLockByEvent_IdAndIdIn(order.getEvent().getId(), categoryIds).stream()
            .collect(Collectors.toMap(TicketCategory::getId, category -> category));
        List<Long> seatIds = order.getItems().stream()
            .flatMap(item -> item.getSeats().stream())
            .map(seat -> seat.getEventSeatId())
            .sorted()
            .toList();
        Map<Long, EventSeat> seats = seatIds.isEmpty()
            ? Map.of()
            : seatRepository.findWithLockByIdIn(seatIds).stream()
                .collect(Collectors.toMap(EventSeat::getId, seat -> seat));

        for (OrderItem item : order.getItems()) {
            categories.get(item.getCategory().getId()).commitReservation(item.getQuantity());
            for (var seatSnapshot : item.getSeats()) {
                EventSeat seat = seats.get(seatSnapshot.getEventSeatId());
                if (seat == null) {
                    throw new IllegalStateException("Assento reservado nao encontrado");
                }
                seat.sell(order.getId());
            }
        }
    }

    private void validateRequestShape(CreateOrderRequest request) {
        if (request.items().isEmpty()) {
            throw new IllegalArgumentException("Pedido precisa conter ao menos um item");
        }
        Set<Long> categories = new HashSet<>();
        for (CreateOrderRequest.Item item : request.items()) {
            if (item.quantity() > MAX_TICKETS_PER_CATEGORY) {
                throw new IllegalArgumentException("Limite de 10 ingressos por categoria por pedido");
            }
            if (!categories.add(item.categoryId())) {
                throw new IllegalArgumentException("Cada categoria deve aparecer uma vez no pedido");
            }
            if (item.seatIds() != null && !item.seatIds().isEmpty() && item.seatIds().size() != item.quantity()) {
                throw new IllegalArgumentException("Selecione um assento para cada ingresso marcado");
            }
        }
    }

    private void validateLine(TicketCategory category, CreateOrderRequest.Item item) {
        if (item.quantity() > MAX_TICKETS_PER_CATEGORY) {
            throw new IllegalArgumentException("Limite de 10 ingressos por categoria por pedido");
        }
        boolean hasSeatIds = item.seatIds() != null && !item.seatIds().isEmpty();
        if (category.getAdmissionMode() == AdmissionMode.GENERAL_ADMISSION) {
            if (hasSeatIds || category.getAvailableQuantity() < item.quantity()) {
                throw new IllegalArgumentException("Estoque insuficiente ou assentos indevidos para categoria geral");
            }
        } else if (!hasSeatIds || item.seatIds().size() != item.quantity()) {
            throw new IllegalArgumentException("Selecione um assento para cada ingresso marcado");
        }
    }

    private void validateRequestedSeats(
        Event event,
        List<CreateOrderRequest.Item> items,
        Map<Long, TicketCategory> categories,
        Map<Long, EventSeat> seats
    ) {
        for (CreateOrderRequest.Item item : items) {
            if (item.seatIds() == null) {
                continue;
            }
            TicketCategory category = categories.get(item.categoryId());
            for (Long seatId : item.seatIds()) {
                EventSeat seat = seats.get(seatId);
                if (seat == null || seat.getStatus() != SeatStatus.AVAILABLE
                    || !seat.getCategory().getId().equals(category.getId())
                    || !seat.getRow().getSector().getEvent().getId().equals(event.getId())) {
                    throw new IllegalArgumentException("Assento indisponivel ou nao pertence a categoria/evento");
                }
            }
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key obrigatoria, com no maximo 128 caracteres");
        }
    }

    private String hashRequest(CreateOrderRequest request) {
        String canonical = request.eventId() + ":" + request.items().stream()
            .sorted(Comparator.comparing(CreateOrderRequest.Item::categoryId))
            .map(item -> item.categoryId() + "," + item.quantity() + ","
                + (item.seatIds() == null ? "" : item.seatIds().stream().sorted().map(String::valueOf).collect(Collectors.joining("."))))
            .collect(Collectors.joining(";"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }

    private String generateAttemptKey() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private OrderResponse toResponse(PurchaseOrder order) {
        PaymentAttempt attempt = order.getPaymentAttempts().stream().findFirst().orElse(null);
        return new OrderResponse(
            order.getId(),
            order.getEvent().getId(),
            order.getEvent().getTitle(),
            order.getStatus(),
            order.getTotalInCents(),
            order.getCreatedAt(),
            order.getReservationExpiresAt(),
            order.getPaidAt(),
            order.getItems().stream().map(item -> new OrderResponse.Item(
                item.getCategory().getId(),
                item.getCategoryCodeSnapshot(),
                item.getCategoryNameSnapshot(),
                item.getAdmissionModeSnapshot(),
                item.getUnitPriceInCents(),
                item.getQuantity(),
                item.getSeats().stream().map(seat -> new OrderResponse.Seat(
                    seat.getEventSeatId(), seat.getSectorName(), seat.getRowLabel(), seat.getSeatLabel()
                )).toList()
            )).toList(),
            attempt == null ? null : new OrderResponse.Payment(
                attempt.getAttemptKey(), attempt.getProvider(), attempt.getOutcome(),
                attempt.getStartedAt(), attempt.getCompletedAt()
            ),
            ticketDeliveryService.toResponse(order)
        );
    }
}