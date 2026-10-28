import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export const API_BASE_URL = '';
export type AppointmentStatus = 'CONFIRMED' | 'RESCHEDULE_PENDING' | 'RESCHEDULED' | 'COMPLETED';
export type ProviderStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE';
export type SlotStatus = 'OPEN' | 'BOOKED' | 'CANCELLED';
export type AppointmentType = 'ROUTINE' | 'URGENT' | 'FOLLOW_UP';
export type TriggerReason = 'INITIAL' | 'PROVIDER_OFFLINE' | 'SLOT_CANCELLED';

export interface Appointment { id: string; patientName: string; appointmentType: AppointmentType; providerId: string; slotId: string; status: AppointmentStatus; slotStartTime: string; }
export interface Provider { id: string; name: string; specialty: string; status: ProviderStatus; }
export interface Slot { id: string; startTime: string; durationMinutes: number; providerId: string; status: SlotStatus; }
export interface Suggestion { id: string; appointmentId: string; recommendedSlotId: string; providerId: string; confidence: number; reasoning: string; status: 'PENDING' | 'ACCEPTED' | 'REJECTED'; triggerReason: TriggerReason; recommendedStartTime: string; }
export interface CreateAppointment { patientName: string; appointmentType: AppointmentType; providerId: string; slotId: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;
  appointments(status?: AppointmentStatus): Observable<Appointment[]> { const params = status ? new HttpParams().set('status', status) : undefined; return this.http.get<Appointment[]>(`${this.base}/appointments`, { params }); }
  providers(): Observable<Provider[]> { return this.http.get<Provider[]>(`${this.base}/providers`); }
  slots(): Observable<Slot[]> { return this.http.get<Slot[]>(`${this.base}/slots`); }
  pendingSuggestions(): Observable<Suggestion[]> { return this.http.get<Suggestion[]>(`${this.base}/suggestions`, { params: new HttpParams().set('status', 'PENDING') }); }
  createAppointment(request: CreateAppointment): Observable<Appointment> { return this.http.post<Appointment>(`${this.base}/appointments`, request); }
  setProviderStatus(id: string, status: ProviderStatus): Observable<Provider> { return this.http.patch<Provider>(`${this.base}/providers/${id}/status`, { status }); }
  setSlotStatus(id: string, status: SlotStatus): Observable<Slot> { return this.http.patch<Slot>(`${this.base}/slots/${id}/status`, { status }); }
  decideSuggestion(id: string, decision: 'ACCEPTED' | 'REJECTED'): Observable<Suggestion> { return this.http.patch<Suggestion>(`${this.base}/suggestions/${id}`, { decision }); }
  suggest(appointmentId: string): Observable<Suggestion> { return this.http.post<Suggestion>(`${this.base}/appointments/${appointmentId}/suggest`, null); }
}