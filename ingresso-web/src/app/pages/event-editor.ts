import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  AdmissionMode,
  EventApi,
  EventDetail,
  EventInput,
  EventSeatSectorInput,
} from '../core/event-api';

interface CategoryDraft {
  code: string;
  name: string;
  price: string;
  admissionMode: AdmissionMode;
  quantity: number | null;
}

interface SeatDraft {
  label: string;
  positionIndex: number;
  categoryCode: string;
}

interface RowDraft {
  label: string;
  positionIndex: number;
  seatCount: number;
  seats: SeatDraft[];
}

interface SectorDraft {
  name: string;
  positionIndex: number;
  rows: RowDraft[];
}

@Component({
  imports: [FormsModule, RouterLink],
  template: `
    <section class="market-page event-editor-page">
      <a class="text-link" routerLink="/produtor/eventos">← Voltar aos meus eventos</a>
      <div class="market-heading editor-heading">
        <div>
          <span class="section-kicker">PRODUTOR</span>
          <h1>{{ eventId() ? 'Editar evento' : 'Novo evento' }}<span class="accent">.</span></h1>
          <p class="market-intro">
            {{ eventId() ? 'Atualize os detalhes e as ofertas do evento.' : 'Cadastre os detalhes e publique seu evento.' }}
          </p>
        </div>
      </div>

      @if (loading()) {
        <p class="market-message" role="status">Carregando evento...</p>
      } @else {
        @if (error()) { <div class="notice error" role="alert">{{ error() }}</div> }
        <form class="event-editor-form" (ngSubmit)="save()">
          <section class="editor-section">
            <div class="editor-section-heading">
              <span class="section-kicker">01 / EVENTO</span>
              <h2>Informações públicas</h2>
            </div>
            <div class="event-fields-grid">
              <label class="event-field field-wide">
                <span>Título</span>
                <input name="title" required maxlength="160" [(ngModel)]="title" />
              </label>
              <label class="event-field field-wide">
                <span>Descrição</span>
                <textarea name="description" required maxlength="5000" rows="5" [(ngModel)]="description"></textarea>
              </label>
              <label class="event-field field-wide">
                <span>URL da imagem (HTTPS)</span>
                <input name="imageUrl" type="url" required placeholder="https://..." [(ngModel)]="imageUrl" />
              </label>
              <label class="event-field">
                <span>Data e horário</span>
                <input name="startsAt" type="datetime-local" required [(ngModel)]="startsAt" />
              </label>
              <label class="event-field">
                <span>Classificação indicativa</span>
                <input name="ageClassification" required maxlength="80" [(ngModel)]="ageClassification" />
              </label>
              <label class="event-field">
                <span>Local</span>
                <input name="venueName" required maxlength="200" [(ngModel)]="venueName" />
              </label>
              <label class="event-field">
                <span>Endereço</span>
                <input name="streetAddress" required maxlength="300" [(ngModel)]="streetAddress" />
              </label>
              <label class="event-field">
                <span>Cidade</span>
                <input name="city" required maxlength="120" [(ngModel)]="city" />
              </label>
              <label class="event-field">
                <span>UF</span>
                <input name="stateCode" required minlength="2" maxlength="2" autocomplete="address-level1" [(ngModel)]="stateCode" />
              </label>
              <label class="event-field field-wide">
                <span>Organizador</span>
                <input name="organizer" required maxlength="200" [(ngModel)]="organizer" />
              </label>
            </div>
            <p class="field-hint">Horários são interpretados no fuso America/Sao_Paulo.</p>
          </section>

          <section class="editor-section">
            <div class="editor-section-heading editor-heading-row">
              <div>
                <span class="section-kicker">02 / OFERTAS</span>
                <h2>Categorias de ingresso</h2>
              </div>
              <button type="button" class="button-secondary" (click)="addCategory()">Adicionar categoria <span>＋</span></button>
            </div>
            <div class="category-editor-list">
              @for (category of categories; track category.code; let index = $index) {
                <article class="category-editor-row">
                  <label class="event-field">
                    <span>Nome da categoria</span>
                    <input [value]="category.name" required maxlength="120" (input)="category.name = $any($event.target).value" />
                  </label>
                  <div class="event-field">
                    <span>Modalidade</span>
                    <div class="segmented-control" role="group" [attr.aria-label]="'Modalidade de ' + category.name">
                      <button type="button" [class.selected]="category.admissionMode === 'GENERAL_ADMISSION'" (click)="setCategoryMode(category, 'GENERAL_ADMISSION')">Geral</button>
                      <button type="button" [class.selected]="category.admissionMode === 'ASSIGNED_SEAT'" (click)="setCategoryMode(category, 'ASSIGNED_SEAT')">Lugar marcado</button>
                    </div>
                  </div>
                  <label class="event-field">
                    <span>Preço (R$)</span>
                    <input [value]="category.price" type="number" min="0" step="0.01" required (input)="category.price = $any($event.target).value" />
                  </label>
                  @if (category.admissionMode === 'GENERAL_ADMISSION') {
                    <label class="event-field">
                      <span>Quantidade</span>
                      <input [value]="category.quantity" type="number" min="1" step="1" required (input)="category.quantity = $any($event.target).valueAsNumber" />
                    </label>
                  } @else {
                    <div class="category-derived-quantity">
                      <span>Quantidade</span>
                      <strong>{{ assignedSeatCount(category.code) }} assentos</strong>
                    </div>
                  }
                  <button type="button" class="icon-action" [attr.aria-label]="'Remover ' + category.name" [disabled]="categories.length === 1 || categoryHasSeats(category.code)" (click)="removeCategory(index)">×</button>
                </article>
              }
            </div>
            <p class="field-hint">Valores em BRL. Ingressos gratuitos são permitidos. Para assentos marcados, a quantidade é calculada pelo mapa.</p>
          </section>

          @if (hasAssignedCategories()) {
            <section class="editor-section">
              <div class="editor-section-heading editor-heading-row">
                <div>
                  <span class="section-kicker">03 / ASSENTOS</span>
                  <h2>Mapa do evento</h2>
                </div>
                <button type="button" class="button-secondary" (click)="addSector()">Adicionar setor <span>＋</span></button>
              </div>
              <p class="field-hint">Gere fileiras e assentos; cada lugar recebe uma categoria e herda seu preço.</p>
              @for (sector of sectors; track sector.positionIndex; let sectorIndex = $index) {
                <section class="map-sector-editor">
                  <div class="map-sector-heading">
                    <label class="event-field">
                      <span>Setor</span>
                      <input [value]="sector.name" required maxlength="100" (input)="sector.name = $any($event.target).value" />
                    </label>
                    <button type="button" class="text-link danger-link" (click)="removeSector(sectorIndex)">Remover setor</button>
                    <button type="button" class="button-secondary" (click)="addRow(sectorIndex)">Gerar fileira <span>＋</span></button>
                  </div>
                  @for (row of sector.rows; track row.positionIndex; let rowIndex = $index) {
                    <div class="map-row-editor">
                      <div class="map-row-heading">
                        <label class="event-field row-label-field">
                          <span>Fileira</span>
                          <input [value]="row.label" required maxlength="20" (input)="row.label = $any($event.target).value" />
                        </label>
                        <label class="event-field row-count-field">
                          <span>Lugares</span>
                          <input [value]="row.seatCount" type="number" min="1" step="1" required (input)="row.seatCount = $any($event.target).valueAsNumber" />
                        </label>
                        <button type="button" class="button-secondary" (click)="generateSeats(sectorIndex, rowIndex)">Atualizar fileira</button>
                        <button type="button" class="text-link danger-link" (click)="removeRow(sectorIndex, rowIndex)">Remover</button>
                      </div>
                      <div class="seat-editor-grid">
                        @for (seat of row.seats; track seat.positionIndex) {
                          <label class="seat-editor-tile">
                            <span>{{ seat.label }}</span>
                            <select [value]="seat.categoryCode" required (change)="seat.categoryCode = $any($event.target).value">
                              @for (category of assignedCategories(); track category.code) {
                                <option [value]="category.code">{{ category.name }}</option>
                              }
                            </select>
                          </label>
                        }
                      </div>
                    </div>
                  }
                  @if (!sector.rows.length) { <p class="field-hint">Adicione uma fileira para começar a montar este setor.</p> }
                </section>
              }
              @if (!sectors.length) {
                <div class="map-empty">Adicione um setor e gere fileiras para configurar os assentos.</div>
              }
            </section>
          }

          <div class="event-editor-actions">
            <a class="button-secondary" routerLink="/produtor/eventos">Descartar</a>
            <button class="button-primary" type="submit" [disabled]="saving()">
              {{ saving() ? 'Salvando…' : eventId() ? 'Salvar alterações' : 'Publicar evento' }}
              <span aria-hidden="true">{{ saving() ? '◌' : '↗' }}</span>
            </button>
          </div>
        </form>
      }
    </section>
  `,
})
export class EventEditor implements OnInit {
  private readonly api = inject(EventApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly eventId = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected title = '';
  protected description = '';
  protected imageUrl = '';
  protected startsAt = '';
  protected venueName = '';
  protected streetAddress = '';
  protected city = '';
  protected stateCode = '';
  protected ageClassification = '';
  protected organizer = '';
  protected categories: CategoryDraft[] = [];
  protected sectors: SectorDraft[] = [];
  private categoryCounter = 0;

  ngOnInit(): void {
    const idValue = this.route.snapshot.paramMap.get('id');
    if (!idValue) {
      this.addCategory();
      return;
    }
    const id = Number(idValue);
    if (!Number.isSafeInteger(id) || id < 1) {
      this.error.set('Não foi possível encontrar este evento.');
      return;
    }
    this.eventId.set(id);
    this.loading.set(true);
    this.api.getProducer(id).subscribe({
      next: (event) => {
        this.populate(event);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Esta conta não tem permissão de produtor.'
          : 'Não foi possível carregar o evento.');
        this.loading.set(false);
      },
    });
  }

  protected hasAssignedCategories(): boolean {
    return this.categories.some((category) => category.admissionMode === 'ASSIGNED_SEAT');
  }

  protected assignedCategories(): CategoryDraft[] {
    return this.categories.filter((category) => category.admissionMode === 'ASSIGNED_SEAT');
  }

  protected categoryHasSeats(code: string): boolean {
    return this.sectors.some((sector) => sector.rows.some((row) => row.seats.some((seat) => seat.categoryCode === code)));
  }

  protected assignedSeatCount(code: string): number {
    return this.sectors.reduce((count, sector) =>
      count + sector.rows.reduce((rowCount, row) =>
        rowCount + row.seats.filter((seat) => seat.categoryCode === code).length, 0), 0);
  }

  protected addCategory(): void {
    this.categories = [...this.categories, {
      code: this.newCategoryCode(),
      name: '',
      price: '0.00',
      admissionMode: 'GENERAL_ADMISSION',
      quantity: 1,
    }];
  }

  protected removeCategory(index: number): void {
    const category = this.categories[index];
    if (this.categoryHasSeats(category.code)) {
      this.error.set('Remova ou reclassifique os assentos desta categoria antes de removê-la.');
      return;
    }
    this.categories = this.categories.filter((_, categoryIndex) => categoryIndex !== index);
    this.error.set('');
  }

  protected setCategoryMode(category: CategoryDraft, mode: AdmissionMode): void {
    if (mode === category.admissionMode) return;
    if (mode === 'GENERAL_ADMISSION' && this.categoryHasSeats(category.code)) {
      this.error.set('Reclassifique os assentos desta categoria antes de alterar a modalidade.');
      return;
    }
    category.admissionMode = mode;
    category.quantity = mode === 'GENERAL_ADMISSION' ? 1 : null;
    this.error.set('');
  }

  protected addSector(): void {
    this.sectors = [...this.sectors, { name: `Setor ${this.sectors.length + 1}`, positionIndex: this.sectors.length, rows: [] }];
  }

  protected removeSector(index: number): void {
    this.sectors = this.sectors.filter((_, sectorIndex) => sectorIndex !== index)
      .map((sector, positionIndex) => ({ ...sector, positionIndex }));
  }

  protected addRow(sectorIndex: number): void {
    const firstCategory = this.assignedCategories()[0]?.code;
    if (!firstCategory) {
      this.error.set('Adicione uma categoria com lugar marcado antes de gerar fileiras.');
      return;
    }
    const sector = this.sectors[sectorIndex];
    const row: RowDraft = {
      label: String.fromCharCode(65 + sector.rows.length),
      positionIndex: sector.rows.length,
      seatCount: 8,
      seats: this.buildSeats(8, firstCategory),
    };
    this.sectors = this.sectors.map((current, index) => index === sectorIndex
      ? { ...current, rows: [...current.rows, row] }
      : current);
    this.error.set('');
  }

  protected generateSeats(sectorIndex: number, rowIndex: number): void {
    const row = this.sectors[sectorIndex].rows[rowIndex];
    const count = Math.floor(Number(row.seatCount));
    if (!Number.isFinite(count) || count < 1) {
      this.error.set('A fileira precisa ter ao menos um assento.');
      return;
    }
    const defaultCategory = this.assignedCategories()[0]?.code;
    if (!defaultCategory) {
      this.error.set('Adicione uma categoria com lugar marcado antes de gerar fileiras.');
      return;
    }
    const seats = Array.from({ length: count }, (_, index) => ({
      label: String(index + 1),
      positionIndex: index,
      categoryCode: row.seats[index]?.categoryCode ?? defaultCategory,
    }));
    this.updateRow(sectorIndex, rowIndex, { ...row, seatCount: count, seats });
    this.error.set('');
  }

  protected removeRow(sectorIndex: number, rowIndex: number): void {
    const sector = this.sectors[sectorIndex];
    this.sectors = this.sectors.map((current, index) => index === sectorIndex
      ? { ...current, rows: current.rows.filter((_, currentRow) => currentRow !== rowIndex)
          .map((row, positionIndex) => ({ ...row, positionIndex })) }
      : current);
  }

  protected save(): void {
    this.error.set('');
    const payload = this.buildPayload();
    if (!payload) return;
    this.saving.set(true);
    const request = this.eventId()
      ? this.api.update(this.eventId()!, payload)
      : this.api.create(payload);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/produtor/eventos']);
      },
      error: (error: unknown) => {
        const response = error instanceof HttpErrorResponse ? error.error : null;
        this.error.set(error instanceof HttpErrorResponse && error.status === 403
          ? 'Esta conta não tem permissão de produtor.'
          : response && typeof response.message === 'string'
            ? response.message
            : 'Não foi possível salvar o evento. Confira os campos e tente novamente.');
        this.saving.set(false);
      },
    });
  }

  private buildPayload(): EventInput | null {
    if (this.categories.some((category) => !category.name.trim())) {
      this.error.set('Informe o nome de cada categoria.');
      return null;
    }
    const pricesInCents = this.categories.map((category) => {
      const rawPrice = String(category.price ?? '').trim().replace(',', '.');
      const price = rawPrice === '' ? Number.NaN : Number(rawPrice);
      return Number.isFinite(price) && price >= 0 ? Math.round(price * 100) : Number.NaN;
    });
    if (pricesInCents.some((price) => !Number.isSafeInteger(price))) {
      this.error.set('Informe um preço válido em reais para cada categoria.');
      return null;
    }
    const categories = this.categories.map((category, index) => ({
      code: category.code,
      name: category.name.trim(),
      priceInCents: pricesInCents[index],
      admissionMode: category.admissionMode,
      quantity: category.admissionMode === 'GENERAL_ADMISSION' ? Number(category.quantity) : null,
    }));
    if (categories.some((category) => category.admissionMode === 'GENERAL_ADMISSION' && (!Number.isInteger(category.quantity) || category.quantity! < 1))) {
      this.error.set('Cada categoria sem assento marcado precisa ter uma quantidade positiva.');
      return null;
    }
    if (categories.some((category) => category.admissionMode === 'ASSIGNED_SEAT' && this.assignedSeatCount(category.code) === 0)) {
      this.error.set('Cada categoria com assento marcado precisa estar atribuída a pelo menos um lugar.');
      return null;
    }
    if (this.categories.some((category) => category.admissionMode === 'ASSIGNED_SEAT') && !this.sectors.length) {
      this.error.set('Adicione setores e assentos ao mapa.');
      return null;
    }

    const sectors: EventSeatSectorInput[] = this.sectors.map((sector) => ({
      name: sector.name.trim(),
      positionIndex: sector.positionIndex,
      rows: sector.rows.map((row) => ({
        label: row.label.trim(),
        positionIndex: row.positionIndex,
        seats: row.seats.map((seat) => ({ ...seat })),
      })),
    }));

    return {
      title: this.title.trim(),
      description: this.description.trim(),
      imageUrl: this.imageUrl.trim(),
      startsAt: this.startsAt,
      venueName: this.venueName.trim(),
      streetAddress: this.streetAddress.trim(),
      city: this.city.trim(),
      stateCode: this.stateCode.trim().toUpperCase(),
      ageClassification: this.ageClassification.trim(),
      organizer: this.organizer.trim(),
      categories,
      sectors,
    };
  }

  private populate(event: EventDetail): void {
    this.title = event.title;
    this.description = event.description;
    this.imageUrl = event.imageUrl;
    this.startsAt = this.toLocalInput(event.startsAt);
    this.venueName = event.venue.name;
    this.streetAddress = event.venue.streetAddress;
    this.city = event.venue.city;
    this.stateCode = event.venue.stateCode;
    this.ageClassification = event.ageClassification;
    this.organizer = event.organizer;
    this.categories = event.categories.map((category) => ({
      code: category.code,
      name: category.name,
      price: (category.priceInCents / 100).toFixed(2),
      admissionMode: category.admissionMode,
      quantity: category.admissionMode === 'GENERAL_ADMISSION' ? category.configuredQuantity : null,
    }));
    this.sectors = event.sectors.map((sector) => ({
      name: sector.name,
      positionIndex: sector.positionIndex,
      rows: sector.rows.map((row) => ({
        label: row.label,
        positionIndex: row.positionIndex,
        seatCount: row.seats.length,
        seats: row.seats.map((seat) => ({
          label: seat.label,
          positionIndex: seat.positionIndex,
          categoryCode: seat.categoryCode,
        })),
      })),
    }));
  }

  private toLocalInput(value: string): string {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Sao_Paulo',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(new Date(value));
    const part = (type: string) => parts.find((item) => item.type === type)?.value ?? '';
    return `${part('year')}-${part('month')}-${part('day')}T${part('hour')}:${part('minute')}`;
  }

  private updateRow(sectorIndex: number, rowIndex: number, row: RowDraft): void {
    this.sectors = this.sectors.map((sector, index) => index === sectorIndex
      ? { ...sector, rows: sector.rows.map((current, currentRow) => currentRow === rowIndex ? row : current) }
      : sector);
  }

  private buildSeats(count: number, categoryCode: string): SeatDraft[] {
    return Array.from({ length: count }, (_, index) => ({
      label: String(index + 1),
      positionIndex: index,
      categoryCode,
    }));
  }

  private newCategoryCode(): string {
    let code: string;
    do {
      code = `category-${++this.categoryCounter}`;
    } while (this.categories.some((category) => category.code === code));
    return code;
  }
}