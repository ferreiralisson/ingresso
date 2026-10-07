import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, NgZone, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BrowserCodeReader, BrowserQRCodeReader, IScannerControls } from '@zxing/browser';
import { HttpErrorResponse } from '@angular/common/http';
import { EntryApi, EntryResult, OfflineManifest, PendingOfflineScan } from '../core/entry-api';
import { EntryOfflineStore } from '../core/entry-offline-store';
import { EventStaffManager } from './event-staff-manager';
import { AuthService } from '../core/auth.service';

@Component({
  imports: [RouterLink, EventStaffManager],
  template: `
    <section class="market-page entry-page">
      <div class="market-heading">
        <div>
          <span class="section-kicker">OPERAÇÃO DE ENTRADA</span>
          <h1>{{ manifest()?.eventTitle || 'Validar ingresso' }}<span class="accent">.</span></h1>
          <p class="market-intro">{{ manifest() ? formatDate(manifest()!.eventStartsAt) : 'Evento #' + eventId }}</p>
        </div>
        <a class="text-link" routerLink="/produtor/eventos">Voltar aos eventos</a>
      </div>

      <app-event-staff-manager [eventId]="eventId" />

      <div class="entry-status-bar" [class.entry-status-offline]="!online()">
        <span class="entry-status-dot" aria-hidden="true"></span>
        <strong>{{ canValidateOnline() ? 'Online' : online() ? 'Sessão expirada' : 'Offline' }}</strong>
        @if (manifest()) {
          <span>Dados locais até {{ formatDate(manifest()!.expiresAt) }}</span>
          <span>{{ manifest()!.tickets.length }} ingressos</span>
        } @else {
          <span>Dados do evento não preparados</span>
        }
      </div>

      <div class="entry-layout">
        <section class="entry-scanner" aria-label="Leitor de ingresso">
          <div class="entry-camera-frame">
            <video #video autoplay muted playsinline aria-label="Prévia da câmera para leitura do ingresso"></video>
            @if (!cameraActive()) {
              <div class="entry-camera-placeholder">Câmera inativa</div>
            }
          </div>
          <div class="entry-scanner-actions">
            @if (cameraActive()) {
              <button type="button" class="button-secondary" (click)="stopCamera()">Parar câmera</button>
            } @else {
              <button type="button" class="button-primary" (click)="startCamera()">Ativar câmera</button>
            }
            @if (canValidateOnline()) {
              <button type="button" class="button-secondary" [disabled]="loadingManifest()" (click)="prepareOffline()">
                {{ loadingManifest() ? 'Atualizando...' : 'Preparar para offline' }}
              </button>
            }
          </div>
          @if (cameraError()) {
            <div class="notice error" role="alert">{{ cameraError() }}</div>
          }
        </section>

        <section class="entry-manual" aria-labelledby="manual-entry-title">
          <span class="section-kicker">ENTRADA MANUAL</span>
          <h2 id="manual-entry-title">Código do ingresso</h2>
          <form (submit)="$event.preventDefault(); submitManual()">
            <label for="entry-code">QR/token</label>
            <input #manualCode id="entry-code" autocomplete="off" autocapitalize="off" spellcheck="false" required />
            <button class="button-primary" type="submit" [disabled]="processing()">Validar ingresso</button>
          </form>

          @if (result(); as validation) {
            <div class="entry-result" [class]="'entry-result entry-result-' + resultTone(validation.outcome)" role="status" aria-live="polite">
              <strong>{{ resultLabel(validation.outcome) }}</strong>
              <span>{{ validation.message }}</span>
              @if (validation.previousUseAt) {
                <small>{{ formatDate(validation.previousUseAt) }} · {{ validation.previousOperator }}</small>
              }
            </div>
          } @else if (provisionalMessage()) {
            <div class="entry-result entry-result-provisional" role="status" aria-live="polite">
              <strong>Entrada provisória</strong>
              <span>{{ provisionalMessage() }}</span>
            </div>
          }

          <div class="entry-sync-row">
            <span>{{ pendingCount() }} pendentes</span>
            <button type="button" class="button-secondary" [disabled]="!online() || pendingCount() === 0 || syncing()" (click)="syncPending()">
              {{ syncing() ? 'Sincronizando...' : 'Sincronizar' }}
            </button>
          </div>
          @if (syncMessage()) {
            <p class="entry-sync-message" role="status">{{ syncMessage() }}</p>
          }
        </section>
      </div>

        @if (auditAllowed()) {
          <section class="entry-audit" aria-labelledby="entry-audit-title">
            <div class="event-section-heading">
              <div>
                <span class="section-kicker">ADMIN</span>
                <h2 id="entry-audit-title">Auditoria de entrada</h2>
              </div>
              <button type="button" class="button-secondary" (click)="loadAudit(true)">Atualizar</button>
            </div>
            @for (record of auditRecords(); track record.checkInId) {
              <article class="entry-audit-row">
                <div>
                  <strong>{{ resultLabel(record.outcome) }}</strong>
                  <span>Ingresso {{ record.ticketId ?? 'não identificado' }} · {{ record.operatorEmail }}</span>
                  <small>{{ formatDate(record.deviceScannedAt || record.processedAt) }} · {{ record.source === 'OFFLINE' ? 'Offline' : 'Online' }}</small>
                  @if (record.correctedAt) {
                    <small>Corrigido por {{ record.correctedBy }} · {{ formatDate(record.correctedAt) }} · {{ record.correctionReason }}</small>
                  }
                </div>
                @if (record.outcome === 'ACCEPTED' && record.ticketId && !record.correctedAt) {
                  <div class="entry-audit-actions">
                    @if (correctingId() === record.checkInId) {
                      <form (submit)="$event.preventDefault(); correctEntry(record.checkInId, reason.value)">
                        <input #reason aria-label="Motivo da correção" placeholder="Motivo obrigatório" required maxlength="500" />
                        <button class="button-danger" type="submit" [disabled]="workingCorrection()">Reabrir ingresso</button>
                      </form>
                    } @else {
                      <button class="button-danger" type="button" (click)="correctingId.set(record.checkInId)">Corrigir check-in</button>
                    }
                  </div>
                }
              </article>
            } @empty {
              <p class="market-message">Nenhuma tentativa de entrada registrada.</p>
            }
            @if (auditHasMore()) {
              <button type="button" class="button-secondary" (click)="loadAudit(false)">Carregar mais registros</button>
            }
            @if (auditMessage()) {
              <p class="entry-sync-message" role="status">{{ auditMessage() }}</p>
            }
          </section>
        }
    </section>
  `,
})
export class EntryValidation implements OnInit, OnDestroy {
  @ViewChild('video') private video?: ElementRef<HTMLVideoElement>;
  @ViewChild('manualCode') private manualCode?: ElementRef<HTMLInputElement>;

  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(EntryApi);
  private readonly offlineStore = inject(EntryOfflineStore);
  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);
  private scanner?: BrowserQRCodeReader;
  private controls?: IScannerControls;
  protected eventId = 0;
  protected readonly manifest = signal<OfflineManifest | null>(null);
  protected readonly online = signal(navigator.onLine);
  protected readonly canValidateOnline = computed(() => this.online() && !!this.auth.session());
  protected readonly loadingManifest = signal(false);
  protected readonly cameraActive = signal(false);
  protected readonly cameraError = signal('');
  protected readonly processing = signal(false);
  protected readonly syncing = signal(false);
  protected readonly pendingCount = signal(0);
  protected readonly result = signal<EntryResult | null>(null);
  protected readonly provisionalMessage = signal('');
  protected readonly syncMessage = signal('');
  protected readonly auditRecords = signal<EntryResult[]>([]);
  protected readonly auditAllowed = signal(false);
  protected readonly auditHasMore = signal(false);
  protected readonly auditMessage = signal('');
  protected readonly correctingId = signal<number | null>(null);
  protected readonly workingCorrection = signal(false);
  private auditPage = 0;

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(this.eventId) || this.eventId < 1) return;
    void this.restoreOfflineState();
    if (this.canValidateOnline()) this.prepareOffline();
    if (this.canValidateOnline()) this.loadAudit(true);
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  @HostListener('window:online')
  protected onOnline(): void {
    this.online.set(true);
    if (this.auth.session()) {
      this.prepareOffline();
      void this.syncPending();
      this.loadAudit(true);
    }
  }

  @HostListener('window:offline')
  protected onOffline(): void {
    this.online.set(false);
  }

  protected async startCamera(): Promise<void> {
    this.cameraError.set('');
    const video = this.video?.nativeElement;
    if (!video) return;
    try {
      const devices = await BrowserCodeReader.listVideoInputDevices();
      if (devices.length === 0) throw new Error('Nenhuma câmera disponível. Digite o código manualmente.');
      this.scanner = new BrowserQRCodeReader();
      this.controls = await this.scanner.decodeFromVideoDevice(
        devices.at(-1)?.deviceId,
        video,
        (decoded) => {
          if (!decoded) return;
          const value = decoded.getText();
          this.zone.run(() => {
            this.stopCamera();
            void this.processQr(value);
          });
        },
      );
      this.cameraActive.set(true);
    } catch {
      this.cameraActive.set(false);
      this.cameraError.set('Não foi possível acessar a câmera. Digite o código manualmente.');
    }
  }

  protected stopCamera(): void {
    this.controls?.stop();
    this.controls = undefined;
    this.cameraActive.set(false);
  }

  protected submitManual(): void {
    const value = this.manualCode?.nativeElement.value.trim() ?? '';
    if (value) void this.processQr(value);
  }

  protected async prepareOffline(): Promise<void> {
    if (!this.canValidateOnline() || this.loadingManifest() || this.eventId < 1) return;
    this.loadingManifest.set(true);
    this.api.provisionOffline(this.eventId).subscribe({
      next: (manifest) => {
        void this.offlineStore.saveManifest(manifest).then(() => {
          this.manifest.set(manifest);
          this.loadingManifest.set(false);
          void this.refreshPendingCount();
        }).catch(() => {
          this.loadingManifest.set(false);
          this.syncMessage.set('Não foi possível guardar os dados offline neste dispositivo.');
        });
      },
      error: () => {
        this.loadingManifest.set(false);
        this.syncMessage.set('Não foi possível preparar o evento para uso offline.');
      },
    });
  }

  protected async processQr(qrCodeValue: string): Promise<void> {
    if (this.processing()) return;
    this.processing.set(true);
    this.result.set(null);
    this.provisionalMessage.set('');
    if (this.canValidateOnline()) {
      this.api.validate(this.eventId, qrCodeValue, crypto.randomUUID()).subscribe({
        next: (result) => {
          this.result.set(result);
          this.processing.set(false);
          if (this.manualCode) this.manualCode.nativeElement.value = '';
        },
        error: (error: unknown) => {
          this.processing.set(false);
          this.syncMessage.set(error instanceof HttpErrorResponse && error.status === 403
            ? 'Esta conta não tem acesso à operação deste evento.'
            : 'Não foi possível validar online. O resultado não foi presumido como aceito.');
        },
      });
      return;
    }

    await this.processOfflineQr(qrCodeValue);
  }

  protected async syncPending(): Promise<void> {
    if (!navigator.onLine || this.syncing() || this.eventId < 1) return;
    if (!this.auth.session()) {
      this.syncMessage.set('Entre novamente para sincronizar os registros pendentes.');
      return;
    }
    const scans = await this.offlineStore.pendingScans(this.eventId);
    this.pendingCount.set(scans.length);
    if (scans.length === 0) return;
    this.syncing.set(true);
    const groups = new Map<string, PendingOfflineScan[]>();
    for (const scan of scans) {
      const batch = groups.get(scan.manifestId) ?? [];
      batch.push(scan);
      groups.set(scan.manifestId, batch);
    }
    for (const [manifestId, batch] of groups) {
      const manifest = await this.offlineStore.manifestById(manifestId);
      if (!manifest || manifest.operatorEmail !== this.auth.session()?.email) {
        this.syncMessage.set('Entre na mesma conta que preparou os dados offline para sincronizar.');
        continue;
      }
      await new Promise<void>((resolve) => {
        this.api.synchronize(this.eventId, manifestId, batch[0].deviceId, batch).subscribe({
          next: (response) => {
            void this.offlineStore.deleteScans(batch.map((scan) => scan.scanId)).then(() => {
              const accepted = response.results.filter((item) => item.outcome === 'ACCEPTED').length;
              const conflicts = response.results.filter((item) => item.outcome === 'OFFLINE_CONFLICT').length;
              this.syncMessage.set(`${accepted} confirmados · ${conflicts} conflitos · demais recusados`);
              resolve();
            });
          },
          error: () => {
            this.syncMessage.set('A sincronização falhou. Os registros permanecem neste dispositivo.');
            resolve();
          },
        });
      });
    }
    this.syncing.set(false);
    await this.refreshPendingCount();
  }

  protected loadAudit(reset: boolean): void {
    if (!navigator.onLine) return;
    const page = reset ? 0 : this.auditPage + 1;
    this.api.history(this.eventId, page).subscribe({
      next: (response) => {
        this.auditAllowed.set(true);
        this.auditRecords.update((current) => reset ? response.content : [...current, ...response.content]);
        this.auditPage = response.number;
        this.auditHasMore.set(response.number + 1 < response.totalPages);
      },
      error: () => this.auditAllowed.set(false),
    });
  }

  protected correctEntry(checkInId: number, reason: string): void {
    if (!reason.trim()) return;
    this.workingCorrection.set(true);
    this.api.correct(checkInId, reason.trim()).subscribe({
      next: () => {
        this.workingCorrection.set(false);
        this.correctingId.set(null);
        this.auditMessage.set('Ingresso reaberto; a correção foi registrada.');
        this.loadAudit(true);
      },
      error: () => {
        this.workingCorrection.set(false);
        this.auditMessage.set('Não foi possível corrigir este check-in.');
      },
    });
  }

  protected resultLabel(outcome: EntryResult['outcome']): string {
    if (outcome === 'ACCEPTED') return 'Entrada aceita';
    if (outcome === 'ALREADY_USED' || outcome === 'REFUNDED' || outcome === 'OFFLINE_CONFLICT' || outcome === 'OPERATOR_UNAUTHORIZED') return 'Entrada recusada';
    return 'Ingresso não validado';
  }

  protected resultTone(outcome: EntryResult['outcome']): string {
    return outcome === 'ACCEPTED' ? 'accepted'
      : outcome === 'ALREADY_USED' || outcome === 'OFFLINE_CONFLICT' ? 'rejected' : 'invalid';
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'America/Sao_Paulo',
    }).format(new Date(value));
  }

  private async restoreOfflineState(): Promise<void> {
    const manifest = await this.offlineStore.latestManifest(this.eventId, this.auth.session()?.email);
    const sessionEmail = this.auth.session()?.email;
    this.manifest.set(manifest && (!sessionEmail || manifest.operatorEmail === sessionEmail) ? manifest : null);
    await this.refreshPendingCount();
  }

  private async refreshPendingCount(): Promise<void> {
    this.pendingCount.set((await this.offlineStore.pendingScans(this.eventId)).length);
  }

  private async processOfflineQr(qrCodeValue: string): Promise<void> {
    const manifest = this.manifest() ?? await this.offlineStore.latestManifest(this.eventId, this.auth.session()?.email);
    const sessionEmail = this.auth.session()?.email;
    if (!manifest || (sessionEmail && manifest.operatorEmail !== sessionEmail)
      || new Date(manifest.expiresAt).getTime() <= Date.now()) {
      this.provisionalMessage.set('Dados offline ausentes ou expirados. Conecte-se para preparar o evento.');
      this.processing.set(false);
      return;
    }
    try {
      const digest = await this.hash(qrCodeValue);
      const ticket = manifest.tickets.find((entry) => entry.qrTokenHash === digest);
      if (!ticket) {
        this.provisionalMessage.set('Ingresso não consta nos dados offline. Não confirme a entrada.');
      } else if (ticket.used || await this.offlineStore.hasPendingTicket(this.eventId, digest)) {
        this.provisionalMessage.set('Este ingresso já consta como usado neste dispositivo.');
      } else {
        const scan: PendingOfflineScan = {
          eventId: this.eventId,
          manifestId: manifest.id,
          deviceId: this.getDeviceId(),
          scanId: crypto.randomUUID(),
          qrTokenHash: digest,
          scannedAt: new Date().toISOString(),
        };
        await this.offlineStore.saveScan(scan);
        this.provisionalMessage.set('Registrado localmente. A entrada só será confirmada após sincronizar.');
        await this.refreshPendingCount();
      }
    } catch {
      this.provisionalMessage.set('Armazenamento offline indisponível neste dispositivo.');
    }
    this.processing.set(false);
    if (this.manualCode) this.manualCode.nativeElement.value = '';
  }

  private async hash(value: string): Promise<string> {
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value));
    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  private getDeviceId(): string {
    const key = 'ingresso.entry.device-id';
    const existing = localStorage.getItem(key);
    if (existing) return existing;
    const id = crypto.randomUUID();
    localStorage.setItem(key, id);
    return id;
  }
}