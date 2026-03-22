//
//  ProfileView.swift
//  ios_monytix
//
//  Profile pillar: 6th tab in main navigation.
//

import SwiftUI

struct ProfileView: View {
    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                VStack(spacing: MonytixSpace.md) {
                    Text("Profile")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(MonytixTheme.text1)
                    Text("Account and settings")
                        .font(.system(size: 14))
                        .foregroundStyle(MonytixTheme.text2)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

#Preview {
    ProfileView()
}
