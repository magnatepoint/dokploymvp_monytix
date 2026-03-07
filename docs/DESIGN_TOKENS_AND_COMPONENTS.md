# MONYTIX Design Tokens & Reusable Component Inventory

Single source of truth for visual and motion tokens and for reusable UI components on **Android** and **iOS**. Implementation should match these so both apps feel one product.

---

## 1. Color tokens

| Role | Hex | Android (Color.kt) | iOS (MonytixTheme) |
|------|-----|--------------------|--------------------|
| Background | #070A12 | Background | bg |
| Surface primary | #0D1220 | SurfacePrimary | surface |
| Surface secondary | #111A2E | SurfaceSecondary | surface2 |
| Stroke / border | #23304A | BorderSubtle | stroke |
| Text primary | #EAF0FF | TextPrimary | text1 |
| Text secondary | #9AA7C0 | TextSecondary | text2 |
| Text tertiary | #6E7A98 | — | text3 |
| Brand primary | #00D4FF | CyanPrimary | cyan1 |
| Brand secondary | #00A3FF | CyanSecondary | cyan2 |
| On accent | #0B1220 | onPrimary | onAccent |
| Success | #2ED573 | SuccessGreen | success |
| Warning | #FF9F43 | — | warn |
| Danger / Error | #FF4D4F | ErrorRed | danger |
| Info | #00C2FF | — | info |
| Glow (primary) | #00D4FF @ 18% | CyanGlow | cyanGlow |

**Chart palette (order for series):** Cyan, Blue, Purple, Orange, Green, Yellow, Red. Map to existing Chart.* / MonytixTheme.Chart.*

---

## 2. Typography scale

| Style | Use | Android (Type.kt) | iOS |
|-------|-----|-------------------|-----|
| Display | Hero numbers | displayLarge 57sp / displaySmall 36sp | .system(size: 34, weight: .bold) |
| Headline | Section titles | headlineMedium 28sp | .system(size: 22, weight: .semibold) |
| Title | Card titles | titleLarge 22sp | .system(size: 17, weight: .semibold) |
| Body | Body copy | bodyLarge 16sp | .system(size: 15) |
| Callout | Secondary copy | bodyMedium 14sp | .system(size: 13) |
| Caption | Metadata, labels | labelSmall 11sp | .system(size: 11) |

---

## 3. Spacing scale

| Token | Android (dp) | iOS (pt) |
|-------|--------------|----------|
| xs | 4–6 | 6 |
| sm | 8–10 | 10 |
| md | 16 | 16 |
| lg | 24 | 24 |
| xl | 32 | 32 |
| xxl | 40 | 40 |

Section spacing: lg–xl. In-card padding: md.

---

## 4. Corner radius

| Token | Android (dp) | iOS (pt) |
|-------|--------------|----------|
| small | 8 | 8 |
| medium | 12–16 | 16 |
| card | 16–24 | 24 |
| button | 12–20 | 20 |
| modal | 24 | 24 |

---

## 5. Motion

| Use | Duration | Curve |
|-----|----------|--------|
| Fast (press, toggle) | 200 ms | easeOut |
| Base (card, sheet) | 300 ms | easeOut |
| Chart draw, score | 400–500 ms | easeOut |
| Stagger offset | 50 ms | — |

---

## 6. Reusable component inventory

### Buttons

- **Primary:** Filled with brand primary; on-accent text; card radius.
- **Secondary:** Outline stroke primary; primary text.
- **Text:** No fill; primary or text secondary.

### Cards

- **StatCard:** Title (caption), value (display/headline), optional delta (green/red), optional icon.
- **ContentCard:** Title, body, optional CTA button; surface background, stroke optional.
- **InsightCard:** Icon, title, body, severity (badge or border), primary CTA.

### Empty state

- **Layout:** Optional icon/illustration, title (headline), subtitle (body), primary button, optional secondary. Centered or leading-aligned per screen.

### Trust block

- **Layout:** Icon (e.g. lock/shield), headline, 1–2 sentence body, optional “Learn more” link. Used on login, upload confirm, onboarding.

### Loading

- **Skeleton:** Rectangles with shimmer (same color as surface, animated gradient or opacity).
- **Spinner:** Existing MonytixRing / MonytixRingLoader.
- **Inline:** Small spinner next to content.

### Charts

- **ForecastChart:** Line or area; X = time, Y = balance; optional “risk zone” below threshold; theme colors; animate path on appear.
- **RankedBars:** Horizontal bars; label, amount, optional delta; category color from palette.
- **WeekdayHeatmap:** Grid (e.g. week × day); intensity color; tap optional.

### Home-specific (can live in feature)

- **HealthCard:** Score (0–100), optional trend, short subtext.
- **RiskCard:** Label (“On track” / “Low cash in 12 days”), short reason.
- **CtaCard:** Single primary CTA text + button; optional secondary.
- **GoalPulseRow:** One row: “N goals · ₹X to go” + mini progress indicators.
- **ForecastStrip:** One line of text + optional “See Future” link.

### Future-specific

- **ForecastChartSection:** Wraps ForecastChart + title + confidence line.
- **RiskStripView:** One line with severity color.
- **RecommendationCard:** Title, body, optional CTA.

---

## 7. State models (shared concept)

- **HealthState:** score (Int), trend (up/down/neutral), subtext (String).
- **RiskState:** label (String), reason (String), severity (low/medium/high).
- **NextAction:** type (enum), label (String), payload (optional).
- **Insight:** id, type, title, body, severity, ctaLabel, ctaAction.
- **Recommendation:** id, title, body, optional ctaLabel, ctaAction.
- **GoalPulseItem:** goalId, name, progress (0–1), amountRemaining (optional).

Use these (or equivalent) on both platforms so backend can drive one contract later.

---

*Reference this doc when implementing or extending design/theme and when adding new shared components on Android or iOS.*
