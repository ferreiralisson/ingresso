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
