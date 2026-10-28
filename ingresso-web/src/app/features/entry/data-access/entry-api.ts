import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import {
  EntryHistoryPage,
  EntryResult,
  EventStaffGrant,
  EventStaffInvite,
  OfflineManifest,
  PendingOfflineScan,
} from '../models/entry.models';

@Injectable({ providedIn: 'root' })
export class EntryApi {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  validate(eventId: number, qrCodeValue: string, scanId: string) {
    return this.http.post<EntryResult>(`${this.api}/entrada/eventos/${eventId}/validar`, {
      scanId,
      qrCodeValue,
    }).pipe(timeout(15000));
  }

  history(eventId: number, page = 0, size = 50) {
    return this.http.get<EntryHistoryPage>(`${this.api}/entrada/eventos/${eventId}/auditoria`, {
      params: { page, size, sort: 'processedAt,desc' },
    }).pipe(timeout(15000));
  }

  correct(checkInId: number, reason: string) {
    return this.http.post<void>(`${this.api}/entrada/auditoria/${checkInId}/corrigir`, { reason })
      .pipe(timeout(15000));
  }

  provisionOffline(eventId: number) {
    return this.http.get<OfflineManifest>(`${this.api}/entrada/eventos/${eventId}/manifesto-offline`)
      .pipe(timeout(30000));
  }

  synchronize(eventId: number, manifestId: string, deviceId: string, scans: PendingOfflineScan[]) {
    return this.http.post<{ manifestId: string; synchronizedAt: string; results: EntryResult[] }>(
      `${this.api}/entrada/eventos/${eventId}/sincronizar`,
      { manifestId, deviceId, scans: scans.map(({ scanId, qrTokenHash, scannedAt }) => ({ scanId, qrTokenHash, scannedAt })) },
    ).pipe(timeout(30000));
  }

  inviteStaff(eventId: number, email: string) {
    return this.http.post<EventStaffInvite>(`${this.api}/entrada/eventos/${eventId}/equipe`, { email })
      .pipe(timeout(15000));
  }

  listStaff(eventId: number) {
    return this.http.get<EventStaffGrant[]>(`${this.api}/entrada/eventos/${eventId}/equipe`).pipe(timeout(15000));
  }

  revokeStaff(eventId: number, invitationId: number) {
    return this.http.delete<void>(`${this.api}/entrada/eventos/${eventId}/equipe/${invitationId}`)
      .pipe(timeout(15000));
  }

  acceptStaffInvite(token: string) {
    return this.http.post<void>(`${this.api}/entrada/convites/equipe/aceitar`, { token })
      .pipe(timeout(15000));
  }
}
