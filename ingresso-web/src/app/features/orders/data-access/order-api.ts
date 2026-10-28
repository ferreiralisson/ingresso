import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { CreateOrderRequest, OrderPage, OrderResponse, TicketRefundResponse } from '../models/order.models';

@Injectable({ providedIn: 'root' })
export class OrderApi {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  create(request: CreateOrderRequest, idempotencyKey: string) {
    return this.http.post<OrderResponse>(`${this.api}/pedidos`, request, {
      headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }),
    }).pipe(timeout(15000));
  }

  startPayment(orderId: number) {
    return this.http.post<OrderResponse>(`${this.api}/pedidos/${orderId}/pagamento`, {}).pipe(timeout(15000));
  }

  listMine(page = 0, size = 20) {
    return this.http.get<OrderPage>(`${this.api}/pedidos`, { params: { page, size, sort: 'createdAt,desc' } })
      .pipe(timeout(15000));
  }

  getMine(orderId: number) {
    return this.http.get<OrderResponse>(`${this.api}/pedidos/${orderId}`).pipe(timeout(15000));
  }

  requestTicketRefund(orderId: number, ticketId: number, reason?: string) {
    return this.http.post<TicketRefundResponse>(`${this.api}/pedidos/${orderId}/tickets/${ticketId}/reembolsos`,
      reason ? { reason } : {}).pipe(timeout(15000));
  }
}
