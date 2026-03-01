//
//  FormattingHelpers.swift
//  ios
//
//  Formatting utilities - matches APK formatCurrency
//

import Foundation

func formatCurrency(_ amount: Double) -> String {
    let absAmount = abs(amount)
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.groupingSeparator = ","
    formatter.usesGroupingSeparator = true
    formatter.maximumFractionDigits = 0
    formatter.minimumFractionDigits = 0
    let formatted = formatter.string(from: NSNumber(value: absAmount)) ?? "\(Int(absAmount))"
    return amount < 0 ? "-₹\(formatted)" : "₹\(formatted)"
}

func formatMonthLabel(_ yearMonth: String) -> String {
    guard yearMonth.count >= 7 else { return yearMonth }
    let parts = yearMonth.split(separator: "-")
    guard parts.count >= 2, let year = Int(parts[0]), let month = Int(parts[1]) else { return yearMonth }
    let formatter = DateFormatter()
    formatter.dateFormat = "MMMM"
    var components = DateComponents()
    components.year = year
    components.month = month
    components.day = 1
    if let date = Calendar.current.date(from: components) {
        return formatter.string(from: date) + " \(year)"
    }
    return yearMonth
}
