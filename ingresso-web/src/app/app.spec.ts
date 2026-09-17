import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './core/auth.service';

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

  it('cadastra como USER, evita envio duplicado e redireciona com confirmação', async () => {
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
      perfil: 'USER',
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
