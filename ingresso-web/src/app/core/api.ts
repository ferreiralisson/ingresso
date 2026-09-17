import { InjectionToken } from '@angular/core';

/** Use a same-origin reverse proxy in production, or override this provider. */
export const API_URL = new InjectionToken<string>('API_URL', {
  providedIn: 'root',
  factory: () => '/api',
});

export interface Credentials {
  email: string;
  password: string;
}
export interface CreateUser extends Credentials {
  nome: string;
  perfil: 'USER' | 'ADMIN';
}
export interface AuthResponse {
  token: string;
}
export interface Session {
  token: string;
  email: string;
  expiresAt: number;
}
