//
//  MainTabView.swift
//  ios_monytix
//
//  Bottom tab navigation: Home, Memory, State, Foresight, Guidance, Profile (6 pillars).
//

import SwiftUI

enum MainTab: Int, CaseIterable {
    case home = 0
    case memory = 1
    case state = 2
    case foresight = 3
    case guidance = 4
    case profile = 5

    var title: String {
        switch self {
        case .home: return "Home"
        case .memory: return "Memory"
        case .state: return "State"
        case .foresight: return "Foresight"
        case .guidance: return "Guidance"
        case .profile: return "Profile"
        }
    }

    var icon: String {
        switch self {
        case .home: return "house.fill"
        case .memory: return "clock.arrow.circlepath"
        case .state: return "chart.pie.fill"
        case .foresight: return "eye.fill"
        case .guidance: return "lightbulb.fill"
        case .profile: return "person.crop.circle.fill"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: MainTab = .home
    @State private var requestUploadOnHome = false

    var body: some View {
        TabView(selection: $selectedTab) {
            MolyConsoleView(
                onNavigateToFuture: { selectedTab = .foresight },
                onNavigateToGoals: { selectedTab = .state },
                onNavigateToSpendSense: { selectedTab = .memory },
                requestUploadOnHome: $requestUploadOnHome
            )
                .tabItem {
                    Label(MainTab.home.title, systemImage: MainTab.home.icon)
                }
                .tag(MainTab.home)

            SpendSenseView()
                .tabItem {
                    Label(MainTab.memory.title, systemImage: MainTab.memory.icon)
                }
                .tag(MainTab.memory)

            StateView(onNavigateToMemory: { selectedTab = .memory })
                .tabItem {
                    Label(MainTab.state.title, systemImage: MainTab.state.icon)
                }
                .tag(MainTab.state)

            ForesightView(onUploadStatementFromFuture: {
                selectedTab = .home
                requestUploadOnHome = true
            })
                .tabItem {
                    Label(MainTab.foresight.title, systemImage: MainTab.foresight.icon)
                }
                .tag(MainTab.foresight)

            GuidanceView()
                .tabItem {
                    Label(MainTab.guidance.title, systemImage: MainTab.guidance.icon)
                }
                .tag(MainTab.guidance)

            ProfileView()
                .tabItem {
                    Label(MainTab.profile.title, systemImage: MainTab.profile.icon)
                }
                .tag(MainTab.profile)
        }
        .tint(MonytixTheme.cyan1)
        .onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithOpaqueBackground()
            appearance.backgroundColor = UIColor(red: 13/255, green: 18/255, blue: 32/255, alpha: 1)
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
}

#Preview {
    MainTabView()
}
