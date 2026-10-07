package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.dto.EventStaffGrantResponse;
import org.example.ingresso.ingresso.dto.EventStaffInviteResponse;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.EventStaffInvitation;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.EventStaffInvitationRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.example.ingresso.ingresso.security.UserSS;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class EventStaffService {

    private static final Duration INVITATION_LIFETIME = Duration.ofHours(48);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EventRepository eventRepository;
    private final EventStaffInvitationRepository invitationRepository;
    private final UsuarioRepository usuarioRepository;

    public EventStaffService(
        EventRepository eventRepository,
        EventStaffInvitationRepository invitationRepository,
        UsuarioRepository usuarioRepository
    ) {
        this.eventRepository = eventRepository;
        this.invitationRepository = invitationRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public EventStaffInviteResponse invite(Long eventId, String email, UserSS principal) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario actor = findUser(principal.getId());
        authorizeOwnerOrAdmin(event, actor);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        if (invitationRepository.existsByEvent_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
            eventId, normalizedEmail, now
        )) {
            throw new IllegalArgumentException("Ja existe convite ativo para este evento e e-mail");
        }
        if (usuarioRepository.findByEmailIgnoreCase(normalizedEmail).filter(user ->
            invitationRepository.existsByEvent_IdAndAcceptedBy_IdAndRevokedAtIsNull(eventId, user.getId())
        ).isPresent()) {
            throw new IllegalArgumentException("Usuario ja possui acesso a este evento");
        }

        String token = newToken();
        EventStaffInvitation invitation = invitationRepository.save(new EventStaffInvitation(
            event, normalizedEmail, hash(token), actor, now, now.plus(INVITATION_LIFETIME)
        ));
        return new EventStaffInviteResponse(invitation.getId(), eventId, normalizedEmail, token, invitation.getExpiresAt());
    }

    @Transactional
    public void accept(String token, UserSS principal) {
        Instant now = Instant.now();
        EventStaffInvitation invitation = invitationRepository.findWithLockByTokenHash(hash(token))
            .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
        if (invitation.getAcceptedAt() != null || invitation.getRevokedAt() != null || !invitation.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Convite expirado, revogado ou ja utilizado");
        }
        if (!invitation.getEmail().equalsIgnoreCase(principal.getUsername())) {
            throw new IllegalArgumentException("Convite pertence a outro e-mail");
        }
        Usuario user = findUser(principal.getId());
        if (invitationRepository.existsByEvent_IdAndAcceptedBy_IdAndRevokedAtIsNull(
            invitation.getEvent().getId(), user.getId()
        )) {
            throw new IllegalArgumentException("Usuario ja possui acesso a este evento");
        }
        invitation.accept(user, now);
    }

    @Transactional(readOnly = true)
    public List<EventStaffGrantResponse> list(Long eventId, UserSS principal) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        authorizeOwnerOrAdmin(event, findUser(principal.getId()));
        return invitationRepository.findByEvent_IdAndAcceptedAtIsNotNullAndRevokedAtIsNullOrderByAcceptedAtAsc(eventId)
            .stream().map(invitation -> new EventStaffGrantResponse(
                invitation.getId(), invitation.getAcceptedBy().getId(), invitation.getEmail(), invitation.getAcceptedAt()
            )).toList();
    }

    @Transactional
    public void revoke(Long eventId, Long invitationId, UserSS principal) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        authorizeOwnerOrAdmin(event, findUser(principal.getId()));
        EventStaffInvitation invitation = invitationRepository.findWithLockById(invitationId)
            .filter(current -> current.getEvent().getId().equals(eventId))
            .orElseThrow(() -> new IllegalArgumentException("Permissao de equipe nao encontrada"));
        if (!invitation.isAcceptedAndActive()) {
            throw new IllegalArgumentException("Somente acessos ativos podem ser revogados");
        }
        invitation.revoke(Instant.now());
    }

    public boolean hasAccess(Long eventId, Long userId) {
        return invitationRepository.existsByEvent_IdAndAcceptedBy_IdAndRevokedAtIsNull(eventId, userId);
    }

    private Usuario findUser(Long id) {
        return usuarioRepository.findWithPerfisById(id)
            .orElseThrow(() -> new AccessDeniedException("Usuario autenticado nao encontrado"));
    }

    private void authorizeOwnerOrAdmin(Event event, Usuario actor) {
        if (actor.getPerfis().contains(UsuarioPerfil.ADMIN)) return;
        if (actor.getPerfis().contains(UsuarioPerfil.PRODUCER) && event.getProducer().getId().equals(actor.getId())) return;
        throw new AccessDeniedException("Somente o produtor dono ou ADMIN pode gerenciar a equipe");
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }
}