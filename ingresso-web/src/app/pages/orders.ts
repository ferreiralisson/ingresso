import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { OrderApi, OrderResponse, OrderStatus } from '../core/order-api';
import { TicketQr } from './ticket-qr';

@Component({
  imports: [RouterLink, TicketQr],
  template: `
    <section class="market-page">
      <div class="market-heading">
        <div>
          <span class="section-kicker">COMPRAS</span>
          <h1>Meus pedidos<span class="accent">.</span></h1>
          <p class="market-intro">Acompanhe pedidos e pagamentos.</p>
        </div>
        <a class="market-action" routerLink="/eventos">Explorar eventos <span>↗</span></a>
      </div>
      @if (loading()) {
        <p class="market-message" role="status">Carregando pedidos...</p>
      } @else if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      } @else if (orders().length === 0) {
        <div class="market-empty">
          <span class="section-kicker">SEU HISTÓRICO</span>
          <h2>Você ainda não fez pedidos.</h2>
          <p>Quando fizer uma compra, ela aparecerá aqui.</p>
          <a class="button-primary event-cta" routerLink="/eventos">Encontrar um evento <span>↗</span></a>
        </div>
      } @else {
        <div class="order-history-list">
          @for (order of orders(); track order.id) {
            <article class="order-history-item">
              <div class="order-history-heading">
                <div>
                  <span class="section-kicker">PEDIDO #{{ order.id }}</span>
                  <h2>{{ order.eventTitle }}</h2>
                </div>
                <span class="event-status" [class]="'event-status order-status-' + order.status.toLowerCase()">
                  {{ statusLabel(order.status) }}
                </span>
              </div>
              <div class="order-history-meta">
                <span>{{ formatDate(order.createdAt) }}</span>
                <strong>{{ formatPrice(order.totalInCents) }}</strong>
              </div>
              <ul class="order-history-lines">
                @for (item of order.items; track item.categoryId) {
                  <li>
                    <span>{{ item.categoryName }} × {{ item.quantity }}</span>
                    <span>{{ formatPrice(item.unitPriceInCents * item.quantity) }}</span>
                  </li>
                  @for (seat of item.seats; track seat.id) {
                    <li class="order-seat-line"><span>{{ seat.sector }} · Fileira {{ seat.row }} · Assento {{ seat.label }}</span></li>
                  }
                }
              </ul>
              @if (order.payment) {
                <p class="order-payment-note">Pagamento: {{ statusLabelForPayment(order.payment.outcome) }}</p>
              }
              @if (order.status === 'PENDING_PAYMENT') {
                <p class="order-payment-note">Reserva até {{ formatDate(order.reservationExpiresAt) }}</p>
              }
              @if (order.status === 'PAID') {
                @if (order.tickets.length === 0) {
                  <p class="order-payment-note">Os ingressos ainda não estão disponíveis.</p>
                } @else {
                  <div class="issued-ticket-list" aria-label="Ingressos deste pedido">
                    @for (ticket of order.tickets; track ticket.id) {
                      <section class="issued-ticket">
                        <div class="issued-ticket-details">
                          <span class="section-kicker">INGRESSO {{ ticket.unitNumber }}</span>
                          <h3>{{ ticket.eventTitle }}</h3>
                          <p>{{ formatDate(ticket.eventStartsAt) }}</p>
                          <p>{{ ticket.venueName }} · {{ ticket.city }} / {{ ticket.stateCode }}</p>
                          <p>{{ ticket.categoryName }}</p>
                          @if (ticket.sector && ticket.row && ticket.seatLabel) {
                            <p>Setor {{ ticket.sector }} · Fileira {{ ticket.row }} · Assento {{ ticket.seatLabel }}</p>
                          }
                        </div>
                        @if (ticket.refundedAt) {
                          <p class="order-payment-note">Reembolsado · valor simulado; nenhum dinheiro foi movimentado.</p>
                        } @else if (ticket.usedAt) {
                          <p class="order-payment-note">Ingresso utilizado em {{ formatDate(ticket.usedAt) }}</p>
                        } @else if (ticket.qrCodeValue) {
                          <div class="issued-ticket-qr">
                            <app-ticket-qr [value]="ticket.qrCodeValue" [label]="ticket.categoryName" />
                            <span>Apresente na entrada</span>
                          </div>
                        }
                      </section>
                      @if (canRequestRefund(order.status, ticket)) {
                        <button class="text-link refund-ticket-action" type="button"
                          [disabled]="refundingTicketId() === ticket.id"
                          (click)="requestRefund(order.id, ticket.id)">
                          {{ refundingTicketId() === ticket.id ? 'Solicitando...' : 'Solicitar reembolso deste ingresso' }}
                        </button>
                      }
                      @if (refundMessages()[ticket.id]) {
                        <p class="order-payment-note" role="status">{{ refundMessages()[ticket.id] }}</p>
                      }
                    }
                  </div>
                }
              }
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class Orders implements OnInit {
  private readonly api = inject(OrderApi);
  protected readonly orders = signal<OrderResponse[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly refundingTicketId = signal<number | null>(null);
  protected readonly refundMessages = signal<Record<number, string>>({});

  ngOnInit(): void {
    this.api.listMine().subscribe({
      next: (page) => {
        this.orders.set(page.content);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Entre na conta usada para fazer os pedidos.'
          : 'Não foi possível carregar seus pedidos. Tente novamente.');
        this.loading.set(false);
      },
    });
  }

  protected statusLabel(status: OrderStatus): string {
    return status === 'PAID' ? 'Pago' : status === 'PENDING_PAYMENT' ? 'Pendente'
      : status === 'PAYMENT_DECLINED' ? 'Recusado' : status === 'EXPIRED' ? 'Expirado' : 'Cancelado';
  }

  protected statusLabelForPayment(status: string): string {
    return status === 'APPROVED' ? 'Aprovado' : status === 'DECLINED' ? 'Recusado' : 'Pendente';
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    }).format(new Date(value));
  }

  protected formatPrice(cents: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
  }

  protected canRequestRefund(status: OrderStatus, ticket: OrderResponse['tickets'][number]): boolean {
    return status === 'PAID' && !ticket.usedAt && !ticket.refundedAt
      && new Date(ticket.eventStartsAt).getTime() > Date.now();
  }

  protected requestRefund(orderId: number, ticketId: number): void {
    this.refundingTicketId.set(ticketId);
    this.api.requestTicketRefund(orderId, ticketId).subscribe({
      next: (refund) => {
        this.orders.update((orders) => orders.map((order) => order.id !== orderId ? order : {
          ...order,
          tickets: order.tickets.map((ticket) => ticket.id !== ticketId ? ticket : {
            ...ticket,
            qrCodeValue: null,
            refundedAt: refund.createdAt,
          }),
        }));
        this.refundMessages.update((messages) => ({
          ...messages,
          [ticketId]: `${this.formatPrice(refund.amountInCents)} · reembolso simulado; nenhum dinheiro foi movimentado.`,
        }));
        this.refundingTicketId.set(null);
      },
      error: () => {
        this.refundMessages.update((messages) => ({
          ...messages,
          [ticketId]: 'Não foi possível solicitar o reembolso. Verifique a elegibilidade do ingresso.',
        }));
        this.refundingTicketId.set(null);
      },
    });
  }

}