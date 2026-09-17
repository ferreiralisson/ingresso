import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { errorMessage, notBlank, passwordsMatch } from '../core/form-utils';
import { AuthLayout } from '../shared/auth-layout';

@Component({
  imports: [ReactiveFormsModule, RouterLink, AuthLayout],
  templateUrl: './register.html',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly pending = signal(false);
  protected readonly error = signal('');
  protected readonly showPassword = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      nome: ['', notBlank],
      email: ['', [notBlank, Validators.email]],
      password: ['', notBlank],
      confirmation: ['', notBlank],
    },
    { validators: passwordsMatch },
  );

  protected submit(): void {
    if (this.pending()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error.set('');
    this.pending.set(true);
    const value = this.form.getRawValue();
    this.auth
      .register({
        nome: value.nome.trim(),
        email: value.email.trim(),
        password: value.password,
        perfil: 'USER',
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.pending.set(false)),
      )
      .subscribe({
        next: () => {
          void this.router.navigate(['/entrar'], { queryParams: { cadastro: 'concluido' } });
        },
        error: (error) => this.error.set(errorMessage(error, 'register')),
      });
  }
}
