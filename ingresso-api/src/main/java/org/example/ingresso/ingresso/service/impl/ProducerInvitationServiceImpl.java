package org.example.ingresso.ingresso.service.impl;

import org.example.ingresso.ingresso.service.ProducerInvitationService;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.ProducerInvitationResult;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.AcceptInvitationCommand;
import org.example.ingresso.ingresso.service.exception.ApplicationServiceException;
import org.example.ingresso.ingresso.model.PermissionAudit;
import org.example.ingresso.ingresso.model.ProducerInvitation;
import org.example.ingresso.ingresso.model.Usuario;
import org.example.ingresso.ingresso.model.enums.PermissionAuditAction;
import org.example.ingresso.ingresso.model.enums.ProducerInvitationStatus;
import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;
import org.example.ingresso.ingresso.repository.PermissionAuditRepository;
import org.example.ingresso.ingresso.repository.ProducerInvitationRepository;
import org.example.ingresso.ingresso.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class ProducerInvitationServiceImpl implements ProducerInvitationService {

    private static final Duration INVITATION_LIFETIME = Duration.ofHours(48);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ProducerInvitationRepository invitationRepository;
    private final PermissionAuditRepository auditRepository;
    private final UsuarioRepository usuarioRepository;

    public ProducerInvitationServiceImpl(
        ProducerInvitationRepository invitationRepository,
        PermissionAuditRepository auditRepository,
        UsuarioRepository usuarioRepository
    ) {
        this.invitationRepository = invitationRepository;
        this.auditRepository = auditRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ProducerInvitationResult inviteProducer(String email, Long actorId) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();

        usuarioRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            if (user.getPerfis().contains(UsuarioPerfil.PRODUCER)) {
                throw new IllegalArgumentException("Usuario ja possui perfil de produtor");
            }
        });

        if (invitationRepository.existsByEmailIgnoreCaseAndStatusAndExpiresAtAfter(
            normalizedEmail,
            ProducerInvitationStatus.PENDING,
            now
        )) {
            throw new IllegalArgumentException("Ja existe convite ativo para este e-mail");
        }

        String rawToken = generateToken();
        Instant expiresAt = now.plus(INVITATION_LIFETIME);
        Usuario inviter = usuarioRepository.findWithPerfisById(actorId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Administrador nao encontrado"
            ));
        ProducerInvitation invitation = invitationRepository.save(new ProducerInvitation(
            normalizedEmail,
            hashToken(rawToken),
            inviter,
            now,
            expiresAt
        ));
        auditRepository.save(new PermissionAudit(
            inviter.getId(),
            inviter.getEmail(),
            null,
            normalizedEmail,
            UsuarioPerfil.PRODUCER,
            PermissionAuditAction.INVITATION_CREATED,
            now
        ));

        return new ProducerInvitationResult(invitation.getId(), normalizedEmail, rawToken, expiresAt);
    }

    @Transactional
    public void acceptProducerInvitation(AcceptInvitationCommand request) {
        Instant now = Instant.now();
        ProducerInvitation invitation = invitationRepository.findByTokenHashForUpdate(hashToken(request.token()))
            .orElseThrow(() -> new IllegalArgumentException("Convite invalido"));

        if (invitation.getStatus() != ProducerInvitationStatus.PENDING || !invitation.getExpiresAt().isAfter(now)) {
            throw new IllegalArgumentException("Convite expirado, revogado ou ja utilizado");
        }
        if (!invitation.getEmail().equalsIgnoreCase(request.email())) {
            throw new IllegalArgumentException("O convite pertence a outro e-mail");
        }

        Usuario user = usuarioRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Usuario autenticado nao encontrado"
            ));
        if (user.getPerfis().contains(UsuarioPerfil.PRODUCER)) {
            throw new IllegalArgumentException("Usuario ja possui perfil de produtor");
        }

        user.addPerfil(UsuarioPerfil.PRODUCER);
        usuarioRepository.save(user);
        invitation.accept(user, now);
        auditRepository.save(new PermissionAudit(
            user.getId(),
            user.getEmail(),
            user.getId(),
            user.getEmail(),
            UsuarioPerfil.PRODUCER,
            PermissionAuditAction.PRODUCER_GRANTED,
            now
        ));
    }

    @Transactional
    public void revokeInvitation(Long invitationId, Long actorId) {
        Instant now = Instant.now();
        ProducerInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
            .orElseThrow(() -> new IllegalArgumentException("Convite nao encontrado"));
        if (invitation.getStatus() != ProducerInvitationStatus.PENDING) {
            throw new IllegalArgumentException("Somente convites pendentes podem ser revogados");
        }

        Usuario admin = usuarioRepository.findWithPerfisById(actorId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Administrador nao encontrado"
            ));
        invitation.revoke(admin, now);
        auditRepository.save(new PermissionAudit(
            admin.getId(),
            admin.getEmail(),
            null,
            invitation.getEmail(),
            UsuarioPerfil.PRODUCER,
            PermissionAuditAction.INVITATION_REVOKED,
            now
        ));
    }

    @Transactional
    public void revokeProducer(Long userId, Long actorId) {
        Instant now = Instant.now();
        Usuario producer = usuarioRepository.findWithPerfisById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
        if (!producer.getPerfis().contains(UsuarioPerfil.PRODUCER)) {
            throw new IllegalArgumentException("Usuario nao possui perfil de produtor");
        }

        Usuario admin = usuarioRepository.findWithPerfisById(actorId)
            .orElseThrow(() -> new ApplicationServiceException(
                ApplicationServiceException.Code.UNAUTHORIZED, "Administrador nao encontrado"
            ));
        producer.removePerfil(UsuarioPerfil.PRODUCER);
        usuarioRepository.save(producer);
        auditRepository.save(new PermissionAudit(
            admin.getId(),
            admin.getEmail(),
            producer.getId(),
            producer.getEmail(),
            UsuarioPerfil.PRODUCER,
            PermissionAuditAction.PRODUCER_REVOKED,
            now
        ));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponivel", exception);
        }
    }
}