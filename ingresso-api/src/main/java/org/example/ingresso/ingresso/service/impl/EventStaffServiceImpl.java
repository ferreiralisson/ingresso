package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.EventStaffService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.EventStaffGrant;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.EventStaffInvite;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.AcceptInvitationCommand;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.example.ingresso.ingresso.model.Event;
import org.example.ingresso.ingresso.model.EventStaffInvitation;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.EventRepository;
import org.example.ingresso.ingresso.repository.EventStaffInvitationRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
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
public class EventStaffServiceImpl implements EventStaffService {

    private static final Duration INVITATION_LIFETIME = Duration.ofHours(48);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EventRepository eventRepository;
    private final EventStaffInvitationRepository invitationRepository;
    private final UsuarioRepository usuarioRepository;

    public EventStaffServiceImpl(
        EventRepository eventRepository,
        EventStaffInvitationRepository invitationRepository,
        UsuarioRepository usuarioRepository
    ) {
        this.eventRepository = eventRepository;
        this.invitationRepository = invitationRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public EventStaffInvite invite(Long eventId, String email, Long actorId) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        Usuario actor = findUser(actorId);
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
        return new EventStaffInvite(invitation.getId(), eventId, normalizedEmail, token, invitation.getExpiresAt());
    }

    @Transactional
    public void accept(AcceptInvitationCommand request) {
        Instant now = Instant.now();
        EventStaffInvitation invitation = invitationRepository.findWithLockByTokenHash(hash(request.token()))
            .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));
        if (invitation.getAcceptedAt() != null || invitation.getRevokedAt() != null || !invitation.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Convite expirado, revogado ou ja utilizado");
        }
        if (!invitation.getEmail().equalsIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Convite pertence a outro e-mail");
        }
        Usuario user = findUser(request.userId());
        if (invitationRepository.existsByEvent_IdAndAcceptedBy_IdAndRevokedAtIsNull(
            invitation.getEvent().getId(), user.getId()
        )) {
            throw new IllegalArgumentException("Usuario ja possui acesso a este evento");
        }
        invitation.accept(user, now);
    }

    @Transactional(readOnly = true)
    public List<EventStaffGrant> list(Long eventId, Long actorId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        authorizeOwnerOrAdmin(event, findUser(actorId));
        return invitationRepository.findByEvent_IdAndAcceptedAtIsNotNullAndRevokedAtIsNullOrderByAcceptedAtAsc(eventId)
            .stream().map(invitation -> new EventStaffGrant(
                invitation.getId(), invitation.getAcceptedBy().getId(), invitation.getEmail(), invitation.getAcceptedAt()
            )).toList();
    }

    @Transactional
    public void revoke(Long eventId, Long invitationId, Long actorId) {
        Event event = eventRepository.findWithLockById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Evento nao encontrado"));
        authorizeOwnerOrAdmin(event, findUser(actorId));
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
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Usuario autenticado nao encontrado"
            ));
    }

    private void authorizeOwnerOrAdmin(Event event, Usuario actor) {
        if (actor.getPerfis().contains(UsuarioPerfil.ADMIN)) return;
        if (actor.getPerfis().contains(UsuarioPerfil.PRODUCER) && event.getProducer().getId().equals(actor.getId())) return;
        throw new ApplicationServiceException(
            ApplicationServiceException.Code.FORBIDDEN, "Somente o produtor dono ou ADMIN pode gerenciar a equipe"
        );
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