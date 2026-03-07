//
//  MonytixUtilities.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

// MARK: - Currency Formatter

struct CurrencyFormatter {
    static func formatINR(_ value: Double, showDecimals: Bool = false) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "INR"
        formatter.maximumFractionDigits = showDecimals ? 2 : 0
        return formatter.string(from: NSNumber(value: value)) ?? "₹0"
    }
    
    static func formatCompact(_ value: Double) -> String {
        let absValue = abs(value)
        let sign = value < 0 ? "-" : ""
        
        switch absValue {
        case 0..<1_000:
            return "\(sign)₹\(Int(absValue))"
        case 1_000..<100_000:
            return "\(sign)₹\(String(format: "%.1fK", absValue / 1_000))"
        case 100_000..<10_000_000:
            return "\(sign)₹\(String(format: "%.1fL", absValue / 100_000))"
        default:
            return "\(sign)₹\(String(format: "%.2fCr", absValue / 10_000_000))"
        }
    }
}

// MARK: - Date Utilities

extension Date {
    var weekdayIndex: Int {
        Calendar.current.component(.weekday, from: self) - 1
    }
    
    var weekOfMonth: Int {
        Calendar.current.component(.weekOfMonth, from: self)
    }
    
    func formattedMonthYear() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: self)
    }
    
    func formattedShort() -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        return formatter.string(from: self)
    }
}

// MARK: - Haptic Feedback

struct HapticManager {
    static func light() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.impactOccurred()
    }
    
    static func medium() {
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
    }
    
    static func heavy() {
        let generator = UIImpactFeedbackGenerator(style: .heavy)
        generator.impactOccurred()
    }
    
    static func success() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.success)
    }
    
    static func warning() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.warning)
    }
    
    static func error() {
        let generator = UINotificationFeedbackGenerator()
        generator.notificationOccurred(.error)
    }
}

// MARK: - View Extensions

extension View {
    /// Adds haptic feedback to button tap
    func hapticFeedback(style: UIImpactFeedbackGenerator.FeedbackStyle = .light) -> some View {
        self.simultaneousGesture(
            TapGesture().onEnded {
                let generator = UIImpactFeedbackGenerator(style: style)
                generator.impactOccurred()
            }
        )
    }
    
    /// Conditional modifier
    @ViewBuilder
    func `if`<Transform: View>(_ condition: Bool, transform: (Self) -> Transform) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

// MARK: - Transaction Model (Example)

struct Transaction: Identifiable, Hashable {
    let id = UUID()
    let date: Date
    let amount: Double
    let category: String
    let description: String
    let type: TransactionType
    let icon: String?
    
    enum TransactionType {
        case expense
        case income
        
        var color: Color {
            switch self {
            case .expense: return MonytixTheme.danger
            case .income: return MonytixTheme.success
            }
        }
        
        var symbol: String {
            switch self {
            case .expense: return "-"
            case .income: return "+"
            }
        }
    }
    
    init(date: Date, amount: Double, category: String, description: String, type: TransactionType, icon: String? = nil) {
        self.date = date
        self.amount = amount
        self.category = category
        self.description = description
        self.type = type
        self.icon = icon
    }
}

// MARK: - Transaction Row Component

struct TransactionRow: View {
    let transaction: Transaction
    
    var body: some View {
        HStack(spacing: 12) {
            // Icon
            ZStack {
                Circle()
                    .fill(transaction.type.color.opacity(0.15))
                    .frame(width: 44, height: 44)
                
                Image(systemName: transaction.icon ?? "arrow.up.arrow.down")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(transaction.type.color)
            }
            
            // Details
            VStack(alignment: .leading, spacing: 4) {
                Text(transaction.description)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(MonytixTheme.text1)
                
                HStack(spacing: 6) {
                    Text(transaction.category)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(MonytixTheme.text3)
                    
                    Text("•")
                        .foregroundStyle(MonytixTheme.text3)
                    
                    Text(transaction.date.formattedShort())
                        .font(.system(size: 13, weight: .regular))
                        .foregroundStyle(MonytixTheme.text3)
                }
            }
            
            Spacer()
            
            // Amount
            Text("\(transaction.type.symbol)\(CurrencyFormatter.formatINR(transaction.amount))")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(transaction.type.color)
        }
        .padding(12)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(MonytixTheme.stroke.opacity(0.5), lineWidth: 1)
        )
    }
}

// MARK: - Category Badge

struct CategoryBadge: View {
    let name: String
    let color: Color
    let icon: String?
    
    init(name: String, color: Color = MonytixTheme.cyan1, icon: String? = nil) {
        self.name = name
        self.color = color
        self.icon = icon
    }
    
    var body: some View {
        HStack(spacing: 6) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: 12, weight: .semibold))
            }
            
            Text(name)
                .font(.system(size: 12, weight: .medium))
        }
        .foregroundStyle(color)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(color.opacity(0.15))
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(color.opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Empty State View

struct EmptyStateView: View {
    let icon: String
    let title: String
    let subtitle: String
    let actionTitle: String?
    let action: (() -> Void)?
    
    init(
        icon: String,
        title: String,
        subtitle: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.title = title
        self.subtitle = subtitle
        self.actionTitle = actionTitle
        self.action = action
    }
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: icon)
                .font(.system(size: 64, weight: .light))
                .foregroundStyle(MonytixTheme.text3.opacity(0.5))
            
            VStack(spacing: 8) {
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(MonytixTheme.text1)
                
                Text(subtitle)
                    .font(.system(size: 14, weight: .regular))
                    .foregroundStyle(MonytixTheme.text3)
                    .multilineTextAlignment(.center)
            }
            
            if let actionTitle = actionTitle, let action = action {
                Button(actionTitle, action: action)
                    .buttonStyle(MonytixPrimaryButton(fullWidth: false))
            }
        }
        .padding(MonytixSpace.xl)
    }
}

// MARK: - Preview

#Preview("Transaction Row") {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        VStack(spacing: 12) {
            TransactionRow(
                transaction: Transaction(
                    date: Date(),
                    amount: 1250,
                    category: "Food",
                    description: "Lunch at Cafe",
                    type: .expense,
                    icon: "fork.knife"
                )
            )
            
            TransactionRow(
                transaction: Transaction(
                    date: Date(),
                    amount: 5000,
                    category: "Salary",
                    description: "Monthly Salary",
                    type: .income,
                    icon: "banknote"
                )
            )
        }
        .padding()
    }
}

#Preview("Empty State") {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        EmptyStateView(
            icon: "chart.bar.xaxis",
            title: "No transactions yet",
            subtitle: "Start adding transactions to see your spending patterns",
            actionTitle: "Add Transaction",
            action: {}
        )
    }
}
