package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.AcceptInvitationCommand;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.EventStaffGrant;
import org.example.ingresso.ingresso.service.dto.CoreServiceContracts.EventStaffInvite;

import java.util.List;

public interface EventStaffService {

    EventStaffInvite invite(Long eventId, String email, Long actorId);

    void accept(AcceptInvitationCommand request);

    List<EventStaffGrant> list(Long eventId, Long actorId);

    void revoke(Long eventId, Long invitationId, Long actorId);

    boolean hasAccess(Long eventId, Long userId);
}
