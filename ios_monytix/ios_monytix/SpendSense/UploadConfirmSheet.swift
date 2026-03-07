//
//  UploadConfirmSheet.swift
//  ios_monytix
//
//  Shown after user picks a file: optional PDF password, then Upload/Cancel.
//

import SwiftUI

struct UploadConfirmSheet: View {
    @Binding var isPresented: Bool
    let filename: String
    var isPDF: Bool { filename.lowercased().hasSuffix(".pdf") }
    let isUploading: Bool
    let onUpload: (String?) -> Void
    let onCancel: () -> Void

    @State private var passwordText = ""

    var body: some View {
        NavigationStack {
            ZStack {
                MonytixTheme.bg.ignoresSafeArea()
                VStack(alignment: .leading, spacing: MonytixSpace.lg) {
                    Text("Upload \"\(filename)\"")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(MonytixTheme.text1)

                    TrustBannerUpload()

                    if isPDF {
                        VStack(alignment: .leading, spacing: MonytixSpace.xs) {
                            Text("PDF password (if protected)")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundStyle(MonytixTheme.text2)
                            SecureField("Leave blank if not password-protected", text: $passwordText)
                                .textFieldStyle(.roundedBorder)
                                .autocapitalization(.none)
                                .autocorrectionDisabled()
                        }
                    }

                    HStack(spacing: MonytixSpace.md) {
                        Button("Cancel") {
                            onCancel()
                            isPresented = false
                        }
                        .foregroundStyle(MonytixTheme.text2)
                        Spacer()
                        Button {
                            onUpload(passwordText.isEmpty ? nil : passwordText)
                            isPresented = false
                        } label: {
                            if isUploading {
                                ProgressView()
                                    .tint(MonytixTheme.onAccent)
                            } else {
                                Text("Upload")
                            }
                        }
                        .padding(.horizontal, MonytixSpace.lg)
                        .padding(.vertical, MonytixSpace.sm)
                        .background(MonytixTheme.cyan1)
                        .foregroundStyle(MonytixTheme.onAccent)
                        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.buttonRadius))
                        .disabled(isUploading)
                    }
                }
                .padding(MonytixSpace.lg)
            }
            .navigationTitle("Upload statement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(MonytixTheme.bg, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        onCancel()
                        isPresented = false
                    }
                    .foregroundStyle(MonytixTheme.cyan1)
                }
            }
        }
    }
}
