import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { EntryOfflineStore } from '../data-access/entry-offline-store';

export const entryAuthGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated()) return true;
  const eventId = Number(route.paramMap.get('id'));
  if (!Number.isSafeInteger(eventId) || eventId < 1) return router.createUrlTree(['/entrar']);
  return inject(EntryOfflineStore).latestManifest(eventId)
    .then((manifest) => manifest !== null || router.createUrlTree(['/entrar']))
    .catch(() => router.createUrlTree(['/entrar']));
};