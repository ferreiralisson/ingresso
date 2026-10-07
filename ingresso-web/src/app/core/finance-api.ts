import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from './api';

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

@Injectable({ providedIn: 'root' })
export class FinanceApi {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  producerEvent(eventId: number) {
    return this.http.get<EventFinancialReport>(`${this.api}/produtor/eventos/${eventId}/financeiro`)
      .pipe(timeout(15000));
  }

  administrator() {
    return this.http.get<AdminFinancialReport>(`${this.api}/admin/financeiro`).pipe(timeout(15000));
  }
}