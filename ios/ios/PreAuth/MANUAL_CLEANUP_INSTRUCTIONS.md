# 🔴 CRITICAL: Manual File Cleanup Required

## Errors to Fix

### 1. Multiple Commands Produce Error (GlassCardView.stringsdata)
This means `GlassCardView.swift` has been added to the build target multiple times.

### 2. Ambiguous Initializer (DeviceVerificationView)
You have TWO files with identical struct definitions.

---

## 🛠️ STEP-BY-STEP FIX

### Step 1: Delete Duplicate DeviceVerificationView
**In Xcode:**

1. Open the Project Navigator (Cmd+1)
2. Find **`DeviceVerificationView 2.swift`**
3. Right-click on it
4. Select **"Delete"** → **"Move to Trash"**
5. ✅ Confirm deletion

**Keep:** `DeviceVerificationView.swift` (without the "2")  
**Delete:** `DeviceVerificationView 2.swift`

---

### Step 2: Fix GlassCardView Duplicate Build Phase

**Option A: Check if file exists multiple times**
1. In Project Navigator, search for "GlassCardView"
2. If you see TWO `GlassCardView.swift` files, delete one
3. If you see only ONE file, proceed to Option B

**Option B: Remove from build phases and re-add**
1. Select your project in Project Navigator (the blue icon at top)
2. Select your app target (usually named "ios")
3. Go to **"Build Phases"** tab
4. Expand **"Compile Sources"**
5. Find **`GlassCardView.swift`** in the list
6. If it appears **TWICE**, select one and click the **"-"** button to remove it
7. If it appears only once, continue to Step 3

---

### Step 3: Clean Build Folder
**In Xcode menu:**
1. **Product** → **Clean Build Folder** (Cmd+Shift+K)
2. Wait for it to complete

---

### Step 4: Delete Derived Data
**In Xcode menu:**
1. **Xcode** → **Settings** (or **Preferences**)
2. Go to **"Locations"** tab
3. Click the arrow next to "Derived Data" path
4. Find the **`ios-dzzwkqwdbyejmfgjoqzhgfaztzjw`** folder
5. **Move it to Trash**
6. Close the Finder window

**OR use Terminal:**
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData/ios-*
```

---

### Step 5: Rebuild
1. **Product** → **Build** (Cmd+B)
2. ✅ Check that errors are gone

---

## 🔍 If GlassCardView.swift Doesn't Exist Yet

If you can't find `GlassCardView.swift` in your project:

1. In Xcode, go to **File** → **New** → **File**
2. Choose **Swift File**
3. Name it **`GlassCardView.swift`**
4. Make sure your app target is checked
5. Click **Create**
6. Copy this code into it:

```swift
//
//  GlassCardView.swift
//  ios
//
//  Glass morphism card component
//

import SwiftUI

/// A card view with glass morphism effect
struct GlassCardView<Content: View>: View {
    let content: () -> Content
    
    init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }
    
    var body: some View {
        content()
            .padding(MonytixLayout.cardPadding)
            .background(MonytixColors.glassCard)
            .clipShape(RoundedRectangle(cornerRadius: MonytixLayout.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixLayout.cardRadius)
                    .stroke(MonytixColors.border, lineWidth: MonytixLayout.borderThin)
            )
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        GlassCardView {
            VStack(alignment: .leading, spacing: 8) {
                Text("Card Title")
                    .font(MonytixTypography.titleMedium)
                    .foregroundColor(.white)
                Text("Card content goes here")
                    .font(MonytixTypography.bodyMedium)
                    .foregroundColor(.white.opacity(0.7))
            }
        }
        .padding()
    }
}
```

7. Save the file
8. Clean and rebuild

---

## ✅ Expected Result

After completing these steps, you should have:
- ❌ **0 duplicate file errors**
- ❌ **0 ambiguous initializer errors**
- ✅ **Clean build** with only optional warnings (app icon, .md files)

---

## 📱 Quick Checklist

- [ ] Delete `DeviceVerificationView 2.swift`
- [ ] Check for duplicate GlassCardView in Build Phases
- [ ] Clean Build Folder (Cmd+Shift+K)
- [ ] Delete Derived Data
- [ ] Rebuild (Cmd+B)
- [ ] Verify no errors remain

---

## 🆘 If Problems Persist

If you still see errors after these steps:

1. **Restart Xcode** completely
2. **Clean again** (Cmd+Shift+K)
3. **Build again** (Cmd+B)
4. Check the error message carefully - it might be pointing to a different issue

Most Xcode build cache issues are resolved by deleting Derived Data! 🎯
