import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService, SESSION_KEY } from './core/auth.service';

describe('Fluxos dos formulários', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    TestBed.inject(AuthService).ngOnDestroy();
    sessionStorage.clear();
  });

  async function render(path: string) {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl(path);
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return {
      fixture,
      el,
      fill: (id: string, value: string) => {
        const input = el.querySelector<HTMLInputElement>(`#${id}`)!;
        input.value = value;
        input.dispatchEvent(new Event('input'));
      },
      submit: () => {
        el.querySelector('form')!.dispatchEvent(
          new Event('submit', { bubbles: true, cancelable: true }),
        );
        fixture.detectChanges();
      },
    };
  }

  it('impede envio de login vazio e exibe validações', async () => {
    const page = await render('/entrar');
    page.submit();
    expect(page.el.textContent).toContain('Informe um e-mail válido.');
    expect(page.el.textContent).toContain('Informe sua senha.');
    http.expectNone('/api/auth/login');
  });

  it('carrega catálogo público sem enviar Bearer para visitante', async () => {
    const page = await render('/eventos');
    const request = http.expectOne((candidate) => candidate.url === '/api/eventos');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.params.get('sort')).toBe('startsAt,asc');
    request.flush({
      content: [{
        id: 41,
        title: 'Festival de Verão',
        imageUrl: 'https://images.example.com/festival.jpg',
        startsAt: '2026-12-10T19:30:00-03:00',
        city: 'Sao Paulo',
        stateCode: 'SP',
        lowestPriceInCents: 0,
        status: 'PUBLISHED',
      }],
      number: 0,
      totalPages: 1,
      totalElements: 1,
      first: true,
      last: true,
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Festival de Verão');
    expect(page.el.textContent).toContain('Gratuito');
  });

  it('mostra detalhes, categorias e mapa de assentos do evento público', async () => {
    const page = await render('/eventos/41');
    const request = http.expectOne('/api/eventos/41');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({
      id: 41,
      title: 'Festival de Verão',
      description: 'Uma noite ao vivo.',
      imageUrl: 'https://images.example.com/festival.jpg',
      startsAt: '2026-12-10T19:30:00-03:00',
      timeZone: 'America/Sao_Paulo',
      venue: { name: 'Teatro Central', streetAddress: 'Rua Principal, 10', city: 'Sao Paulo', stateCode: 'SP' },
      ageClassification: 'Livre',
      organizer: 'Produtora Exemplo',
      status: 'PUBLISHED',
      categories: [{
        id: 4,
        code: 'vip',
        name: 'VIP',
        priceInCents: 22000,
        admissionMode: 'ASSIGNED_SEAT',
        configuredQuantity: 1,
        availableQuantity: 1,
        soldQuantity: 0,
      }],
      sectors: [{
        id: 2,
        name: 'Platéia',
        positionIndex: 0,
        rows: [{
          id: 3,
          label: 'A',
          positionIndex: 0,
          seats: [{
            id: 7,
            label: '1',
            positionIndex: 0,
            categoryCode: 'vip',
            categoryName: 'VIP',
            priceInCents: 22000,
            status: 'AVAILABLE',
          }],
        }],
      }],
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Festival de Verão');
    expect(page.el.textContent).toContain('VIP');
    expect(page.el.textContent).toContain('1 disponíveis');
  });

  it('cria pedido com Idempotency-Key e mostra o resultado do mock sem escolher outcome', async () => {
    const claims = btoa(JSON.stringify({
      sub: 'buyer@example.com',
      exp: Date.now() / 1000 + 3600,
      iss: 'ingresso-api',
    }));
    sessionStorage.setItem(SESSION_KEY, `header.${claims}.signature`);
    const page = await render('/eventos/41/comprar');
    const eventRequest = http.expectOne('/api/eventos/41');
    eventRequest.flush({
      id: 41,
      title: 'Festival de Verão',
      description: 'Uma noite ao vivo.',
      imageUrl: 'https://images.example.com/festival.jpg',
      startsAt: '2026-12-10T19:30:00-03:00',
      timeZone: 'America/Sao_Paulo',
      venue: { name: 'Teatro Central', streetAddress: 'Rua Principal, 10', city: 'Sao Paulo', stateCode: 'SP' },
      ageClassification: 'Livre',
      organizer: 'Produtora Exemplo',
      status: 'PUBLISHED',
      categories: [{
        id: 4,
        code: 'pista',
        name: 'Pista',
        priceInCents: 12500,
        admissionMode: 'GENERAL_ADMISSION',
        configuredQuantity: 20,
        availableQuantity: 20,
        soldQuantity: 0,
      }],
      sectors: [],
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    page.el.querySelector<HTMLButtonElement>('.stepper button:last-child')!.click();
    page.fixture.detectChanges();
    page.el.querySelector<HTMLButtonElement>('.checkout-summary .button-primary')!.click();

    const createRequest = http.expectOne('/api/pedidos');
    expect(createRequest.request.method).toBe('POST');
    expect(createRequest.request.headers.get('Idempotency-Key')).toBeTruthy();
    expect(createRequest.request.body).toEqual({
      eventId: 41,
      items: [{ categoryId: 4, quantity: 1 }],
    });
    createRequest.flush({
      id: 101,
      eventId: 41,
      eventTitle: 'Festival de Verão',
      status: 'PENDING_PAYMENT',
      totalInCents: 12500,
      createdAt: '2026-12-01T10:00:00Z',
      reservationExpiresAt: '2026-12-01T10:15:00Z',
      paidAt: null,
      items: [],
      payment: null,
      tickets: [],
    });
    await page.fixture.whenStable();

    const paymentRequest = http.expectOne('/api/pedidos/101/pagamento');
    expect(paymentRequest.request.method).toBe('POST');
    expect(paymentRequest.request.body).toEqual({});
    paymentRequest.flush({
      id: 101,
      eventId: 41,
      eventTitle: 'Festival de Verão',
      status: 'PAID',
      totalInCents: 12500,
      createdAt: '2026-12-01T10:00:00Z',
      reservationExpiresAt: '2026-12-01T10:15:00Z',
      paidAt: '2026-12-01T10:01:00Z',
      items: [],
      payment: { attemptKey: 'attempt-1', provider: 'mock', outcome: 'APPROVED', startedAt: '2026-12-01T10:00:30Z', completedAt: '2026-12-01T10:01:00Z' },
      tickets: [],
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Pedido confirmado');
    expect(page.el.textContent).toContain('125,00');
    expect(page.el.textContent).not.toContain('Aprovado');
    page.fixture.destroy();
  });

  it('lista apenas a resposta de histórico da conta autenticada', async () => {
    const claims = btoa(JSON.stringify({
      sub: 'buyer@example.com',
      exp: Date.now() / 1000 + 3600,
      iss: 'ingresso-api',
    }));
    sessionStorage.setItem(SESSION_KEY, `header.${claims}.signature`);
    const page = await render('/pedidos');
    const request = http.expectOne((candidate) => candidate.url === '/api/pedidos');
    expect(request.request.method).toBe('GET');
    request.flush({
      content: [{
        id: 101,
        eventId: 41,
        eventTitle: 'Festival de Verão',
        status: 'PAID',
        totalInCents: 12500,
        createdAt: '2026-12-01T10:00:00Z',
        reservationExpiresAt: '2026-12-01T10:15:00Z',
        paidAt: '2026-12-01T10:01:00Z',
        items: [{
          categoryId: 4,
          categoryCode: 'pista',
          categoryName: 'Pista',
          admissionMode: 'GENERAL_ADMISSION',
          unitPriceInCents: 12500,
          quantity: 1,
          seats: [],
        }],
        payment: { attemptKey: 'attempt-1', provider: 'mock', outcome: 'APPROVED', startedAt: '2026-12-01T10:00:30Z', completedAt: '2026-12-01T10:01:00Z' },
        tickets: [{
          id: 501,
          categoryName: 'Pista',
          unitNumber: 1,
          eventSeatId: null,
          sector: null,
          row: null,
          seatLabel: null,
          qrCodeValue: 'opaque-ticket-token-501',
          usedAt: null,
          refundedAt: null,
          issuedAt: '2026-12-01T10:01:01Z',
          eventTitle: 'Festival de Verão',
          eventStartsAt: '2026-12-10T19:30:00-03:00',
          venueName: 'Teatro Central',
          streetAddress: 'Rua Principal, 10',
          city: 'Sao Paulo',
          stateCode: 'SP',
        }],
      }],
      number: 0,
      totalPages: 1,
      totalElements: 1,
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Festival de Verão');
    expect(page.el.textContent).toContain('Pista × 1');
    expect(page.el.textContent).toContain('Pago');
    expect(page.el.textContent).toContain('INGRESSO 1');
    expect(page.el.textContent).toContain('Apresente na entrada');
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.querySelector<HTMLImageElement>('.ticket-qr-image')?.src).toContain('data:image/svg+xml;charset=utf-8,');
    page.el.querySelector<HTMLButtonElement>('.refund-ticket-action')!.click();
    const refundRequest = http.expectOne('/api/pedidos/101/tickets/501/reembolsos');
    expect(refundRequest.request.method).toBe('POST');
    refundRequest.flush({
      id: 901,
      ticketId: 501,
      orderId: 101,
      eventId: 41,
      source: 'BUYER_REQUEST',
      status: 'SIMULATED',
      amountInCents: 12500,
      reason: 'Solicitação do comprador',
      createdAt: '2026-12-01T11:00:00Z',
    }, { status: 201, statusText: 'Created' });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('reembolso simulado');
    expect(page.el.querySelector('.ticket-qr-image')).toBeNull();
  });

  it('mostra o relatório agregado do ADMIN sem inventar comissão', async () => {
    const claims = btoa(JSON.stringify({
      sub: 'admin@example.com',
      exp: Date.now() / 1000 + 3600,
      iss: 'ingresso-api',
    }));
    sessionStorage.setItem(SESSION_KEY, `header.${claims}.signature`);
    const page = await render('/admin/financeiro');
    const request = http.expectOne('/api/admin/financeiro');
    request.flush({
      paidOrderCount: 1,
      paidTicketCount: 2,
      refundedTicketCount: 1,
      grossSalesInCents: 25000,
      simulatedRefundsInCents: 12500,
      remainingGrossInCents: 12500,
      events: [{
        eventId: 41,
        eventTitle: 'Festival de Verão',
        paidOrderCount: 1,
        paidTicketCount: 2,
        refundedTicketCount: 1,
        grossSalesInCents: 25000,
        simulatedRefundsInCents: 12500,
        remainingGrossInCents: 12500,
      }],
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Vendas brutas');
    expect(page.el.textContent).toContain('Reembolsos simulados');
    expect(page.el.textContent).toContain('Festival de Verão');
    expect(page.el.textContent).not.toContain('Comissão:');
  });

  it('carrega relatório financeiro somente pela rota do evento do produtor', async () => {
    const claims = btoa(JSON.stringify({
      sub: 'producer@example.com',
      exp: Date.now() / 1000 + 3600,
      iss: 'ingresso-api',
    }));
    sessionStorage.setItem(SESSION_KEY, `header.${claims}.signature`);
    const page = await render('/produtor/eventos/41/financeiro');
    const request = http.expectOne('/api/produtor/eventos/41/financeiro');
    request.flush({
      eventId: 41,
      eventTitle: 'Festival de Verão',
      paidOrderCount: 1,
      paidTicketCount: 2,
      refundedTicketCount: 1,
      grossSalesInCents: 25000,
      simulatedRefundsInCents: 12500,
      remainingGrossInCents: 12500,
    });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('Vendas brutas');
    expect(page.el.textContent).toContain('250,00');
    expect(page.el.textContent).toContain('125,00');
  });

  it('envia do editor um evento com preço convertido para centavos BRL', async () => {
    const claims = btoa(JSON.stringify({
      sub: 'producer@example.com',
      exp: Date.now() / 1000 + 3600,
      iss: 'ingresso-api',
    }));
    sessionStorage.setItem(SESSION_KEY, `header.${claims}.signature`);
    const page = await render('/produtor/eventos/novo');
    const fill = (name: string, value: string) => {
      const input = page.el.querySelector<HTMLInputElement | HTMLTextAreaElement>(`[name="${name}"]`)!;
      if (!input) throw new Error(`Campo ${name} ausente: ${page.el.innerHTML}`);
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    fill('title', 'Show do Produtor');
    fill('description', 'Uma apresentação ao vivo.');
    fill('imageUrl', 'https://images.example.com/show.jpg');
    fill('startsAt', '2026-12-10T19:30');
    fill('venueName', 'Casa de Shows');
    fill('streetAddress', 'Rua das Flores, 10');
    fill('city', 'Sao Paulo');
    fill('stateCode', 'sp');
    fill('ageClassification', 'Livre');
    fill('organizer', 'Produtora Exemplo');
    const categoryInputs = page.el.querySelectorAll<HTMLInputElement>('.category-editor-row input');
    categoryInputs[0].value = 'Pista';
    categoryInputs[0].dispatchEvent(new Event('input'));
    categoryInputs[1].value = '12.50';
    categoryInputs[1].dispatchEvent(new Event('input'));
    categoryInputs[2].value = '80';
    categoryInputs[2].dispatchEvent(new Event('input'));
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    const addCategory = [...page.el.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Adicionar categoria'))!;
    addCategory.click();
    page.fixture.detectChanges();
    const categoryRows = page.el.querySelectorAll('.category-editor-row');
    const vipInputs = categoryRows[1].querySelectorAll<HTMLInputElement>('input');
    vipInputs[0].value = 'VIP';
    vipInputs[0].dispatchEvent(new Event('input'));
    vipInputs[1].value = '220';
    vipInputs[1].dispatchEvent(new Event('input'));
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    [...categoryRows[1].querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Lugar marcado'))!.click();
    page.fixture.detectChanges();
    [...page.el.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Adicionar setor'))!.click();
    page.fixture.detectChanges();
    [...page.el.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Gerar fileira'))!.click();
    page.fixture.detectChanges();
    page.submit();

    const request = http.expectOne('/api/produtor/eventos');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toMatchObject({
      title: 'Show do Produtor',
      stateCode: 'SP',
      categories: [
        expect.objectContaining({
          name: 'Pista',
          priceInCents: 1250,
          admissionMode: 'GENERAL_ADMISSION',
          quantity: 80,
        }),
        expect.objectContaining({
          name: 'VIP',
          priceInCents: 22000,
          admissionMode: 'ASSIGNED_SEAT',
          quantity: null,
        }),
      ],
    });
    const seatMap = request.request.body.sectors;
    expect(seatMap).toHaveLength(1);
    expect(seatMap[0].rows[0].seats).toHaveLength(8);
    expect(seatMap[0].rows[0].seats.every((seat: { categoryCode: string }) =>
      seat.categoryCode === request.request.body.categories[1].code,
    )).toBe(true);
    request.flush({ message: 'validation test' }, { status: 409, statusText: 'Conflict' });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.textContent).toContain('validation test');
    page.fixture.destroy();
  });

  it('impede cadastro com senhas diferentes', async () => {
    const page = await render('/cadastro');
    page.fill('nome', 'Ana');
    page.fill('email', 'ana@exemplo.com');
    page.fill('password', 'senha');
    page.fill('confirmation', 'outra');
    page.submit();
    expect(page.el.textContent).toContain('As senhas precisam ser iguais.');
    http.expectNone('/api/usuarios');
  });

  it('envia cadastro sem papel controlado pelo cliente e redireciona com confirmação', async () => {
    const page = await render('/cadastro');
    page.fill('nome', ' Ana ');
    page.fill('email', 'ana@exemplo.com');
    page.fill('password', 'senha');
    page.fill('confirmation', 'senha');
    page.submit();
    page.submit();
    const request = http.expectOne('/api/usuarios');
    expect(request.request.body).toEqual({
      nome: 'Ana',
      email: 'ana@exemplo.com',
      password: 'senha',
    });
    expect(page.el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
    request.flush(null, { status: 201, statusText: 'Created' });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/entrar?cadastro=concluido');
    expect(page.el.textContent).toContain('Conta criada!');
  });

  it('mostra conflito de e-mail e libera nova tentativa', async () => {
    const page = await render('/cadastro');
    page.fill('nome', 'Ana');
    page.fill('email', 'ana@exemplo.com');
    page.fill('password', 'senha');
    page.fill('confirmation', 'senha');
    page.submit();
    http
      .expectOne('/api/usuarios')
      .flush({ message: 'Email ja cadastrado' }, { status: 409, statusText: 'Conflict' });
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.querySelector('[role=alert]')!.textContent).toContain(
      'Este e-mail já está cadastrado.',
    );
    expect(page.el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
  });

  it('mostra erro de rede e permite tentar novamente', async () => {
    const page = await render('/entrar');
    page.fill('email', 'ana@exemplo.com');
    page.fill('password', 'senha');
    page.submit();
    http.expectOne('/api/auth/login').error(new ProgressEvent('error'));
    await page.fixture.whenStable();
    page.fixture.detectChanges();
    expect(page.el.querySelector('[role=alert]')!.textContent).toContain(
      'Não foi possível conectar',
    );
    expect(page.el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
  });
});
