//
//  WebSocketService.swift
//  ios_monytix
//
//  Realtime WebSocket client. Connects to backend /ws?token=..., receives JSON messages, and notifies so the dashboard can refresh.
//

import Foundation

final class WebSocketService: @unchecked Sendable {
    static let shared = WebSocketService()
    private var task: URLSessionWebSocketTask?
    private let session = URLSession(configuration: .default)
    private var isActive = false
    private var onMessage: (() -> Void)?
    private let queue = DispatchQueue(label: "com.monytix.websocket")

    private init() {}

    /// Callback invoked on main thread when any message is received (e.g. to refresh dashboard).
    func setOnMessage(_ block: @escaping () -> Void) {
        queue.async { [weak self] in
            self?.onMessage = block
        }
    }

    /// Connect to wss://<base>/ws?token=<idToken>. Call when user is signed in and home is visible.
    func connect(idToken: String) {
        queue.async { [weak self] in
            self?.disconnectImmediate()
            let base = BackendConfig.baseURL
                .replacingOccurrences(of: "https://", with: "wss://")
                .replacingOccurrences(of: "http://", with: "ws://")
            let path = base.hasSuffix("/") ? "ws" : "/ws"
            var comp = URLComponents(string: "\(base)\(path)")
            comp?.queryItems = [URLQueryItem(name: "token", value: idToken)]
            guard let url = comp?.url else { return }
            let wsTask = self?.session.webSocketTask(with: url)
            self?.task = wsTask
            self?.isActive = true
            wsTask?.resume()
            self?.receiveLoop()
        }
    }

    /// Disconnect. Call when home disappears or user signs out.
    func disconnect() {
        queue.async { [weak self] in
            self?.disconnectImmediate()
        }
    }

    private func disconnectImmediate() {
        isActive = false
        task?.cancel(with: .goingAway, reason: nil)
        task = nil
    }

    private func receiveLoop() {
        task?.receive { [weak self] result in
            guard let self = self, self.isActive else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    _ = text
                    self.notifyMessage()
                case .data:
                    self.notifyMessage()
                @unknown default:
                    break
                }
                self.queue.async { self.receiveLoop() }
            case .failure:
                break
            }
        }
    }

    private func notifyMessage() {
        let block = queue.sync { self.onMessage }
        DispatchQueue.main.async { block?() }
    }
}
