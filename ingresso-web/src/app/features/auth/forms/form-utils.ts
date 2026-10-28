import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { TimeoutError } from 'rxjs';

export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim().length ? null : { required: true };

export const passwordsMatch: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  control.get('password')?.value === control.get('confirmation')?.value
    ? null
    : { passwordsMismatch: true };

export function errorMessage(error: unknown, action: 'login' | 'register'): string {
  if (error instanceof TimeoutError)
    return 'A conexão demorou mais que o esperado. Tente novamente.';
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status === 502 || error.status === 504)
      return 'Não foi possível conectar ao serviço. Tente novamente em instantes.';
    if (error.status === 409)
      return 'Este e-mail já está cadastrado. Entre na sua conta ou use outro e-mail.';
    if ((error.status === 401 || error.status === 403) && action === 'login')
      return 'E-mail ou senha incorretos. Confira os dados e tente novamente.';
    if (error.status === 400) return 'Confira os dados informados e tente novamente.';
    if (error.status === 429)
      return 'Muitas tentativas. Aguarde um pouco antes de tentar novamente.';
    return 'Não foi possível concluir agora. Tente novamente em instantes.';
  }
  return 'Não foi possível concluir a solicitação. Tente novamente.';
}
