package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.TokenService;
import org.example.ingresso.ingresso.security.UserSS;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:event-test;DB_CLOSE_DELAY=-1",
        "token.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
)
@AutoConfigureMockMvc
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private String producerToken;
    private String otherProducerToken;
    private String buyerToken;
    private String administratorToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM event_seats");
        jdbcTemplate.execute("DELETE FROM seat_rows");
        jdbcTemplate.execute("DELETE FROM seat_sectors");
        jdbcTemplate.execute("DELETE FROM ticket_categories");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM permission_audits");
        jdbcTemplate.execute("DELETE FROM producer_invitations");
        jdbcTemplate.execute("DELETE FROM usuario_perfis");
        jdbcTemplate.execute("DELETE FROM usuarios");

        producerToken = tokenFor(createUser("Producer", "producer@example.com", UsuarioPerfil.PRODUCER));
        otherProducerToken = tokenFor(createUser("Other producer", "other-producer@example.com", UsuarioPerfil.PRODUCER));
        buyerToken = tokenFor(createUser("Buyer", "buyer@example.com", UsuarioPerfil.USER));
        Usuario admin = new Usuario("Admin", "admin@example.com", passwordEncoder.encode("password"), UsuarioPerfil.USER);
        admin.addPerfil(UsuarioPerfil.ADMIN);
        administratorToken = tokenFor(usuarioRepository.save(admin));
    }

    @Test
    void producerPublishesEventAndVisitorsCanBrowseOffers() throws Exception {
        long eventId = createGeneralEvent(producerToken, "Show ao vivo");

        mockMvc.perform(get("/api/eventos"))
            .andExpect(status().isOk())
            .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("Show ao vivo")));

        mockMvc.perform(get("/api/eventos/{id}", eventId))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("America/Sao_Paulo"));
                assertTrue(body.contains("\"priceInCents\":12500"));
                assertTrue(body.contains("\"configuredQuantity\":80"));
                assertTrue(body.contains("\"availableQuantity\":80"));
            });
    }

    @Test
    void buyersCannotCreateEventsAndProducersCannotEditAnotherOwnersEvent() throws Exception {
        mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(generalEventJson("Unauthorized event")))
            .andExpect(status().isForbidden());

        long eventId = createGeneralEvent(producerToken, "Owned event");
        mockMvc.perform(put("/api/produtor/eventos/{id}", eventId)
                .header("Authorization", "Bearer " + otherProducerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(generalEventJson("Taken over event")))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/produtor/eventos/{id}", eventId)
                .header("Authorization", "Bearer " + otherProducerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void producerCanUpdateOwnedEventDetailsAndCurrentOffer() throws Exception {
        long eventId = createGeneralEvent(producerToken, "Original event");
        String updated = generalEventJson("Updated event")
            .replace("12500", "15000")
            .replace("Produtora Exemplo", "Produtora Atualizada");

        mockMvc.perform(put("/api/produtor/eventos/{id}", eventId)
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updated))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("Updated event"));
                assertTrue(body.contains("\"priceInCents\":15000"));
                assertTrue(body.contains("\"organizer\":\"Produtora Atualizada\""));
            });
    }

    @Test
    void assignedSeatMapGeneratesPerSeatAvailabilityAndCategoryPrice() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignedEventJson()))
            .andExpect(status().isCreated())
            .andReturn();
        long eventId = responseId(created);

        mockMvc.perform(get("/api/eventos/{id}", eventId))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("\"configuredQuantity\":3"));
                assertTrue(body.contains("\"availableQuantity\":3"));
                assertTrue(body.contains("\"categoryName\":\"VIP\""));
                assertTrue(body.contains("\"priceInCents\":22000"));
            });
    }

    @Test
    void invalidSeatCategoryIsRejectedAndAdminCanSuspendReactivateAndCancel() throws Exception {
        String invalidMap = assignedEventJson().replace("\"categoryCode\":\"vip\"", "\"categoryCode\":\"missing\"");
        mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + producerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidMap))
            .andExpect(status().isConflict());

        long eventId = createGeneralEvent(producerToken, "State event");
        mockMvc.perform(get("/api/admin/eventos/{id}", eventId)
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/eventos/{id}", eventId)
                .header("Authorization", "Bearer " + buyerToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/eventos/{id}/suspender", eventId)
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/eventos/{id}", eventId)).andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/produtor/eventos/{id}/reativar", eventId)
                .header("Authorization", "Bearer " + producerToken))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/eventos/{id}/cancelar", eventId)
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/eventos/{id}", eventId))
            .andExpect(status().isOk())
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                assertTrue(body.contains("\"status\":\"CANCELLED\""));
                assertTrue(body.contains("\"availableQuantity\":0"));
            });
        mockMvc.perform(patch("/api/produtor/eventos/{id}/reativar", eventId)
                .header("Authorization", "Bearer " + producerToken))
            .andExpect(status().isConflict());
    }

    private long createGeneralEvent(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/produtor/eventos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(generalEventJson(title)))
            .andExpect(status().isCreated())
            .andReturn();
        return responseId(result);
    }

    private String generalEventJson(String title) {
        return """
            {
              "title":"%s",
              "description":"Uma noite de música ao vivo.",
              "imageUrl":"https://images.example.com/show.jpg",
              "startsAt":"2026-12-10T19:30:00",
              "venueName":"Casa de Shows",
              "streetAddress":"Rua das Flores, 10",
              "city":"Sao Paulo",
              "stateCode":"SP",
              "ageClassification":"Livre",
              "organizer":"Produtora Exemplo",
              "categories":[{"code":"pista","name":"Pista","priceInCents":12500,"admissionMode":"GENERAL_ADMISSION","quantity":80}],
              "sectors":[]
            }
            """.formatted(title);
    }

    private String assignedEventJson() {
        return """
            {
              "title":"Espetaculo com assentos",
              "description":"Apresentacao com lugares marcados.",
              "imageUrl":"https://images.example.com/seats.jpg",
              "startsAt":"2026-12-11T20:00:00",
              "venueName":"Teatro Central",
              "streetAddress":"Avenida Principal, 50",
              "city":"Sao Paulo",
              "stateCode":"SP",
              "ageClassification":"12 anos",
              "organizer":"Produtora Exemplo",
              "categories":[{"code":"vip","name":"VIP","priceInCents":22000,"admissionMode":"ASSIGNED_SEAT","quantity":null}],
              "sectors":[{"name":"Platea","positionIndex":0,"rows":[{"label":"A","positionIndex":0,"seats":[
                {"label":"1","positionIndex":0,"categoryCode":"vip"},
                {"label":"2","positionIndex":1,"categoryCode":"vip"},
                {"label":"3","positionIndex":2,"categoryCode":"vip"}
              ]}]}]
            }
            """;
    }

    private long responseId(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\\\"id\\\":(\\d+)").matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new AssertionError("Resposta sem identificador de evento");
        }
        return Long.parseLong(matcher.group(1));
    }

    private Usuario createUser(String name, String email, UsuarioPerfil profile) {
        Usuario user = new Usuario(name, email, passwordEncoder.encode("password"), UsuarioPerfil.USER);
        user.addPerfil(profile);
        return usuarioRepository.save(user);
    }

    private String tokenFor(Usuario user) {
        return tokenService.generateToken(new UserSS(user.getId(), user.getEmail(), user.getSenha(), user.getPerfis()));
    }
}