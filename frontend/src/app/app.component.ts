import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, forkJoin, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ApiService, Appointment, AppointmentType, Provider, ProviderStatus, Slot, SlotStatus, Suggestion } from './api.service';

@Component({ selector: 'app-root', standalone: true, imports: [CommonModule, ReactiveFormsModule], templateUrl: './app.component.html' })
export class AppComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private polling?: Subscription;
  appointments: Appointment[] = []; pending: Suggestion[] = []; providers: Provider[] = []; slots: Slot[] = [];
  loading = true; busy = new Set<string>(); error = ''; message = '';
  readonly appointmentTypes: AppointmentType[] = ['ROUTINE', 'URGENT', 'FOLLOW_UP'];
  readonly form = this.fb.nonNullable.group({ patientName: ['', Validators.required], appointmentType: ['ROUTINE' as AppointmentType, Validators.required], providerId: ['', Validators.required], slotId: ['', Validators.required] });

  ngOnInit(): void { this.refresh(); this.polling = timer(10000, 10000).pipe(switchMap(() => this.api.pendingSuggestions())).subscribe({ next: value => this.pending = value, error: () => this.error = 'Unable to refresh pending suggestions.' }); }
  ngOnDestroy(): void { this.polling?.unsubscribe(); }
  refresh(): void { this.loading = true; this.error = ''; forkJoin({ appointments: this.api.appointments(), pending: this.api.pendingSuggestions(), providers: this.api.providers(), slots: this.api.slots() }).subscribe({ next: data => { this.appointments = data.appointments; this.pending = data.pending; this.providers = data.providers; this.slots = data.slots; this.loading = false; }, error: () => { this.loading = false; this.error = 'Unable to load the front desk data.'; } }); }
  providerName(id: string): string { return this.providers.find(item => item.id === id)?.name ?? id; }
  slotLabel(id: string): string { const slot = this.slots.find(item => item.id === id); return slot ? `${this.format(slot.startTime)} (${slot.id})` : id; }
  format(value: string): string { return new Date(value).toLocaleString(); }
  isBusy(id: string): boolean { return this.busy.has(id); }
  suggestionUnavailable(suggestion: Suggestion): boolean { return this.slots.find(slot => slot.id === suggestion.recommendedSlotId)?.status !== 'OPEN'; }
  offline(provider: Provider): void { this.run(provider.id, this.api.setProviderStatus(provider.id, 'OFFLINE'), 'Provider marked offline.'); }
  cancel(slot: Slot): void { this.run(slot.id, this.api.setSlotStatus(slot.id, 'CANCELLED'), 'Slot cancelled.'); }
  decide(suggestion: Suggestion, decision: 'ACCEPTED' | 'REJECTED'): void {
    if (decision === 'ACCEPTED' && this.suggestionUnavailable(suggestion)) {
      this.error = 'This slot is no longer available. Refresh the dashboard and reject this outdated suggestion.';
      this.refresh();
      return;
    }
    this.run(suggestion.id, this.api.decideSuggestion(suggestion.id, decision), decision === 'ACCEPTED' ? 'Suggestion accepted.' : 'Suggestion rejected.');
  }
  book(): void { if (this.form.invalid) { this.form.markAllAsTouched(); return; } this.run('booking', this.api.createAppointment(this.form.getRawValue()), 'Appointment booked.', () => this.form.reset({ patientName: '', appointmentType: 'ROUTINE', providerId: '', slotId: '' })); }
  private run<T>(id: string, request: import('rxjs').Observable<T>, success: string, after?: () => void): void {
    if (this.busy.has(id)) return;
    this.busy.add(id); this.error = ''; this.message = '';
    request.subscribe({
      next: () => { this.busy.delete(id); this.message = success; after?.(); this.refresh(); },
      error: (error: unknown) => {
        this.busy.delete(id);
        this.error = this.errorMessage(error);
        if (error instanceof HttpErrorResponse && error.status === 409) this.refresh();
      }
    });
  }
  private errorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) return 'Unable to reach the server. Check that the backend is running and try again.';
    const serverMessage = typeof error.error?.message === 'string' ? error.error.message : undefined;
    if (serverMessage) return serverMessage;
    if (error.status === 400) return 'The request is invalid. Check the entered values and try again.';
    if (error.status === 404) return 'The requested item was not found. Refresh the dashboard.';
    if (error.status === 409) return 'This action conflicts with the latest appointment state. Refresh the dashboard and try again.';
    if (error.status >= 500) return 'The server could not complete the request. Try again shortly.';
    return 'The request could not be completed. Try again.';
  }
}