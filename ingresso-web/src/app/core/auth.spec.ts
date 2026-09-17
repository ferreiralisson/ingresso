import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService, readSession, SESSION_KEY } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { authGuard, guestGuard } from './auth.guard';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

function token(exp = Date.now() / 1000 + 7200, sub = 'pessoa@exemplo.com'): string {
  return `eyJhbGciOiJIUzUxMiJ9.${btoa(JSON.stringify({ sub, exp, iss: 'ingresso-api' }))}.signature`;
}

describe('Autenticação e integração HTTP', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    auth = TestBed.inject(AuthService);
  });
  afterEach(() => {
    http.verify();
    auth.ngOnDestroy();
    sessionStorage.clear();
    vi.useRealTimers();
  });

  async function login(): Promise<string> {
    const jwt = token();
    const result = firstValueFrom(auth.login({ email: 'pessoa@exemplo.com', password: 'senha' }));
    const request = http.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'pessoa@exemplo.com', password: 'senha' });
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ token: jwt });
    await result;
    return jwt;
  }

  it('envia o contrato de login, guarda o token e lê o e-mail', async () => {
    const jwt = await login();
    expect(auth.session()?.email).toBe('pessoa@exemplo.com');
    expect(sessionStorage.getItem(SESSION_KEY)).toBe(jwt);
    expect(auth.isAuthenticated()).toBe(true);
  });

  it('aceita cadastro com resposta 201 sem corpo', async () => {
    const user = {
      nome: 'Ana',
      email: 'ana@exemplo.com',
      password: 'senha',
      perfil: 'USER' as const,
    };
    const result = firstValueFrom(auth.register(user));
    const request = http.expectOne('/api/usuarios');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(user);
    request.flush(null, { status: 201, statusText: 'Created' });
    await result;
    expect(auth.session()).toBeNull();
  });

  it('não cria sessão quando as credenciais são rejeitadas', async () => {
    const result = firstValueFrom(auth.login({ email: 'ana@exemplo.com', password: 'errada' }));
    const assertion = expect(result).rejects.toMatchObject({ status: 403 });
    http.expectOne('/api/auth/login').flush(null, { status: 403, statusText: 'Forbidden' });
    await assertion;
    expect(auth.session()).toBeNull();
  });

  it('rejeita token inválido devolvido pelo servidor', async () => {
    const result = firstValueFrom(auth.login({ email: 'ana@exemplo.com', password: 'senha' }));
    const assertion = expect(result).rejects.toThrow('inválida');
    http.expectOne('/api/auth/login').flush({ token: 'invalid' });
    await assertion;
    expect(auth.session()).toBeNull();
  });

  it('envia Bearer somente para chamadas privadas da própria API', async () => {
    const jwt = await login();
    const client = TestBed.inject(HttpClient);
    client.get('/api/recurso-futuro').subscribe();
    const request = http.expectOne('/api/recurso-futuro');
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${jwt}`);
    request.flush({});
    for (const url of ['https://externo.example/api', '/api-outro/recurso']) {
      client.get(url).subscribe();
      const external = http.expectOne(url);
      expect(external.request.headers.has('Authorization')).toBe(false);
      external.flush({});
    }
    auth
      .register({ nome: 'Ana', email: 'ana@exemplo.com', password: 'senha', perfil: 'USER' })
      .subscribe();
    const publicRequest = http.expectOne('/api/usuarios');
    expect(publicRequest.request.headers.has('Authorization')).toBe(false);
    publicRequest.flush(null);
  });

  it('encerra a sessão ao receber 401 em chamada privada', async () => {
    await login();
    TestBed.inject(HttpClient)
      .get('/api/recurso-futuro')
      .subscribe({ error: () => {} });
    http.expectOne('/api/recurso-futuro').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.session()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/entrar'], {
      queryParams: { sessao: 'expirada' },
    });
  });

  it('encerra automaticamente quando o token expira', async () => {
    vi.useFakeTimers();
    await login();
    vi.advanceTimersByTime(7200001);
    expect(auth.session()).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    expect(router.navigate).toHaveBeenCalled();
  });

  it('limpa o token ao sair', async () => {
    await login();
    auth.clearSession();
    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('protege a conta e redireciona usuários autenticados para ela', async () => {
    const run = (guard: typeof authGuard) =>
      TestBed.runInInjectionContext(() =>
        guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      );
    expect(run(authGuard)?.toString()).toBe('/entrar');
    expect(run(guestGuard)).toBe(true);
    await login();
    expect(run(authGuard)).toBe(true);
    expect(run(guestGuard)?.toString()).toBe('/conta');
  });
});

describe('Restauração da sessão', () => {
  afterEach(() => {
    TestBed.inject(AuthService).ngOnDestroy();
    sessionStorage.clear();
  });
  function restore(value: string): AuthService {
    sessionStorage.setItem(SESSION_KEY, value);
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
    return TestBed.inject(AuthService);
  }
  it('restaura uma sessão válida ao recarregar', () => {
    expect(restore(token()).session()?.email).toBe('pessoa@exemplo.com');
  });
  it('remove uma sessão expirada ao recarregar', () => {
    expect(restore(token(Date.now() / 1000 - 1)).session()).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });
  it('remove dados corrompidos', () => {
    expect(restore('invalid').session()).toBeNull();
  });
});

describe('Leitura do token', () => {
  it('rejeita tokens malformados e expirados', () => {
    expect(readSession('abc')).toBeNull();
    expect(readSession('a.%%%%.c')).toBeNull();
    expect(readSession(token(0))).toBeNull();
    expect(readSession(token(Infinity))).toBeNull();
    expect(readSession(token(Date.now() / 1000 + 1, ''))).toBeNull();
  });
});
