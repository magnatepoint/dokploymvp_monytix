//
//  BackendApi.swift
//  ios_monytix
//
//  Backend API client. Same contract as APK BackendApi: session, health, spendsense, goals. Use Bearer token from auth.
//

import Foundation

// MARK: - Session (matches backend GET /auth/session)

struct SessionResponse: Codable {
    let userId: String
    let email: String?
    let role: String?
}

// MARK: - Health

struct HealthResponse: Codable {
    let status: String?
}

// MARK: - SpendSense KPIs (GET /v1/spendsense/kpis)

struct CategorySpendKpi: Codable {
    let categoryCode: String?
    let categoryName: String?
    let txnCount: Int?
    let spendAmount: Double?
    let incomeAmount: Double?
    let deltaPct: Double?
}

struct KpiResponse: Codable {
    let month: String?
    let incomeAmount: Double?
    let needsAmount: Double?
    let wantsAmount: Double?
    let assetsAmount: Double?
    let totalDebitsAmount: Double?
    let topCategories: [CategorySpendKpi]?
}

// MARK: - SpendSense Accounts (GET /v1/spendsense/accounts)

struct AccountItemResponse: Codable {
    let id: String
    let bankCode: String?
    let bankName: String?
    let accountNumber: String?
    let balance: Double
    let accountType: String?
    let transactionCount: Int?
    let lastTxnDate: String?
}

struct AccountsResponse: Codable {
    let accounts: [AccountItemResponse]
}

// MARK: - SpendSense Insights (GET /v1/spendsense/insights)

struct TimeSeriesPoint: Codable {
    let date: String
    let value: Double
    let label: String?
}

struct CategoryBreakdownItem: Codable {
    let categoryCode: String?
    let categoryName: String?
    let amount: Double
    let percentage: Double
    let transactionCount: Int?
    let avgTransaction: Double?
}

struct SpendingTrendItem: Codable {
    let period: String?
    let income: Double?
    let expenses: Double?
    let net: Double?
    let needs: Double?
    let wants: Double?
    let assets: Double?
}

struct RecurringTransactionItem: Codable {
    let merchantName: String
    let categoryCode: String
    let categoryName: String
    let subcategoryCode: String?
    let subcategoryName: String?
    let frequency: String
    let avgAmount: Double
    let lastOccurrence: String?
    let nextExpected: String?
    let transactionCount: Int
    let totalAmount: Double
}

struct InsightsResponse: Codable {
    let timeSeries: [TimeSeriesPoint]?
    let categoryBreakdown: [CategoryBreakdownItem]?
    let spendingTrends: [SpendingTrendItem]?
    let recurringTransactions: [RecurringTransactionItem]?
}

// MARK: - SpendSense Transactions (GET /v1/spendsense/transactions)

struct TransactionRecordResponse: Codable {
    let txnId: String
    let txnDate: String
    let txnTime: String?
    let merchant: String?
    let category: String?
    let subcategory: String?
    let bankCode: String?
    let channel: String?
    let amount: Double
    let direction: String
    let confidence: Double?
}

struct TransactionListResponse: Codable {
    let transactions: [TransactionRecordResponse]
    let total: Int?
    let page: Int?
    let pageSize: Int?
}

// MARK: - SpendSense Categories / Subcategories / Channels / Months

struct CategoryResponse: Codable {
    let categoryCode: String?
    let categoryName: String?
    let isCustom: Bool?
    let txnType: String?
}

struct SubcategoryResponse: Codable {
    let subcategoryCode: String?
    let subcategoryName: String?
    let categoryCode: String?
    let isCustom: Bool?
}

struct AvailableMonthsResponse: Codable {
    let data: [String]
}

struct TransactionSummaryResponse: Codable {
    let debitTotal: Double?
    let creditTotal: Double?
    let debitCount: Int?
    let creditCount: Int?
}

// MARK: - Create transaction (POST /v1/spendsense/transactions)

struct TransactionCreateRequest: Encodable {
    let txnDate: String
    let merchantName: String
    let description: String?
    let amount: Double
    let direction: String
    let categoryCode: String?
    let subcategoryCode: String?
    let channel: String?
    let accountRef: String?

    enum CodingKeys: String, CodingKey {
        case txnDate = "txn_date"
        case merchantName = "merchant_name"
        case description
        case amount
        case direction
        case categoryCode = "category_code"
        case subcategoryCode = "subcategory_code"
        case channel
        case accountRef = "account_ref"
    }
}

struct TransactionCreateResponse: Codable {
    let txnId: String
    let txnDate: String?
    let merchant: String?
    let category: String?
    let subcategory: String?
    let bankCode: String?
    let channel: String?
    let amount: Double
    let direction: String
    let confidence: Double?
}

// MARK: - Upload batch (POST /v1/spendsense/uploads/file, GET /v1/spendsense/batches/{id})

struct UploadBatchResponse: Codable {
    let uploadId: String
    let userId: String?
    let sourceType: String?
    let status: String
    let createdAt: String?
    let errorMessage: String?

    enum CodingKeys: String, CodingKey {
        case uploadId = "upload_id"
        case userId = "user_id"
        case sourceType = "source_type"
        case status
        case createdAt = "created_at"
        case errorMessage = "error_message"
    }
}

// MARK: - Goals Progress (GET /v1/goals/progress)

struct GoalProgressItem: Codable {
    let goalId: String
    let goalName: String
    let progressPct: Double?
    let currentSavingsClose: Double?
    let remainingAmount: Double?
    let projectedCompletionDate: String?
    let milestones: [Int]?
    let monthlyRequired: Double?
    let monthlyAvgContribution: Double?
    let paceDescription: String?
    let daysToTarget: Int?
}

struct GoalsProgressResponse: Codable {
    let goals: [GoalProgressItem]
}

// MARK: - Forecast (GET /v1/forecast)

struct ForecastRecommendationResponse: Codable {
    let title: String
    let body: String
}

struct ForecastResponse: Codable {
    let projectionPoints: [[Double]]
    let confidenceLabel: String
    let riskStripLabel: String?
    let riskStripSeverity: String?
    let savingsOpportunity: String?
    let recommendations: [ForecastRecommendationResponse]
}

// MARK: - Top Insights (GET /v1/spendsense/insights/top)

struct TopInsightItemResponse: Codable {
    let id: String
    let title: String
    let message: String
    let type: String
    let confidence: Double?
}

struct TopInsightsResponse: Codable {
    let insights: [TopInsightItemResponse]
}

// MARK: - Assistant (POST /v1/assistant/ask)

struct AskRequest: Encodable {
    let prompt: String
}

struct AskResponse: Codable {
    let answer: String
}

// MARK: - Goals (GET/POST/PUT/DELETE /v1/goals)

struct GoalResponse: Codable, Identifiable, Hashable {
    var id: String { goalId }
    let goalId: String
    let goalCategory: String
    let goalName: String
    let goalType: String
    let linkedTxnType: String?
    let estimatedCost: Double
    let targetDate: String?
    let currentSavings: Double
    let importance: Int?
    let priorityRank: Int?
    let status: String
    let notes: String?
    let createdAt: String?
    let updatedAt: String?
}

struct CreateGoalResponse: Codable {
    let status: String?
    let goalId: String?
}

struct CreateGoalRequest: Encodable {
    let goalCategory: String
    let goalName: String
    let estimatedCost: Double
    let targetDate: String?
    let currentSavings: Double
    let goalType: String?
    let importance: Int?
}

struct GoalUpdateRequest: Encodable {
    let estimatedCost: Double?
    let targetDate: String?
    let currentSavings: Double?
    let goalType: String?
    let importance: Int?
}

// MARK: - BudgetPilot (GET/POST /v1/budget/*)

struct BudgetStateActual: Codable {
    let needsPct: Double?
    let wantsPct: Double?
    let savingsPct: Double?
    let needsAmt: Double?
    let wantsAmt: Double?
    let savingsAmt: Double?
}

struct BudgetStateDeviation: Codable {
    let needs: Double?
    let wants: Double?
    let savings: Double?
}

struct BudgetStateCommittedPlan: Codable {
    let planId: String?
    let target: [String: Double]?
}

struct BudgetStatePlan: Codable {
    let planId: String?
    let name: String?
    let score: Double?
    let reason: String?
}

struct BudgetGoalImpact: Codable {
    let goalId: String?
    let goalName: String?
    let status: String?
    let plannedAmount: Double?
    let shortfall: Double?
}

struct BudgetStateResponse: Codable {
    let month: String?
    let committedPlan: BudgetStateCommittedPlan?
    let incomeAmt: Double?
    let actual: BudgetStateActual?
    let deviation: BudgetStateDeviation?
    let plans: [BudgetStatePlan]?
    let goalImpact: [BudgetGoalImpact]?
    let lastUpdatedAt: String?

    enum CodingKeys: String, CodingKey {
        case month
        case committedPlan = "committed_plan"
        case incomeAmt = "income_amt"
        case actual
        case deviation
        case plans
        case goalImpact = "goal_impact"
        case lastUpdatedAt = "last_updated_at"
    }
}

struct CommittedBudget: Codable {
    let userId: String?
    let month: String?
    let planCode: String?
    let allocNeedsPct: Double?
    let allocWantsPct: Double?
    let allocAssetsPct: Double?
    let notes: String?
    let committedAt: String?
    let goalAllocations: [GoalAllocationItem]?

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case month
        case planCode = "plan_code"
        case allocNeedsPct = "alloc_needs_pct"
        case allocWantsPct = "alloc_wants_pct"
        case allocAssetsPct = "alloc_assets_pct"
        case notes
        case committedAt = "committed_at"
        case goalAllocations = "goal_allocations"
    }
}

struct GoalAllocationItem: Codable {
    let ubcgaId: String?
    let goalId: String?
    let goalName: String?
    let weightPct: Double?
    let plannedAmount: Double?
}

struct BudgetVariance: Codable {
    let userId: String?
    let month: String?
    let incomeAmt: Double?
    let needsAmt: Double?
    let plannedNeedsAmt: Double?
    let varianceNeedsAmt: Double?
    let wantsAmt: Double?
    let plannedWantsAmt: Double?
    let varianceWantsAmt: Double?
    let assetsAmt: Double?
    let plannedAssetsAmt: Double?
    let varianceAssetsAmt: Double?
    let computedAt: String?

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case month
        case incomeAmt = "income_amt"
        case needsAmt = "needs_amt"
        case plannedNeedsAmt = "planned_needs_amt"
        case varianceNeedsAmt = "variance_needs_amt"
        case wantsAmt = "wants_amt"
        case plannedWantsAmt = "planned_wants_amt"
        case varianceWantsAmt = "variance_wants_amt"
        case assetsAmt = "assets_amt"
        case plannedAssetsAmt = "planned_assets_amt"
        case varianceAssetsAmt = "variance_assets_amt"
        case computedAt = "computed_at"
    }
}

struct BudgetRecommendation: Codable, Identifiable {
    var id: String { planCode ?? "" }
    let planCode: String?
    let name: String?
    let description: String?
    let needsBudgetPct: Double?
    let wantsBudgetPct: Double?
    let savingsBudgetPct: Double?
    let score: Double?
    let recommendationReason: String?

    enum CodingKeys: String, CodingKey {
        case planCode = "plan_code"
        case name
        case description
        case needsBudgetPct = "needs_budget_pct"
        case wantsBudgetPct = "wants_budget_pct"
        case savingsBudgetPct = "savings_budget_pct"
        case score
        case recommendationReason = "recommendation_reason"
    }
}

struct BudgetCommitRequest: Encodable {
    let planCode: String
    let month: String?
    let goalAllocations: [String: Double]?
    let notes: String?

    enum CodingKeys: String, CodingKey {
        case planCode = "plan_code"
        case month
        case goalAllocations = "goal_allocations"
        case notes
    }
}

struct BudgetCommitResponse: Codable {
    let status: String?
    let budget: CommittedBudget?
}

struct BudgetVarianceResponse: Codable {
    let status: String?
    let aggregate: BudgetVariance?
}

struct BudgetApplyAdjustmentRequest: Encodable {
    let shiftFrom: String
    let shiftTo: String
    let pct: Double
    let month: String?

    enum CodingKeys: String, CodingKey {
        case shiftFrom = "shift_from"
        case shiftTo = "shift_to"
        case pct
        case month
    }
}

struct BudgetApplyAdjustmentResponse: Codable {
    let status: String?
    let reason: String?
    let budget: CommittedBudget?
}

// MARK: - MoneyMoments (GET/POST /v1/moneymoments/*)

struct MoneyMoment: Codable, Identifiable {
    var id: String { "\(habitId ?? "")-\(month ?? "")" }
    let userId: String?
    let month: String?
    let habitId: String?
    let value: Double?
    let label: String?
    let insightText: String?
    let confidence: Double?
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case month
        case habitId = "habit_id"
        case value
        case label
        case insightText = "insight_text"
        case confidence
        case createdAt = "created_at"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        userId = try? c.decodeIfPresent(String.self, forKey: .userId)
        month = try? c.decodeIfPresent(String.self, forKey: .month)
        habitId = try? c.decodeIfPresent(String.self, forKey: .habitId)
        value = try? c.decodeIfPresent(Double.self, forKey: .value)
            ?? (try? c.decodeIfPresent(Int.self, forKey: .value)).map { Double($0) }
        label = try? c.decodeIfPresent(String.self, forKey: .label)
        insightText = try? c.decodeIfPresent(String.self, forKey: .insightText)
        confidence = try? c.decodeIfPresent(Double.self, forKey: .confidence)
            ?? (try? c.decodeIfPresent(Int.self, forKey: .confidence)).map { Double($0) }
        createdAt = try? c.decodeIfPresent(String.self, forKey: .createdAt)
    }
}

struct MoneyMomentsResponse: Codable {
    let moments: [MoneyMoment]?
}

struct Nudge: Codable, Identifiable {
    var id: String { deliveryId ?? "" }
    let deliveryId: String?
    let userId: String?
    let ruleId: String?
    let templateCode: String?
    let channel: String?
    let sentAt: String?
    let sendStatus: String?
    let metadataJson: [String: String]?
    let titleTemplate: String?
    let bodyTemplate: String?
    let title: String?
    let body: String?
    let ctaText: String?
    let ctaDeeplink: String?
    let ruleName: String?

    enum CodingKeys: String, CodingKey {
        case deliveryId = "delivery_id"
        case userId = "user_id"
        case ruleId = "rule_id"
        case templateCode = "template_code"
        case channel
        case sentAt = "sent_at"
        case sendStatus = "send_status"
        case metadataJson = "metadata_json"
        case titleTemplate = "title_template"
        case bodyTemplate = "body_template"
        case title
        case body
        case ctaText = "cta_text"
        case ctaDeeplink = "cta_deeplink"
        case ruleName = "rule_name"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        deliveryId = try? c.decodeIfPresent(String.self, forKey: .deliveryId)
        userId = try? c.decodeIfPresent(String.self, forKey: .userId)
        ruleId = try? c.decodeIfPresent(String.self, forKey: .ruleId)
        templateCode = try? c.decodeIfPresent(String.self, forKey: .templateCode)
        channel = try? c.decodeIfPresent(String.self, forKey: .channel)
        sentAt = try? c.decodeIfPresent(String.self, forKey: .sentAt)
        sendStatus = try? c.decodeIfPresent(String.self, forKey: .sendStatus)
        metadataJson = (try? c.decodeIfPresent([String: String].self, forKey: .metadataJson)) ?? nil
        titleTemplate = try? c.decodeIfPresent(String.self, forKey: .titleTemplate)
        bodyTemplate = try? c.decodeIfPresent(String.self, forKey: .bodyTemplate)
        title = try? c.decodeIfPresent(String.self, forKey: .title)
        body = try? c.decodeIfPresent(String.self, forKey: .body)
        ctaText = try? c.decodeIfPresent(String.self, forKey: .ctaText)
        ctaDeeplink = try? c.decodeIfPresent(String.self, forKey: .ctaDeeplink)
        ruleName = try? c.decodeIfPresent(String.self, forKey: .ruleName)
    }
}

struct NudgesResponse: Codable {
    let nudges: [Nudge]?
}

struct NudgeInteractionRequest: Encodable {
    let eventType: String
    let metadata: [String: String]?

    enum CodingKeys: String, CodingKey {
        case eventType = "event_type"
        case metadata
    }
}

struct ComputeMomentsResponse: Codable {
    let status: String?
    let moments: [MoneyMoment]?
    let count: Int?
    let message: String?
}

struct EvaluateNudgesResponse: Codable {
    let status: String?
    let count: Int?
    let candidates: [String]? // backend may return list of dicts; we only need status/count

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        status = try? c.decodeIfPresent(String.self, forKey: .status)
        count = try? c.decodeIfPresent(Int.self, forKey: .count)
        // Backend returns "candidates" as list of objects, not [String]. Skip to avoid decode failure.
        candidates = nil
    }

    private enum CodingKeys: String, CodingKey {
        case status
        case count
        case candidates
    }
}

struct ProcessNudgesResponse: Codable {
    let status: String?
    let delivered: [Nudge]?
    let count: Int?
}

struct ComputeSignalResponse: Codable {
    let status: String?
    let signal: [String: String]? // backend returns signal with mixed types (UUID, date, numbers); we ignore for app

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        status = try? c.decodeIfPresent(String.self, forKey: .status)
        // Backend signal is dict with non-string values (e.g. user_id UUID, as_of_date date, numbers). Skip decoding to avoid failure.
        signal = nil
    }

    private enum CodingKeys: String, CodingKey {
        case status
        case signal
    }
}

// MARK: - Errors

enum BackendApiError: Error, LocalizedError {
    case invalidURL
    case httpStatus(Int, String?)
    case decoding(Error)
    case network(Error)

    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Invalid server URL"
        case .httpStatus(let code, let msg): return msg ?? "Server error (\(code))"
        case .decoding: return "Invalid response from server"
        case .network(let e): return (e as NSError).localizedDescription
        }
    }
}

// MARK: - API

private let decoder: JSONDecoder = {
    let d = JSONDecoder()
    d.keyDecodingStrategy = .convertFromSnakeCase
    return d
}()

private let encoder: JSONEncoder = {
    let e = JSONEncoder()
    e.keyEncodingStrategy = .convertToSnakeCase
    return e
}()

/// Avoid showing raw HTML or huge response bodies in the UI (e.g. Cloudflare error pages).
private func sanitizeErrorBody(data: Data, statusCode: Int) -> String? {
    guard let raw = String(data: data, encoding: .utf8) else { return "Server error (\(statusCode))" }
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    if trimmed.lowercased().hasPrefix("<!doctype") || trimmed.lowercased().hasPrefix("<html") || trimmed.contains("</") {
        return "Service unavailable (\(statusCode)). Please try again later."
    }
    if trimmed.count > 200 {
        return String(trimmed.prefix(200)) + "…"
    }
    return trimmed.isEmpty ? "Server error (\(statusCode))" : trimmed
}

private func runWithFallbackRequest<T: Decodable>(
    _ path: String,
    method: String,
    accessToken: String,
    body: Data? = nil,
    decode: (Data) throws -> T
) async -> Result<T, BackendApiError> {
    let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
    var lastError: Error?
    for base in bases {
        guard let url = URL(string: "\(base)\(path.hasPrefix("/") ? "" : "/")\(path)") else { continue }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        req.httpMethod = method
        if let body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = body
        }
        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            if code >= 200 && code < 300 {
                do {
                    let value = try decode(data)
                    return .success(value)
                } catch {
                    return .failure(.decoding(error))
                }
            }
            if code == 401 {
                return .failure(.httpStatus(401, "Unauthorized. Please sign out and sign in again."))
            }
            return .failure(.httpStatus(code, sanitizeErrorBody(data: data, statusCode: code)))
        } catch {
            lastError = error
            continue
        }
    }
    if let lastError {
        return .failure(.network(lastError))
    }
    return .failure(.invalidURL)
}

private func runWithFallbackNoDecode(
    _ path: String,
    method: String,
    accessToken: String,
    body: Data? = nil
) async -> Result<Void, BackendApiError> {
    let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
    var lastError: Error?
    for base in bases {
        guard let url = URL(string: "\(base)\(path.hasPrefix("/") ? "" : "/")\(path)") else { continue }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        req.httpMethod = method
        if let body {
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
            req.httpBody = body
        }
        do {
            let (_, response) = try await URLSession.shared.data(for: req)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            if code >= 200 && code < 300 {
                return .success(())
            }
            if code == 401 {
                return .failure(.httpStatus(401, "Unauthorized. Please sign out and sign in again."))
            }
            return .failure(.httpStatus(code, "HTTP \(code)"))
        } catch {
            lastError = error
            continue
        }
    }
    if let lastError {
        return .failure(.network(lastError))
    }
    return .failure(.invalidURL)
}

private func runWithFallback<T: Decodable>(
    _ path: String,
    queryItems: [URLQueryItem]? = nil,
    accessToken: String,
    decode: (Data) throws -> T
) async -> Result<T, BackendApiError> {
    let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
    var lastError: Error?
    for base in bases {
        var pathWithQuery = path
        if let q = queryItems, !q.isEmpty {
            pathWithQuery += "?" + q.map { "\($0.name)=\($0.value?.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")" }.joined(separator: "&")
        }
        guard let url = URL(string: "\(base)\(pathWithQuery.hasPrefix("/") ? "" : "/")\(pathWithQuery)") else { continue }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        req.httpMethod = "GET"
        do {
            let (data, response) = try await URLSession.shared.data(for: req)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            if code >= 200 && code < 300 {
                do {
                    let value = try decode(data)
                    return .success(value)
                } catch {
                    return .failure(.decoding(error))
                }
            }
            if code == 401 {
                return .failure(.httpStatus(401, "Unauthorized. Please sign out and sign in again."))
            }
            return .failure(.httpStatus(code, sanitizeErrorBody(data: data, statusCode: code)))
        } catch {
            lastError = error
            continue
        }
    }
    if let last = lastError {
        return .failure(.network(last))
    }
    return .failure(.invalidURL)
}

enum BackendApi {

    /// Validate Firebase ID token with backend; returns user_id, email, role.
    static func getSession(accessToken: String) async -> Result<SessionResponse, BackendApiError> {
        let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
        for (idx, base) in bases.enumerated() {
            let url = URL(string: "\(base)/auth/session")
            guard let u = url else { continue }
            var req = URLRequest(url: u)
            req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
            req.httpMethod = "GET"
            do {
                let (data, response) = try await URLSession.shared.data(for: req)
                let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                if code >= 200 && code < 300 {
                    let session = try decoder.decode(SessionResponse.self, from: data)
                    return .success(session)
                }
                if code == 401 {
                    return .failure(.httpStatus(401, "Unauthorized. Please sign out and sign in again."))
                }
                return .failure(.httpStatus(code, sanitizeErrorBody(data: data, statusCode: code)))
            } catch {
                if idx == bases.count - 1 { return .failure(.network(error)) }
            }
        }
        return .failure(.invalidURL)
    }

    static func healthCheck() async -> Result<HealthResponse, BackendApiError> {
        let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
        for base in bases {
            guard let url = URL(string: "\(base)/health") else { continue }
            do {
                let (data, response) = try await URLSession.shared.data(from: url)
                let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                if code >= 200 && code < 300 {
                    let health = try decoder.decode(HealthResponse.self, from: data)
                    return .success(health)
                }
            } catch { continue }
        }
        return .failure(.invalidURL)
    }

    // MARK: - SpendSense

    static func getKpis(accessToken: String, month: String? = nil) async -> Result<KpiResponse, BackendApiError> {
        let path = month != nil ? "/v1/spendsense/kpis?month=\(month!)" : "/v1/spendsense/kpis"
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode(KpiResponse.self, from: data)
        }
    }

    static func getAccounts(accessToken: String) async -> Result<AccountsResponse, BackendApiError> {
        return await runWithFallback("/v1/spendsense/accounts", accessToken: accessToken) { data in
            try decoder.decode(AccountsResponse.self, from: data)
        }
    }

    static func getInsights(
        accessToken: String,
        startDate: String? = nil,
        endDate: String? = nil
    ) async -> Result<InsightsResponse, BackendApiError> {
        let path = "/v1/spendsense/insights"
        var items: [URLQueryItem] = []
        if let s = startDate { items.append(URLQueryItem(name: "start_date", value: s)) }
        if let e = endDate { items.append(URLQueryItem(name: "end_date", value: e)) }
        return await runWithFallback(path, queryItems: items.isEmpty ? nil : items, accessToken: accessToken) { data in
            try decoder.decode(InsightsResponse.self, from: data)
        }
    }

    static func getTransactions(
        accessToken: String,
        limit: Int = 25,
        offset: Int = 0,
        search: String? = nil,
        categoryCode: String? = nil,
        subcategoryCode: String? = nil,
        channel: String? = nil,
        direction: String? = nil,
        bankCode: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil
    ) async -> Result<TransactionListResponse, BackendApiError> {
        var items: [URLQueryItem] = [
            URLQueryItem(name: "limit", value: "\(limit)"),
            URLQueryItem(name: "offset", value: "\(offset)")
        ]
        if let v = search, !v.isEmpty { items.append(URLQueryItem(name: "search", value: v)) }
        if let v = categoryCode { items.append(URLQueryItem(name: "category_code", value: v)) }
        if let v = subcategoryCode { items.append(URLQueryItem(name: "subcategory_code", value: v)) }
        if let v = channel { items.append(URLQueryItem(name: "channel", value: v)) }
        if let v = direction { items.append(URLQueryItem(name: "direction", value: v)) }
        if let v = bankCode { items.append(URLQueryItem(name: "bank_code", value: v)) }
        if let v = startDate { items.append(URLQueryItem(name: "start_date", value: v)) }
        if let v = endDate { items.append(URLQueryItem(name: "end_date", value: v)) }
        let path = "/v1/spendsense/transactions"
        return await runWithFallback(path, queryItems: items, accessToken: accessToken) { data in
            try decoder.decode(TransactionListResponse.self, from: data)
        }
    }

    static func getTransactionsSummary(
        accessToken: String,
        startDate: String? = nil,
        endDate: String? = nil,
        direction: String? = nil,
        categoryCode: String? = nil,
        subcategoryCode: String? = nil,
        channel: String? = nil,
        bankCode: String? = nil,
        search: String? = nil
    ) async -> Result<TransactionSummaryResponse, BackendApiError> {
        var items: [URLQueryItem] = []
        if let v = startDate { items.append(URLQueryItem(name: "start_date", value: v)) }
        if let v = endDate { items.append(URLQueryItem(name: "end_date", value: v)) }
        if let v = direction { items.append(URLQueryItem(name: "direction", value: v)) }
        if let v = categoryCode { items.append(URLQueryItem(name: "category_code", value: v)) }
        if let v = subcategoryCode { items.append(URLQueryItem(name: "subcategory_code", value: v)) }
        if let v = channel { items.append(URLQueryItem(name: "channel", value: v)) }
        if let v = bankCode { items.append(URLQueryItem(name: "bank_code", value: v)) }
        if let v = search, !v.isEmpty { items.append(URLQueryItem(name: "search", value: v)) }
        let path = "/v1/spendsense/transactions/summary"
        return await runWithFallback(path, queryItems: items.isEmpty ? nil : items, accessToken: accessToken) { data in
            try decoder.decode(TransactionSummaryResponse.self, from: data)
        }
    }

    static func getCategories(accessToken: String) async -> Result<[CategoryResponse], BackendApiError> {
        return await runWithFallback("/v1/spendsense/categories", accessToken: accessToken) { data in
            try decoder.decode([CategoryResponse].self, from: data)
        }
    }

    static func getSubcategories(accessToken: String, categoryCode: String? = nil) async -> Result<[SubcategoryResponse], BackendApiError> {
        var path = "/v1/spendsense/subcategories"
        if let code = categoryCode { path += "?category_code=\(code.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? code)" }
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode([SubcategoryResponse].self, from: data)
        }
    }

    static func getChannels(accessToken: String) async -> Result<[String], BackendApiError> {
        return await runWithFallback("/v1/spendsense/channels", accessToken: accessToken) { data in
            try decoder.decode([String].self, from: data)
        }
    }

    static func getAvailableMonths(accessToken: String) async -> Result<AvailableMonthsResponse, BackendApiError> {
        return await runWithFallback("/v1/spendsense/kpis/available-months", accessToken: accessToken) { data in
            try decoder.decode(AvailableMonthsResponse.self, from: data)
        }
    }

    /// Create a manual transaction. Date format: YYYY-MM-DD.
    static func createTransaction(accessToken: String, request: TransactionCreateRequest) async -> Result<TransactionCreateResponse, BackendApiError> {
        guard let body = try? encoder.encode(request) else {
            return .failure(.decoding(NSError(domain: "BackendApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "Encode failed"])))
        }
        return await runWithFallbackRequest("/v1/spendsense/transactions", method: "POST", accessToken: accessToken, body: body) { data in
            try decoder.decode(TransactionCreateResponse.self, from: data)
        }
    }

    /// Upload statement file (PDF/CSV/XLS/XLSX). Optional password for protected PDFs.
    static func uploadStatement(accessToken: String, fileData: Data, filename: String, password: String? = nil) async -> Result<UploadBatchResponse, BackendApiError> {
        let boundary = "Boundary-\(UUID().uuidString)"
        var body = Data()
        let boundaryPrefix = "--\(boundary)\r\n"
        let contentType: String
        if filename.lowercased().hasSuffix(".pdf") {
            contentType = "application/pdf"
        } else if filename.lowercased().hasSuffix(".csv") {
            contentType = "text/csv"
        } else if filename.lowercased().hasSuffix(".xlsx") {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        } else if filename.lowercased().hasSuffix(".xls") {
            contentType = "application/vnd.ms-excel"
        } else {
            contentType = "application/octet-stream"
        }
        body.append(Data(boundaryPrefix.utf8))
        body.append(Data("Content-Disposition: form-data; name=\"file\"; filename=\"\(filename)\"\r\n".utf8))
        body.append(Data("Content-Type: \(contentType)\r\n\r\n".utf8))
        body.append(fileData)
        body.append(Data("\r\n".utf8))
        if let p = password, !p.isEmpty {
            body.append(Data(boundaryPrefix.utf8))
            body.append(Data("Content-Disposition: form-data; name=\"password\"\r\n\r\n".utf8))
            body.append(Data(p.utf8))
            body.append(Data("\r\n".utf8))
        }
        body.append(Data("--\(boundary)--\r\n".utf8))

        let bases = [BackendConfig.baseURL, BackendConfig.backupBaseURL]
        var lastError: Error?
        for base in bases {
            guard let url = URL(string: "\(base)/v1/spendsense/uploads/file") else { continue }
            var req = URLRequest(url: url)
            req.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
            req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
            req.httpMethod = "POST"
            req.httpBody = body
            do {
                let (data, response) = try await URLSession.shared.data(for: req)
                let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                if code >= 200 && code < 300 {
                    do {
                        let value = try decoder.decode(UploadBatchResponse.self, from: data)
                        return .success(value)
                    } catch {
                        return .failure(.decoding(error))
                    }
                }
                if code == 401 {
                    return .failure(.httpStatus(401, "Unauthorized. Please sign out and sign in again."))
                }
                return .failure(.httpStatus(code, sanitizeErrorBody(data: data, statusCode: code)))
            } catch {
                lastError = error
                continue
            }
        }
        if let last = lastError {
            return .failure(.network(last))
        }
        return .failure(.invalidURL)
    }

    /// Get upload batch status (e.g. after upload for polling).
    static func getBatchStatus(accessToken: String, batchId: String) async -> Result<UploadBatchResponse, BackendApiError> {
        let path = "/v1/spendsense/batches/\(batchId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? batchId)"
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode(UploadBatchResponse.self, from: data)
        }
    }

    // MARK: - Goals

    static func getGoalsProgress(accessToken: String) async -> Result<GoalsProgressResponse, BackendApiError> {
        return await runWithFallback("/v1/goals/progress", accessToken: accessToken) { data in
            try decoder.decode(GoalsProgressResponse.self, from: data)
        }
    }

    static func getForecast(accessToken: String) async -> Result<ForecastResponse, BackendApiError> {
        return await runWithFallback("/v1/forecast", accessToken: accessToken) { data in
            try decoder.decode(ForecastResponse.self, from: data)
        }
    }

    static func getTopInsights(accessToken: String, limit: Int = 5) async -> Result<TopInsightsResponse, BackendApiError> {
        return await runWithFallback("/v1/spendsense/insights/top?limit=\(limit)", accessToken: accessToken) { data in
            try decoder.decode(TopInsightsResponse.self, from: data)
        }
    }

    static func postAssistantAsk(accessToken: String, prompt: String) async -> Result<AskResponse, BackendApiError> {
        let body = AskRequest(prompt: prompt)
        guard let data = try? encoder.encode(body) else { return .failure(.decoding(NSError(domain: "BackendApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "Encode failed"]))) }
        return await runWithFallbackRequest("/v1/assistant/ask", method: "POST", accessToken: accessToken, body: data) { respData in
            try decoder.decode(AskResponse.self, from: respData)
        }
    }

    static func getUserGoals(accessToken: String) async -> Result<[GoalResponse], BackendApiError> {
        return await runWithFallback("/v1/goals", accessToken: accessToken) { data in
            try decoder.decode([GoalResponse].self, from: data)
        }
    }

    static func createGoal(
        accessToken: String,
        goalCategory: String,
        goalName: String,
        estimatedCost: Double,
        targetDate: String? = nil,
        currentSavings: Double = 0,
        goalType: String? = nil,
        importance: Int? = nil
    ) async -> Result<CreateGoalResponse, BackendApiError> {
        let body = try? encoder.encode(
            CreateGoalRequest(
                goalCategory: goalCategory,
                goalName: goalName,
                estimatedCost: estimatedCost,
                targetDate: targetDate,
                currentSavings: currentSavings,
                goalType: goalType,
                importance: importance
            )
        )
        return await runWithFallbackRequest("/v1/goals", method: "POST", accessToken: accessToken, body: body) { data in
            try decoder.decode(CreateGoalResponse.self, from: data)
        }
    }

    static func updateGoal(
        accessToken: String,
        goalId: String,
        estimatedCost: Double? = nil,
        targetDate: String? = nil,
        currentSavings: Double? = nil,
        goalType: String? = nil,
        importance: Int? = nil
    ) async -> Result<GoalResponse, BackendApiError> {
        let body = try? encoder.encode(
            GoalUpdateRequest(
                estimatedCost: estimatedCost,
                targetDate: targetDate,
                currentSavings: currentSavings,
                goalType: goalType,
                importance: importance
            )
        )
        return await runWithFallbackRequest("/v1/goals/\(goalId)", method: "PUT", accessToken: accessToken, body: body) { data in
            try decoder.decode(GoalResponse.self, from: data)
        }
    }

    static func deleteGoal(accessToken: String, goalId: String) async -> Result<Void, BackendApiError> {
        await runWithFallbackNoDecode("/v1/goals/\(goalId)", method: "DELETE", accessToken: accessToken)
    }

    // MARK: - BudgetPilot

    /// Month format: "YYYY-MM" (we append "-01" for state/commit/recalculate/apply-adjustment).
    static func getBudgetState(accessToken: String, month: String? = nil) async -> Result<BudgetStateResponse, BackendApiError> {
        let monthParam = month.flatMap { m in m.isEmpty ? nil : "\(m)-01" }
        let path = monthParam != nil ? "/v1/budget/state?month=\(monthParam!)" : "/v1/budget/state"
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode(BudgetStateResponse.self, from: data)
        }
    }

    static func commitBudget(accessToken: String, planCode: String, month: String? = nil) async -> Result<CommittedBudget, BackendApiError> {
        let monthVal = month.flatMap { m in m.isEmpty ? nil : "\(m)-01" }
        let body = try? encoder.encode(BudgetCommitRequest(planCode: planCode, month: monthVal, goalAllocations: nil, notes: nil))
        return await runWithFallbackRequest("/v1/budget/commit", method: "POST", accessToken: accessToken, body: body) { data in
            let resp = try decoder.decode(BudgetCommitResponse.self, from: data)
            guard let budget = resp.budget else {
                throw NSError(domain: "BackendApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "No budget in response"])
            }
            return budget
        }
    }

    static func recalculateBudget(accessToken: String, month: String? = nil) async -> Result<Void, BackendApiError> {
        let monthParam = month.flatMap { m in m.isEmpty ? nil : "\(m)-01" }
        let path = monthParam != nil ? "/v1/budget/recalculate?month=\(monthParam!)" : "/v1/budget/recalculate"
        return await runWithFallbackNoDecode(path, method: "POST", accessToken: accessToken)
    }

    static func getBudgetVariance(accessToken: String, month: String? = nil) async -> Result<BudgetVarianceResponse, BackendApiError> {
        let monthParam = month.flatMap { m in m.isEmpty ? nil : "\(m)-01" }
        let path = monthParam != nil ? "/v1/budget/variance?month=\(monthParam!)" : "/v1/budget/variance"
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode(BudgetVarianceResponse.self, from: data)
        }
    }

    static func applyBudgetAdjustment(
        accessToken: String,
        shiftFrom: String,
        shiftTo: String,
        pct: Double,
        month: String? = nil
    ) async -> Result<BudgetApplyAdjustmentResponse, BackendApiError> {
        let monthVal = month.flatMap { m in m.isEmpty ? nil : "\(m)-01" }
        let body = try? encoder.encode(BudgetApplyAdjustmentRequest(shiftFrom: shiftFrom, shiftTo: shiftTo, pct: pct, month: monthVal))
        return await runWithFallbackRequest("/v1/budget/apply-adjustment", method: "POST", accessToken: accessToken, body: body) { data in
            try decoder.decode(BudgetApplyAdjustmentResponse.self, from: data)
        }
    }

    // MARK: - MoneyMoments

    static func getMoments(accessToken: String, month: String? = nil, allMonths: Bool = false) async -> Result<MoneyMomentsResponse, BackendApiError> {
        var path = "/v1/moneymoments/moments"
        var query: [String] = []
        if let m = month, !m.isEmpty { query.append("month=\(m)") }
        if allMonths { query.append("all_months=true") }
        if !query.isEmpty { path += "?" + query.joined(separator: "&") }
        return await runWithFallback(path, accessToken: accessToken) { data in
            try decoder.decode(MoneyMomentsResponse.self, from: data)
        }
    }

    static func computeMoments(accessToken: String, targetMonth: String? = nil) async -> Result<ComputeMomentsResponse, BackendApiError> {
        let path = targetMonth.map { "/v1/moneymoments/moments/compute?target_month=\($0)" } ?? "/v1/moneymoments/moments/compute"
        return await runWithFallbackRequest(path, method: "POST", accessToken: accessToken, body: nil) { data in
            try decoder.decode(ComputeMomentsResponse.self, from: data)
        }
    }

    static func getNudges(accessToken: String, limit: Int = 20) async -> Result<NudgesResponse, BackendApiError> {
        return await runWithFallback("/v1/moneymoments/nudges?limit=\(limit)", accessToken: accessToken) { data in
            try decoder.decode(NudgesResponse.self, from: data)
        }
    }

    static func logNudgeInteraction(accessToken: String, deliveryId: String, eventType: String, metadata: [String: String]? = nil) async -> Result<Void, BackendApiError> {
        let body = try? encoder.encode(NudgeInteractionRequest(eventType: eventType, metadata: metadata))
        return await runWithFallbackNoDecode("/v1/moneymoments/nudges/\(deliveryId)/interact", method: "POST", accessToken: accessToken, body: body)
    }

    static func evaluateNudges(accessToken: String, asOfDate: String? = nil) async -> Result<EvaluateNudgesResponse, BackendApiError> {
        let path = asOfDate.map { "/v1/moneymoments/nudges/evaluate?as_of_date=\($0)" } ?? "/v1/moneymoments/nudges/evaluate"
        return await runWithFallbackRequest(path, method: "POST", accessToken: accessToken, body: nil) { data in
            try decoder.decode(EvaluateNudgesResponse.self, from: data)
        }
    }

    static func processNudges(accessToken: String, limit: Int = 10) async -> Result<ProcessNudgesResponse, BackendApiError> {
        return await runWithFallbackRequest("/v1/moneymoments/nudges/process?limit=\(limit)", method: "POST", accessToken: accessToken, body: nil) { data in
            try decoder.decode(ProcessNudgesResponse.self, from: data)
        }
    }

    static func computeSignal(accessToken: String, asOfDate: String? = nil) async -> Result<ComputeSignalResponse, BackendApiError> {
        let path = asOfDate.map { "/v1/moneymoments/signals/compute?as_of_date=\($0)" } ?? "/v1/moneymoments/signals/compute"
        return await runWithFallbackRequest(path, method: "POST", accessToken: accessToken, body: nil) { data in
            try decoder.decode(ComputeSignalResponse.self, from: data)
        }
    }
}
