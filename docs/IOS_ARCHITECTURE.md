# MONYTIX iOS Native Architecture

**Stack:** Swift, SwiftUI, iOS 16+ (or 17+ for Swift Charts if used).  
**Aligns with:** [MONYTIX_PREMIUM_REDESIGN.md](./MONYTIX_PREMIUM_REDESIGN.md) sections U, V, W, X, Y.

---

## 1. Folder structure

Current layout under `ios_monytix/ios_monytix/`. For premium redesign, group by feature + design without breaking existing structure:

```
ios_monytix/
├── App/
│   ├── ios_monytixApp.swift
│   ├── AppDelegate.swift
│   └── MainTabView.swift              # Add Future tab; 5 tabs: Home, Future, Spend, Goals, More
│
├── Design/
│   ├── Theme/
│   │   ├── MonytixTheme.swift         # Existing
│   │   ├── MonytixChartTokens.swift
│   │   └── MonytixMotion.swift
│   ├── Components/
│   │   ├── MonytixComponents.swift   # Buttons, StatCard, AlertBanner, SectionHeader
│   │   ├── MonytixRingLoader.swift   # Spinner, Skeleton, overlay
│   │   ├── EmptyStateView.swift      # Reuse from Utilities or move here
│   │   └── TrustBannerView.swift     # NEW
│   └── Charts/
│       ├── WeekdayHeatmap.swift
│       ├── RankedBars.swift
│       └── ForecastChartView.swift   # NEW: line/area for Future
│
├── Features/
│   ├── Home/
│   │   ├── MolyConsoleView.swift     # Refactor to single scroll: health, risk, CTA, goal pulse, insights, recent
│   │   ├── MolyConsoleViewModel.swift
│   │   └── Components/
│   │       ├── HealthCardView.swift
│   │       ├── RiskCardView.swift
│   │       ├── CtaCardView.swift
│   │       ├── GoalPulseRowView.swift
│   │       └── InsightCardView.swift
│   │
│   ├── Future/                        # NEW
│   │   ├── FutureView.swift
│   │   ├── FutureViewModel.swift
│   │   └── Components/
│   │       ├── ForecastChartSection.swift
│   │       ├── RiskStripView.swift
│   │       └── RecommendationCardView.swift
│   │
│   ├── SpendSense/
│   ├── GoalTracker/
│   ├── BudgetPilot/
│   ├── MoneyMoments/
│   ├── Profile/
│   ├── Auth/
│   └── Upload/
│
├── Utilities/
│   ├── BackendApi.swift
│   ├── BackendConfig.swift
│   └── MonytixUtilities.swift
└── Resources/
```

**Note:** If you prefer not to rename existing folders, keep `Home/`, `SpendSense/`, etc. at current paths and add only `Future/` and new components under `Design/` and feature `Components/`.

---

## 2. Navigation architecture

- **Root:** `TabView` with 5 tabs: Home, Future, Spend, Goals, More.
- **More:** Single view with list: Budget, Moments, Profile, Settings. Each item pushes or presents the existing BudgetPilotView, MoneyMomentsView, ProfileView.
- **Home:** One scroll (no inner tab bar); optional `ScrollView` + `LazyVStack` with sections.
- **Future:** One scroll; same pattern.
- **Spend:** Keep existing segmented control (Categories, Transactions, Insights).
- **Goals:** `NavigationStack` with list; `navigationDestination(item: $selectedGoal)` → `GoalDetailView`; sheets for Add/Edit goal.
- **Sheets:** Upload confirm, Add transaction, Add/Edit goal, Assistant — use `.sheet(item:)` or `.sheet(isPresented:)`.

---

## 3. State management approach

- **ViewModels:** `ObservableObject` with `@Published` properties; call `BackendApi` in `Task { @MainActor in ... }` or async methods; update state on main thread.
- **Screen state:** One main `@StateObject` per screen (e.g. `MolyConsoleViewModel`, `FutureViewModel`). Child views receive `@ObservedObject` or pass bindings where needed.
- **Future:** `FutureViewModel` with `@Published var projectionPoints: [CGPoint] or [(Date, Double)]`, `riskStrip`, `savingsOpportunity`, `recommendations`, `isLoading`, `errorMessage`. Start with mock data.

---

## 4. Reusable SwiftUI component system

| Component | Purpose | Location |
|-----------|---------|----------|
| MonytixPrimaryButton | Filled CTA | Design/Components |
| MonytixSecondaryButton | Outline | Design/Components |
| MonytixStatCard | Title, value, delta | Design/Components |
| InsightCardView | Icon, title, body, severity, CTA | Home/Components or Design |
| EmptyStateView | Icon, title, subtitle, CTA | Design/Components or Utilities |
| TrustBannerView | Icon, headline, body | Design/Components (NEW) |
| SkeletonCard, MonytixRingLoader | Loading | Design/Components |
| ForecastChartView | Line/area cash projection | Design/Charts (NEW) |
| HealthCardView, RiskCardView, CtaCardView | Home first fold | Home/Components (NEW) |

---

## 5. Screen implementation order (iOS)

1. **Design tokens** — Confirm MonytixTheme, MonytixSpace, MonytixShape match redesign; add any missing semantic tokens.
2. **Empty state** — Single `EmptyStateView` (or extend existing) with premium copy; use across Home, Future, Spend, Goals.
3. **Trust block** — `TrustBannerView`; add to AuthScreen and UploadConfirmSheet.
4. **Home first fold** — Refactor `MolyConsoleView` to single scroll: `HealthCardView`, `RiskCardView`, `CtaCardView`; drop or merge inner tabs into one vertical stack.
5. **Home rest** — `GoalPulseRowView`, forecast strip, list of `InsightCardView` (mock), recent transactions.
6. **Future screen** — Add `FutureView`, `FutureViewModel`; add Future tab in `MainTabView`; layout: header, confidence, `ForecastChartView` (mock), risk strip, savings, recommendations.
7. **Insight model** — Struct for insight (type, title, body, severity, cta); use in Home and SpendSense.
8. **Goal detail** — Add pace, projected date, gap, “AI plan” line, risk state to `GoalDetailView`.
9. **Charts** — Implement `ForecastChartView` (SwiftUI `Path` + `trim` animation or Swift Charts); reuse RankedBars, WeekdayHeatmap where needed.
10. **Assistant** — Entry point (e.g. from Home or More); sheet with prompt chips and answer area; mock answers.

---

## 6. Design system structure

- **Theme:** `MonytixTheme.swift` — colors, gradients, MonytixShape, MonytixSpace. Keep existing; ensure chart and semantic colors are used consistently.
- **Motion:** `MonytixMotion.swift` — durations, curves; use in `.animation()`, `withAnimation`, and custom modifiers (e.g. staggered entrance).
- **Charts:** `MonytixChartTokens` for axis, grid, line/fill; `ForecastChartView` in Design/Charts.

---

## 7. Mock data structure

- **Home:** Health score (value, trend), risk state (label, reason), next action (type, label), goal pulse (count, amountToGo, items), forecast strip text, `[Insight]`, recent transactions.
- **Future:** `projectionPoints: [(Date, Double)]`, risk strip string, savings opportunity string, `[Recommendation]`.
- **Insight:** `struct Insight { id, type, title, body, severity, ctaLabel, ctaAction }`.

Use static preview data or mock in ViewModel; replace with BackendApi when endpoints are ready.

---

## 8. Animation implementation direction

- **Card entrance:** `.opacity` + `.offset` with `withAnimation(.easeOut(duration: 0.3).delay(Double(index) * 0.05))` or custom `MonytixMotion.Enter` modifier.
- **Chart:** `trim(from: 0, to: progress)` with `animation(.easeOut(duration: 0.5), value: progress)`.
- **Loading:** Skeleton with `shimmer` (gradient + animation) or existing `MonytixRingLoader`.
- **Button:** `ButtonStyle` with scaleEffect on press (0.98).

---

## 9. Chart integration approach

- **Option A:** SwiftUI `Path` + `Shape` for line/area; animate with `trim` or `AnimatableData`.
- **Option B:** Swift Charts (iOS 16+) for line/area and bars; style with MonytixTheme colors.
- Same data contract as Android: array of (date or index, value) for forecast; array of (name, amount) for ranked bars.

---

## 10. Theming / token structure

- **MonytixTheme:** bg, surface, surface2, stroke; text1, text2, text3; cyan1, cyan2; success, warn, danger, info; Chart palette.
- **MonytixSpace:** xs 6, sm 10, md 16, lg 24, xl 32, xxl 40.
- **MonytixShape:** cardRadius 24, inputRadius 18, buttonRadius 20, smallRadius 16.
- **MonytixMotion:** fast/base/slow durations; easeOut, spring curves.

---

*Use this document with MONYTIX_PREMIUM_REDESIGN.md. Start with Home first fold and Future screen, then empty states and trust, then insights and goal detail.*
