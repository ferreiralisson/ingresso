package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.model.TicketCategory;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.OrderStatus;
import org.example.ingresso.ingresso.model.enums.PaymentOutcome;
import org.example.ingresso.ingresso.model.enums.SeatStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EventSeatRepository;
import org.example.ingresso.ingresso.repository.EntryCheckInRepository;
import org.example.ingresso.ingresso.repository.IssuedTicketRepository;
import org.example.ingresso.ingresso.repository.PurchaseOrderRepository;
import org.example.ingresso.ingresso.repository.TicketCategoryRepository;
import org.example.ingresso.ingresso.repository.TicketRefundRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.TokenService;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.MockPaymentGateway;
import org.example.ingresso.ingresso.service.MockEmailGateway;
import org.example.ingresso.ingresso.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.time.LocalDate;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:orders-test;DB_CLOSE_DELAY=-1",
        "token.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
)
@AutoConfigureMockMvc
class PurchaseOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TicketCategoryRepository categoryRepository;

    @Autowired
    private EventSeatRepository seatRepository;

    @Autowired
    private IssuedTicketRepository ticketRepository;

    @Autowired
    private EntryCheckInRepository checkInRepository;

    @Autowired
    private PurchaseOrderRepository orderRepository;

    @Autowired
    private TicketRefundRepository ticketRefundRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MockPaymentGateway paymentGateway;

    @Autowired
    private MockEmailGateway emailGateway;

    @Autowired
    private PurchaseOrderService orderService;

    private String producerToken;
    private String otherProducerToken;
    private String buyerToken;
    private String secondBuyerToken;
    private String administratorToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM entry_check_in_corrections");
        jdbcTemplate.execute("DELETE FROM entry_check_ins");
        jdbcTemplate.execute("DELETE FROM offline_entry_manifest_tickets");
        jdbcTemplate.execute("DELETE FROM offline_entry_manifests");
        jdbcTemplate.execute("DELETE FROM event_staff_invitations");
        jdbcTemplate.execute("DELETE FROM ticket_refunds");
        jdbcTemplate.execute("DELETE FROM issued_tickets");
        jdbcTemplate.execute("DELETE FROM event_seats");
        jdbcTemplate.execute("DELETE FROM order_item_seats");
        jdbcTemplate.execute("DELETE FROM payment_attempts");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM ticket_orders");
        jdbcTemplate.execute("DELETE FROM seat_rows");
        jdbcTemplate.execute("DELETE FROM seat_sectors");
        jdbcTemplate.execute("DELETE FROM ticket_categories");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM permission_audits");
        jdbcTemplate.execute("DELETE FROM producer_invitations");
        jdbcTemplate.execute("DELETE FROM usuario_perfis");
        jdbcTemplate.execute("DELETE FROM usuarios");
        producerToken = createUser("Producer", "producer@example.com", UsuarioPerfil.PRODUCER);
        otherProducerToken = createUser("Other producer", "other-producer@example.com", UsuarioPerfil.PRODUCER);
        buyerToken = createUser("Buyer", "buyer@example.com", UsuarioPerfil.USER);
        secondBuyerToken = createUser("Second buyer", "second@example.com", UsuarioPerfil.USER);
        administratorToken = createUser("Admin", "admin@example.com", UsuarioPerfil.ADMIN);
        paymentGateway.setNextOutcomeForTest(PaymentOutcome.APPROVED);
        emailGateway.clear();
    }

    @Test
    void createsOneEventOrderAndReplaysSameIdempotencyKeyWithoutReservingTwice() throws Exception {
        EventSeed event = createGeneralEvent("Idempotent", 3);
        String key = "same-order-key";
        String body = orderJson(event.id(), event.categoryId(), 2);

        long orderId = createOrder(buyerToken, key, body, status().isCreated());
        assertEquals(orderId, createOrder(buyerToken, key, body, status().isCreated()));

        assertEquals(2, category(event.categoryId()).getReservedQuantity());
        assertEquals(1, orderRepository.count());
        mockMvc.perform(post("/api/pedidos")
                .header("Authorization", "Bearer " + buyerToken)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(event.id(), event.categoryId(), 1)))
            .andExpect(status().isConflict());
        assertEquals(2, category(event.categoryId()).getReservedQuantity());
    }

    @Test
    void approvedMockCommitsInventoryAndOrderHistoryIsPrivate() throws Exception {
        EventSeed event = createGeneralEvent("Approval", 2);
        long orderId = createOrder(buyerToken, "approved-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());

        paymentGateway.setNextOutcomeForTest(PaymentOutcome.APPROVED);
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"PAID\"")));
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());

        TicketCategory category = category(event.categoryId());
        assertEquals(1, category.getSoldQuantity());
        assertEquals(0, category.getReservedQuantity());
        assertEquals(1, orderRepository.count());
        assertEquals(1, ticketRepository.countByOrder_Id(orderId));
        assertEquals(1, emailGateway.getCapturedMessages().size());
        var ticket = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst();
        assertEquals(43, ticket.getQrToken().length());
        var confirmation = emailGateway.getCapturedMessages().getFirst();
        assertEquals("buyer@example.com", confirmation.recipient());
        assertEquals("Approval", confirmation.eventTitle());
        assertEquals(ticket.getQrToken(), confirmation.tickets().getFirst().qrCodeValue());
        assertTrue(resultHasTicket(orderId));

        mockMvc.perform(get("/api/pedidos/{id}", orderId).header("Authorization", "Bearer " + secondBuyerToken))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/api/pedidos").header("Authorization", "Bearer " + secondBuyerToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"totalElements\":0")));
    }

    @Test
    void declinedAndPendingPaymentsReleaseInventoryOnFailureOrExpiry() throws Exception {
        EventSeed event = createGeneralEvent("Decline and expiry", 3);
        long declinedOrder = createOrder(buyerToken, "declined-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        paymentGateway.setNextOutcomeForTest(PaymentOutcome.DECLINED);
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", declinedOrder)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"PAYMENT_DECLINED\"")));
        assertEquals(0, category(event.categoryId()).getReservedQuantity());
        assertEquals(0, category(event.categoryId()).getSoldQuantity());
        assertEquals(0, ticketRepository.count());

        long pendingOrder = createOrder(buyerToken, "pending-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        paymentGateway.setNextOutcomeForTest(PaymentOutcome.PENDING);
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", pendingOrder)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isAccepted());

        jdbcTemplate.update("UPDATE ticket_orders SET reservation_expires_at = DATEADD('SECOND', -1, CURRENT_TIMESTAMP) WHERE id = ?", pendingOrder);
        mockMvc.perform(get("/api/pedidos/{id}", pendingOrder)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"EXPIRED\"")));
        assertEquals(0, category(event.categoryId()).getReservedQuantity());
        assertEquals(0, category(event.categoryId()).getSoldQuantity());
    }

    @Test
    void lateApprovalDoesNotConfirmExpiredOrderAndDuplicateResultIsIdempotent() throws Exception {
        EventSeed event = createGeneralEvent("Late approval", 1);
        long orderId = createOrder(buyerToken, "late-approval", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        paymentGateway.setNextOutcomeForTest(PaymentOutcome.PENDING);
        MvcResult payment = mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isAccepted())
            .andReturn();
        String attemptKey = jsonString(payment.getResponse().getContentAsString(), "attemptKey");
        jdbcTemplate.update("UPDATE ticket_orders SET reservation_expires_at = DATEADD('SECOND', -1, CURRENT_TIMESTAMP) WHERE id = ?", orderId);

        assertEquals(OrderStatus.EXPIRED,
            orderService.processPaymentResult(orderId, attemptKey, PaymentOutcome.APPROVED).status());
        assertEquals(OrderStatus.EXPIRED,
            orderService.processPaymentResult(orderId, attemptKey, PaymentOutcome.APPROVED).status());
        assertEquals(0, category(event.categoryId()).getReservedQuantity());
        assertEquals(0, category(event.categoryId()).getSoldQuantity());
    }

    @Test
    void asynchronousApprovalIssuesTicketsAndEmailOnlyOnce() throws Exception {
        EventSeed event = createGeneralEvent("Async approval", 2);
        long orderId = createOrder(buyerToken, "async-approval-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        paymentGateway.setNextOutcomeForTest(PaymentOutcome.PENDING);
        MvcResult payment = mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isAccepted())
            .andReturn();
        String attemptKey = jsonString(payment.getResponse().getContentAsString(), "attemptKey");

        assertEquals(OrderStatus.PAID,
            orderService.processPaymentResult(orderId, attemptKey, PaymentOutcome.APPROVED).status());
        assertEquals(OrderStatus.PAID,
            orderService.processPaymentResult(orderId, attemptKey, PaymentOutcome.APPROVED).status());
        assertEquals(2, ticketRepository.countByOrder_Id(orderId));
        assertEquals(1, emailGateway.getCapturedMessages().size());
    }

    @Test
    void assignedSeatsAreExclusiveAndEventCancellationReleasesPendingOrders() throws Exception {
        EventSeed event = createAssignedEvent("Exclusive seat");
        String buyerOrderBody = "{\"eventId\":" + event.id() + ",\"items\":[{\"categoryId\":"
            + event.categoryId() + ",\"quantity\":1,\"seatIds\":[" + event.seatId() + "]}]}";
        long orderId = createOrder(buyerToken, "seat-order", buyerOrderBody, status().isCreated());
        assertEquals(SeatStatus.RESERVED, seatRepository.findById(event.seatId()).orElseThrow().getStatus());

        mockMvc.perform(post("/api/pedidos")
                .header("Authorization", "Bearer " + secondBuyerToken)
                .header("Idempotency-Key", "second-seat-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(buyerOrderBody))
            .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/admin/eventos/{id}/suspender", event.id())
            .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());
        assertEquals(OrderStatus.CANCELLED, orderRepository.findById(orderId).orElseThrow().getStatus());
        assertEquals(SeatStatus.AVAILABLE, seatRepository.findById(event.seatId()).orElseThrow().getStatus());
    }

    @Test
    void approvedAssignedOrderIssuesOneTicketForItsSeat() throws Exception {
        EventSeed event = createAssignedEvent("Ticketed seat");
        String body = "{\"eventId\":" + event.id() + ",\"items\":[{\"categoryId\":"
            + event.categoryId() + ",\"quantity\":1,\"seatIds\":[" + event.seatId() + "]}]}";
        long orderId = createOrder(buyerToken, "ticketed-seat-order", body, status().isCreated());

        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());

        var ticket = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst();
        assertEquals(event.seatId(), ticket.getEventSeatId());
        assertEquals("Setor A", ticket.getSector());
        assertEquals("A", ticket.getRow());
        assertEquals("1", ticket.getSeatLabel());
        assertEquals(SeatStatus.SOLD, seatRepository.findById(event.seatId()).orElseThrow().getStatus());
    }

    @Test
    void emailFailureDoesNotRollbackTicketsOrPaidOrder() throws Exception {
        EventSeed event = createGeneralEvent("Email failure", 2);
        long orderId = createOrder(buyerToken, "email-failure-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        emailGateway.failNextForTest();

        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"status\":\"PAID\"")));

        assertEquals(1, ticketRepository.countByOrder_Id(orderId));
        assertTrue(emailGateway.getCapturedMessages().isEmpty());
    }

    @Test
    void concurrentEntryScansAcceptTicketOnlyOnceAndAuditBothAttempts() throws Exception {
        EventSeed event = createGeneralEvent("Entry race", 1);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "entry-race-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        String qr = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().getQrToken();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> concurrentValidate(event.id(), qr, ready, start));
            Future<String> second = executor.submit(() -> concurrentValidate(event.id(), qr, ready, start));
            ready.await();
            start.countDown();
            List<String> outcomes = List.of(first.get(), second.get()).stream().sorted().toList();
            assertEquals(List.of("ACCEPTED", "ALREADY_USED"), outcomes);
        }

        assertEquals(2, checkInRepository.count());
        assertTrue(ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().isUsed());
    }

    @Test
    void administratorCanReopenAConsumedTicketOnlyOnce() throws Exception {
        EventSeed event = createGeneralEvent("Entry correction", 1);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "entry-correction-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        var ticket = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst();
        String scanId = UUID.randomUUID().toString();
        String accepted = validateEntry(event.id(), ticket.getQrToken(), scanId, producerToken);
        assertTrue(accepted.contains("\"outcome\":\"ACCEPTED\""));
        long checkInId = checkInRepository.findAll().getFirst().getId();

        mockMvc.perform(post("/api/entrada/auditoria/{id}/corrigir", checkInId)
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Leitura incorreta\"}"))
            .andExpect(status().isNoContent());
        assertTrue(!ticketRepository.findById(ticket.getId()).orElseThrow().isUsed());
        mockMvc.perform(get("/api/entrada/eventos/{id}/auditoria", event.id())
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"correctionReason\":\"Leitura incorreta\"")))
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"correctedBy\":\"admin@example.com\"")));

        String acceptedAgain = validateEntry(event.id(), ticket.getQrToken(), UUID.randomUUID().toString(), producerToken);
        assertTrue(acceptedAgain.contains("\"outcome\":\"ACCEPTED\""));
        mockMvc.perform(post("/api/entrada/auditoria/{id}/corrigir", checkInRepository.findAll().get(1).getId())
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Segunda tentativa\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void offlineSyncUsesFirstSyncWinsAndIsIdempotent() throws Exception {
        EventSeed event = createGeneralEvent("Offline entry", 1);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "offline-entry-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        String qr = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().getQrToken();

        String firstManifest = provisionManifest(event.id(), producerToken);
        String secondManifest = provisionManifest(event.id(), producerToken);
        String firstManifestId = jsonString(firstManifest, "id");
        String secondManifestId = jsonString(secondManifest, "id");
        assertTrue(firstManifest.contains("\"qrTokenHash\""));
        assertTrue(!firstManifest.contains(qr));

        String firstScanId = UUID.randomUUID().toString();
        String firstSync = syncOffline(event.id(), firstManifestId, "device-1", firstScanId, qr, producerToken);
        assertTrue(firstSync.contains("\"outcome\":\"ACCEPTED\""));
        assertTrue(syncOffline(event.id(), firstManifestId, "device-1", firstScanId, qr, producerToken)
            .contains("\"outcome\":\"ACCEPTED\""));

        String secondSync = syncOffline(event.id(), secondManifestId, "device-2", UUID.randomUUID().toString(), qr, producerToken);
        assertTrue(secondSync.contains("\"outcome\":\"OFFLINE_CONFLICT\""));
        assertEquals(2, checkInRepository.count());
    }

    @Test
    void eventStaffInvitationIsBoundToEventAndRevocationTakesEffectImmediately() throws Exception {
        EventSeed event = createGeneralEvent("Staff event", 2);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "staff-event-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        List<String> qrs = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).stream()
            .map(ticket -> ticket.getQrToken()).toList();

        java.time.Instant requestedAt = java.time.Instant.now();
        MvcResult inviteResult = mockMvc.perform(post("/api/entrada/eventos/{id}/equipe", event.id())
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"second@example.com\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String invite = inviteResult.getResponse().getContentAsString();
        long inviteId = responseId(inviteResult);
        String token = jsonString(invite, "token");
        java.time.Instant expiresAt = java.time.Instant.parse(jsonString(invite, "expiresAt"));
        assertTrue(!expiresAt.isBefore(requestedAt.plus(java.time.Duration.ofHours(48)).minusSeconds(5)));
        assertTrue(!expiresAt.isAfter(requestedAt.plus(java.time.Duration.ofHours(48)).plusSeconds(5)));

        mockMvc.perform(post("/api/entrada/convites/equipe/aceitar")
                .header("Authorization", "Bearer " + secondBuyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isNoContent());
        Usuario invitedStaff = usuarioRepository.findByEmailIgnoreCase("second@example.com").orElseThrow();
        assertTrue(invitedStaff.getPerfis().contains(UsuarioPerfil.USER));
        assertTrue(!invitedStaff.getPerfis().contains(UsuarioPerfil.PRODUCER));
        assertTrue(!invitedStaff.getPerfis().contains(UsuarioPerfil.ADMIN));
        String manifest = provisionManifest(event.id(), secondBuyerToken);
        String manifestId = jsonString(manifest, "id");
        assertTrue(validateEntry(event.id(), qrs.getFirst(), UUID.randomUUID().toString(), secondBuyerToken)
            .contains("\"outcome\":\"ACCEPTED\""));
        mockMvc.perform(get("/api/entrada/eventos/{id}/auditoria", event.id())
                .header("Authorization", "Bearer " + secondBuyerToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/entrada/eventos/{eventId}/equipe/{inviteId}", event.id(), inviteId)
                .header("Authorization", "Bearer " + producerToken))
            .andExpect(status().isNoContent());
        String revokedSync = syncOffline(event.id(), manifestId, "staff-device", UUID.randomUUID().toString(),
            qrs.get(1), secondBuyerToken);
        assertTrue(revokedSync.contains("\"outcome\":\"OPERATOR_UNAUTHORIZED\""));
        assertTrue(!ticketRepository.findByOrder_IdOrderByIdAsc(orderId).get(1).isUsed());
        mockMvc.perform(post("/api/entrada/eventos/{id}/validar", event.id())
                .header("Authorization", "Bearer " + secondBuyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scanId\":\"" + UUID.randomUUID() + "\",\"qrCodeValue\":\"" + qrs.get(1) + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void entryValidationRequiresPublishedEventAndItsLocalCalendarDay() throws Exception {
        EventSeed event = createGeneralEvent("Entry window", 1);
        long orderId = createOrder(buyerToken, "entry-window-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        String qr = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().getQrToken();

        assertTrue(validateEntry(event.id(), qr, UUID.randomUUID().toString(), producerToken)
            .contains("\"outcome\":\"OUTSIDE_EVENT_DAY\""));
        mockMvc.perform(patch("/api/admin/eventos/{id}/cancelar", event.id())
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());
        assertTrue(validateEntry(event.id(), qr, UUID.randomUUID().toString(), producerToken)
            .contains("\"outcome\":\"REFUNDED\""));
        assertTrue(!ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().isUsed());
    }

    @Test
    void buyerCanRefundOneUnusedTicketAndRepeatedRequestIsIdempotent() throws Exception {
        EventSeed event = createGeneralEvent("Partial refund", 2);
        long orderId = createOrder(buyerToken, "partial-refund-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        List<Long> ticketIds = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).stream()
            .map(ticket -> ticket.getId()).toList();

        MvcResult refund = mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketIds.get(0))
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Planos alterados\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        assertTrue(refund.getResponse().getContentAsString().contains("\"amountInCents\":12500"));
        assertTrue(refund.getResponse().getContentAsString().contains("\"status\":\"SIMULATED\""));
        assertTrue(refund.getResponse().getContentAsString().contains("\"source\":\"BUYER_REQUEST\""));

        String repeat = mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketIds.get(0))
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        assertTrue(repeat.contains("\"id\":" + jsonLong(refund.getResponse().getContentAsString(), "id")));
        assertEquals(1, ticketRefundRepository.count());
        assertTrue(ticketRepository.findById(ticketIds.get(0)).orElseThrow().isRefunded());
        assertTrue(!ticketRepository.findById(ticketIds.get(1)).orElseThrow().isRefunded());
        assertTrue(getOrderBody(orderId).contains("\"qrCodeValue\":null"));

        mockMvc.perform(post("/api/entrada/eventos/{eventId}/validar", event.id())
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scanId\":\"" + UUID.randomUUID() + "\",\"qrCodeValue\":\""
                    + ticketRepository.findById(ticketIds.get(0)).orElseThrow().getQrToken() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"outcome\":\"REFUNDED\"")));

        mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketIds.get(1))
                .header("Authorization", "Bearer " + secondBuyerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void producerCancellationRefundsOnlyPaidUnusedTickets() throws Exception {
        EventSeed event = createGeneralEvent("Cancelled with refunds", 2);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "cancel-refund-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        List<Long> ticketIds = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).stream()
            .map(ticket -> ticket.getId()).toList();
        validateEntry(event.id(), ticketRepository.findById(ticketIds.get(0)).orElseThrow().getQrToken(),
            UUID.randomUUID().toString(), producerToken);

        mockMvc.perform(patch("/api/produtor/eventos/{id}/cancelar", event.id())
                .header("Authorization", "Bearer " + producerToken))
            .andExpect(status().isNoContent());

        assertEquals(1, ticketRefundRepository.count());
        var refund = ticketRefundRepository.findByTicket_Id(ticketIds.get(1)).orElseThrow();
        assertEquals(org.example.ingresso.ingresso.model.enums.TicketRefundSource.EVENT_CANCELLATION, refund.getSource());
        assertEquals("producer@example.com", jdbcTemplate.queryForObject(
            "SELECT email FROM usuarios WHERE id = (SELECT actor_user_id FROM ticket_refunds WHERE ticket_id = ?)",
            String.class,
            ticketIds.get(1)
        ));
        assertEquals(12500, refund.getAmountInCents());
        assertTrue(!ticketRepository.findById(ticketIds.get(0)).orElseThrow().isRefunded());
        assertTrue(ticketRepository.findById(ticketIds.get(1)).orElseThrow().isRefunded());
    }

    @Test
    void administratorCancellationAutomaticallyCreatesSimulatedRefunds() throws Exception {
        EventSeed event = createGeneralEvent("Admin cancellation refund", 1);
        long orderId = createOrder(buyerToken, "admin-cancel-refund-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        var ticket = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst();

        mockMvc.perform(patch("/api/admin/eventos/{id}/cancelar", event.id())
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());

        var refund = ticketRefundRepository.findByTicket_Id(ticket.getId()).orElseThrow();
        assertEquals(org.example.ingresso.ingresso.model.enums.TicketRefundSource.EVENT_CANCELLATION, refund.getSource());
        assertEquals("admin@example.com", jdbcTemplate.queryForObject(
            "SELECT email FROM usuarios WHERE id = ?", String.class, refund.getActor().getId()
        ));
        assertEquals(12500, refund.getAmountInCents());
    }

    @Test
    void buyerRefundRejectsUsedOrStartedTicket() throws Exception {
        EventSeed event = createGeneralEvent("Refund cutoff", 2);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "refund-cutoff-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        List<Long> ticketIds = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).stream()
            .map(ticket -> ticket.getId()).toList();
        validateEntry(event.id(), ticketRepository.findById(ticketIds.get(0)).orElseThrow().getQrToken(),
            UUID.randomUUID().toString(), producerToken);

        mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketIds.get(0))
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isConflict());
        jdbcTemplate.update("UPDATE events SET starts_at = ? WHERE id = ?",
            Timestamp.from(java.time.Instant.now().minusSeconds(1)), event.id());
        mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketIds.get(1))
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isConflict());
        assertEquals(0, ticketRefundRepository.count());
    }

    @Test
    void financialReportsReconcileSnapshotGrossAndMockRefundsWithinOwnership() throws Exception {
        EventSeed event = createGeneralEvent("Finance totals", 2);
        long orderId = createOrder(buyerToken, "finance-report-order", orderJson(event.id(), event.categoryId(), 2), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        Long ticketId = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst().getId();
        mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/produtor/eventos/{id}/financeiro", event.id())
                .header("Authorization", "Bearer " + producerToken))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("\"paidOrderCount\":1"));
                assertTrue(body.contains("\"paidTicketCount\":2"));
                assertTrue(body.contains("\"refundedTicketCount\":1"));
                assertTrue(body.contains("\"grossSalesInCents\":25000"));
                assertTrue(body.contains("\"simulatedRefundsInCents\":12500"));
                assertTrue(body.contains("\"remainingGrossInCents\":12500"));
            });

        mockMvc.perform(get("/api/produtor/eventos/{id}/financeiro", event.id())
                .header("Authorization", "Bearer " + otherProducerToken))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/financeiro")
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"simulatedRefundsInCents\":12500")));
        mockMvc.perform(get("/api/admin/financeiro").header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void refundAndCheckInRaceCanCommitOnlyOneTerminalState() throws Exception {
        EventSeed event = createGeneralEvent("Refund race", 1);
        setEventStartToToday(event.id());
        long orderId = createOrder(buyerToken, "refund-race-order", orderJson(event.id(), event.categoryId(), 1), status().isCreated());
        mockMvc.perform(post("/api/pedidos/{id}/pagamento", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk());
        var ticket = ticketRepository.findByOrder_IdOrderByIdAsc(orderId).getFirst();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> checkIn = executor.submit(() -> concurrentValidate(
                event.id(), ticket.getQrToken(), ready, start
            ));
            Future<Integer> refund = executor.submit(() -> concurrentRefund(
                orderId, ticket.getId(), ready, start
            ));
            ready.await();
            start.countDown();
            String outcome = checkIn.get();
            int refundStatus = refund.get();
            assertTrue((outcome.equals("ACCEPTED") && refundStatus == 409)
                || (outcome.equals("REFUNDED") && refundStatus == 201));
        }
        assertTrue(ticketRepository.findById(ticket.getId()).orElseThrow().isUsed()
            ^ ticketRepository.findById(ticket.getId()).orElseThrow().isRefunded());
        assertTrue(ticketRefundRepository.count() <= 1);
    }

    private boolean resultHasTicket(long orderId) throws Exception {
        String body = mockMvc.perform(get("/api/pedidos/{id}", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return body.contains("\"tickets\":[{\"id\":") && body.contains("\"qrCodeValue\":\"");
    }

    private String concurrentValidate(Long eventId, String qr, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return jsonString(validateEntry(eventId, qr, UUID.randomUUID().toString(), producerToken), "outcome");
    }

    private int concurrentRefund(Long orderId, Long ticketId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/api/pedidos/{orderId}/tickets/{ticketId}/reembolsos", orderId, ticketId)
                .header("Authorization", "Bearer " + buyerToken))
            .andReturn().getResponse().getStatus();
    }

    private String validateEntry(Long eventId, String qr, String scanId, String token) throws Exception {
        return mockMvc.perform(post("/api/entrada/eventos/{id}/validar", eventId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scanId\":\"" + scanId + "\",\"qrCodeValue\":\"" + qr + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private void setEventStartToToday(Long eventId) {
        var startOfDay = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
            .atTime(23, 59)
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .toInstant();
        jdbcTemplate.update("UPDATE events SET starts_at = ? WHERE id = ?", Timestamp.from(startOfDay), eventId);
    }

    private String provisionManifest(Long eventId, String token) throws Exception {
        return mockMvc.perform(get("/api/entrada/eventos/{id}/manifesto-offline", eventId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String syncOffline(Long eventId, String manifestId, String deviceId, String scanId, String qr, String token)
        throws Exception {
        return mockMvc.perform(post("/api/entrada/eventos/{id}/sincronizar", eventId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"manifestId\":\"" + manifestId + "\",\"deviceId\":\"" + deviceId
                    + "\",\"scans\":[{\"scanId\":\"" + scanId + "\",\"qrTokenHash\":\"" + hashQr(qr)
                    + "\",\"scannedAt\":\"" + java.time.Instant.now() + "\"}]}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private String hashQr(String qr) throws Exception {
        byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(qr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(bytes);
    }

    @Test
    void concurrentBuyersCannotReserveTheLastGeneralAdmissionTicket() throws Exception {
        EventSeed event = createGeneralEvent("Last ticket", 1);
        String body = orderJson(event.id(), event.categoryId(), 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> concurrentCreate(buyerToken, "race-first", body, ready, start));
            Future<Integer> second = executor.submit(() -> concurrentCreate(secondBuyerToken, "race-second", body, ready, start));
            ready.await();
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get()).stream().sorted().toList();
            assertEquals(List.of(201, 409), statuses);
        }
        assertEquals(1, orderRepository.count());
        assertEquals(1, category(event.categoryId()).getReservedQuantity());
    }

    @Test
    void producerCannotReduceOfferBelowPendingReservations() throws Exception {
        EventSeed event = createGeneralEvent("Reserved inventory", 3);
        createOrder(buyerToken, "reserved-units", orderJson(event.id(), event.categoryId(), 2), status().isCreated());

        mockMvc.perform(put("/api/produtor/eventos/{id}", event.id())
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(generalEventJson("Reserved inventory", 1)))
            .andExpect(status().isConflict());

        assertEquals(2, category(event.categoryId()).getReservedQuantity());
        assertEquals(3, category(event.categoryId()).getConfiguredQuantity());
    }

    private int concurrentCreate(
        String token,
        String key,
        String body,
        CountDownLatch ready,
        CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return mockMvc.perform(post("/api/pedidos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn().getResponse().getStatus();
    }

    private long createOrder(String token, String key, String body,
        org.springframework.test.web.servlet.ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/pedidos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(resultMatcher)
            .andReturn();
        return responseId(result);
    }

    private EventSeed createGeneralEvent(String title, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(generalEventJson(title, quantity)))
            .andExpect(status().isCreated())
            .andReturn();
        String body = result.getResponse().getContentAsString();
        return new EventSeed(responseId(result), responseIdAfter(body, "categories"), null);
    }

    private EventSeed createAssignedEvent(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignedEventJson(title)))
            .andExpect(status().isCreated())
            .andReturn();
        String body = result.getResponse().getContentAsString();
        return new EventSeed(responseId(result), responseIdAfter(body, "categories"), responseIdAfter(body, "seats"));
    }

    private TicketCategory category(Long id) {
        return categoryRepository.findById(id).orElseThrow();
    }

    private String orderJson(Long eventId, Long categoryId, int quantity) {
        return "{\"eventId\":" + eventId + ",\"items\":[{\"categoryId\":" + categoryId
            + ",\"quantity\":" + quantity + "}]}";
    }

    private String generalEventJson(String title, int quantity) {
        return """
            {
              "title":"%s",
              "description":"Show para teste de pedidos.",
              "imageUrl":"https://images.example.com/order.jpg",
              "startsAt":"2026-12-10T19:30:00",
              "venueName":"Casa de Shows",
              "streetAddress":"Rua das Flores, 10",
              "city":"Sao Paulo",
              "stateCode":"SP",
              "ageClassification":"Livre",
              "organizer":"Produtora Exemplo",
              "categories":[{"code":"pista","name":"Pista","priceInCents":12500,"admissionMode":"GENERAL_ADMISSION","quantity":%d}],
              "sectors":[]
            }
            """.formatted(title, quantity);
    }

    private String assignedEventJson(String title) {
        return """
            {
              "title":"%s",
              "description":"Evento com assento.",
              "imageUrl":"https://images.example.com/seat-order.jpg",
              "startsAt":"2026-12-10T19:30:00",
              "venueName":"Teatro Central",
              "streetAddress":"Rua Principal, 10",
              "city":"Sao Paulo",
              "stateCode":"SP",
              "ageClassification":"Livre",
              "organizer":"Produtora Exemplo",
              "categories":[{"code":"vip","name":"VIP","priceInCents":22000,"admissionMode":"ASSIGNED_SEAT","quantity":null}],
              "sectors":[{"name":"Setor A","positionIndex":0,"rows":[{"label":"A","positionIndex":0,"seats":[
                {"label":"1","positionIndex":0,"categoryCode":"vip"}
              ]}]}]
            }
            """.formatted(title);
    }

    private long responseId(MvcResult result) throws Exception {
        return responseIdAfter(result.getResponse().getContentAsString(), "id");
    }

    private String jsonString(String body, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(body);
        if (!matcher.find()) throw new AssertionError("Campo ausente: " + field);
        return matcher.group(1);
    }

    private long jsonLong(String body, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*(\\d+)").matcher(body);
        if (!matcher.find()) throw new AssertionError("Campo numerico ausente: " + field);
        return Long.parseLong(matcher.group(1));
    }

    private String getOrderBody(long orderId) throws Exception {
        return mockMvc.perform(get("/api/pedidos/{id}", orderId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
    }

    private long responseIdAfter(String body, String field) {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\[(?:\\s*\\{)?\\s*\\\"id\\\"\\s*:\\s*(\\d+)").matcher(body);
        if (matcher.find()) return Long.parseLong(matcher.group(1));
        Matcher idMatcher = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)").matcher(body);
        if (idMatcher.find()) return Long.parseLong(idMatcher.group(1));
        throw new AssertionError("Resposta sem id: " + body);
    }

    private String createUser(String name, String email, UsuarioPerfil profile) {
        Usuario user = usuarioRepository.save(new Usuario(name, email, passwordEncoder.encode("password"), profile));
        return tokenService.generateToken(new UserSS(user.getId(), user.getEmail(), user.getSenha(), user.getPerfis()));
    }

    private record EventSeed(Long id, Long categoryId, Long seatId) {
    }
}