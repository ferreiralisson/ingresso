package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.AcceptInvitationCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.ProducerInvitationResult;

public interface ProducerInvitationService {

    ProducerInvitationResult inviteProducer(String email, Long actorId);

    void acceptProducerInvitation(AcceptInvitationCommand request);

    void revokeInvitation(Long invitationId, Long actorId);

    void revokeProducer(Long userId, Long actorId);
}
