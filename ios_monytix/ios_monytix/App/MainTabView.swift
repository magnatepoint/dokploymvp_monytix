//
//  MainTabView.swift
//  ios_monytix
//
//  Bottom tab navigation: Home (MolyConsole) and SpendSense.
//

import SwiftUI

enum MainTab: Int, CaseIterable {
    case home = 0
    case future = 1
    case spendSense = 2
    case goals = 3
    case budget = 4
    case moneyMoments = 5

    var title: String {
        switch self {
        case .home: return "Home"
        case .future: return "Future"
        case .spendSense: return "SpendSense"
        case .goals: return "Goals"
        case .budget: return "Budget"
        case .moneyMoments: return "Moments"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .future: return "chart.line.uptrend.xyaxis"
        case .spendSense: return "indianrupeesign.circle.fill"
        case .goals: return "target"
        case .budget: return "chart.pie.fill"
        case .moneyMoments: return "sparkles"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: MainTab = .home
    @State private var requestUploadOnHome = false

    var body: some View {
        TabView(selection: $selectedTab) {
            MolyConsoleView(
                onNavigateToFuture: { selectedTab = .future },
                onNavigateToGoals: { selectedTab = .goals },
                onNavigateToSpendSense: { selectedTab = .spendSense },
                requestUploadOnHome: $requestUploadOnHome
            )
                .tabItem {
                    Label(MainTab.home.title, systemImage: MainTab.home.icon)
                }
                .tag(MainTab.home)

            FutureView(onUploadStatement: {
                selectedTab = .home
                requestUploadOnHome = true
            })
                .tabItem {
                    Label(MainTab.future.title, systemImage: MainTab.future.icon)
                }
                .tag(MainTab.future)

            SpendSenseView()
                .tabItem {
                    Label(MainTab.spendSense.title, systemImage: MainTab.spendSense.icon)
                }
                .tag(MainTab.spendSense)

            GoalTrackerView()
                .tabItem {
                    Label(MainTab.goals.title, systemImage: MainTab.goals.icon)
                }
                .tag(MainTab.goals)

            BudgetPilotView(onOpenSpendSense: {
                selectedTab = .spendSense
            })
                .tabItem {
                    Label(MainTab.budget.title, systemImage: MainTab.budget.icon)
                }
                .tag(MainTab.budget)

            MoneyMomentsView()
                .tabItem {
                    Label(MainTab.moneyMoments.title, systemImage: MainTab.moneyMoments.icon)
                }
                .tag(MainTab.moneyMoments)
        }
        .tint(MonytixTheme.cyan1)
        .onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            // Surface #0D1220
            appearance.backgroundColor = UIColor(red: 13/255, green: 18/255, blue: 32/255, alpha: 1)
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}

#Preview {
    MainTabView()
}
