//
//  ApiModels.swift
//  ios
//
//  API response models - matches APK BackendApi data classes
//

import Foundation

// MARK: - Health & Config
struct HealthResponse: Codable {
    let status: String
}

struct ConfigResponse: Codable {
    let minVersionCode: Int?
    let appStoreUrl: String?
    let featureFlags: [String: Bool]?
    let maintenanceMode: Bool?

    enum CodingKeys: String, CodingKey {
        case minVersionCode = "min_version_code"
        case appStoreUrl = "app_store_url"
        case featureFlags = "feature_flags"
        case maintenanceMode = "maintenance_mode"
    }
}

struct SessionResponse: Codable {
    let userId: String
    let email: String?
    let role: String?

    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case email, role
    }
}

// MARK: - KPIs
struct KpiResponse: Codable {
    let month: String?
    let incomeAmount: Double?
    let needsAmount: Double?
    let wantsAmount: Double?
    let assetsAmount: Double?
    let totalDebitsAmount: Double?
    let topCategories: [CategorySpendKpi]?

    enum CodingKeys: String, CodingKey {
        case month
        case incomeAmount = "income_amount"
        case needsAmount = "needs_amount"
        case wantsAmount = "wants_amount"
        case assetsAmount = "assets_amount"
        case totalDebitsAmount = "total_debits_amount"
        case topCategories = "top_categories"
    }
}

struct CategorySpendKpi: Codable {
    let categoryCode: String?
    let categoryName: String?
    let txnCount: Int?
    let spendAmount: Double?
    let incomeAmount: Double?
    let deltaPct: Double?

    enum CodingKeys: String, CodingKey {
        case categoryCode = "category_code"
        case categoryName = "category_name"
        case txnCount = "txn_count"
        case spendAmount = "spend_amount"
        case incomeAmount = "income_amount"
        case deltaPct = "delta_pct"
    }
}

// MARK: - Accounts
struct AccountsResponse: Codable {
    let accounts: [AccountItemResponse]

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        accounts = try c.decodeIfPresent([AccountItemResponse].self, forKey: .accounts) ?? []
    }

    enum CodingKeys: String, CodingKey {
        case accounts
    }
}

struct AccountItemResponse: Codable {
    let id: String
    let bankCode: String?
    let bankName: String?
    let accountNumber: String?
    let balance: Double
    let accountType: String?
    let transactionCount: Int?
    let lastTxnDate: String?

    enum CodingKeys: String, CodingKey {
        case id
        case bankCode = "bank_code"
        case bankName = "bank_name"
        case accountNumber = "account_number"
        case balance
        case accountType = "account_type"
        case transactionCount = "transaction_count"
        case lastTxnDate = "last_txn_date"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id) ?? ""
        bankCode = try c.decodeIfPresent(String.self, forKey: .bankCode)
        bankName = try c.decodeIfPresent(String.self, forKey: .bankName)
        accountNumber = try c.decodeIfPresent(String.self, forKey: .accountNumber)
        balance = try c.decodeIfPresent(Double.self, forKey: .balance) ?? 0
        accountType = try c.decodeIfPresent(String.self, forKey: .accountType)
        transactionCount = try c.decodeIfPresent(Int.self, forKey: .transactionCount)
        lastTxnDate = try c.decodeIfPresent(String.self, forKey: .lastTxnDate)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encodeIfPresent(bankCode, forKey: .bankCode)
        try c.encodeIfPresent(bankName, forKey: .bankName)
        try c.encodeIfPresent(accountNumber, forKey: .accountNumber)
        try c.encode(balance, forKey: .balance)
        try c.encodeIfPresent(accountType, forKey: .accountType)
        try c.encodeIfPresent(transactionCount, forKey: .transactionCount)
        try c.encodeIfPresent(lastTxnDate, forKey: .lastTxnDate)
    }
}

// MARK: - Transactions
struct TransactionRecordResponse: Codable {
    let txnId: String
    let txnDate: String
    let merchant: String?
    let category: String?
    let subcategory: String?
    let bankCode: String?
    let channel: String?
    let amount: Double
    let direction: String
    let confidence: Double?

    enum CodingKeys: String, CodingKey {
        case txnId = "txn_id"
        case txnDate = "txn_date"
        case merchant, category, subcategory
        case bankCode = "bank_code"
        case channel, amount, direction, confidence
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        txnId = try c.decodeIfPresent(String.self, forKey: .txnId) ?? ""
        txnDate = try c.decodeIfPresent(String.self, forKey: .txnDate) ?? ""
        merchant = try c.decodeIfPresent(String.self, forKey: .merchant)
        category = try c.decodeIfPresent(String.self, forKey: .category)
        subcategory = try c.decodeIfPresent(String.self, forKey: .subcategory)
        bankCode = try c.decodeIfPresent(String.self, forKey: .bankCode)
        channel = try c.decodeIfPresent(String.self, forKey: .channel)
        amount = try c.decodeIfPresent(Double.self, forKey: .amount) ?? 0
        direction = try c.decodeIfPresent(String.self, forKey: .direction) ?? "debit"
        confidence = try c.decodeIfPresent(Double.self, forKey: .confidence)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(txnId, forKey: .txnId)
        try c.encode(txnDate, forKey: .txnDate)
        try c.encodeIfPresent(merchant, forKey: .merchant)
        try c.encodeIfPresent(category, forKey: .category)
        try c.encodeIfPresent(subcategory, forKey: .subcategory)
        try c.encodeIfPresent(bankCode, forKey: .bankCode)
        try c.encodeIfPresent(channel, forKey: .channel)
        try c.encode(amount, forKey: .amount)
        try c.encode(direction, forKey: .direction)
        try c.encodeIfPresent(confidence, forKey: .confidence)
    }
}

struct TransactionListResponse: Codable {
    let transactions: [TransactionRecordResponse]
    let total: Int?
    let page: Int?
    let pageSize: Int?

    enum CodingKeys: String, CodingKey {
        case transactions, total, page
        case pageSize = "page_size"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        transactions = try c.decodeIfPresent([TransactionRecordResponse].self, forKey: .transactions) ?? []
        total = try c.decodeIfPresent(Int.self, forKey: .total)
        page = try c.decodeIfPresent(Int.self, forKey: .page)
        pageSize = try c.decodeIfPresent(Int.self, forKey: .pageSize)
    }
}

struct TransactionCreateRequest: Codable {
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
        case description, amount, direction
        case categoryCode = "category_code"
        case subcategoryCode = "subcategory_code"
        case channel
        case accountRef = "account_ref"
    }
}

// MARK: - Insights
struct InsightsResponse: Codable {
    let timeSeries: [TimeSeriesPoint]?
    let categoryBreakdown: [CategoryBreakdownItem]?
    let spendingTrends: [SpendingTrendItem]

    enum CodingKeys: String, CodingKey {
        case timeSeries = "time_series"
        case categoryBreakdown = "category_breakdown"
        case spendingTrends = "spending_trends"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        timeSeries = try c.decodeIfPresent([TimeSeriesPoint].self, forKey: .timeSeries)
        categoryBreakdown = try c.decodeIfPresent([CategoryBreakdownItem].self, forKey: .categoryBreakdown)
        spendingTrends = try c.decodeIfPresent([SpendingTrendItem].self, forKey: .spendingTrends) ?? []
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encodeIfPresent(timeSeries, forKey: .timeSeries)
        try c.encodeIfPresent(categoryBreakdown, forKey: .categoryBreakdown)
        try c.encode(spendingTrends, forKey: .spendingTrends)
    }
}

struct TimeSeriesPoint: Codable {
    let date: String
    let value: Double
    let label: String?

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        date = try c.decodeIfPresent(String.self, forKey: .date) ?? ""
        value = try c.decodeIfPresent(Double.self, forKey: .value) ?? 0
        label = try c.decodeIfPresent(String.self, forKey: .label)
    }

    enum CodingKeys: String, CodingKey {
        case date, value, label
    }
}

struct CategoryBreakdownItem: Codable {
    let categoryCode: String?
    let categoryName: String?
    let amount: Double
    let percentage: Double
    let transactionCount: Int?
    let avgTransaction: Double?

    enum CodingKeys: String, CodingKey {
        case categoryCode = "category_code"
        case categoryName = "category_name"
        case amount, percentage
        case transactionCount = "transaction_count"
        case avgTransaction = "avg_transaction"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        categoryCode = try c.decodeIfPresent(String.self, forKey: .categoryCode)
        categoryName = try c.decodeIfPresent(String.self, forKey: .categoryName)
        amount = try c.decodeIfPresent(Double.self, forKey: .amount) ?? 0
        percentage = try c.decodeIfPresent(Double.self, forKey: .percentage) ?? 0
        transactionCount = try c.decodeIfPresent(Int.self, forKey: .transactionCount)
        avgTransaction = try c.decodeIfPresent(Double.self, forKey: .avgTransaction)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encodeIfPresent(categoryCode, forKey: .categoryCode)
        try c.encodeIfPresent(categoryName, forKey: .categoryName)
        try c.encode(amount, forKey: .amount)
        try c.encode(percentage, forKey: .percentage)
        try c.encodeIfPresent(transactionCount, forKey: .transactionCount)
        try c.encodeIfPresent(avgTransaction, forKey: .avgTransaction)
    }
}

struct SpendingTrendItem: Codable {
    let period: String
    let income: Double
    let expenses: Double
    let net: Double
    let needs: Double?
    let wants: Double?
    let assets: Double?

    enum CodingKeys: String, CodingKey {
        case period, income, expenses, net, needs, wants, assets
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        period = try c.decodeIfPresent(String.self, forKey: .period) ?? ""
        income = try c.decodeIfPresent(Double.self, forKey: .income) ?? 0
        expenses = try c.decodeIfPresent(Double.self, forKey: .expenses) ?? 0
        net = try c.decodeIfPresent(Double.self, forKey: .net) ?? 0
        needs = try c.decodeIfPresent(Double.self, forKey: .needs)
        wants = try c.decodeIfPresent(Double.self, forKey: .wants)
        assets = try c.decodeIfPresent(Double.self, forKey: .assets)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(period, forKey: .period)
        try c.encode(income, forKey: .income)
        try c.encode(expenses, forKey: .expenses)
        try c.encode(net, forKey: .net)
        try c.encodeIfPresent(needs, forKey: .needs)
        try c.encodeIfPresent(wants, forKey: .wants)
        try c.encodeIfPresent(assets, forKey: .assets)
    }
}

// MARK: - Goals
struct GoalResponse: Codable, Hashable {
    let goalId: String
    let goalCategory: String?
    let goalName: String
    let goalType: String?
    let estimatedCost: Double
    let targetDate: String?
    let currentSavings: Double
    let status: String?
    let notes: String?
    let createdAt: String?
    let updatedAt: String?

    enum CodingKeys: String, CodingKey {
        case goalId = "goal_id"
        case goalCategory = "goal_category"
        case goalName = "goal_name"
        case goalType = "goal_type"
        case estimatedCost = "estimated_cost"
        case targetDate = "target_date"
        case currentSavings = "current_savings"
        case status, notes
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        goalId = try c.decodeIfPresent(String.self, forKey: .goalId) ?? ""
        goalCategory = try c.decodeIfPresent(String.self, forKey: .goalCategory)
        goalName = try c.decodeIfPresent(String.self, forKey: .goalName) ?? ""
        goalType = try c.decodeIfPresent(String.self, forKey: .goalType)
        estimatedCost = try c.decodeIfPresent(Double.self, forKey: .estimatedCost) ?? 0
        targetDate = try c.decodeIfPresent(String.self, forKey: .targetDate)
        currentSavings = try c.decodeIfPresent(Double.self, forKey: .currentSavings) ?? 0
        status = try c.decodeIfPresent(String.self, forKey: .status)
        notes = try c.decodeIfPresent(String.self, forKey: .notes)
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
        updatedAt = try c.decodeIfPresent(String.self, forKey: .updatedAt)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(goalId, forKey: .goalId)
        try c.encodeIfPresent(goalCategory, forKey: .goalCategory)
        try c.encode(goalName, forKey: .goalName)
        try c.encodeIfPresent(goalType, forKey: .goalType)
        try c.encode(estimatedCost, forKey: .estimatedCost)
        try c.encodeIfPresent(targetDate, forKey: .targetDate)
        try c.encode(currentSavings, forKey: .currentSavings)
        try c.encodeIfPresent(status, forKey: .status)
        try c.encodeIfPresent(notes, forKey: .notes)
        try c.encodeIfPresent(createdAt, forKey: .createdAt)
        try c.encodeIfPresent(updatedAt, forKey: .updatedAt)
    }
}

struct GoalProgressItem: Codable {
    let goalId: String
    let goalName: String
    let progressPct: Double
    let currentSavingsClose: Double
    let remainingAmount: Double
    let projectedCompletionDate: String?
    let monthlyRequired: Double?
    let monthlyAvgContribution: Double?
    let paceDescription: String?
    let daysToTarget: Int?

    enum CodingKeys: String, CodingKey {
        case goalId = "goal_id"
        case goalName = "goal_name"
        case progressPct = "progress_pct"
        case currentSavingsClose = "current_savings_close"
        case remainingAmount = "remaining_amount"
        case projectedCompletionDate = "projected_completion_date"
        case monthlyRequired = "monthly_required"
        case monthlyAvgContribution = "monthly_avg_contribution"
        case paceDescription = "pace_description"
        case daysToTarget = "days_to_target"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        goalId = try c.decodeIfPresent(String.self, forKey: .goalId) ?? ""
        goalName = try c.decodeIfPresent(String.self, forKey: .goalName) ?? ""
        progressPct = try c.decodeIfPresent(Double.self, forKey: .progressPct) ?? 0
        currentSavingsClose = try c.decodeIfPresent(Double.self, forKey: .currentSavingsClose) ?? 0
        remainingAmount = try c.decodeIfPresent(Double.self, forKey: .remainingAmount) ?? 0
        projectedCompletionDate = try c.decodeIfPresent(String.self, forKey: .projectedCompletionDate)
        monthlyRequired = try c.decodeIfPresent(Double.self, forKey: .monthlyRequired)
        monthlyAvgContribution = try c.decodeIfPresent(Double.self, forKey: .monthlyAvgContribution)
        paceDescription = try c.decodeIfPresent(String.self, forKey: .paceDescription)
        daysToTarget = try c.decodeIfPresent(Int.self, forKey: .daysToTarget)
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(goalId, forKey: .goalId)
        try c.encode(goalName, forKey: .goalName)
        try c.encode(progressPct, forKey: .progressPct)
        try c.encode(currentSavingsClose, forKey: .currentSavingsClose)
        try c.encode(remainingAmount, forKey: .remainingAmount)
        try c.encodeIfPresent(projectedCompletionDate, forKey: .projectedCompletionDate)
        try c.encodeIfPresent(monthlyRequired, forKey: .monthlyRequired)
        try c.encodeIfPresent(monthlyAvgContribution, forKey: .monthlyAvgContribution)
        try c.encodeIfPresent(paceDescription, forKey: .paceDescription)
        try c.encodeIfPresent(daysToTarget, forKey: .daysToTarget)
    }
}

struct GoalsProgressResponse: Codable {
    let goals: [GoalProgressItem]
}

struct CreateGoalResponse: Codable {
    let status: String
    let goalId: String?

    enum CodingKeys: String, CodingKey {
        case status
        case goalId = "goal_id"
    }
}

// MARK: - Budget
struct BudgetStateResponse: Codable {
    let committedPlan: BudgetStateCommittedPlan?
    let actual: BudgetStateActual?
    let deviation: BudgetStateDeviation?
    let plan: BudgetStatePlan?

    enum CodingKeys: String, CodingKey {
        case committedPlan = "committed_plan"
        case actual, deviation, plan
    }
}

struct BudgetStateCommittedPlan: Codable {
    let planCode: String?
    let month: String?
    let needs: Double?
    let wants: Double?
    let savings: Double?

    enum CodingKeys: String, CodingKey {
        case planCode = "plan_code"
        case month, needs, wants, savings
    }
}

struct BudgetStateActual: Codable {
    let needs: Double?
    let wants: Double?
    let savings: Double?
}

struct BudgetStateDeviation: Codable {
    let needs: Double?
    let wants: Double?
    let savings: Double?
}

struct BudgetStatePlan: Codable {
    let needs: Double?
    let wants: Double?
    let savings: Double?
}

struct BudgetVarianceResponse: Codable {
    let variance: [BudgetVariance]?
}

struct BudgetVariance: Codable {
    let category: String?
    let planned: Double?
    let actual: Double?
    let variance: Double?
    let variancePct: Double?

    enum CodingKeys: String, CodingKey {
        case category, planned, actual, variance
        case variancePct = "variance_pct"
    }
}

struct BudgetRecommendation: Codable {
    let planCode: String?
    let planName: String?
    let needs: Double?
    let wants: Double?
    let savings: Double?
}

struct BudgetRecommendationsResponse: Codable {
    let recommendations: [BudgetRecommendation]?
}

struct CommittedBudget: Codable {
    let planCode: String?
    let month: String?
    let needs: Double?
    let wants: Double?
    let savings: Double?

    enum CodingKeys: String, CodingKey {
        case planCode = "plan_code"
        case month, needs, wants, savings
    }
}

struct CommittedBudgetResponse: Codable {
    let budget: CommittedBudget
}

struct BudgetCommitRequest: Codable {
    let planCode: String
    let month: String?

    enum CodingKeys: String, CodingKey {
        case planCode = "plan_code"
        case month
    }
}

// MARK: - MoneyMoments
struct MoneyMoment: Codable {
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
        case value, label
        case insightText = "insight_text"
        case confidence
        case createdAt = "created_at"
    }
}

struct Nudge: Codable {
    let deliveryId: String?
    let userId: String?
    let ruleId: String?
    let templateCode: String?
    let channel: String?
    let sentAt: String?
    let sendStatus: String?
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
        case title, body
        case ctaText = "cta_text"
        case ctaDeeplink = "cta_deeplink"
        case ruleName = "rule_name"
    }
}

struct MoneyMomentsResponse: Codable {
    let moments: [MoneyMoment]?
}

struct NudgesResponse: Codable {
    let nudges: [Nudge]?
}

// MARK: - Upload
struct UploadBatchResponse: Codable {
    let uploadId: String
    let batchId: String?
    let userId: String?
    let sourceType: String?
    let status: String
    let createdAt: String?
    let errorMessage: String?

    enum CodingKeys: String, CodingKey {
        case uploadId = "upload_id"
        case batchId = "batch_id"
        case userId = "user_id"
        case sourceType = "source_type"
        case status
        case createdAt = "created_at"
        case errorMessage = "error_message"
    }
}

// MARK: - Delete
struct DeleteDataResponse: Codable {
    let transactionsDeleted: Int?
    let batchesDeleted: Int?
    let stagingDeleted: Int?
    let overridesDeleted: Int?

    enum CodingKeys: String, CodingKey {
        case transactionsDeleted = "transactions_deleted"
        case batchesDeleted = "batches_deleted"
        case stagingDeleted = "staging_deleted"
        case overridesDeleted = "overrides_deleted"
    }
}

// MARK: - Categories
struct CategoryResponse: Codable {
    let categoryCode: String?
    let categoryName: String?
    let isCustom: Bool?
    let txnType: String?

    enum CodingKeys: String, CodingKey {
        case categoryCode = "category_code"
        case categoryName = "category_name"
        case isCustom = "is_custom"
        case txnType = "txn_type"
    }
}

// MARK: - Available Months
struct AvailableMonthsResponse: Codable {
    let data: [String]?
}
