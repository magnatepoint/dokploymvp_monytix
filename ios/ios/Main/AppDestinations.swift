//
//  AppDestinations.swift
//  ios
//
//  Tab destinations - matches APK AppDestinations
//

import SwiftUI

enum AppDestinations: String, CaseIterable {
    case home = "Home"
    case spend = "Spend"
    case goals = "Goals"
    case budget = "Budget"
    case moments = "Moments"
    case profile = "Profile"

    var label: String {
        switch self {
        case .home: return "Home"
        case .spend: return "Spend"
        case .goals: return "Goals"
        case .budget: return "Budget"
        case .moments: return "Moments"
        case .profile: return "Profile"
        }
    }

    var fullName: String {
        switch self {
        case .home: return "MolyConsole"
        case .spend: return "SpendSense"
        case .goals: return "GoalTracker"
        case .budget: return "BudgetPilot"
        case .moments: return "MoneyMoments"
        case .profile: return "Profile"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .spend: return "indianrupeesign"
        case .goals: return "flag.fill"
        case .budget: return "chart.pie.fill"
        case .moments: return "heart.fill"
        case .profile: return "person.crop.circle.fill"
        }
    }
}
