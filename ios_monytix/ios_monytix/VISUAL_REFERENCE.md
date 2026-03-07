# 🎨 Monytix iOS Visual Reference

## Color Palette

### Primary Colors (Dark Theme)
```
Background Colors:
┌─────────────────────────────────────┐
│ App Background    #070A12  ████████ │  Deep Navy (main)
│ Card Surface      #0D1220  ████████ │  Slightly lighter
│ Elevated Surface  #111A2E  ████████ │  Modals/sheets
│ Stroke/Border     #23304A  ████████ │  Dividers
└─────────────────────────────────────┘

Brand Colors (Cyan Spectrum):
┌─────────────────────────────────────┐
│ Primary Cyan      #00D4FF  ████████ │  Main accent
│ Secondary Cyan    #00A3FF  ████████ │  Gradients
│ Accent Blue       #2B3FFF  ████████ │  Charts
│ Highlight Cyan    #5BE7FF  ████████ │  Hover/glow
└─────────────────────────────────────┘

Text Colors:
┌─────────────────────────────────────┐
│ Primary Text      #EAF0FF  ████████ │  Main text
│ Secondary Text    #9AA7C0  ████████ │  Subtitles
│ Muted Text        #6E7A98  ████████ │  Labels
│ On Accent         #0B1220  ████████ │  Button text
└─────────────────────────────────────┘

Status Colors:
┌─────────────────────────────────────┐
│ Success Green     #2ED573  ████████ │  Positive
│ Warning Orange    #FF9F43  ████████ │  Alerts
│ Error Red         #FF4D4F  ████████ │  Danger
│ Info Blue         #00C2FF  ████████ │  Information
└─────────────────────────────────────┘

Chart Palette:
┌─────────────────────────────────────┐
│ Cyan              #00D4FF  ████████ │
│ Blue              #1E90FF  ████████ │
│ Purple            #6C5CE7  ████████ │
│ Orange            #FF7A00  ████████ │
│ Green             #2ED573  ████████ │
│ Yellow            #F7C948  ████████ │
│ Red               #FF4D4F  ████████ │
└─────────────────────────────────────┘
```

---

## Component Visual Guide

### Buttons

```
Primary Button (Gradient):
┌─────────────────────────────────────┐
│  [Cyan Gradient]                    │
│  ┌───────────────────────────────┐  │
│  │    Add Transaction            │  │
│  │    [Dark text on cyan]        │  │
│  └───────────────────────────────┘  │
│  + Glow shadow underneath           │
└─────────────────────────────────────┘

Secondary Button (Outlined):
┌─────────────────────────────────────┐
│  [Dark background + cyan border]    │
│  ┌───────────────────────────────┐  │
│  │    View Details               │  │
│  │    [Cyan text]                │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Stat Cards

```
┌─────────────────┐  ┌─────────────────┐
│ This Month   📊 │  │ Budget Left  ₹  │
│                 │  │                 │
│ ₹45,230         │  │ ₹12,770         │
│                 │  │                 │
│ ↑ 15% vs last   │  │ ↓ 8% vs last    │
│ [orange]        │  │ [green]         │
└─────────────────┘  └─────────────────┘
```

### Weekday Heatmap

```
┌──────────────────────────────────────┐
│ Spending patterns        Mon → Sun   │
│                                      │
│   M   T   W   T   F   S   S         │
│  ██  ░░  ▓▓  ██  ░░  ███ ▓▓  Week 1│
│  ██  ░░  ▓▓  ░░  ░░  ███ ██  Week 2│
│  ░░  ▓▓  ▓▓  ▓▓  ░░  ███ ▓▓  Week 3│
│  ██  ░░  ██  ▓▓  ░░  ███ ▓▓  Week 4│
│  ▓▓  ░░  ░░  ░░  ░░  ░░  ░░  Week 5│
│                                      │
│ ░░ Low  ▓▓ Medium  ██ High           │
│                                      │
│ Tap a day to see transactions        │
└──────────────────────────────────────┘
```

### Ranked Category Bars

```
┌──────────────────────────────────────┐
│ Top categories          View all →   │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ 🍴 Food & Dining      ₹12,500    │ │
│ │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░          │ │
│ │ 85%      ↑ 15% vs last month     │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ 🚗 Transportation     ₹8,200     │ │
│ │ ▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░         │ │
│ │ 62%      ↓ 8% vs last month      │ │
│ └──────────────────────────────────┘ │
│                                      │
│ [More categories...]                 │
└──────────────────────────────────────┘
```

### Alert Banners

```
Success:
┌──────────────────────────────────────┐
│ ✓ Transaction added successfully     │
│ [Green icon + text]                  │
└──────────────────────────────────────┘

Warning:
┌──────────────────────────────────────┐
│ ⚠ You're close to your budget        │
│ [Orange icon + text]                 │
└──────────────────────────────────────┘

Error:
┌──────────────────────────────────────┐
│ ✕ Failed to sync transactions        │
│ [Red icon + text]                    │
└──────────────────────────────────────┘

Info:
┌──────────────────────────────────────┐
│ ℹ AI detected unusual spending       │
│ [Cyan icon + text]                   │
└──────────────────────────────────────┘
```

### Loading States

```
Ring Loader:
    ◐
   ○  ○    [Spinning cyan gradient ring]
    ○

Inline Loader:
┌──────────────────────────────────────┐
│  ◐  Analyzing transactions...        │
└──────────────────────────────────────┘

Full Screen Overlay:
┌──────────────────────────────────────┐
│                                      │
│                                      │
│          ┌──────────────┐            │
│          │              │            │
│          │      ◐       │            │
│          │              │            │
│          │  Loading...  │            │
│          │              │            │
│          └──────────────┘            │
│                                      │
│                                      │
└──────────────────────────────────────┘

Skeleton Card:
┌──────────────────────────────────────┐
│ ████████ [shimmering gray bars]      │
│ ███████████████████                  │
│ ████████      ████████               │
└──────────────────────────────────────┘
```

### Text Input

```
┌──────────────────────────────────────┐
│ Email                                │
│ ┌──────────────────────────────────┐ │
│ │ 📧  you@example.com              │ │
│ └──────────────────────────────────┘ │
└──────────────────────────────────────┘
```

### Transaction Row

```
┌──────────────────────────────────────┐
│  ⭕  Lunch at Cafe                   │
│  🍴  Food • 3/6/26          -₹1,250  │
│                           [red text] │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│  ⭕  Monthly Salary                  │
│  💵  Income • 3/1/26        +₹50,000 │
│                          [green text]│
└──────────────────────────────────────┘
```

---

## Layout Structure

### Dashboard Screen Layout

```
┌──────────────────────────────────────┐
│ #070A12 Background (Deep Navy)       │
│                                      │
│  Welcome to                          │
│  Monytix [AI] 👤                     │
│  Your AI-powered financial insights  │
│                                      │
│  ┌────────────┐  ┌────────────┐     │
│  │This Month  │  │Budget Left │     │
│  │  ₹45,230   │  │  ₹12,770   │     │
│  │  ↑ 15%     │  │  ↓ 8%      │     │
│  └────────────┘  └────────────┘     │
│                                      │
│  ┌──────────────────────────────┐   │
│  │   Weekday Heatmap            │   │
│  │   [5×7 grid]                 │   │
│  └──────────────────────────────┘   │
│                                      │
│  ┌──────────────────────────────┐   │
│  │   Ranked Category Bars       │   │
│  │   [4-5 category rows]        │   │
│  └──────────────────────────────┘   │
│                                      │
│  ┌──────────────────────────────┐   │
│  │ ℹ AI detected unusual spend  │   │
│  └──────────────────────────────┘   │
│                                      │
│  ┌──────────────────────────────┐   │
│  │   Add Transaction            │   │
│  └──────────────────────────────┘   │
│                                      │
│  ┌──────────────────────────────┐   │
│  │   View All Analytics         │   │
│  └──────────────────────────────┘   │
│                                      │
└──────────────────────────────────────┘
```

---

## Typography Scale

```
Large Heading:    32pt Bold    (Monytix title)
Title:            20pt Bold    (Section headers)
Chart Title:      16pt Semibold
Body:             15pt Regular (Main text)
Button:           16pt Semibold
Chart Value:      22-28pt Bold  (Numbers)
Label:            13pt Medium   (Secondary)
Caption:          12pt Regular  (Hints)
```

---

## Spacing System

```
xs:   6pt   ●
sm:   10pt  ●●
md:   16pt  ●●●      ← Most common (cards, padding)
lg:   24pt  ●●●●     ← Section spacing
xl:   32pt  ●●●●●    ← Large spacing
xxl:  40pt  ●●●●●●
```

---

## Corner Radius System

```
Small:   16pt  ⌜⌝  Small cards
Input:   18pt  ⌜⌝  Text fields
Button:  20pt  ⌜⌝  Action buttons
Card:    24pt  ⌜⌝  Main cards (most common)
Modal:   24pt  ⌜⌝  Bottom sheets
```

---

## Animation Timing

```
Fast:  0.18s  →    Quick interactions
Base:  0.28s  →→   Standard animations
Slow:  0.45s  →→→  Emphasis animations
```

---

## Glow Effects

```
Static Glow:
  Shadow radius: 12pt
  Opacity: 0.3
  Color: Cyan

Pulsing Glow:
  Min radius: 10pt → Max radius: 18pt
  Min opacity: 0.12 → Max opacity: 0.28
  Duration: 1.6s (continuous)
  Scale: 0.98 → 1.02
```

---

## Icon System

All icons use SF Symbols:

```
Financial:
- chart.bar.fill
- indianrupeesign.circle.fill
- banknote.fill

Categories:
- fork.knife (Food)
- car.fill (Transport)
- bag.fill (Shopping)
- tv.fill (Entertainment)
- bolt.fill (Utilities)

Actions:
- plus.circle.fill
- arrow.up.right / arrow.down.right
- checkmark.circle.fill
- xmark.circle.fill

UI:
- person.circle.fill
- magnifyingglass
- slider.horizontal.3
```

---

## Brand Recognition Elements

### 1. AI Badge
```
[Monytix] [AI]
           ↑
        Pulsing cyan glow
        Gradient background
```

### 2. Cyan Gradient
```
#00D4FF → #00A3FF
(Primary → Secondary)
Used on all primary actions
```

### 3. Deep Navy Background
```
#070A12 (NOT pure black!)
Signature premium fintech look
```

### 4. High Contrast Text
```
#EAF0FF on #070A12
WCAG AAA compliant
```

---

## Responsive Behavior

### iPhone SE (Small)
- Single column stat cards
- Compressed spacing (use sm/md)

### iPhone Pro (Standard)
- Two column stat cards
- Standard spacing (use md/lg)

### iPhone Pro Max (Large)
- Two column stat cards
- Generous spacing (use lg/xl)

### iPad
- Three column stat cards
- Maximum width container: 800pt

---

This visual guide should help you understand how all the components come together to create the premium Monytix fintech experience! 🎨
