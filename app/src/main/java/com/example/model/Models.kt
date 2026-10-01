package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val email: String,
    val username: String,
    val phone: String? = null,
    val wilaya: String = "16 - الجزائر (Alger)",
    val role: String = "USER",
    val balance: Double = 0.0,
    val avatarUrl: String? = null,
    val isBlocked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Category(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val slug: String,
    val icon: String,
    val order: Int = 0
)

@JsonClass(generateAdapter = true)
data class ListingImage(
    val id: String = "",
    val url: String,
    val isPrimary: Boolean = false,
    val order: Int = 0
)

@JsonClass(generateAdapter = true)
data class Listing(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val categoryId: String,
    val category: Category? = null,
    val userId: String,
    val user: User? = null,
    val wilaya: String,
    val commune: String? = null,
    val phone: String,
    val isNegotiable: Boolean = true,
    val status: String = "ACTIVE", // PENDING, ACTIVE, REJECTED, SOLD
    val rejectionReason: String? = null,
    val views: Int = 0,
    val isFeatured: Boolean = false,
    val images: List<ListingImage> = emptyList(),
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class RechargeRequest(
    val id: String,
    val userId: String,
    val user: User? = null,
    val amount: Double,
    val paymentMethod: String, // CCP, BARIDIMOB, CASH, VOUCHER
    val receiptNumber: String,
    val receiptImageUrl: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNotes: String? = null,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class BalanceTransaction(
    val id: String,
    val userId: String,
    val amount: Double,
    val type: String, // RECHARGE, FEATURED_AD_FEE, REFUND, ADJUSTMENT
    val description: String,
    val balanceAfter: Double,
    val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String = "SYSTEM",
    val isRead: Boolean = false,
    val createdAt: String = ""
)

object AlgerianWilayas {
    val all = listOf(
        "01 - أدرار (Adrar)",
        "02 - الشلف (Chlef)",
        "03 - الأغواط (Laghouat)",
        "04 - أم البواقي (Oum El Bouaghi)",
        "05 - باتنة (Batna)",
        "06 - بجاية (Béjaïa)",
        "07 - بسكرة (Biskra)",
        "08 - بشار (Béchar)",
        "09 - البليدة (Blida)",
        "10 - البويرة (Bouira)",
        "11 - تمنراست (Tamanrasset)",
        "12 - تبسة (Tébessa)",
        "13 - تلمسان (Tlemcen)",
        "14 - تيارت (Tiaret)",
        "15 - تيزي وزو (Tizi Ouzou)",
        "16 - الجزائر (Alger)",
        "17 - الجلفة (Djelfa)",
        "18 - جيجل (Jijel)",
        "19 - سطيف (Sétif)",
        "20 - سعيدة (Saïda)",
        "21 - سكيكدة (Skikda)",
        "22 - سيدي بلعباس (Sidi Bel Abbès)",
        "23 - عنابة (Annaba)",
        "24 - قالمة (Guelma)",
        "25 - قسنطينة (Constantine)",
        "26 - المدية (Médéa)",
        "27 - مستغانم (Mostaganem)",
        "28 - المسيلة (M'Sila)",
        "29 - معسكر (Mascara)",
        "30 - ورقلة (Ouargla)",
        "31 - وهران (Oran)",
        "32 - البيض (El Bayadh)",
        "33 - إليزي (Illizi)",
        "34 - برج بوعريريج (Bordj Bou Arreridj)",
        "35 - بومرداس (Boumerdès)",
        "36 - الطارف (El Tarf)",
        "37 - تندوف (Tindouf)",
        "38 - تسمسيلت (Tissemsilt)",
        "39 - الوادي (El Oued)",
        "40 - خنشلة (Khenchela)",
        "41 - سوق أهراس (Souk Ahras)",
        "42 - تيبازة (Tipaza)",
        "43 - ميلة (Mila)",
        "44 - عين الدفلى (Aïn Defla)",
        "45 - النعامة (Naâma)",
        "46 - عين تموشنت (Aïn Témouchent)",
        "47 - غرداية (Ghardaïa)",
        "48 - غليزان (Relizane)",
        "49 - المغير (El M'Ghair)",
        "50 - المنيعة (El Menia)",
        "51 - أولاد جلال (Ouled Djellal)",
        "52 - برج باجي مختار (Bordj Baji Mokhtar)",
        "53 - بني عباس (Béni Abbès)",
        "54 - تيميمون (Timimoun)",
        "55 - تقرت (Touggourt)",
        "56 - جانت (Djanet)",
        "57 - عين صالح (In Salah)",
        "58 - عين قزام (In Guezzam)"
    )
}
