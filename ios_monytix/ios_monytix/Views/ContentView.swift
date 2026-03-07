//
//  ContentView.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

struct ContentView: View {
    @State private var isLoading = false
    
    var body: some View {
        ZStack {
            // Deep navy background (premium fintech feel)
            MonytixTheme.bg.ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: MonytixSpace.lg) {
                    // Header
                    headerView
                        .enter(delay: 0.1)
                    
                    // Stats Cards
                    statsGrid
                        .enter(delay: 0.2)
                    
                    // Spending Heatmap
                    WeekdayHeatmap(
                        weeks: Self.sampleWeeks,
                        onTap: { _, _, _ in }
                    )
                    .enter(delay: 0.3)
                    
                    // Category Rankings
                    RankedBars(
                        rows: Self.sampleCategories,
                        onTap: { _ in }
                    )
                    .enter(delay: 0.4)
                    
                    // AI Insights Alert
                    MonytixAlertBanner(
                        type: .info,
                        message: "AI detected unusual spending on weekends"
                    )
                    .enter(delay: 0.5)
                    
                    // Action Buttons
                    actionButtons
                        .enter(delay: 0.6)
                }
                .padding(MonytixSpace.md)
            }
            .loadingOverlay(isLoading: isLoading, message: "Analyzing transactions...")
        }
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Welcome to")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(MonytixTheme.text2)
                    
                    HStack(spacing: 8) {
                        Text("Monytix")
                            .font(.system(size: 32, weight: .bold))
                            .foregroundStyle(MonytixTheme.text1)
                        
                        Text("AI")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(MonytixTheme.onAccent)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(MonytixTheme.gradientPrimary)
                            .clipShape(Capsule())
                            .glowPulse()
                    }
                }
                
                Spacer()

                Button {
                    AuthManager.shared.signOut()
                } label: {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 20, weight: .medium))
                        .foregroundStyle(MonytixTheme.text2)
                }
            }
            
            Text("Your AI-powered financial insights")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(MonytixTheme.text3)
        }
    }
    
    // MARK: - Stats Grid
    
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
    
    // MARK: - Action Buttons
    
    private var actionButtons: some View {
        VStack(spacing: 12) {
            Button("Add Transaction") {
                // Add transaction action
            }
            .buttonStyle(MonytixPrimaryButton())
            
            Button("View All Analytics") {
                // Analytics action
            }
            .buttonStyle(MonytixSecondaryButton())
        }
    }
    
    // MARK: - Sample Data (static to avoid recomputation in body)
    
    private static let sampleWeeks: [[Double]] = [
        [1200, 0, 340, 800, 120, 4500, 1800],
        [900, 110, 600, 0, 250, 3200, 900],
        [0, 400, 700, 300, 120, 2800, 1400],
        [800, 0, 900, 350, 0, 2100, 500],
        [650, 220, 0, 0, 0, 0, 0]
    ]
    
    private static let sampleCategories: [RankedBars.Row] = [
        .init(name: "Food & Dining", amount: 12500, percent: 0.85, delta: 0.15, icon: "fork.knife"),
        .init(name: "Transportation", amount: 8200, percent: 0.62, delta: -0.08, icon: "car.fill"),
        .init(name: "Shopping", amount: 6800, percent: 0.48, delta: 0.22, icon: "bag.fill"),
        .init(name: "Entertainment", amount: 4500, percent: 0.32, delta: -0.12, icon: "tv.fill")
    ]
}

#Preview {
    ContentView()
}
