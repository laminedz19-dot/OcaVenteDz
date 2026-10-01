package com.example.data.models

/**
 * Status of a marketplace advertisement in OcaVente DZ.
 */
enum class ListingStatus(val labelAr: String, val labelFr: String) {
    DRAFT("مسودة", "Brouillon"),
    PAYMENT_PENDING("في انتظار الدفع", "Paiement en attente"),
    PAYMENT_FAILED("فشل الدفع", "Échec du paiement"),
    PAID("تم الدفع", "Payé"),
    UNDER_REVIEW("قيد المراجعة", "En cours de révision"),
    PUBLISHED("منشور", "Publié"),
    REJECTED("مرفوض", "Rejeté"),
    EXPIRED("منتهي الصلاحية", "Expiré"),
    SOLD("تم البيع", "Vendu"),
    DELETED("محذوف", "Supprimé");

    companion object {
        fun fromString(value: String?): ListingStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PAYMENT_PENDING
        }
    }
}

/**
 * Physical condition of the item being sold.
 */
enum class ItemCondition(val labelAr: String, val labelFr: String) {
    NEW("جديد", "Neuf"),
    LIKE_NEW("كالجديد", "Comme neuf"),
    USED("مستعمل", "Occasion"),
    FOR_PARTS("قطع غيار", "Pour pièces");

    companion object {
        fun fromString(value: String?): ItemCondition {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USED
        }
    }
}

/**
 * Promotion packages for published ads.
 */
enum class ListingPackageType(val defaultPriceDzd: Int, val labelAr: String, val labelFr: String) {
    STANDARD(100, "عادي", "Standard"),
    FEATURED(200, "مميز", "En vedette"),
    URGENT(300, "عاجل", "Urgent");

    companion object {
        fun fromString(value: String?): ListingPackageType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: STANDARD
        }
    }
}

/**
 * Access and authorization roles.
 */
enum class UserRole {
    USER,
    ADMIN,
    MODERATOR;

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: USER
        }
    }
}

/**
 * Operational status of user accounts.
 */
enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    BANNED;

    companion object {
        fun fromString(value: String?): UserStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

/**
 * Account verification status for trusted sellers.
 */
enum class VerificationStatus {
    UNVERIFIED,
    PENDING,
    VERIFIED,
    REJECTED;

    companion object {
        fun fromString(value: String?): VerificationStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNVERIFIED
        }
    }
}
