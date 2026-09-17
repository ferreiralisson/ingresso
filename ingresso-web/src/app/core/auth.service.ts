import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, timeout } from 'rxjs';
import { API_URL, AuthResponse, CreateUser, Credentials, Session } from './api';

export const SESSION_KEY = 'ingresso.session';

/** Claims only drive the UI; signature validation and authorization belong to the API. */
export function readSession(token: string): Session | null {
  try {
    if (token.split('.').length !== 3) return null;
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), (c) =>
      c.charCodeAt(0),
    );
    const claims = JSON.parse(new TextDecoder().decode(bytes));
    if (
      typeof claims.sub !== 'string' ||
      !claims.sub ||
      claims.iss !== 'ingresso-api' ||
      typeof claims.exp !== 'number' ||
      !Number.isFinite(claims.exp) ||
      claims.exp * 1000 <= Date.now()
    )
      return null;
    return { token, email: claims.sub, expiresAt: claims.exp * 1000 };
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = inject(API_URL);
  private readonly currentSession = signal<Session | null>(null);
  readonly session = this.currentSession.asReadonly();
  private expiryTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    try {
      const token = sessionStorage.getItem(SESSION_KEY);
      const session = token ? readSession(token) : null;
      if (session) this.setSession(session);
      else sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* Keep an in-memory session if browser storage is unavailable. */
    }
  }

  login(credentials: Credentials) {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, credentials).pipe(
      timeout(15000),
      tap((response) => {
        const session = readSession(response.token);
        if (!session) throw new Error('A resposta de acesso é inválida. Tente entrar novamente.');
        this.setSession(session);
      }),
    );
  }

  register(user: CreateUser) {
    return this.http.post<void>(`${this.api}/usuarios`, user).pipe(timeout(15000));
  }

  isAuthenticated(): boolean {
    const session = this.session();
    if (session && session.expiresAt > Date.now()) return true;
    this.clearSession();
    return false;
  }

  clearSession(): void {
    clearTimeout(this.expiryTimer);
    this.currentSession.set(null);
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      /* Storage may be disabled. */
    }
  }

  expireSession(): void {
    this.clearSession();
    void this.router.navigate(['/entrar'], { queryParams: { sessao: 'expirada' } });
  }

  private setSession(session: Session): void {
    clearTimeout(this.expiryTimer);
    this.currentSession.set(session);
    try {
      sessionStorage.setItem(SESSION_KEY, session.token);
    } catch {
      /* In-memory fallback. */
    }
    this.expiryTimer = setTimeout(
      () => this.expireSession(),
      Math.min(session.expiresAt - Date.now(), 2147483647),
    );
  }

  ngOnDestroy(): void {
    clearTimeout(this.expiryTimer);
  }
}
