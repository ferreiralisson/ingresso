export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'PAYMENT_DECLINED'
  | 'EXPIRED'
  | 'CANCELLED';

export type PaymentOutcome = 'PENDING' | 'APPROVED' | 'DECLINED';

export interface CreateOrderRequest {
  eventId: number;
  items: Array<{
    categoryId: number;
    quantity: number;
    seatIds?: number[];
  }>;
}

export interface OrderResponse {
  id: number;
  eventId: number;
  eventTitle: string;
  status: OrderStatus;
  totalInCents: number;
  createdAt: string;
  reservationExpiresAt: string;
  paidAt: string | null;
  items: Array<{
    categoryId: number;
    categoryCode: string;
    categoryName: string;
    admissionMode: string;
    unitPriceInCents: number;
    quantity: number;
    seats: Array<{ id: number; sector: string; row: string; label: string }>;
  }>;
  payment: {
    attemptKey: string;
    provider: string;
    outcome: PaymentOutcome;
    startedAt: string;
    completedAt: string | null;
  } | null;
  tickets: OrderTicket[];
}

export interface OrderTicket {
  id: number;
  categoryName: string;
  unitNumber: number;
  eventSeatId: number | null;
  sector: string | null;
  row: string | null;
  seatLabel: string | null;
  qrCodeValue: string | null;
  usedAt: string | null;
  refundedAt: string | null;
  issuedAt: string;
  eventTitle: string;
  eventStartsAt: string;
  venueName: string;
  streetAddress: string;
  city: string;
  stateCode: string;
}

export interface TicketRefundResponse {
  id: number;
  ticketId: number;
  orderId: number;
  eventId: number;
  source: 'BUYER_REQUEST' | 'EVENT_CANCELLATION';
  status: 'SIMULATED';
  amountInCents: number;
  reason: string | null;
  createdAt: string;
}

export interface OrderPage {
  content: OrderResponse[];
  number: number;
  totalPages: number;
  totalElements: number;
}
