package com.example.data.model

import com.parse.ParseClassName
import com.parse.ParseFile
import com.parse.ParseObject
import com.parse.ParseUser
import java.util.Date

/**
 * Parse Classes for OcaVenteDz
 * These are the data models used throughout the application
 */

// ===================== Listing / Product =====================
@ParseClassName("Listing")
class Listing : ParseObject() {
    var title: String?
        get() = getString("title")
        set(value) = put("title", value)

    var description: String?
        get() = getString("description")
        set(value) = put("description", value)

    var category: String?
        get() = getString("category")
        set(value) = put("category", value)

    var priceInDZD: Double?
        get() = getDouble("priceInDZD")
        set(value) = put("priceInDZD", value)

    var imageUrl: String?
        get() = getString("imageUrl")
        set(value) = put("imageUrl", value)

    var imageFile: ParseFile?
        get() = getParseFile("imageFile")
        set(value) = put("imageFile", value)

    var owner: ParseUser?
        get() = getParseUser("owner")
        set(value) = put("owner", value)

    var status: String? // DRAFT, PUBLISHED, SOLD, REMOVED
        get() = getString("status")
        set(value) = put("status", value)

    var location: String?
        get() = getString("location")
        set(value) = put("location", value)

    var latitude: Double?
        get() = getDouble("latitude")
        set(value) = put("latitude", value)

    var longitude: Double?
        get() = getDouble("longitude")
        set(value) = put("longitude", value)

    var condition: String? // NEW, LIKE_NEW, GOOD, FAIR, POOR
        get() = getString("condition")
        set(value) = put("condition", value)

    var views: Int?
        get() = getInt("views")
        set(value) = put("views", value)

    var isFeatured: Boolean
        get() = getBoolean("isFeatured")
        set(value) = put("isFeatured", value)

    var isUrgent: Boolean
        get() = getBoolean("isUrgent")
        set(value) = put("isUrgent", value)

    var createdAtDate: Date?
        get() = createdAt
        set(value) {} // Read-only from Parse

    var updatedAtDate: Date?
        get() = updatedAt
        set(value) {} // Read-only from Parse
}

// ===================== Favorite =====================
@ParseClassName("Favorite")
class Favorite : ParseObject() {
    var user: ParseUser?
        get() = getParseUser("user")
        set(value) = put("user", value)

    var listing: Listing?
        get() = getParseObject("listing") as? Listing
        set(value) = put("listing", value)
}

// ===================== Wallet =====================
@ParseClassName("Wallet")
class Wallet : ParseObject() {
    var user: ParseUser?
        get() = getParseUser("user")
        set(value) = put("user", value)

    var balanceDzd: Double?
        get() = getDouble("balanceDzd")
        set(value) = put("balanceDzd", value)

    var pendingBalanceDzd: Double?
        get() = getDouble("pendingBalanceDzd")
        set(value) = put("pendingBalanceDzd", value)

    var currency: String? // DZD
        get() = getString("currency")
        set(value) = put("currency", value)

    var isActive: Boolean
        get() = getBoolean("isActive")
        set(value) = put("isActive", value)
}

// ===================== TopUpRequest =====================
@ParseClassName("TopUpRequest")
class TopUpRequest : ParseObject() {
    var user: ParseUser?
        get() = getParseUser("user")
        set(value) = put("user", value)

    var amountDzd: Double?
        get() = getDouble("amountDzd")
        set(value) = put("amountDzd", value)

    var status: String? // PENDING, APPROVED, REJECTED
        get() = getString("status")
        set(value) = put("status", value)

    var adminNote: String?
        get() = getString("adminNote")
        set(value) = put("adminNote", value)

    var reviewedBy: ParseUser?
        get() = getParseUser("reviewedBy")
        set(value) = put("reviewedBy", value)

    var reviewedAt: Date?
        get() = getDate("reviewedAt")
        set(value) = put("reviewedAt", value)
}

// ===================== WalletTransaction =====================
@ParseClassName("WalletTransaction")
class WalletTransaction : ParseObject() {
    var user: ParseUser?
        get() = getParseUser("user")
        set(value) = put("user", value)

    var type: String? // TOPUP, DEBIT, REFUND
        get() = getString("type")
        set(value) = put("type", value)

    var amount: Double?
        get() = getDouble("amount")
        set(value) = put("amount", value)

    var description: String?
        get() = getString("description")
        set(value) = put("description", value)

    var referenceId: String? // Link to TopUpRequest or Order
        get() = getString("referenceId")
        set(value) = put("referenceId", value)

    var timestamp: Date?
        get() = getDate("timestamp")
        set(value) = put("timestamp", value)
}

// ===================== ChatMessage =====================
@ParseClassName("ChatMessage")
class ChatMessage : ParseObject() {
    var sender: ParseUser?
        get() = getParseUser("sender")
        set(value) = put("sender", value)

    var receiver: ParseUser?
        get() = getParseUser("receiver")
        set(value) = put("receiver", value)

    var listing: Listing?
        get() = getParseObject("listing") as? Listing
        set(value) = put("listing", value)

    var message: String?
        get() = getString("message")
        set(value) = put("message", value)

    var isRead: Boolean
        get() = getBoolean("isRead")
        set(value) = put("isRead", value)

    var readAt: Date?
        get() = getDate("readAt")
        set(value) = put("readAt", value)

    var sentAt: Date?
        get() = getDate("sentAt")
        set(value) = put("sentAt", value)
}

// ===================== Report / Complaint =====================
@ParseClassName("Report")
class Report : ParseObject() {
    var reporter: ParseUser?
        get() = getParseUser("reporter")
        set(value) = put("reporter", value)

    var targetUser: ParseUser?
        get() = getParseUser("targetUser")
        set(value) = put("targetUser", value)

    var listing: Listing?
        get() = getParseObject("listing") as? Listing
        set(value) = put("listing", value)

    var reason: String?
        get() = getString("reason")
        set(value) = put("reason", value)

    var description: String?
        get() = getString("description")
        set(value) = put("description", value)

    var status: String? // PENDING, REVIEWED, RESOLVED
        get() = getString("status")
        set(value) = put("status", value)

    var adminNote: String?
        get() = getString("adminNote")
        set(value) = put("adminNote", value)
}

// ===================== AppNotification =====================
@ParseClassName("AppNotification")
class AppNotification : ParseObject() {
    var recipient: ParseUser?
        get() = getParseUser("recipient")
        set(value) = put("recipient", value)

    var type: String? // GENERAL, MESSAGE, LISTING, SYSTEM
        get() = getString("type")
        set(value) = put("type", value)

    var title: String?
        get() = getString("title")
        set(value) = put("title", value)

    var body: String?
        get() = getString("body")
        set(value) = put("body", value)

    var data: Map<String, Any>?
        get() = getMap("data")
        set(value) = put("data", value)

    var isRead: Boolean
        get() = getBoolean("isRead")
        set(value) = put("isRead", value)

    var sentAt: Date?
        get() = getDate("sentAt")
        set(value) = put("sentAt", value)
}

// ===================== BlockedUser =====================
@ParseClassName("BlockedUser")
class BlockedUser : ParseObject() {
    var blocker: ParseUser?
        get() = getParseUser("blocker")
        set(value) = put("blocker", value)

    var blocked: ParseUser?
        get() = getParseUser("blocked")
        set(value) = put("blocked", value)

    var reason: String?
        get() = getString("reason")
        set(value) = put("reason", value)
}

// ===================== Order (Optional - for future use) =====================
@ParseClassName("Order")
class Order : ParseObject() {
    var buyer: ParseUser?
        get() = getParseUser("buyer")
        set(value) = put("buyer", value)

    var seller: ParseUser?
        get() = getParseUser("seller")
        set(value) = put("seller", value)

    var listing: Listing?
        get() = getParseObject("listing") as? Listing
        set(value) = put("listing", value)

    var status: String? // PENDING, CONFIRMED, DELIVERED, CANCELLED
        get() = getString("status")
        set(value) = put("status", value)

    var isPaid: Boolean
        get() = getBoolean("isPaid")
        set(value) = put("isPaid", value)

    var totalAmount: Double?
        get() = getDouble("totalAmount")
        set(value) = put("totalAmount", value)
}
