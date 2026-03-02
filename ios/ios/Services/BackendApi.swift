//
//  BackendApi.swift
//  ios
//
//  Backend API client - matches APK BackendApi.kt
//

import Foundation

enum BackendConfig {
    static let defaultBaseUrl = "https://backend.monytix.ai"
    static let backupBaseUrl = "https://api.monytix.ai"
}

@MainActor
final class BackendApi {
    private let baseUrl: String
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init(baseUrl: String? = nil) {
        let url = baseUrl ?? (Bundle.main.object(forInfoDictionaryKey: "BACKEND_URL") as? String) ?? BackendConfig.defaultBaseUrl
        self.baseUrl = url.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        self.decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        self.encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
    }

    private var spendsenseBase: String { "\(baseUrl)/v1/spendsense" }
    private var moneymomentsBase: String { "\(baseUrl)/v1/moneymoments" }
    private var budgetBase: String { "\(baseUrl)/v1/budget" }
    private var goalsBase: String { "\(baseUrl)/v1/goals" }

    private func isRetryable(_ error: Error) -> Bool {
        if case .httpError(let code, _) = error as? ApiError, code >= 500 { return true }
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            let c = ns.code
            return c == NSURLErrorTimedOut || c == NSURLErrorCannotConnectToHost || c == NSURLErrorNetworkConnectionLost || c == NSURLErrorNotConnectedToInternet
        }
        return false
    }

    private func backupUrl(for url: URL) -> URL? {
        guard let backup = URL(string: BackendConfig.backupBaseUrl), baseUrl != BackendConfig.backupBaseUrl else { return nil }
        var c = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        c.scheme = backup.scheme
        c.host = backup.host
        c.port = backup.port
        return c.url
    }

    private func requestWithBackup<T: Decodable>(
        _ url: URL,
        method: String = "GET",
        body: Data? = nil,
        accessToken: String? = nil
    ) async throws -> T {
        do {
            return try await request(url, method: method, body: body, accessToken: accessToken)
        } catch {
            guard isRetryable(error), let backup = backupUrl(for: url) else { throw error }
            return try await request(backup, method: method, body: body, accessToken: accessToken)
        }
    }

    private func requestVoidWithBackup(
        _ url: URL,
        method: String = "POST",
        body: Data? = nil,
        accessToken: String? = nil
    ) async throws {
        do {
            try await requestVoid(url, method: method, body: body, accessToken: accessToken)
        } catch {
            guard isRetryable(error), let backup = backupUrl(for: url) else { throw error }
            try await requestVoid(backup, method: method, body: body, accessToken: accessToken)
        }
    }

    private func request<T: Decodable>(
        _ url: URL,
        method: String = "GET",
        body: Data? = nil,
        accessToken: String? = nil
    ) async throws -> T {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ApiError.invalidResponse }
        guard (200...299).contains(http.statusCode) else {
            throw ApiError.httpError(statusCode: http.statusCode, body: String(data: data, encoding: .utf8))
        }
        guard !data.isEmpty else {
            throw ApiError.decodingError(endpoint: url.absoluteString, path: "", underlying: DecodingError.dataCorrupted(.init(codingPath: [], debugDescription: "Empty response body")), snippet: "")
        }
        do {
            return try decoder.decode(T.self, from: data)
        } catch let error as DecodingError {
            let path: String
            switch error {
            case .keyNotFound(_, let context), .valueNotFound(_, let context),
                 .typeMismatch(_, let context), .dataCorrupted(let context):
                path = context.codingPath.map { $0.stringValue }.joined(separator: ".")
            }
            let snippet = String(data: data.prefix(200), encoding: .utf8) ?? ""
            throw ApiError.decodingError(endpoint: url.absoluteString, path: path, underlying: error, snippet: snippet)
        }
    }

    private func requestVoid(
        _ url: URL,
        method: String = "POST",
        body: Data? = nil,
        accessToken: String? = nil
    ) async throws {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.httpBody = body

        let (_, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw ApiError.invalidResponse }
        guard (200...299).contains(http.statusCode) else { throw ApiError.httpError(statusCode: http.statusCode, body: nil) }
    }

    // MARK: - Health & Config
    func healthCheck() async throws -> HealthResponse {
        let url = URL(string: "\(baseUrl)/health")!
        return try await requestWithBackup(url)
    }

    func getConfig() async throws -> ConfigResponse {
        let url = URL(string: "\(baseUrl)/config")!
        return try await requestWithBackup(url)
    }

    // MARK: - KPIs
    func getKpis(accessToken: String, month: String? = nil) async throws -> KpiResponse {
        var urlString = "\(spendsenseBase)/kpis"
        if let m = month { urlString += "?month=\(m)" }
        let url = URL(string: urlString)!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Accounts
    func getAccounts(accessToken: String) async throws -> AccountsResponse {
        let url = URL(string: "\(spendsenseBase)/accounts")!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Transactions
    func getTransactions(
        accessToken: String,
        limit: Int = 25,
        offset: Int = 0,
        search: String? = nil,
        categoryCode: String? = nil,
        startDate: String? = nil,
        endDate: String? = nil
    ) async throws -> TransactionListResponse {
        var components = URLComponents(string: "\(spendsenseBase)/transactions")!
        var queryItems = [URLQueryItem(name: "limit", value: "\(limit)"), URLQueryItem(name: "offset", value: "\(offset)")]
        if let s = search { queryItems.append(URLQueryItem(name: "search", value: s)) }
        if let c = categoryCode { queryItems.append(URLQueryItem(name: "category_code", value: c)) }
        if let s = startDate { queryItems.append(URLQueryItem(name: "start_date", value: s)) }
        if let e = endDate { queryItems.append(URLQueryItem(name: "end_date", value: e)) }
        components.queryItems = queryItems
        let url = components.url!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Insights
    func getInsights(accessToken: String, startDate: String? = nil, endDate: String? = nil) async throws -> InsightsResponse {
        var components = URLComponents(string: "\(spendsenseBase)/insights")!
        var queryItems: [URLQueryItem] = []
        if let s = startDate { queryItems.append(URLQueryItem(name: "start_date", value: s)) }
        if let e = endDate { queryItems.append(URLQueryItem(name: "end_date", value: e)) }
        if !queryItems.isEmpty { components.queryItems = queryItems }
        let url = components.url!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Goals
    func getGoalsProgress(accessToken: String) async throws -> GoalsProgressResponse {
        let url = URL(string: "\(goalsBase)/progress")!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    func getUserGoals(accessToken: String) async throws -> [GoalResponse] {
        let url = URL(string: "\(goalsBase)")!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    func createGoal(
        accessToken: String,
        goalCategory: String,
        goalName: String,
        estimatedCost: Double,
        targetDate: String? = nil,
        currentSavings: Double = 0,
        goalType: String? = nil,
        importance: Int? = nil
    ) async throws -> CreateGoalResponse {
        var body: [String: Any] = [
            "goal_category": goalCategory,
            "goal_name": goalName,
            "estimated_cost": estimatedCost,
            "current_savings": currentSavings
        ]
        if let t = targetDate { body["target_date"] = t }
        if let g = goalType { body["goal_type"] = g }
        if let i = importance { body["importance"] = i }
        let data = try JSONSerialization.data(withJSONObject: body)
        let url = URL(string: "\(goalsBase)")!
        return try await requestWithBackup(url, method: "POST", body: data, accessToken: accessToken)
    }

    func updateGoal(
        accessToken: String,
        goalId: String,
        estimatedCost: Double? = nil,
        targetDate: String? = nil,
        currentSavings: Double? = nil
    ) async throws -> GoalResponse {
        var body: [String: Any] = [:]
        if let e = estimatedCost { body["estimated_cost"] = e }
        if let t = targetDate { body["target_date"] = t }
        if let c = currentSavings { body["current_savings"] = c }
        let data = try JSONSerialization.data(withJSONObject: body)
        let url = URL(string: "\(goalsBase)/\(goalId)")!
        return try await requestWithBackup(url, method: "PUT", body: data, accessToken: accessToken)
    }

    func deleteGoal(accessToken: String, goalId: String) async throws {
        let url = URL(string: "\(goalsBase)/\(goalId)")!
        try await requestVoidWithBackup(url, method: "DELETE", accessToken: accessToken)
    }

    // MARK: - Budget
    func getBudgetState(accessToken: String, month: String? = nil) async throws -> BudgetStateResponse {
        var urlString = "\(budgetBase)/state"
        if let m = month, !m.isEmpty { urlString += "?month=\(m)-01" }
        let url = URL(string: urlString)!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    func getBudgetVariance(accessToken: String, month: String? = nil) async throws -> BudgetVarianceResponse {
        var urlString = "\(budgetBase)/variance"
        if let m = month { urlString += "?month=\(m)" }
        let url = URL(string: urlString)!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    func getBudgetRecommendations(accessToken: String, month: String? = nil) async throws -> BudgetRecommendationsResponse {
        var urlString = "\(budgetBase)/recommendations"
        if let m = month { urlString += "?month=\(m)" }
        let url = URL(string: urlString)!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - MoneyMoments
    func getMoments(accessToken: String, month: String? = nil, allMonths: Bool = false) async throws -> MoneyMomentsResponse {
        var components = URLComponents(string: "\(moneymomentsBase)/moments")!
        var queryItems: [URLQueryItem] = []
        if let m = month { queryItems.append(URLQueryItem(name: "month", value: m)) }
        if allMonths { queryItems.append(URLQueryItem(name: "all_months", value: "true")) }
        if !queryItems.isEmpty { components.queryItems = queryItems }
        let url = components.url!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    func getNudges(accessToken: String, limit: Int = 20) async throws -> NudgesResponse {
        let url = URL(string: "\(moneymomentsBase)/nudges?limit=\(limit)")!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Available Months
    func getAvailableMonths(accessToken: String) async throws -> AvailableMonthsResponse {
        let url = URL(string: "\(spendsenseBase)/kpis/available-months")!
        return try await requestWithBackup(url, accessToken: accessToken)
    }

    // MARK: - Delete Data
    func deleteAllData(accessToken: String) async throws -> DeleteDataResponse {
        let url = URL(string: "\(spendsenseBase)/data")!
        return try await requestWithBackup(url, method: "DELETE", accessToken: accessToken)
    }
}

enum ApiError: LocalizedError {
    case invalidResponse
    case httpError(statusCode: Int, body: String?)
    case decodingError(endpoint: String, path: String, underlying: DecodingError, snippet: String)

    var errorDescription: String? {
        switch self {
        case .invalidResponse: return "Invalid response"
        case .httpError(let code, let body): return body ?? "HTTP error \(code)"
        case .decodingError(let endpoint, let path, let underlying, _):
            let apiPath = URL(string: endpoint)?.path ?? endpoint
            let pathInfo = path.isEmpty ? "" : " at \(path)"
            return "\(apiPath)\(pathInfo): \(underlying.localizedDescription)"
        }
    }
}
