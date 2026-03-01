//
//  MainContentView.swift
//  ios
//
//  Main tab navigation - matches APK MainContent
//

import SwiftUI

struct MainContentView: View {
    @Environment(AuthViewModel.self) private var authViewModel
    @State private var currentDestination: AppDestinations = .home

    var body: some View {
        PremiumGradientBackground {
            TabView(selection: $currentDestination) {
                HomeView()
                    .tabItem {
                        Label(AppDestinations.home.label, systemImage: AppDestinations.home.icon)
                    }
                    .tag(AppDestinations.home)

                SpendSenseView()
                    .tabItem {
                        Label(AppDestinations.spend.label, systemImage: AppDestinations.spend.icon)
                    }
                    .tag(AppDestinations.spend)

                GoalTrackerView()
                    .tabItem {
                        Label(AppDestinations.goals.label, systemImage: AppDestinations.goals.icon)
                    }
                    .tag(AppDestinations.goals)

                BudgetPilotView()
                    .tabItem {
                        Label(AppDestinations.budget.label, systemImage: AppDestinations.budget.icon)
                    }
                    .tag(AppDestinations.budget)

                MoneyMomentsView()
                    .tabItem {
                        Label(AppDestinations.moments.label, systemImage: AppDestinations.moments.icon)
                    }
                    .tag(AppDestinations.moments)

                ProfileView()
                    .tabItem {
                        Label(AppDestinations.profile.label, systemImage: AppDestinations.profile.icon)
                    }
                    .tag(AppDestinations.profile)
            }
            .tint(MonytixColors.accentPrimary)
        }
    }
}

#Preview {
    MainContentView()
        .environment(AuthViewModel())
}
