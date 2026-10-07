import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from './api';

export type AdmissionMode = 'GENERAL_ADMISSION' | 'ASSIGNED_SEAT';
export type EventStatus = 'PUBLISHED' | 'SUSPENDED' | 'CANCELLED';
export type SeatStatus = 'AVAILABLE' | 'SOLD' | 'UNAVAILABLE';

export interface EventPage<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
}

export interface EventSummary {
  id: number;
  title: string;
  imageUrl: string;
  startsAt: string;
  city: string;
  stateCode: string;
  lowestPriceInCents: number;
  status: EventStatus;
}

export interface EventCategory {
  id: number;
  code: string;
  name: string;
  priceInCents: number;
  admissionMode: AdmissionMode;
  configuredQuantity: number;
  availableQuantity: number;
  soldQuantity: number;
}

export interface EventSeat {
  id: number;
  label: string;
  positionIndex: number;
  categoryCode: string;
  categoryName: string;
  priceInCents: number;
  status: SeatStatus;
}

export interface EventSeatRow {
  id: number;
  label: string;
  positionIndex: number;
  seats: EventSeat[];
}

export interface EventSeatSector {
  id: number;
  name: string;
  positionIndex: number;
  rows: EventSeatRow[];
}

export interface EventDetail {
  id: number;
  title: string;
  description: string;
  imageUrl: string;
  startsAt: string;
  timeZone: string;
  venue: { name: string; streetAddress: string; city: string; stateCode: string };
  ageClassification: string;
  organizer: string;
  status: EventStatus;
  categories: EventCategory[];
  sectors: EventSeatSector[];
}

export interface EventCategoryInput {
  code: string;
  name: string;
  priceInCents: number;
  admissionMode: AdmissionMode;
  quantity: number | null;
}

export interface EventSeatInput {
  label: string;
  positionIndex: number;
  categoryCode: string;
}

export interface EventSeatRowInput {
  label: string;
  positionIndex: number;
  seats: EventSeatInput[];
}

export interface EventSeatSectorInput {
  name: string;
  positionIndex: number;
  rows: EventSeatRowInput[];
}

export interface EventInput {
  title: string;
  description: string;
  imageUrl: string;
  startsAt: string;
  venueName: string;
  streetAddress: string;
  city: string;
  stateCode: string;
  ageClassification: string;
  organizer: string;
  categories: EventCategoryInput[];
  sectors: EventSeatSectorInput[];
}

@Injectable({ providedIn: 'root' })
export class EventApi {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  listPublic(page = 0, size = 12) {
    return this.http
      .get<EventPage<EventSummary>>(`${this.api}/eventos`, {
        params: { page, size, sort: 'startsAt,asc' },
      })
      .pipe(timeout(15000));
  }

  getPublic(id: number) {
    return this.http.get<EventDetail>(`${this.api}/eventos/${id}`).pipe(timeout(15000));
  }

  listProducer(page = 0, size = 20) {
    return this.http
      .get<EventPage<EventSummary>>(`${this.api}/produtor/eventos`, { params: { page, size } })
      .pipe(timeout(15000));
  }

  getProducer(id: number) {
    return this.http.get<EventDetail>(`${this.api}/produtor/eventos/${id}`).pipe(timeout(15000));
  }

  create(input: EventInput) {
    return this.http.post<EventDetail>(`${this.api}/produtor/eventos`, input).pipe(timeout(15000));
  }

  update(id: number, input: EventInput) {
    return this.http.put<EventDetail>(`${this.api}/produtor/eventos/${id}`, input).pipe(timeout(15000));
  }

  reactivate(id: number) {
    return this.http.patch<EventDetail>(`${this.api}/produtor/eventos/${id}/reativar`, {}).pipe(timeout(15000));
  }

  listAdmin(page = 0, size = 20) {
    return this.http
      .get<EventPage<EventSummary>>(`${this.api}/admin/eventos`, { params: { page, size } })
      .pipe(timeout(15000));
  }

  suspend(id: number) {
    return this.http.patch<void>(`${this.api}/admin/eventos/${id}/suspender`, {}).pipe(timeout(15000));
  }

  cancel(id: number) {
    return this.http.patch<void>(`${this.api}/admin/eventos/${id}/cancelar`, {}).pipe(timeout(15000));
  }

  cancelOwned(id: number) {
    return this.http.patch<void>(`${this.api}/produtor/eventos/${id}/cancelar`, {}).pipe(timeout(15000));
  }
}