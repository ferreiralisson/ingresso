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
