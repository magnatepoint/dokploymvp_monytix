# MONYTIX Android Native Architecture

**Stack:** Kotlin, Jetpack Compose, Material 3 (customized), single Activity.  
**Aligns with:** [MONYTIX_PREMIUM_REDESIGN.md](./MONYTIX_PREMIUM_REDESIGN.md) sections T, V, W, X, Y.

---

## 1. Package / module structure

Current layout is feature-flat under `com.example.monytix`. For premium redesign, keep one app module and organize by feature + design:

```
app/src/main/java/com/example/monytix/
├── MainActivity.kt                    # Single Activity, Compose, nav state
├── MonytixApplication.kt
├── AppDestinations.kt                 # Enum: HOME, FUTURE, SPEND, GOALS, MORE
├── NavigationSuiteScaffold / MainContent
│
├── design/                            # Design system (new or rename from ui)
│   ├── theme/
│   │   ├── Color.kt                   # Existing; add semantic aliases if needed
│   │   ├── MonytixColors.kt
│   │   ├── Type.kt
│   │   ├── Shapes.kt
│   │   └── Theme.kt                   # MonytixTheme, PremiumGradientBackground
│   ├── components/                   # Reusable composables
│   │   ├── buttons/
│   │   ├── cards/
│   │   ├── empty/
│   │   ├── trust/
│   │   └── loading/
│   └── charts/
│       ├── ForecastChart.kt           # Line/area cash projection
│       ├── RankedBars.kt
│       └── WeekdayHeatmap.kt
│
├── feature-home/                     # Command center (rename/refactor home)
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   └── components/
│       ├── HealthCard.kt
│       ├── RiskCard.kt
│       ├── CtaCard.kt
│       ├── GoalPulseRow.kt
│       ├── ForecastStrip.kt
│       └── InsightCard.kt
│
├── feature-future/                   # NEW: Financial Future
│   ├── FutureScreen.kt
│   ├── FutureViewModel.kt
│   └── components/
│       ├── ForecastChartSection.kt
│       ├── RiskStrip.kt
│       └── RecommendationCard.kt
│
├── feature-spend/                    # SpendSense (rename spendsense)
│   ├── SpendSenseScreen.kt
│   ├── SpendSenseViewModel.kt
│   └── components/                   # Existing InteractivePieChart, InsightsComponents
│
├── feature-goals/
│   ├── GoalTrackerScreen.kt
│   ├── GoalDetailScreen.kt
│   ├── GoalTrackerViewModel.kt
│   └── components/
│
├── feature-budget/
├── feature-moments/
├── feature-profile/
├── feature-auth/
├── feature-preauth/
├── feature-datasupport/              # Upload, analyzing, success
│
├── data/                             # BackendApi, DTOs
├── common/                           # EmptyState templates, caches
└── ui/                               # MonytixRing, legacy theme if not moved
```

**Navigation:** Keep current `AppDestinations` + `rememberSaveable` or migrate to `NavController` with a single `NavHost` and bottom bar. Add `FUTURE` destination and tab.

---

## 2. Navigation architecture

- **Root:** `MainContent` with bottom bar (5 items: Home, Future, Spend, Goals, More).
- **Destination state:** `var selectedDestination: AppDestinations`; content via `AnimatedContent(selectedDestination)` or `NavHost` + routes.
- **More:** Either a fifth screen (list: Budget, Moments, Profile) or a modal; from there navigate to BudgetPilot, MoneyMoments, Profile.
- **Sheets / dialogs:** Upload confirm, Add transaction, Add/Edit goal as `ModalBottomSheet` or full-screen composables triggered by state.
- **Deep links:** Define routes for `/home`, `/future`, `/spend`, `/goals`, `/goals/{id}` for future use.

---

## 3. UI layer structure

- **One ViewModel per main screen:** `HomeViewModel`, `FutureViewModel`, `SpendSenseViewModel`, `GoalTrackerViewModel`, etc.
- **State:** `data class XxxUiState` with all fields the UI needs; expose as `StateFlow<XxxUiState>`.
- **Events:** `sealed class XxxEvent` or callback interface (e.g. `onUploadRequested`, `onGoalTapped`). ViewModel updates state and/or navigates.
- **Composables:** Screen composable takes `state: XxxUiState` and `onEvent: (XxxEvent) -> Unit` (or ViewModel). Prefer stateless UI; state from ViewModel.

---

## 4. Design system structure

- **Theme:** `Theme.kt` — `MonytixTheme` with `MaterialTheme` + `MonytixDarkColorScheme`, `Typography`, `MonytixShapes`. Use `PremiumGradientBackground` where needed.
- **Tokens:** `Color.kt`, `Type.kt`, `Shapes.kt` in `design/theme`. Add any missing semantic tokens from MONYTIX_PREMIUM_REDESIGN section W.
- **Components:** Under `design/components/`: buttons (Primary, Secondary), cards (StatCard, ContentCard, InsightCard), empty (EmptyStateTemplate), trust (TrustBanner), loading (Skeleton, Shimmer, MonytixSpinner). Reuse `common/EmptyState.kt` patterns with new copy from redesign doc.

---

## 5. ViewModel / state model approach

- **Load on attach:** `init` or `LaunchedEffect(Unit)` call `loadDashboard()` / `loadFuture()` etc.
- **Refresh:** Expose `refresh()`; call from pull-to-refresh and after upload/add.
- **Caches:** Keep `BudgetUpdateCache`, `GoalUpdateCache`, `PendingUploadHolder` for cross-screen invalidation.
- **Future state:** `FutureUiState(confidenceLabel, projectionPoints, riskStrip, savingsOpportunity, recommendations, isLoading, error)`. Start with mock list; later wire to backend.

---

## 6. Reusable Compose components (inventory)

| Component | Purpose | Location |
|-----------|---------|----------|
| MonytixPrimaryButton | Filled CTA | design/components/buttons |
| MonytixSecondaryButton | Outline | design/components/buttons |
| StatCard | Title, value, delta | design/components/cards |
| InsightCard | Icon, title, body, severity, CTA | design/components/cards |
| EmptyStateTemplate | Icon, title, subtitle, primary CTA | common/EmptyState + design |
| TrustBanner | Icon, headline, body | design/components/trust |
| SkeletonCard / Shimmer | Loading | design/components/loading |
| ForecastChart | Line/area for cash | design/charts |
| RankedBars | Category bars | design/charts or feature-spend |
| HealthCard, RiskCard, CtaCard | Home first fold | feature-home/components |

---

## 7. Screen implementation order (Android)

1. **Design tokens** — Verify Color, Type, Shapes match redesign; add any missing.
2. **Empty state** — One `EmptyStateTemplate` (or extend existing) with premium copy; use in Home, Future, Spend, Goals, Upload.
3. **Trust block** — `TrustBanner` composable; add to login and upload confirm.
4. **Home first fold** — Refactor `HomeScreen` to single scroll: `HealthCard`, `RiskCard`, `CtaCard`; remove or collapse inner tabs to one stream.
5. **Home rest** — `GoalPulseRow`, `ForecastStrip`, list of `InsightCard` (mock), recent transactions.
6. **Future screen** — New `FutureScreen` + `FutureViewModel`; add FUTURE tab; layout: header, confidence, `ForecastChart` (mock data), risk strip, savings, recommendations.
7. **Insight model** — Data class for insight (type, title, body, severity, cta); ranking in VM; use in Home and Spend.
8. **Goal detail** — Add pace, projected date, gap, “AI plan” line, risk state to `GoalDetailScreen`.
9. **Charts** — Implement `ForecastChart` (Compose Canvas or Vico); then reuse/refine ranked bars and heatmap.
10. **Assistant** — Entry (FAB or More); bottom sheet with prompt chips and answer area; mock answers.

---

## 8. Mock data structure

- **Home:** `HealthScore(score, trend)`, `RiskState(label, reason)`, `NextAction(type, label, payload)`, `GoalPulse(count, amountToGo, items)`, `ForecastStrip(text)`, `List<Insight>`, `List<Transaction>`.
- **Future:** `projectionPoints: List<Pair<Long, Double>>`, `riskStrip`, `savingsOpportunity`, `recommendations: List<Recommendation>`.
- **Insight:** `id, type, title, body, severity, ctaLabel, ctaAction`.

Provide static `PreviewData` or mock in ViewModel for demos; swap to `BackendApi` when endpoints exist.

---

## 9. Animation implementation direction

- **Card entrance:** `AnimatedVisibility` with `fadeIn` + `slideInVertically`; use `remember { Animatable(0f) }` and stagger delay (e.g. index * 50L).
- **Chart:** Animate path with `Animatable` or use library (e.g. Vico) animation.
- **Loading:** Replace full-screen spinner with skeleton blocks where possible; use `shimmer` modifier or `Brush` animation.
- **Button:** `Modifier.clickable` with `scale` or `graphicsLayer` on press (0.98f).

---

## 10. Chart integration approach

- **Option A:** Custom Compose `Canvas` for forecast line/area (full control; more code).
- **Option B:** Vico or similar Compose chart library for line/area and bars (faster; align colors with MonytixColors).
- Use same data contract: `List<Pair<timestampOrIndex, value>>` for forecast; list of `CategoryAmount` for ranked bars. Theme colors from `MaterialTheme` or `MonytixColors`.

---

## 11. Theming / token structure

- **Colors:** `Color.kt` — Background, SurfacePrimary, SurfaceSecondary, TextPrimary, TextSecondary, CyanPrimary, CyanSecondary, ErrorRed, SuccessGreen, etc. Map to `MonytixDarkColorScheme` in `Theme.kt`.
- **Typography:** `Type.kt` — Display, Headline, Title, Body, Label; use in `Typography` and in composables via `MaterialTheme.typography`.
- **Spacing:** Use `Dimension` or `Dp` constants (e.g. 16.dp, 24.dp) or a small object `MonytixSpacing`.
- **Shapes:** `MonytixShapes` — small, medium, large radius; use for cards, buttons, chips.

---

*Use this document together with MONYTIX_PREMIUM_REDESIGN.md for implementation. Start with Home first fold and Future screen, then empty states and trust, then insights and goal detail.*
