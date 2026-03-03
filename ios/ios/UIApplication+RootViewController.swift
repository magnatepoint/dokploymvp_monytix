//
//  UIApplication+RootViewController.swift
//  ios
//
//  Helper to get the key window's root view controller for presenting Google Sign-In.
//

import UIKit

extension UIApplication {
    /// Key window's root view controller (for presenting sign-in sheets).
    var rootViewController: UIViewController? {
        guard let scene = connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              let window = scene.windows.first(where: \.isKeyWindow) ?? scene.windows.first else {
            return nil
        }
        return window.rootViewController
    }
}
