import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'conta' },
  {
    path: 'entrar',
    title: 'Entrar | Ingresso',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    title: 'Criar conta | Ingresso',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register').then((m) => m.Register),
  },
  {
    path: 'conta',
    title: 'Minha conta | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/account').then((m) => m.Account),
  },
  { path: '**', redirectTo: 'conta' },
];
