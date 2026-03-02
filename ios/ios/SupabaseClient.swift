//
//  APIClient.swift
//  ios
//
//  Created by santosh on 24/02/26.
//

import Foundation

// API configuration
struct APIConfig {
    static let baseURL = "https://backend.monytix.ai"
    static let backupBaseURL = "https://api.monytix.ai"
}

// API Error types
enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int, message: String)
    case decodingError
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let statusCode, let message):
            return "Error \(statusCode): \(message)"
        case .decodingError:
            return "Failed to decode response"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}

// User model
struct User: Codable, Identifiable {
    let id: String
    let email: String
    let createdAt: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case email
        case createdAt = "created_at"
    }
}

// Auth response models
struct AuthResponse: Codable {
    let accessToken: String
    let tokenType: String
    let user: User
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case user
    }
}

struct SignUpRequest: Codable {
    let email: String
    let password: String
}

struct SignInRequest: Codable {
    let email: String
    let password: String
}

struct MessageResponse: Codable {
    let message: String
}

// API Client
class APIClient {
    static let shared = APIClient()
    
    private init() {}
    
    private func isRetryable(_ error: Error) -> Bool {
        if case .httpError(let code, _) = error as? APIError, code >= 500 { return true }
        let ns = error as NSError
        if ns.domain == NSURLErrorDomain {
            let c = ns.code
            return c == NSURLErrorTimedOut || c == NSURLErrorCannotConnectToHost || c == NSURLErrorNetworkConnectionLost || c == NSURLErrorNotConnectedToInternet
        }
        return false
    }

    // Generic request method
    private func request<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        body: (any Encodable)? = nil,
        token: String? = nil,
        baseURL: String = APIConfig.baseURL
    ) async throws -> T {
        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = token {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = try JSONEncoder().encode(body)
        }
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            // Handle non-2xx status codes
            if !(200...299).contains(httpResponse.statusCode) {
                // Try to decode error message
                if let errorResponse = try? JSONDecoder().decode(MessageResponse.self, from: data) {
                    throw APIError.httpError(statusCode: httpResponse.statusCode, message: errorResponse.message)
                } else if let errorString = String(data: data, encoding: .utf8) {
                    throw APIError.httpError(statusCode: httpResponse.statusCode, message: errorString)
                } else {
                    throw APIError.httpError(statusCode: httpResponse.statusCode, message: "Unknown error")
                }
            }
            
            // Decode response
            do {
                let decoder = JSONDecoder()
                return try decoder.decode(T.self, from: data)
            } catch {
                print("Decoding error: \(error)")
                print("Response data: \(String(data: data, encoding: .utf8) ?? "Unable to decode")")
                throw APIError.decodingError
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }

    private func requestWithBackup<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        body: (any Encodable)? = nil,
        token: String? = nil
    ) async throws -> T {
        do {
            return try await request(endpoint: endpoint, method: method, body: body, token: token, baseURL: APIConfig.baseURL)
        } catch {
            guard isRetryable(error), APIConfig.baseURL != APIConfig.backupBaseURL else { throw error }
            return try await request(endpoint: endpoint, method: method, body: body, token: token, baseURL: APIConfig.backupBaseURL)
        }
    }
    
    // Sign up
    func signUp(email: String, password: String) async throws -> AuthResponse {
        let body = SignUpRequest(email: email, password: password)
        return try await requestWithBackup(endpoint: "/auth/signup", method: "POST", body: body)
    }
    
    // Sign in
    func signIn(email: String, password: String) async throws -> AuthResponse {
        let body = SignInRequest(email: email, password: password)
        return try await requestWithBackup(endpoint: "/auth/signin", method: "POST", body: body)
    }
    
    // Sign out
    func signOut(token: String) async throws -> MessageResponse {
        return try await requestWithBackup(endpoint: "/auth/signout", method: "POST", token: token)
    }
    
    // Get current user
    func getCurrentUser(token: String) async throws -> User {
        return try await requestWithBackup(endpoint: "/auth/me", method: "GET", token: token)
    }
    
    // Reset password
    func resetPassword(email: String) async throws -> MessageResponse {
        return try await requestWithBackup(
            endpoint: "/auth/reset-password",
            method: "POST",
            body: ["email": email]
        )
    }
    
    // Sign in with Apple
    func signInWithApple(idToken: String, nonce: String) async throws -> AuthResponse {
        let body = ["id_token": idToken, "nonce": nonce]
        return try await requestWithBackup(endpoint: "/auth/apple", method: "POST", body: body)
    }
}
