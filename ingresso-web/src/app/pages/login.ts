import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { errorMessage, notBlank } from '../core/form-utils';
import { AuthLayout } from '../shared/auth-layout';

@Component({ imports: [ReactiveFormsModule, RouterLink, AuthLayout], templateUrl: './login.html' })
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly params = inject(ActivatedRoute).snapshot.queryParamMap;
  protected readonly pending = signal(false);
  protected readonly error = signal('');
  protected readonly showPassword = signal(false);
  protected readonly registered = this.params.get('cadastro') === 'concluido';
  protected readonly expired = this.params.get('sessao') === 'expirada';
  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [notBlank, Validators.email]],
    password: ['', notBlank],
  });

  protected submit(): void {
    if (this.pending()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error.set('');
    this.pending.set(true);
    const value = this.form.getRawValue();
    this.auth
      .login({ email: value.email.trim(), password: value.password })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.pending.set(false)),
      )
      .subscribe({
        next: () => {
          void this.router.navigate(['/conta']);
        },
        error: (error) => this.error.set(errorMessage(error, 'login')),
      });
  }
}
