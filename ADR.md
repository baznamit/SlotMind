# Architecture Decision Record: SlotMind

**Status:** Accepted
**Date:** 2026-10-28

## Context

SlotMind is a local CareBridge clinic demonstration for recovering confirmed appointments when a provider goes offline or a slot is cancelled. The system must produce recommendations without automatically changing a booking, allow a front-desk user to approve or reject one, and prevent two users from booking the same slot.

## Decisions

### 1. Use a modular Spring Boot monolith

The backend is one Spring Boot 3 application with logical areas for appointments, providers, slots, suggestions, scheduling, AI, workflow, and shared API/domain code. JPA and an in-memory H2 database support the local demo.

**Alternatives:** separate microservices, a message-broker-based workflow, or a single unstructured package.

**Trade-offs:** a monolith is less independently scalable, but keeps the demo simple and makes the appointment/slot transaction local and easy to reason about. Distributed deployment, Kafka, and durable external workflow processing are deferred.

### 2. Use `SchedulingStrategy` with a runtime registry

`SchedulingStrategy.recommend(SchedulingContext)` is the common contract used by both manual HTTP requests and asynchronous disruption processing. `SchedulingStrategyRegistry` selects the configured implementation at runtime (`rule-based` by default or `llm`).

**Alternatives:** hard-code one algorithm in the service or select an implementation with compile-time wiring.

**Trade-offs:** the registry adds indirection, but prevents callers from knowing which strategy is active and makes another strategy implementable without changing workflow orchestration.

### 3. Keep rule scheduling and the LLM behind separate boundaries

`RuleBasedSchedulingStrategy` implements deterministic trigger-aware eligibility and ordering. The LLM path implements the same strategy contract and calls the `LlmGateway` abstraction; `LiteLlmChatCompletionsGateway` owns HTTP, configuration, headers, and authorization assembly.

The rule path excludes non-open slots and offline providers, preserves the original provider preference for cancelled slots, and treats `BUSY` as insufficient by itself to exclude a provider with an explicit open slot.

**Alternatives:** call the LLM directly from the application service or make AI the only scheduler.

**Trade-offs:** the extra gateway boundary is more code, but isolates vendor/protocol concerns and keeps the deterministic path usable without network access.

### 4. Validate LLM output and fall back to rules

The LLM strategy accepts only a structurally valid response with a known slot, finite confidence from 0 to 1, non-blank reasoning, a valid provider/slot relationship, an open slot, a non-offline provider, compatible specialty, and trigger-specific eligibility. Any gateway, parsing, or validation failure invokes the rule-based strategy.

This is a safety boundary: generated text never directly becomes a booking. Live LLM use is configuration-dependent and is not claimed as verified by this ADR.

### 5. Process disruptions asynchronously after commit

Provider and slot status services publish domain events in their transaction. `@TransactionalEventListener(AFTER_COMMIT)` plus `@Async` processes affected confirmed appointments only after the status change commits. A provider-offline event processes every affected appointment; a slot-cancelled event processes the appointment assigned to that slot.

**Alternatives:** process inline before returning or use an external queue.

**Trade-offs:** in-process async work provides the required non-blocking local behavior without operational infrastructure, but work is not durable across process crashes and requires a later retry/dead-letter design for production.

### 6. Protect against duplicate pending suggestions

Suggestion generation first checks for an existing `PENDING` suggestion with the same appointment and trigger reason and returns it instead of creating another. This makes repeated status events and repeated requests idempotent at the application level.

The protection is scoped to appointment plus trigger reason. Different appointments can initially recommend the same open slot because they are processed independently; acceptance remains the final availability checkpoint.

**Deferred improvement:** reserve or allocate candidate slots among pending suggestions if the product wants to reduce stale recommendations.

### 7. Accept suggestions transactionally and reject stale slots

Acceptance is a service transaction. It verifies pending state, reloads the appointment and target slot, checks that the target remains open and eligible, books the target, releases the old booked slot, moves the appointment, and marks the suggestion accepted. A rejected suggestion changes only its own status.

A target that has already been booked, cancelled, or made ineligible produces a conflict and does not perform the move. A second acceptance of the same suggestion is rejected; concurrent acceptance paths are intended to allow only one booking.

**Alternatives:** reserve at suggestion creation or accept with last-write-wins.

**Trade-offs:** delaying reservation avoids holding slots for unapproved recommendations, but means stale suggestions are expected and must be rejected or refreshed. Database optimistic/guarded state handling is the required production hardening point for stronger concurrent guarantees.

### 8. Use Angular polling and a human approval checkpoint

The Angular 17 front desk loads appointments, pending suggestions, providers, and slots; polls pending suggestions every 10 seconds; presents loading/error states; and exposes accept/reject actions. Recommendations never become bookings without the human decision.

**Alternatives:** server-sent events or automatic acceptance.

**Trade-offs:** polling is less immediate and less efficient than SSE, but is sufficient for the demo and easier to operate. SSE and automated rescheduling are intentionally deferred.

## Assumptions

- H2 and Java-based relative seed times are suitable for the local demonstration.
- Matching specialty is the implemented provider compatibility rule.
- A rejection does not move an appointment back from `RESCHEDULE_PENDING`; this follows the implemented service behavior because the brief does not prescribe that transition.
- The recommended slot's persisted provider is authoritative; the optional provider value in a recommendation cannot override it.
- The local in-process executor is acceptable for the hackathon workflow.

## Deferred features

SSE reasoning streams, waitlist auto-fill, notifications, patient self-service, multi-clinic routing, room/equipment constraints, patient time preferences, provider daily limits, full-day calendar views, queue-pressure optimization, durable workflow queues, and slot reservation/fair allocation are not implemented.
