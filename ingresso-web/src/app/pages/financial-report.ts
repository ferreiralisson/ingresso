import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminFinancialReport, EventFinancialReport, FinanceApi } from '../core/finance-api';

@Component({
  imports: [RouterLink],
  template: `
    <section class="market-page finance-page">
      <div class="market-heading">
        <div>
          <span class="section-kicker">FINANCEIRO · VALORES BRUTOS</span>
          <h1>{{ isAdmin() ? 'Resumo da plataforma' : report()?.eventTitle || 'Relatório do evento' }}<span class="accent">.</span></h1>
          <p class="market-intro">Reembolsos simulados; comissão e repasse não estão definidos.</p>
        </div>
        <a class="text-link" [routerLink]="isAdmin() ? '/admin/eventos' : '/produtor/eventos'">Voltar</a>
      </div>

      @if (loading()) {
        <p class="market-message" role="status">Carregando relatório...</p>
      } @else if (error()) {
        <div class="notice error" role="alert">{{ error() }}</div>
      } @else if (isAdmin()) {
        @if (adminReport(); as data) {
          <div class="finance-summary" aria-label="Totais financeiros da plataforma">
            <div><span>Vendas brutas</span><strong>{{ formatPrice(data.grossSalesInCents) }}</strong></div>
            <div><span>Reembolsos simulados</span><strong>{{ formatPrice(data.simulatedRefundsInCents) }}</strong></div>
            <div><span>Valor bruto restante</span><strong>{{ formatPrice(data.remainingGrossInCents) }}</strong></div>
            <div><span>Pedidos / ingressos / reembolsos</span><strong>{{ data.paidOrderCount }} / {{ data.paidTicketCount }} / {{ data.refundedTicketCount }}</strong></div>
          </div>
          <div class="finance-event-list">
            @for (event of data.events; track event.eventId) {
              <article class="finance-event-row">
                <div><span class="section-kicker">EVENTO #{{ event.eventId }}</span><h2>{{ event.eventTitle }}</h2></div>
                <span>{{ formatPrice(event.grossSalesInCents) }}</span>
                <span>{{ formatPrice(event.simulatedRefundsInCents) }} reembolsados</span>
              </article>
            } @empty {
              <p class="market-message">Ainda não há vendas registradas.</p>
            }
          </div>
        }
      } @else if (report(); as data) {
        <div class="finance-summary" aria-label="Totais financeiros do evento">
          <div><span>Vendas brutas</span><strong>{{ formatPrice(data.grossSalesInCents) }}</strong></div>
          <div><span>Reembolsos simulados</span><strong>{{ formatPrice(data.simulatedRefundsInCents) }}</strong></div>
          <div><span>Valor bruto restante</span><strong>{{ formatPrice(data.remainingGrossInCents) }}</strong></div>
          <div><span>Pedidos / ingressos / reembolsos</span><strong>{{ data.paidOrderCount }} / {{ data.paidTicketCount }} / {{ data.refundedTicketCount }}</strong></div>
        </div>
      }
      <p class="finance-disclaimer">Valores calculados pelos preços snapshot dos ingressos. Reembolsos são simulados e não movimentam dinheiro.</p>
    </section>
  `,
})
export class FinancialReport implements OnInit {
  private readonly api = inject(FinanceApi);
  private readonly route = inject(ActivatedRoute);
  protected readonly isAdmin = signal(false);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly report = signal<EventFinancialReport | null>(null);
  protected readonly adminReport = signal<AdminFinancialReport | null>(null);

  ngOnInit(): void {
    const eventIdValue = this.route.snapshot.paramMap.get('id');
    if (eventIdValue) {
      const eventId = Number(eventIdValue);
      if (!Number.isSafeInteger(eventId) || eventId < 1) {
        this.error.set('Evento inválido.');
        this.loading.set(false);
        return;
      }
      this.api.producerEvent(eventId).subscribe({
        next: (report) => { this.report.set(report); this.loading.set(false); },
        error: (error: unknown) => {
          this.error.set(error instanceof HttpErrorResponse && error.status === 404
            ? 'Evento não encontrado ou sem acesso.' : 'Não foi possível carregar o relatório.');
          this.loading.set(false);
        },
      });
      return;
    }
    this.isAdmin.set(true);
    this.api.administrator().subscribe({
      next: (report) => { this.adminReport.set(report); this.loading.set(false); },
      error: () => { this.error.set('Não foi possível carregar o resumo financeiro.'); this.loading.set(false); },
    });
  }

  protected formatPrice(cents: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cents / 100);
  }
}