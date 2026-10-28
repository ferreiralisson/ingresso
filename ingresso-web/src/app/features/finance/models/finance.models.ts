export interface EventFinancialReport {
  eventId: number;
  eventTitle: string;
  paidOrderCount: number;
  paidTicketCount: number;
  refundedTicketCount: number;
  grossSalesInCents: number;
  simulatedRefundsInCents: number;
  remainingGrossInCents: number;
}

export interface AdminFinancialReport {
  paidOrderCount: number;
  paidTicketCount: number;
  refundedTicketCount: number;
  grossSalesInCents: number;
  simulatedRefundsInCents: number;
  remainingGrossInCents: number;
  events: EventFinancialReport[];
}
