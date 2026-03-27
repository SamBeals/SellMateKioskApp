# AGENTS.md

## Project
SellMate Android Kiosk App

This repo is the Android tablet app mounted on the machine. It displays inventory, builds an order, calls the cloud backend, and drives the checkout flow.

## Current Structure
This is a Kotlin / Android / Jetpack Compose project with a standard Gradle layout.
Observed pieces include:
- `MainActivity.kt`
- `SellMateApi.kt`
- Firestore-backed planogram loading
- checkout manager wiring
- inventory and checkout screens

## Current Runtime Assumptions
Observed in code:
- `machineId = "machine_001"`
- cloud base URL is currently hardcoded
- inventory is loaded via Firestore
- checkout uses a `CheckoutManager`
- app switches between Inventory and Checkout screens

## Current Flow
Preferred current flow:

1. Load planogram / inventory for the machine
2. User adds items to an `OrderDraft`
3. App opens checkout
4. App creates order through cloud API
5. App starts payment through cloud API
6. Terminal handles payment
7. Backend webhook triggers vend
8. App polls order status as needed
9. After completion or cancellation, app returns to inventory screen

The kiosk app should not directly control vending hardware.

## API Contract
Observed API methods:
- `createOrder`
- `startPayment`
- `getOrder`

Assume the backend is the source of truth for order state.

## Important Invariants
- Do not vend directly from the tablet app
- Preserve `machine_001` assumptions unless changing configuration intentionally
- Preserve cloud API compatibility
- Preserve Firestore planogram/inventory loading behavior unless migration is explicit
- UI should return cleanly to home/inventory after a completed flow

## Coding Preferences
- Prefer small diffs
- Avoid duplicate data classes or duplicate response models
- Search for existing models before creating new ones
- Preserve Compose navigation/state patterns unless requested otherwise
- Keep UI changes separate from backend-flow changes when possible

## Validation
Before finishing:
- confirm project compiles
- check for duplicate model classes
- check constructor arguments and factory wiring
- verify `baseUrl`, `machineId`, and Retrofit setup still align
- verify checkout-to-home behavior still works

## Useful Tasks for Agents
Good tasks:
- checkout UX improvements
- quantity controls
- cancel-order flow
- polling improvements
- making config less hardcoded
- handling stale or failed orders

High-risk tasks:
- changing order lifecycle semantics
- mixing UI refactors with API refactors
- introducing new models that duplicate old ones
