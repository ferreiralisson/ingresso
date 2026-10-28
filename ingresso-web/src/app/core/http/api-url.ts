import { InjectionToken } from '@angular/core';

/** Use a same-origin reverse proxy in production, or override this provider. */
export const API_URL = new InjectionToken<string>('API_URL', {
  providedIn: 'root',
  factory: () => '/api',
});