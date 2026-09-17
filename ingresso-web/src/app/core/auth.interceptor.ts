import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { API_URL } from './api';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const api = inject(API_URL);
  const isApi = request.url === api || request.url.startsWith(`${api}/`);
  const path = request.url.split('?')[0];
  const isPublic =
    path === `${api}/auth/login` || (path === `${api}/usuarios` && request.method === 'POST');
  const session = isApi && !isPublic && auth.isAuthenticated() ? auth.session() : null;
  const outgoing = session
    ? request.clone({ setHeaders: { Authorization: `Bearer ${session.token}` } })
    : request;
  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (isApi && !isPublic && error instanceof HttpErrorResponse && error.status === 401)
        auth.expireSession();
      return throwError(() => error);
    }),
  );
};
