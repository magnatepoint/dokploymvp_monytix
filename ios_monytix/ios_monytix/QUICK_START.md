# Monytix iOS Theme - Quick Start Guide

## 🚀 Setup Instructions

### Step 1: Add Files to Your Xcode Project

Add all the following files to your Xcode project:

**Core Theme Files:**
1. `MonytixTheme.swift` - Core color, spacing, and shape tokens
2. `MonytixChartTokens.swift` - Chart-specific tokens
3. `MonytixMotion.swift` - Animation system

**Component Files:**
4. `MonytixComponents.swift` - UI components
5. `MonytixRingLoader.swift` - Loading states
6. `MonytixUtilities.swift` - Helpers and utilities

**Chart Components:**
7. `WeekdayHeatmap.swift` - Spending pattern heatmap
8. `RankedBars.swift` - Category rankings

### Step 2: Update Your App Entry Point

Replace your app file content with:

```swift
import SwiftUI

@main
struct ios_monytixApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)  // Essential for Monytix theme
        }
    }
}
```

### Step 3: Use the Updated ContentView

The `ContentView.swift` file now includes a complete demo of:
- Header with AI badge
- Stat cards with deltas
- Weekday heatmap
- Ranked category bars
- Alert banners
- Action buttons

Run the app to see the full design system in action!

---

## 🎨 Key Design Principles

### 1. Always Use Deep Navy Background

```swift
ZStack {
    MonytixTheme.bg.ignoresSafeArea()  // NOT Color.black!
    
    // Your content here
}
```

**Why?** Deep navy (#070A12) feels premium and modern. Pure black (#000000) looks harsh and dated.

### 2. Use Cyan for Primary Actions

```swift
Button("Add Transaction") {}
    .buttonStyle(MonytixPrimaryButton())  // Cyan gradient
```

### 3. Add Entrance Animations

```swift
VStack {
    Header().enter(delay: 0.1)
    StatsRow().enter(delay: 0.2)
    ChartView().enter(delay: 0.3)
}
```

This creates a polished, staggered appearance animation.

### 4. Apply Glow to AI Features

```swift
HStack {
    Text("Monytix")
    Text("AI")
        .glowPulse()  // Pulsing cyan glow
}
```

---

## 📊 Building Your First Screen

### Dashboard Example

```swift
import SwiftUI

struct DashboardView: View {
    var body: some View {
        ZStack {
            MonytixTheme.bg.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: MonytixSpace.lg) {
                    // Header
                    MonytixSectionHeader("Dashboard")
                        .padding(.horizontal, MonytixSpace.md)
                        .enter(delay: 0.1)
                    
                    // Stats
                    statsGrid
                        .padding(.horizontal, MonytixSpace.md)
                        .enter(delay: 0.2)
                    
                    // Charts
                    WeekdayHeatmap(weeks: sampleData, onTap: nil)
                        .padding(.horizontal, MonytixSpace.md)
                        .enter(delay: 0.3)
                }
                .padding(.vertical, MonytixSpace.md)
            }
        }
    }
    
    private var statsGrid: some View {
        HStack(spacing: 12) {
            MonytixStatCard(
                title: "This Month",
                value: "₹45,230",
                delta: 0.15,
                icon: "chart.bar.fill"
            )
            
            MonytixStatCard(
                title: "Budget Left",
                value: "₹12,770",
                delta: -0.08,
                icon: "indianrupeesign.circle.fill"
            )
        }
    }
}
```

---

## 🧩 Common Patterns

### Loading State

```swift
struct ContentView: View {
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            MonytixTheme.bg.ignoresSafeArea()
            
            if isLoading {
                MonytixLoadingOverlay(message: "Loading...")
            } else {
                // Your content
            }
        }
    }
}
```

### Form with Validation

```swift
struct AddTransactionView: View {
    @State private var amount = ""
    @State private var description = ""
    @State private var showError = false
    
    var body: some View {
        ZStack {
            MonytixTheme.bg.ignoresSafeArea()
            
            VStack(spacing: MonytixSpace.lg) {
                MonytixTextField(
                    "Amount",
                    text: $amount,
                    placeholder: "0.00",
                    icon: "indianrupeesign"
                )
                
                MonytixTextField(
                    "Description",
                    text: $description,
                    placeholder: "What was this for?",
                    icon: "text.alignleft"
                )
                
                if showError {
                    MonytixAlertBanner(
                        type: .error,
                        message: "Please fill all fields",
                        onDismiss: { showError = false }
                    )
                }
                
                Button("Add Transaction") {
                    if amount.isEmpty || description.isEmpty {
                        showError = true
                        HapticManager.error()
                    } else {
                        // Process transaction
                        HapticManager.success()
                    }
                }
                .buttonStyle(MonytixPrimaryButton())
            }
            .padding(MonytixSpace.md)
        }
    }
}
```

### Transaction List

```swift
struct TransactionListView: View {
    let transactions: [Transaction]
    
    var body: some View {
        ZStack {
            MonytixTheme.bg.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 12) {
                    MonytixSectionHeader("Recent Transactions")
                        .padding(.horizontal, MonytixSpace.md)
                    
                    if transactions.isEmpty {
                        EmptyStateView(
                            icon: "doc.text.magnifyingglass",
                            title: "No transactions",
                            subtitle: "Add your first transaction to get started",
                            actionTitle: "Add Transaction",
                            action: { /* Add action */ }
                        )
                    } else {
                        ForEach(Array(transactions.enumerated()), id: \.element.id) { index, transaction in
                            TransactionRow(transaction: transaction)
                                .enter(delay: Double(index) * 0.05)
                                .padding(.horizontal, MonytixSpace.md)
                        }
                    }
                }
                .padding(.vertical, MonytixSpace.md)
            }
        }
    }
}
```

---

## 🎯 Component Cheat Sheet

### Buttons
```swift
.buttonStyle(MonytixPrimaryButton())    // Gradient cyan button
.buttonStyle(MonytixSecondaryButton())  // Outlined button
```

### Cards
```swift
MonytixStatCard(title:value:delta:icon:)  // Stat card with change indicator
```

### Input
```swift
MonytixTextField(title, text: $binding, placeholder:, icon:)
```

### Alerts
```swift
MonytixAlertBanner(type: .success/.warning/.error/.info, message:, onDismiss:)
```

### Charts
```swift
WeekdayHeatmap(weeks:, onTap:)  // 5×7 heatmap
RankedBars(rows:, onTap:)       // Category rankings
```

### Loading
```swift
MonytixRingLoader(size:, lineWidth:)                // Spinner
MonytixInlineLoader(message:)                       // Inline with text
.loadingOverlay(isLoading:, message:)               // Full overlay
SkeletonCard()                                      // Skeleton loading
```

### Animations
```swift
.enter(delay:)           // Entrance animation
.glowPulse()            // Pulsing glow
.glowShadow()           // Static glow
.pressScale()           // Press feedback
.shimmer()              // Loading shimmer
.hapticFeedback()       // Haptic on tap
```

---

## 🎨 Color Quick Reference

```swift
// Backgrounds
MonytixTheme.bg        // Main background
MonytixTheme.surface   // Cards
MonytixTheme.surface2  // Elevated
MonytixTheme.stroke    // Borders

// Text
MonytixTheme.text1     // Primary
MonytixTheme.text2     // Secondary
MonytixTheme.text3     // Muted

// Brand
MonytixTheme.cyan1     // Primary cyan
MonytixTheme.cyan2     // Secondary cyan

// Status
MonytixTheme.success   // Green
MonytixTheme.warn      // Orange
MonytixTheme.danger    // Red
MonytixTheme.info      // Cyan
```

---

## 📐 Spacing Quick Reference

```swift
MonytixSpace.xs   // 6pt  - tight spacing
MonytixSpace.sm   // 10pt - small gaps
MonytixSpace.md   // 16pt - default padding (most common)
MonytixSpace.lg   // 24pt - section spacing
MonytixSpace.xl   // 32pt - large spacing
```

---

## ✅ Pre-Launch Checklist

- [ ] All files added to Xcode project
- [ ] App entry point updated with `.preferredColorScheme(.dark)`
- [ ] Using `MonytixTheme.bg` (not `Color.black`)
- [ ] Primary buttons use cyan gradient
- [ ] Entrance animations on main screens
- [ ] Loading states for async operations
- [ ] Haptic feedback on important actions
- [ ] Empty states for lists
- [ ] Error handling with alert banners

---

## 🆘 Troubleshooting

### Colors look wrong
- Ensure `.preferredColorScheme(.dark)` is set in your App file
- Verify you're using `MonytixTheme.bg` not `Color.black`

### Animations not working
- Check that views are wrapped in proper parent containers
- Ensure `.onAppear` is being triggered

### Gradients not showing
- Make sure you're using `MonytixTheme.gradientPrimary` not individual colors
- Apply to `.background()` or `.fill()` modifiers

### Charts not displaying
- Verify your data format matches expected structure
- Check that arrays have correct dimensions (5×7 for heatmap)

---

## 📚 Further Reading

- See `DESIGN_SYSTEM.md` for complete documentation
- Check component preview blocks for usage examples
- Review `ContentView.swift` for full integration example

---

**You're ready to build a premium fintech app! 🚀**
