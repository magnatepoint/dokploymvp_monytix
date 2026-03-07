//
//  AddTransactionSheet.swift
//  ios_monytix
//
//  Shared sheet to add a manual transaction. Used from MolyConsole and SpendSense.
//

import SwiftUI

struct AddTransactionSheet: View {
    @Binding var isPresented: Bool
    var onSuccess: () -> Void

    @State private var txnDate = Date()
    @State private var merchantName = ""
    @State private var descriptionText = ""
    @State private var amountText = ""
    @State private var direction: String = "debit"
    @State private var categoryCode: String?
    @State private var channel: String?
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var categories: [CategoryResponse] = []
    @State private var channels: [String] = []

    private var amountValue: Double {
        Double(amountText.replacingOccurrences(of: ",", with: "")) ?? 0
    }

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: MonytixSpace.md) {
                        if let err = errorMessage {
                            Text(err)
                                .font(.caption)
                                .foregroundStyle(MonytixTheme.danger)
                        }
                        Group {
                            DatePicker("Date", selection: $txnDate, displayedComponents: .date)
                                .tint(MonytixTheme.cyan1)
                            TextField("Merchant / payee", text: $merchantName)
                                .textFieldStyle(.roundedBorder)
                                .autocapitalization(.words)
                            TextField("Description (optional)", text: $descriptionText)
                                .textFieldStyle(.roundedBorder)
                            TextField("Amount", text: $amountText)
                                .textFieldStyle(.roundedBorder)
                                .keyboardType(.decimalPad)
                            Picker("Direction", selection: $direction) {
                                Text("Debit (expense)").tag("debit")
                                Text("Credit (income)").tag("credit")
                            }
                            .pickerStyle(.segmented)
                            if !categories.isEmpty {
                                Picker("Category (optional)", selection: Binding(
                                    get: { categoryCode ?? "" },
                                    set: { categoryCode = $0.isEmpty ? nil : $0 }
                                )) {
                                    Text("— None —").tag("")
                                    ForEach(Array(categories.enumerated()), id: \.offset) { _, c in
                                        Text(c.categoryName ?? c.categoryCode ?? "Unknown").tag(c.categoryCode ?? "")
                                    }
                                }
                                .pickerStyle(.menu)
                            }
                            if !channels.isEmpty {
                                Picker("Channel (optional)", selection: Binding(
                                    get: { channel ?? "" },
                                    set: { channel = $0.isEmpty ? nil : $0 }
                                )) {
                                    Text("— None —").tag("")
                                    ForEach(channels, id: \.self) { ch in
                                        Text(ch).tag(ch)
                                    }
                                }
                                .pickerStyle(.menu)
                            }
                        }
                        .foregroundStyle(MonytixTheme.text1)

                        Spacer(minLength: MonytixSpace.lg)
                        Button {
                            submit()
                        } label: {
                            HStack {
                                if isSubmitting {
                                    ProgressView()
                                        .tint(MonytixTheme.onAccent)
                                } else {
                                    Text("Add transaction")
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, MonytixSpace.sm)
                            .background(MonytixTheme.cyan1)
                            .foregroundStyle(MonytixTheme.onAccent)
                            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
                        }
                        .disabled(isSubmitting || merchantName.trimmingCharacters(in: .whitespaces).isEmpty || amountValue <= 0)
                    }
                    .padding(MonytixSpace.lg)
                }
            }
            .navigationTitle("Add transaction")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        isPresented = false
                    }
                    .foregroundStyle(MonytixTheme.cyan1)
                }
            }
            .onAppear {
                loadCategoriesAndChannels()
            }
        }
    }

    private func loadCategoriesAndChannels() {
        Task { @MainActor in
            guard let token = await AuthManager.shared.getIdToken() else { return }
            async let catResult = BackendApi.getCategories(accessToken: token)
            async let chResult = BackendApi.getChannels(accessToken: token)
            if case .success(let list) = await catResult { categories = list }
            if case .success(let list) = await chResult { channels = list }
        }
    }

    private func submit() {
        let merchant = merchantName.trimmingCharacters(in: .whitespaces)
        guard !merchant.isEmpty, amountValue > 0 else {
            errorMessage = "Enter merchant and a positive amount."
            return
        }
        errorMessage = nil
        isSubmitting = true
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        let txnDateStr = dateFormatter.string(from: txnDate)
        let request = TransactionCreateRequest(
            txnDate: txnDateStr,
            merchantName: merchant,
            description: descriptionText.isEmpty ? nil : descriptionText,
            amount: amountValue,
            direction: direction,
            categoryCode: categoryCode,
            subcategoryCode: nil,
            channel: channel,
            accountRef: nil
        )
        Task { @MainActor in
            defer { isSubmitting = false }
            guard let token = await AuthManager.shared.getIdToken() else {
                errorMessage = "Please sign in again."
                return
            }
            let result = await BackendApi.createTransaction(accessToken: token, request: request)
            switch result {
            case .success:
                onSuccess()
                isPresented = false
            case .failure(let e):
                errorMessage = e.localizedDescription
            }
        }
    }
}
