import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from './api';

export type EntryOutcome =
  | 'ACCEPTED'
  | 'ALREADY_USED'
  | 'REFUNDED'
  | 'INVALID'
  | 'WRONG_EVENT'
  | 'EVENT_UNAVAILABLE'
  | 'OUTSIDE_EVENT_DAY'
  | 'OFFLINE_CONFLICT'
  | 'NOT_IN_OFFLINE_MANIFEST'
  | 'MANIFEST_EXPIRED'
  | 'OPERATOR_UNAUTHORIZED';

export interface EntryResult {
  outcome: EntryOutcome;
  message: string;
  ticketId: number | null;
  checkInId: number;
  processedAt: string;
  previousUseAt: string | null;
  previousOperator: string | null;
  source: 'ONLINE' | 'OFFLINE';
  deviceScannedAt: string | null;
  synchronizedAt: string | null;
  operatorEmail: string;
  correctedAt: string | null;
  correctionReason: string | null;
  correctedBy: string | null;
}

export interface EntryHistoryPage {
  content: EntryResult[];
  number: number;
  totalPages: number;
}

export interface OfflineManifest {
  id: string;
  eventId: number;
  operatorEmail: string;
  eventTitle: string;
  eventStartsAt: string;
  createdAt: string;
  expiresAt: string;
  tickets: Array<{
    id: number;
    qrTokenHash: string;
    used: boolean;
    categoryName: string;
    unitNumber: number;
    sector: string | null;
    row: string | null;
    seatLabel: string | null;
  }>;
}

export interface PendingOfflineScan {
  eventId: number;
  manifestId: string;
  deviceId: string;
  scanId: string;
  qrTokenHash: string;
  scannedAt: string;
}

export interface EventStaffInvite {
  id: number;
  eventId: number;
  email: string;
  token: string;
  expiresAt: string;
}

export interface EventStaffGrant {
  id: number;
  userId: number;
  email: string;
  acceptedAt: string;
}

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
    return this.http.post<EventStaffInvite>(`${this.api}/entrada/eventos/${eventId}/equipe`, { email }).pipe(timeout(15000));
  }

  listStaff(eventId: number) {
    return this.http.get<EventStaffGrant[]>(`${this.api}/entrada/eventos/${eventId}/equipe`).pipe(timeout(15000));
  }

  revokeStaff(eventId: number, invitationId: number) {
    return this.http.delete<void>(`${this.api}/entrada/eventos/${eventId}/equipe/${invitationId}`).pipe(timeout(15000));
  }

  acceptStaffInvite(token: string) {
    return this.http.post<void>(`${this.api}/entrada/convites/equipe/aceitar`, { token }).pipe(timeout(15000));
  }
}