import { Injectable } from '@angular/core';
import { DBSchema, IDBPDatabase, openDB } from 'idb';
import { OfflineManifest, PendingOfflineScan } from './entry-api';

interface StoredManifest {
  id: string;
  eventId: number;
  manifest: OfflineManifest;
}

interface StoredScan extends PendingOfflineScan {
}

interface EntryDatabase extends DBSchema {
  manifests: {
    key: string;
    value: StoredManifest;
    indexes: { 'by-event': number };
  };
  scans: {
    key: string;
    value: StoredScan;
    indexes: { 'by-event': number };
  };
}

@Injectable({ providedIn: 'root' })
export class EntryOfflineStore {
  private database?: Promise<IDBPDatabase<EntryDatabase>>;

  async saveManifest(manifest: OfflineManifest): Promise<void> {
    const database = await this.open();
    await database.put('manifests', { id: manifest.id, eventId: manifest.eventId, manifest });
  }

  async latestManifest(eventId: number, operatorEmail?: string): Promise<OfflineManifest | null> {
    const database = await this.open();
    const manifests = await database.getAllFromIndex('manifests', 'by-event', eventId);
    return manifests
      .map((stored) => stored.manifest)
      .filter((manifest) => !operatorEmail || manifest.operatorEmail === operatorEmail)
      .filter((manifest) => new Date(manifest.expiresAt).getTime() > Date.now())
      .sort((first, second) => second.createdAt.localeCompare(first.createdAt))[0] ?? null;
  }

  async manifestById(id: string): Promise<OfflineManifest | null> {
    const database = await this.open();
    return (await database.get('manifests', id))?.manifest ?? null;
  }

  async saveScan(scan: PendingOfflineScan): Promise<void> {
    const database = await this.open();
    await database.put('scans', scan);
  }

  async pendingScans(eventId: number): Promise<PendingOfflineScan[]> {
    const database = await this.open();
    return database.getAllFromIndex('scans', 'by-event', eventId);
  }

  async hasPendingTicket(eventId: number, qrTokenHash: string): Promise<boolean> {
    const scans = await this.pendingScans(eventId);
    return scans.some((scan) => scan.qrTokenHash === qrTokenHash);
  }

  async deleteScans(scanIds: string[]): Promise<void> {
    const database = await this.open();
    const transaction = database.transaction('scans', 'readwrite');
    await Promise.all(scanIds.map((scanId) => transaction.store.delete(scanId)));
    await transaction.done;
  }

  private open(): Promise<IDBPDatabase<EntryDatabase>> {
    this.database ??= openDB<EntryDatabase>('ingresso-entry', 1, {
      upgrade(database) {
        const manifests = database.createObjectStore('manifests', { keyPath: 'id' });
        manifests.createIndex('by-event', 'eventId');
        const scans = database.createObjectStore('scans', { keyPath: 'scanId' });
        scans.createIndex('by-event', 'eventId');
      },
    });
    return this.database;
  }
}