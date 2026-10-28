package org.example.ingresso.ingresso.controller;

import org.example.ingresso.ingresso.dto.EntryCorrectionRequest;
import org.example.ingresso.ingresso.dto.EntryValidationRequest;
import org.example.ingresso.ingresso.dto.EntryValidationResponse;
import org.example.ingresso.ingresso.dto.OfflineManifestResponse;
import org.example.ingresso.ingresso.dto.OfflineSyncRequest;
import org.example.ingresso.ingresso.dto.OfflineSyncResponse;
import org.example.ingresso.ingresso.security.UserSS;
import org.example.ingresso.ingresso.service.EntryValidationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entrada")
public class EntryValidationController {

    private final EntryValidationService entryValidationService;

    public EntryValidationController(EntryValidationService entryValidationService) {
        this.entryValidationService = entryValidationService;
    }

    @PostMapping("/eventos/{eventId}/validar")
    public ResponseEntity<EntryValidationResponse> validate(
        @PathVariable Long eventId,
        @RequestBody @Valid EntryValidationRequest request,
        @AuthenticationPrincipal UserSS operator
    ) {
        return ResponseEntity.ok(EntryHttpMapper.toResponse(
            entryValidationService.validateOnline(eventId, EntryHttpMapper.toCommand(request), operator.getId())
        ));
    }

    @GetMapping("/eventos/{eventId}/manifesto-offline")
    public ResponseEntity<OfflineManifestResponse> provisionOffline(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS operator
    ) {
        return ResponseEntity.ok(EntryHttpMapper.toResponse(
            entryValidationService.provisionOffline(eventId, operator.getId())
        ));
    }

    @PostMapping("/eventos/{eventId}/sincronizar")
    public ResponseEntity<OfflineSyncResponse> synchronizeOffline(
        @PathVariable Long eventId,
        @RequestBody @Valid OfflineSyncRequest request,
        @AuthenticationPrincipal UserSS operator
    ) {
        return ResponseEntity.ok(EntryHttpMapper.toResponse(entryValidationService.synchronizeOffline(
            eventId, EntryHttpMapper.toCommand(request), operator.getId()
        )));
    }

    @GetMapping("/eventos/{eventId}/auditoria")
    public ResponseEntity<Page<EntryValidationResponse>> history(
        @PathVariable Long eventId,
        @AuthenticationPrincipal UserSS operator,
        Pageable pageable
    ) {
        var result = entryValidationService.history(eventId, operator.getId(), PageHttpMapper.toQuery(pageable));
        var responses = result.content().stream().map(EntryHttpMapper::toResponse).toList();
        return ResponseEntity.ok(PageHttpMapper.toPage(new org.example.ingresso.ingresso.service.dto.PageResult<>(
            responses, result.pageNumber(), result.pageSize(), result.totalElements(), result.sort()
        )));
    }

    @PostMapping("/auditoria/{checkInId}/corrigir")
    public ResponseEntity<Void> correct(
        @PathVariable Long checkInId,
        @RequestBody @Valid EntryCorrectionRequest request,
        @AuthenticationPrincipal UserSS administrator
    ) {
        entryValidationService.correct(checkInId, EntryHttpMapper.toCommand(request), administrator.getId());
        return ResponseEntity.noContent().build();
    }
}