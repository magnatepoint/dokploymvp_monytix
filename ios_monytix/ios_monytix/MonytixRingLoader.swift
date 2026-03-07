//
//  MonytixRingLoader.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

/// Premium branded loading spinner with segmented ring
struct MonytixRingLoader: View {
    @State private var rotation: Double = 0
    @State private var scale: CGFloat = 1.0
    
    let size: CGFloat
    let lineWidth: CGFloat
    
    init(size: CGFloat = 44, lineWidth: CGFloat = 4) {
        self.size = size
        self.lineWidth = lineWidth
    }
    
    var body: some View {
        ZStack {
            // Background ring
            Circle()
                .stroke(MonytixTheme.stroke.opacity(0.3), lineWidth: lineWidth)
                .frame(width: size, height: size)
            
            // Animated gradient ring
            Circle()
                .trim(from: 0, to: 0.7)
                .stroke(
                    MonytixTheme.gradientPrimary,
                    style: StrokeStyle(
                        lineWidth: lineWidth,
                        lineCap: .round
                    )
                )
                .frame(width: size, height: size)
                .rotationEffect(.degrees(rotation))
                .scaleEffect(scale)
        }
        .onAppear {
            withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                rotation = 360
            }
            withAnimation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true)) {
                scale = 1.05
            }
        }
        .glowShadow(radius: 16, opacity: 0.4)
    }
}

/// Compact inline loader with text
struct MonytixInlineLoader: View {
    let message: String
    
    var body: some View {
        HStack(spacing: 12) {
            MonytixRingLoader(size: 20, lineWidth: 3)
            
            Text(message)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(MonytixTheme.text2)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(MonytixTheme.surface2)
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }
}

/// Full-screen loading overlay
struct MonytixLoadingOverlay: View {
    let message: String?
    
    init(message: String? = nil) {
        self.message = message
    }
    
    var body: some View {
        ZStack {
            MonytixTheme.bg.opacity(0.85)
                .ignoresSafeArea()
            
            VStack(spacing: 20) {
                MonytixRingLoader(size: 60, lineWidth: 5)
                
                if let message = message {
                    Text(message)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(MonytixTheme.text1)
                }
            }
            .padding(32)
            .background(MonytixTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
            .overlay(
                RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                    .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
            )
        }
    }
}

// MARK: - Skeleton Loading Card

struct SkeletonCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Title skeleton
            RoundedRectangle(cornerRadius: 6)
                .fill(MonytixTheme.stroke.opacity(0.4))
                .frame(width: 120, height: 16)
                .shimmer()
            
            // Content skeleton
            RoundedRectangle(cornerRadius: 6)
                .fill(MonytixTheme.stroke.opacity(0.4))
                .frame(height: 24)
                .shimmer()
            
            // Sub content skeleton
            HStack {
                RoundedRectangle(cornerRadius: 6)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(width: 80, height: 12)
                    .shimmer()
                
                Spacer()
                
                RoundedRectangle(cornerRadius: 6)
                    .fill(MonytixTheme.stroke.opacity(0.4))
                    .frame(width: 60, height: 12)
                    .shimmer()
            }
        }
        .padding(MonytixSpace.md)
        .background(MonytixTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: MonytixShape.cardRadius))
        .overlay(
            RoundedRectangle(cornerRadius: MonytixShape.cardRadius)
                .stroke(MonytixTheme.stroke.opacity(0.6), lineWidth: 1)
        )
    }
}

// MARK: - View Extension for Loading States

extension View {
    /// Adds a loading overlay
    func loadingOverlay(isLoading: Bool, message: String? = nil) -> some View {
        ZStack {
            self
            
            if isLoading {
                MonytixLoadingOverlay(message: message)
            }
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        MonytixTheme.bg.ignoresSafeArea()
        
        ScrollView {
            VStack(spacing: 30) {
                // Ring loader
                MonytixRingLoader()
                
                // Inline loader
                MonytixInlineLoader(message: "Analyzing transactions...")
                
                // Skeleton card
                SkeletonCard()
                
                Divider()
                
                // Full overlay demo button
                Button("Show Overlay") {}
                    .buttonStyle(MonytixPrimaryButton())
            }
            .padding()
        }
    }
}
