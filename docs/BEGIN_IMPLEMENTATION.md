# MONYTIX Premium Redesign — Begin Implementation

This document maps the first implementation steps to **actual files** in `apk/` and `ios_monytix/` so you can start coding immediately.

**Prerequisites:** [MONYTIX_PREMIUM_REDESIGN.md](./MONYTIX_PREMIUM_REDESIGN.md), [ANDROID_ARCHITECTURE.md](./ANDROID_ARCHITECTURE.md), [IOS_ARCHITECTURE.md](./IOS_ARCHITECTURE.md), [DESIGN_TOKENS_AND_COMPONENTS.md](./DESIGN_TOKENS_AND_COMPONENTS.md).

---

## Phase 1: Quick wins (no new screens)

### 1.1 Empty state copy (both platforms)

- **Android:**  
  - File: `apk/.../common/EmptyState.kt` (or equivalent).  
  - Action: Add or update composables to use the exact titles/subtitles/CTAs from MONYTIX_PREMIUM_REDESIGN section L.  
  - Call sites: Home, SpendSense, GoalTracker, BudgetPilot, MoneyMoments, upload flow.

- **iOS:**  
  - File: `ios_monytix/ios_monytix/Utilities/MonytixUtilities.swift` (EmptyStateView) or `Components`.  
  - Action: Same copy as Android; ensure one EmptyStateView used everywhere.  
  - Call sites: MolyConsoleView, SpendSenseView, GoalTrackerView, BudgetPilotView, MoneyMomentsView, upload.

### 1.2 Trust block (both platforms)

- **Android:**  
  - New file: `apk/.../design/components/trust/TrustBanner.kt` (or under `ui/`).  
  - Composable: icon + headline + body + optional link.  
  - Use in: Auth screen (below sign-in), Upload confirm (in `UploadStatementScreen` or confirm dialog).

- **iOS:**  
  - New file: `ios_monytix/ios_monytix/Components/TrustBannerView.swift` (or Design/Components).  
  - View: same content.  
  - Use in: AuthScreen, UploadConfirmSheet.

### 1.3 Design tokens check

- **Android:** `apk/.../ui/theme/Color.kt`, `Type.kt`, `Shapes.kt`.  
  - Ensure semantic and chart colors exist; add any from DESIGN_TOKENS_AND_COMPONENTS.

- **iOS:** `ios_monytix/ios_monytix/Theme/MonytixTheme.swift`, `MonytixChartTokens.swift`.  
  - Same: add any missing semantic tokens.

---

## Phase 2: Home first fold + Future tab

### 2.1 Android: Home refactor

- **Files:**  
  - `apk/.../home/HomeScreen.kt`  
  - New: `apk/.../home/components/HealthCard.kt`, `RiskCard.kt`, `CtaCard.kt`

- **Actions:**  
  - Change Home from tabbed (Overview, Accounts, Spending, Goals, AI Insight) to **single scroll**.  
  - First section: Health card (score + trend) + Risk card (label + reason) + CTA card (one primary action).  
  - Data: from `HomeViewModel` — add or map `healthScore`, `riskState`, `nextAction` (can be mock initially).  
  - Keep: goal summary row, recent transactions, and one “Insights” section (top 3 cards) below.

### 2.2 Android: Future screen

- **New files:**  
  - `apk/.../feature-future/FutureScreen.kt` (or `future/FutureScreen.kt`)  
  - `apk/.../feature-future/FutureViewModel.kt`

- **Navigation:**  
  - In `AppDestinations` add `FUTURE`.  
  - In `MainContent` / bottom bar add “Future” tab; content = `FutureScreen`.  
  - FutureViewModel: state = confidence label, projection points (mock list), risk strip, savings opportunity, list of recommendations (mock).  
  - Layout: header, confidence, placeholder for chart (or simple line), risk strip, savings, recommendation cards.

### 2.3 iOS: Home refactor

- **Files:**  
  - `ios_monytix/ios_monytix/Home/MolyConsoleView.swift`  
  - New: `ios_monytix/ios_monytix/Home/Components/HealthCardView.swift`, `RiskCardView.swift`, `CtaCardView.swift`

- **Actions:**  
  - Same as Android: single scroll; first fold = Health + Risk + CTA.  
  - MolyConsoleViewModel: add or map `healthScore`, `riskState`, `nextAction`.  
  - Keep goal pulse, recent transactions, insights section below.

### 2.4 iOS: Future screen

- **New files:**  
  - `ios_monytix/ios_monytix/Future/FutureView.swift`  
  - `ios_monytix/ios_monytix/Future/FutureViewModel.swift`

- **Navigation:**  
  - In `MainTabView.swift` add `future` case and tab; content = `FutureView`.  
  - FutureViewModel: same state as Android (mock data).  
  - Layout: same as Android (header, confidence, chart placeholder, risk, savings, recommendations).

---

## Phase 3: Charts and insight cards

### 3.1 Forecast chart (both)

- **Android:** `apk/.../design/charts/ForecastChart.kt` (or under feature-future).  
  - Input: `List<Pair<Long, Double>>` or `List<Pair<Float, Float>>`.  
  - Draw line or area with theme color; optional risk zone; animate path (e.g. Animatable).

- **iOS:** `ios_monytix/ios_monytix/Charts/ForecastChartView.swift` (or Design/Charts).  
  - Input: `[(Date, Double)]` or `[CGPoint]`.  
  - SwiftUI Path + trim animation or Swift Charts.

### 3.2 Insight card + model (both)

- **Android:**  
  - Model: `data class Insight` in ViewModel or data layer.  
  - Component: `apk/.../design/components/cards/InsightCard.kt` (or feature-home).  
  - HomeViewModel: `List<Insight> topInsights` (mock 3 items); render in Home.

- **iOS:**  
  - Model: `struct Insight` in FutureViewModel or shared.  
  - Component: `InsightCardView.swift` in Home/Components or Design.  
  - MolyConsoleViewModel: `var topInsights: [Insight]` (mock); render in MolyConsoleView.

---

## Phase 4: Goal detail + Assistant shell

### 4.1 Goal detail (both)

- **Android:** `apk/.../goaltracker/GoalDetailScreen.kt`.  
  - Add: required monthly, current pace, projected completion date, gap from ideal, one “AI plan” line, risk state (“On track” / “At risk” / “Stalled”).  
  - Data: from GoalTrackerViewModel or GoalDetail state; can mock until backend supports.

- **iOS:** `ios_monytix/ios_monytix/GoalTracker/GoalDetailView.swift` (or equivalent).  
  - Same fields and layout.

### 4.2 Assistant (both)

- **Android:** New screen or bottom sheet: “Ask MONYTIX”; prompt chips (e.g. “Can I afford this?” “Will I run short?”); answer area (mock text).  
  - Entry: FAB on Home or item in More.

- **iOS:** New sheet: same content.  
  - Entry: button on Home or in More.

---

## File checklist (first 2 phases)

**Android (apk):**

- [ ] `common/EmptyState.kt` — premium copy
- [ ] `design/components/trust/TrustBanner.kt` (or ui) — new
- [ ] `home/HomeScreen.kt` — single scroll, first fold
- [ ] `home/components/HealthCard.kt`, `RiskCard.kt`, `CtaCard.kt` — new
- [ ] `home/HomeViewModel.kt` — health, risk, nextAction (mock ok)
- [ ] `future/FutureScreen.kt`, `FutureViewModel.kt` — new
- [ ] `MainActivity` / `MainContent` — add FUTURE destination and tab

**iOS (ios_monytix):**

- [ ] `Utilities/MonytixUtilities.swift` or Components — EmptyStateView premium copy
- [ ] `Components/TrustBannerView.swift` — new
- [ ] `Home/MolyConsoleView.swift` — single scroll, first fold
- [ ] `Home/Components/HealthCardView.swift`, `RiskCardView.swift`, `CtaCardView.swift` — new
- [ ] `Home/MolyConsoleViewModel.swift` — health, risk, nextAction (mock ok)
- [ ] `Future/FutureView.swift`, `FutureViewModel.swift` — new
- [ ] `App/MainTabView.swift` — add Future tab

---

After Phase 2 you will have:  
- Home that feels like a command center (health + risk + one action).  
- A dedicated Financial Future tab with placeholder chart and recommendations.  
- Premium empty states and trust blocks in key flows.  

Then proceed to Phase 3 (charts, insight cards) and Phase 4 (goal detail, assistant) per ANDROID_ARCHITECTURE and IOS_ARCHITECTURE.

---

## Phase 5: Backend wiring + polish (next)

Phases 1–4 are implemented (see [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)). Phase 5 focuses on wiring real data and optional polish.

### 5.1 Forecast: real data (both)

- **Backend:** If a forecast/cash-projection API exists, call it from `FutureViewModel` (APK) and `FutureViewModel` (iOS). Replace `loadMockData()` / mock projection points with API response.
- **Android:** `future/FutureViewModel.kt` — replace mock `projectionPoints`, `riskStripLabel`, `savingsOpportunity`, `recommendations` with API or shared module.
- **iOS:** `Future/FutureViewModel.swift` — same; optional `FutureRepository` or use existing `BackendApi` if endpoints exist.

### 5.2 Insights: unified model + API (both)

- **Backend:** Use existing insights API (e.g. `getInsights` or SpendSense insights) to drive Home “Insights” section. Ensure one canonical list (type, title, body, severity, CTA) per redesign.
- **Android:** `home/HomeViewModel.kt` — `topInsights` from API; ensure `InsightCardCompact` (or shared `InsightCard`) uses it. Remove or reduce mock in `generateAiInsights` if backend returns insights.
- **iOS:** `Home/MolyConsoleViewModel.swift` — same; `generateAiInsights()` can merge API insights with fallback mocks until backend is sufficient.

### 5.3 Assistant: real answers (both)

- **Backend:** When an “Ask MONYTIX” or assistant API exists, call it from the assistant sheet with the selected prompt or user text. Show loading state; render structured answer (bullets, CTA) when possible.
- **Android:** `assistant/AssistantSheet.kt` — add `AssistantViewModel` or use callback to fetch answer; replace `mockAnswerFor()` with API call.
- **iOS:** `Assistant/AssistantSheetView.swift` — same; optional `AssistantViewModel` with async fetch.

### 5.4 Optional polish

- **Chart path animation:** Animate forecast line draw (trim path or animated progress) on both platforms for investor demo.
- **Skeleton loading:** Replace spinner with skeleton/shimmer on Home and Future where loading time is noticeable.
- **Goal projected date from API:** If backend returns `projected_completion_date`, both platforms already display it; ensure it’s wired in goal progress API response.
