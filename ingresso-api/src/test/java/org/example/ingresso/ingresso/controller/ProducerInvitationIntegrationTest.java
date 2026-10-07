package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.ProducerInvitationResponse;
import org.example.ingresso.ingresso.model.ProducerInvitation;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.PermissionAuditAction;
import org.example.ingresso.ingresso.model.enums.ProducerInvitationStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.PermissionAuditRepository;
import org.example.ingresso.ingresso.repository.ProducerInvitationRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.TokenService;
import org.example.ingresso.ingresso.security.UserSS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:invitation-test;DB_CLOSE_DELAY=-1",
        "token.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
)
@AutoConfigureMockMvc
class ProducerInvitationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProducerInvitationRepository invitationRepository;

    @Autowired
    private PermissionAuditRepository auditRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private Usuario administrator;
    private Usuario buyer;
    private String administratorToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAllInBatch();
        invitationRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();

        administrator = new Usuario("Admin", "admin@example.com", passwordEncoder.encode("password"), UsuarioPerfil.USER);
        administrator.addPerfil(UsuarioPerfil.ADMIN);
        administrator = usuarioRepository.save(administrator);
        buyer = usuarioRepository.save(new Usuario(
            "Buyer",
            "buyer@example.com",
            passwordEncoder.encode("password"),
            UsuarioPerfil.USER
        ));

        administratorToken = tokenService.generateToken(new UserSS(
            administrator.getId(), administrator.getEmail(), administrator.getSenha(), administrator.getPerfis()
        ));
        buyerToken = tokenService.generateToken(new UserSS(
            buyer.getId(), buyer.getEmail(), buyer.getSenha(), buyer.getPerfis()
        ));
    }

    @Test
    void onlyAdministratorCanCreateAndRevokeProducerInvitation() throws Exception {
        mockMvc.perform(post("/api/admin/convites/produtores")
                .header("Authorization", "Bearer " + buyerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"producer@example.com\"}"))
            .andExpect(status().isForbidden());

        ProducerInvitationResponse invitation = createInvitation("producer@example.com");
        mockMvc.perform(delete("/api/admin/convites/produtores/{id}", invitation.id())
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());

        ProducerInvitation stored = invitationRepository.findById(invitation.id()).orElseThrow();
        assertEquals(ProducerInvitationStatus.REVOKED, stored.getStatus());
        assertEquals(2, auditRepository.count());
    }

    @Test
    void publicRegistrationCannotGrantAdministratorPermission() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Escalation attempt\",\"email\":\"attempt@example.com\","
                    + "\"password\":\"password\",\"perfil\":\"ADMIN\"}"))
            .andExpect(status().isCreated());

        Usuario createdUser = usuarioRepository.findByEmailIgnoreCase("attempt@example.com").orElseThrow();
        assertEquals(Set.of(UsuarioPerfil.USER), createdUser.getPerfis());
        String token = tokenService.generateToken(new UserSS(
            createdUser.getId(), createdUser.getEmail(), createdUser.getSenha(), createdUser.getPerfis()
        ));

        mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitationIsBoundToEmailExpiresIn48HoursAndCanGrantProducerOnlyOnce() throws Exception {
        ProducerInvitationResponse invitation = createInvitation("buyer@example.com");
        Instant now = Instant.now();
        Duration lifetime = Duration.between(now, invitation.expiresAt());
        assertTrue(lifetime.compareTo(Duration.ofHours(48)) <= 0);
        assertTrue(lifetime.compareTo(Duration.ofHours(47).plusMinutes(59)) > 0);

        ProducerInvitation stored = invitationRepository.findById(invitation.id()).orElseThrow();
        assertNotEquals(invitation.token(), stored.getTokenHash());

        String otherUserToken = createBuyer("Other buyer", "other@example.com");
        mockMvc.perform(post("/api/convites/produtores/aceitar")
                .header("Authorization", "Bearer " + otherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + invitation.token() + "\"}"))
            .andExpect(status().isConflict());

        acceptInvitation(invitation.token(), buyerToken, status().isNoContent());
        acceptInvitation(invitation.token(), buyerToken, status().isConflict());

        Usuario updatedBuyer = usuarioRepository.findByEmailIgnoreCase(buyer.getEmail()).orElseThrow();
        assertTrue(updatedBuyer.getPerfis().contains(UsuarioPerfil.USER));
        assertTrue(updatedBuyer.getPerfis().contains(UsuarioPerfil.PRODUCER));
        assertEquals(2, auditRepository.count());
        assertTrue(auditRepository.findAll().stream()
            .anyMatch(audit -> audit.getAction() == PermissionAuditAction.PRODUCER_GRANTED));
    }

    @Test
    void administratorCanRevokeProducerWithoutRemovingBuyerPermission() throws Exception {
        ProducerInvitationResponse invitation = createInvitation("buyer@example.com");
        acceptInvitation(invitation.token(), buyerToken, status().isNoContent());

        mockMvc.perform(delete("/api/admin/produtores/{userId}", buyer.getId())
                .header("Authorization", "Bearer " + administratorToken))
            .andExpect(status().isNoContent());

        Usuario updatedBuyer = usuarioRepository.findByEmailIgnoreCase(buyer.getEmail()).orElseThrow();
        assertTrue(updatedBuyer.getPerfis().contains(UsuarioPerfil.USER));
        assertFalse(updatedBuyer.getPerfis().contains(UsuarioPerfil.PRODUCER));
        assertTrue(auditRepository.findAll().stream()
            .anyMatch(audit -> audit.getAction() == PermissionAuditAction.PRODUCER_REVOKED));
    }

    private ProducerInvitationResponse createInvitation(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/convites/produtores")
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String body = result.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile(
            "\\\"id\\\":(\\d+),\\\"email\\\":\\\"([^\\\"]+)\\\",\\\"token\\\":\\\"([^\\\"]+)\\\",\\\"expiresAt\\\":\\\"([^\\\"]+)\\\""
        ).matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Resposta de convite inesperada: " + body);
        }
        return new ProducerInvitationResponse(
            Long.parseLong(matcher.group(1)),
            matcher.group(2),
            matcher.group(3),
            Instant.parse(matcher.group(4))
        );
    }

    private String createBuyer(String name, String email) {
        Usuario user = usuarioRepository.save(new Usuario(
            name,
            email,
            passwordEncoder.encode("password"),
            UsuarioPerfil.USER
        ));
        return tokenService.generateToken(new UserSS(user.getId(), user.getEmail(), user.getSenha(), Set.of(UsuarioPerfil.USER)));
    }

    private void acceptInvitation(
        String invitationToken,
        String userToken,
        org.springframework.test.web.servlet.ResultMatcher resultMatcher
    ) throws Exception {
        mockMvc.perform(post("/api/convites/produtores/aceitar")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + invitationToken + "\"}"))
            .andExpect(resultMatcher);
    }
}