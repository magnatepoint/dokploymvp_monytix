//
//  MonytixMotion.swift
//  ios_monytix
//
//  Created by santosh on 06/03/26.
//

import SwiftUI

// MARK: - Animation Tokens

enum MonytixMotion {
    // MARK: Durations
    static let fast  = 0.18
    static let base  = 0.28
    static let slow  = 0.45
    static let xslow = 0.65

    // MARK: Animation Curves
    static let easeOut = Animation.timingCurve(0.2, 0.8, 0.2, 1.0, duration: base)
    static let spring  = Animation.spring(response: 0.45, dampingFraction: 0.85)
    static let bouncy  = Animation.spring(response: 0.35, dampingFraction: 0.7)
    static let smooth  = Animation.easeInOut(duration: base)
}

// MARK: - Entrance Animation Modifier

struct Enter: ViewModifier {
    let delay: Double
    @State private var show = false

    func body(content: Content) -> some View {
        content
            .opacity(show ? 1 : 0)
            .offset(y: show ? 0 : 14)
            .onAppear {
                withAnimation(MonytixMotion.easeOut.delay(delay)) {
                    show = true
                }
            }
    }
}

extension View {
    /// Adds a subtle slide-up entrance animation
    func enter(delay: Double = 0) -> some View {
        modifier(Enter(delay: delay))
    }
}

// MARK: - Glow Pulse Effect

struct GlowPulse: ViewModifier {
    @State private var on = false
    
    func body(content: Content) -> some View {
        content
            .shadow(color: MonytixTheme.cyan1.opacity(on ? 0.28 : 0.12), radius: on ? 18 : 10)
            .scaleEffect(on ? 1.02 : 0.98)
            .onAppear {
                withAnimation(.easeInOut(duration: 1.6).repeatForever(autoreverses: true)) {
                    on = true
                }
            }
    }
}

extension View {
    /// Adds AI-feel pulsing glow effect
    func glowPulse() -> some View {
        modifier(GlowPulse())
    }
}

// MARK: - Shimmer Effect (Loading State)

struct Shimmer: ViewModifier {
    @State private var phase: CGFloat = 0
    
    func body(content: Content) -> some View {
        content
            .overlay(
                LinearGradient(
                    colors: [
                        .clear,
                        MonytixTheme.cyan1.opacity(0.3),
                        .clear
                    ],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .offset(x: phase)
                .mask(content)
            )
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    phase = 300
                }
            }
    }
}

extension View {
    /// Adds shimmer loading effect
    func shimmer() -> some View {
        modifier(Shimmer())
    }
}

// MARK: - Press Scale Effect

struct PressScale: ViewModifier {
    @State private var isPressed = false
    
    func body(content: Content) -> some View {
        content
            .scaleEffect(isPressed ? 0.96 : 1.0)
            .animation(MonytixMotion.spring, value: isPressed)
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in isPressed = true }
                    .onEnded { _ in isPressed = false }
            )
    }
}

extension View {
    /// Adds press feedback animation
    func pressScale() -> some View {
        modifier(PressScale())
    }
}

// MARK: - Glow Shadow

extension View {
    /// Adds cyan glow shadow (static)
    func glowShadow(radius: CGFloat = 12, opacity: Double = 0.3) -> some View {
        self.shadow(color: MonytixTheme.cyan1.opacity(opacity), radius: radius)
    }
    
    /// Adds custom color glow
    func glowShadow(color: Color, radius: CGFloat = 12, opacity: Double = 0.3) -> some View {
        self.shadow(color: color.opacity(opacity), radius: radius)
    }
}
