import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { EntryOfflineStore } from './entry-offline-store';

export const authGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  if (route.routeConfig?.path === 'eventos/:id/entrada') {
    const eventId = Number(route.paramMap.get('id'));
    if (Number.isSafeInteger(eventId) && eventId > 0) {
      return inject(EntryOfflineStore).latestManifest(eventId)
        .then((manifest) => manifest !== null || router.createUrlTree(['/entrar']))
        .catch(() => router.createUrlTree(['/entrar']));
    }
  }
  return router.createUrlTree(['/entrar']);
};
export const guestGuard: CanActivateFn = () =>
  !inject(AuthService).isAuthenticated() || inject(Router).createUrlTree(['/conta']);
