import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { AdminFinancialReport, EventFinancialReport } from '../models/finance.models';

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
