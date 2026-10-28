export interface Credentials {
  email: string;
  password: string;
}

export interface CreateUser extends Credentials {
  nome: string;
}

export interface AuthResponse {
  token: string;
}

export interface Session {
  token: string;
  email: string;
  expiresAt: number;
}