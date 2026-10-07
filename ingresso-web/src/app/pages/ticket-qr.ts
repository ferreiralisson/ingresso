import { Component, Input, OnChanges, signal } from '@angular/core';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-ticket-qr',
  template: `
    @if (imageUrl()) {
      <img class="ticket-qr-image" [src]="imageUrl()" [alt]="'QR do ingresso ' + label">
    } @else if (failed()) {
      <span class="ticket-qr-error" role="alert">Não foi possível gerar o QR.</span>
    } @else {
      <span class="ticket-qr-loading" role="status">Preparando QR...</span>
    }
  `,
})
export class TicketQr implements OnChanges {
  @Input({ required: true }) value = '';
  @Input({ required: true }) label = '';

  protected readonly imageUrl = signal('');
  protected readonly failed = signal(false);

  ngOnChanges(): void {
    const value = this.value;
    this.imageUrl.set('');
    this.failed.set(false);
    void QRCode.toString(value, { errorCorrectionLevel: 'M', margin: 2, type: 'svg' })
      .then((svg) => {
        if (this.value === value) {
          this.imageUrl.set(`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`);
        }
      })
      .catch(() => {
        if (this.value === value) this.failed.set(true);
      });
  }
}