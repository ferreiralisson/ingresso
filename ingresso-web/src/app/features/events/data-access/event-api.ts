import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { timeout } from 'rxjs';
import { API_URL } from '../../../core/http/api-url';
import { EventDetail, EventInput, EventPage, EventSummary } from '../models/event.models';

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