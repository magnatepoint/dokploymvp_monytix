//
//  FutureViewModel.swift
//  ios_monytix
//
//  Financial Future (forecast) screen. Loads from GET /v1/forecast; falls back to mock on failure.
//

import Combine
import Foundation
import SwiftUI

struct FutureRecommendation: Identifiable {
    let id: String
    let title: String
    let body: String
    let ctaLabel: String?
}

@MainActor
final class FutureViewModel: ObservableObject {
    @Published private(set) var confidenceLabel = "Based on this month's cash flow and spending trend"
    @Published private(set) var projectionPoints: [(CGFloat, CGFloat)] = []
    @Published private(set) var heroMessage = "On track — no shortfall in the next 30 days"
    @Published private(set) var heroIsRisk = false
    @Published private(set) var lowPointDayIndex: Int? = nil
    @Published private(set) var riskStripLabel: String?
    @Published private(set) var riskStripSeverity = "neutral"
    @Published private(set) var savingsOpportunity: String?
    @Published private(set) var recommendations: [FutureRecommendation] = []
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String? = nil
    @Published private(set) var hasData = false

    init() {
        loadForecast()
    }

    func loadForecast() {
        isLoading = true
        errorMessage = nil
        Task {
            guard let token = await AuthManager.shared.getIdToken() else {
                loadMockData()
                isLoading = false
                return
            }
            switch await BackendApi.getForecast(accessToken: token) {
            case .success(let r):
                let points = r.projectionPoints.compactMap { list -> (CGFloat, CGFloat)? in
                    guard list.count >= 2 else { return nil }
                    return (CGFloat(list[0]), CGFloat(list[1]))
                }
                confidenceLabel = r.confidenceLabel.isEmpty ? "Based on this month's cash flow and spending trend" : r.confidenceLabel
                projectionPoints = points
                let (hero, isRisk, lowIdx) = deriveHeroAndLowPoint(points: points, severity: r.riskStripSeverity ?? "neutral", riskLabel: r.riskStripLabel)
                heroMessage = hero
                heroIsRisk = isRisk
                lowPointDayIndex = lowIdx
                riskStripLabel = r.riskStripLabel
                riskStripSeverity = r.riskStripSeverity ?? "neutral"
                savingsOpportunity = r.savingsOpportunity
                recommendations = r.recommendations.enumerated().map { i, rec in
                    FutureRecommendation(id: "\(i)", title: rec.title, body: rec.body, ctaLabel: nil)
                }
                hasData = !points.isEmpty
            case .failure(let e):
                errorMessage = e.localizedDescription
                hasData = false
            }
            isLoading = false
        }
    }

    private func deriveHeroAndLowPoint(points: [(CGFloat, CGFloat)], severity: String, riskLabel: String?) -> (String, Bool, Int?) {
        guard points.count >= 2 else { return ("On track — no shortfall in the next 30 days", false, nil) }
        var minY = points[0].1
        var minIdx = 0
        for i in 1..<points.count {
            if points[i].1 < minY {
                minY = points[i].1
                minIdx = i
            }
        }
        let totalDays = 30
        let lowPointDay = min(30, max(1, Int(CGFloat(minIdx) / CGFloat(max(1, points.count - 1)) * CGFloat(totalDays))))
        let isRisk = severity == "warning" || severity == "danger"
        let hero: String
        if isRisk, let label = riskLabel, !label.isEmpty {
            hero = label
        } else if isRisk {
            hero = "Your balance may hit a low point in \(lowPointDay) days."
        } else {
            hero = "On track — no shortfall in the next 30 days"
        }
        return (hero, isRisk, minIdx)
    }

    private func loadMockData() {
        projectionPoints = (0..<30).map { i in
            let x = CGFloat(i) / 29
            let y = 0.5 + 0.4 * (1 - Double(x)) - 0.3 * (1 - abs(Double(x) - 0.5) * 2)
            return (x, CGFloat(min(1, max(0.15, y))))
        }
        let (hero, isRisk, lowIdx) = deriveHeroAndLowPoint(points: projectionPoints, severity: "warning", riskLabel: "Your balance may hit a low point in 18 days.")
        heroMessage = hero
        heroIsRisk = isRisk
        lowPointDayIndex = lowIdx
        riskStripLabel = "Dip ahead — consider delaying non-essential spend until after payday."
        riskStripSeverity = "warning"
        savingsOpportunity = "You could save ₹3,200 by trimming dining 10%."
        recommendations = [
            FutureRecommendation(
                id: "1",
                title: "Trim discretionary spend",
                body: "Your spending is ahead of income this month. Focus on needs and defer wants where possible.",
                ctaLabel: nil
            ),
            FutureRecommendation(
                id: "2",
                title: "Top up Emergency goal by ₹2,000",
                body: "You're ahead of pace this month. Putting ₹2,000 into your Emergency goal keeps you on track.",
                ctaLabel: nil
            )
        ]
        hasData = true
    }

    func refresh() {
        loadForecast()
    }
}
