//
//  MonytixComponents.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

// MARK: - Primary Button Style

struct MonytixPrimaryButton: ButtonStyle {
    let fullWidth: Bool
    
    init(fullWidth: Bool = true) {
        self.fullWidth = fullWidth
    }
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(MonytixTheme.onAccent)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            .background(MonytixTheme.gradientPrimary)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .glowShadow(radius: configuration.isPressed ? 8 : 12, opacity: configuration.isPressed ? 0.2 : 0.3)
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(MonytixMotion.spring, value: configuration.isPressed)
    }
}

// MARK: - Secondary Button Style

struct MonytixSecondaryButton: ButtonStyle {
    let fullWidth: Bool
    
    init(fullWidth: Bool = true) {
        self.fullWidth = fullWidth
    }
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .semibold))
            .foregroundStyle(MonytixTheme.cyan1)
            .frame(maxWidth: fullWidth ? .infinity : nil)
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
            .background(MonytixTheme.surface2)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.buttonRadius)
                    .stroke(MonytixTheme.cyan1.opacity(0.5), lineWidth: 1.5)
            )
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(MonytixMotion.spring, value: configuration.isPressed)
    }
}

// MARK: - Stat Card

struct MonytixStatCard: View {
    let title: String
    let value: String
    let delta: Double?
    let icon: String?
    
    init(title: String, value: String, delta: Double? = nil, icon: String? = nil) {
        self.title = title
        self.value = value
        self.delta = delta
        self.icon = icon
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(MonytixTheme.text2)
                
                Spacer()
                
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(MonytixTheme.cyan1.opacity(0.6))
                }
            }
            
            Text(value)
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            
            if let delta = delta {
                HStack(spacing: 4) {
                    Image(systemName: delta >= 0 ? "arrow.up.right" : "arrow.down.right")
                        .font(.system(size: 10, weight: .bold))
                    Text("\(Int(abs(delta) * 100))% vs last month")
                        .font(.system(size: 11, weight: .medium))
                }
                .foregroundStyle(delta >= 0 ? MonytixTheme.warn : MonytixTheme.success)
            }
        }
        .padding(MonytixSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }
}

// MARK: - Text Input Field

struct MonytixTextField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    let icon: String?
    
    init(_ title: String, text: Binding<String>, placeholder: String = "", icon: String? = nil) {
        self.title = title
        self._text = text
        self.placeholder = placeholder
        self.icon = icon
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
            
            HStack(spacing: 12) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                }
                
                TextField(placeholder, text: $text)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(MonytixTheme.text1)
                    .tint(MonytixTheme.cyan1)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(MonytixTheme.surface2)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.inputRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.inputRadius)
                    .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
            )
        }
    }
}

// MARK: - Alert Banner

struct MonytixAlertBanner: View {
    enum AlertType {
        case success, warning, error, info
        
        var color: Color {
            switch self {
            case .success: return MonytixTheme.success
            case .warning: return MonytixTheme.warn
            case .error: return MonytixTheme.danger
            case .info: return MonytixTheme.info
            }
        }
        
        var icon: String {
            switch self {
            case .success: return "checkmark.circle.fill"
            case .warning: return "exclamationmark.triangle.fill"
            case .error: return "xmark.circle.fill"
            case .info: return "info.circle.fill"
            }
        }
    }
    
    let type: AlertType
    let message: String
    let onDismiss: (() -> Void)?
    
    init(type: AlertType, message: String, onDismiss: (() -> Void)? = nil) {
        self.type = type
        self.message = message
        self.onDismiss = onDismiss
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: type.icon)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(type.color)
            
            Text(message)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(MonytixTheme.text1)
            
            Spacer()
            
            if let onDismiss = onDismiss {
                Button {
                    onDismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(MonytixTheme.text3)
                }
            }
        }
        .padding(16)
        .background(MonytixTheme.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(type.color.opacity(0.3), lineWidth: 1.5)
        )
        .shadow(color: type.color.opacity(0.15), radius: 12)
    }
}

// MARK: - Section Header

struct MonytixSectionHeader: View {
    let title: String
    let action: (() -> Void)?
    let actionLabel: String
    
    init(_ title: String, action: (() -> Void)? = nil, actionLabel: String = "See all") {
        self.title = title
        self.action = action
        self.actionLabel = actionLabel
    }
    
    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(MonytixTheme.text1)
            
            Spacer()
            
            if let action = action {
                Button(action: action) {
                    Text(actionLabel)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(MonytixTheme.cyan1)
                }
            }
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        ScrollView {
            VStack(spacing: 20) {
                MonytixSectionHeader("Components")
                
                // Buttons
                Button("Primary Button") {}
                    .buttonStyle(MonytixPrimaryButton())
                
                Button("Secondary Button") {}
                    .buttonStyle(MonytixSecondaryButton())
                
                // Stat Cards
                HStack(spacing: 12) {
                    MonytixStatCard(
                        title: "Total Spent",
                        value: "₹45,230",
                        delta: 0.15,
                        icon: "chart.bar.fill"
                    )
                    
                    MonytixStatCard(
                        title: "Savings",
                        value: "₹12,500",
                        delta: -0.08,
                        icon: "banknote.fill"
                    )
                }
                
                // Text field
                MonytixTextField(
                    "Email",
                    text: .constant(""),
                    placeholder: "you@example.com",
                    icon: "envelope.fill"
                )
                
                // Alerts
                MonytixAlertBanner(type: .success, message: "Transaction added successfully")
                MonytixAlertBanner(type: .warning, message: "You're close to your monthly budget")
                MonytixAlertBanner(type: .error, message: "Failed to sync transactions")
                MonytixAlertBanner(type: .info, message: "New AI insights available")
            }
            .padding()
        }
    }
}
