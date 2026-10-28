import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';
import { entryAuthGuard } from './features/entry/guards/entry-auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'eventos' },
  {
    path: 'eventos',
    title: 'Eventos | Ingresso',
    loadComponent: () => import('./features/events/pages/events').then((m) => m.Events),
  },
  {
    path: 'eventos/:id',
    title: 'Evento | Ingresso',
    loadComponent: () => import('./features/events/pages/event-detail').then((m) => m.EventDetail),
  },
  {
    path: 'eventos/:id/comprar',
    title: 'Checkout | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/pages/checkout').then((m) => m.Checkout),
  },
  {
    path: 'pedidos',
    title: 'Meus pedidos | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/pages/orders').then((m) => m.Orders),
  },
  {
    path: 'eventos/:id/entrada',
    title: 'Operação de entrada | Ingresso',
    canActivate: [entryAuthGuard],
    loadComponent: () => import('./features/entry/pages/entry-validation').then((m) => m.EntryValidation),
  },
  {
    path: 'convites/equipe/aceitar',
    title: 'Aceitar convite de equipe | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/entry/pages/accept-event-staff-invite').then((m) => m.AcceptEventStaffInvite),
  },
  {
    path: 'pedidos',
    title: 'Meus pedidos | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/orders/pages/orders').then((m) => m.Orders),
  },
  {
    path: 'produtor/eventos/novo',
    title: 'Novo evento | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/events/pages/event-editor').then((m) => m.EventEditor),
  },
  {
    path: 'produtor/eventos/:id/editar',
    title: 'Editar evento | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/events/pages/event-editor').then((m) => m.EventEditor),
  },
  {
    path: 'produtor/eventos/:id/financeiro',
    title: 'Financeiro do evento | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/finance/pages/financial-report').then((m) => m.FinancialReport),
  },
  {
    path: 'admin/financeiro',
    title: 'Financeiro | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/finance/pages/financial-report').then((m) => m.FinancialReport),
  },
  {
    path: 'produtor/eventos',
    title: 'Meus eventos | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/events/pages/producer-events').then((m) => m.ProducerEvents),
  },
  {
    path: 'admin/eventos',
    title: 'Administração de eventos | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/events/pages/admin-events').then((m) => m.AdminEvents),
  },
  {
    path: 'entrar',
    title: 'Entrar | Ingresso',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/pages/login').then((m) => m.Login),
  },
  {
    path: 'cadastro',
    title: 'Criar conta | Ingresso',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/pages/register').then((m) => m.Register),
  },
  {
    path: 'conta',
    title: 'Minha conta | Ingresso',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/pages/account').then((m) => m.Account),
  },
  { path: '**', redirectTo: 'eventos' },
];
