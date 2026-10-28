package org.example.ingresso.ingresso.service;

import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryCorrectionCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.EntryValidationView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineManifestView;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncCommand;
import org.example.ingresso.ingresso.service.dto.EntryContracts.OfflineSyncView;
import org.example.ingresso.ingresso.service.dto.PageQuery;
import org.example.ingresso.ingresso.service.dto.PageResult;

public interface EntryValidationService {

    EntryValidationView validateOnline(Long eventId, EntryValidationCommand request, Long operatorId);

    PageResult<EntryValidationView> history(Long eventId, Long operatorId, PageQuery query);

    OfflineManifestView provisionOffline(Long eventId, Long operatorId);

    OfflineSyncView synchronizeOffline(Long eventId, OfflineSyncCommand request, Long operatorId);

    void correct(Long checkInId, EntryCorrectionCommand request, Long administratorId);
}