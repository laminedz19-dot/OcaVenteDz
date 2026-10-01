package com.example.network

import com.example.model.*
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class LoginRequest(val login: String, val password: String)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val phone: String? = null,
    val wilaya: String = "16 - الجزائر (Alger)"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(val categories: List<Category>)

@JsonClass(generateAdapter = true)
data class ListingsResponse(
    val listings: List<Listing>,
    val meta: MetaData? = null
)

@JsonClass(generateAdapter = true)
data class MetaData(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class ListingDetailResponse(val listing: Listing)

@JsonClass(generateAdapter = true)
data class CreateListingRequest(
    val title: String,
    val description: String,
    val price: Double,
    val categoryId: String,
    val wilaya: String,
    val commune: String? = null,
    val phone: String,
    val isNegotiable: Boolean = true,
    val isFeatured: Boolean = false,
    val images: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CreateListingResponse(
    val message: String,
    val listing: Listing
)

@JsonClass(generateAdapter = true)
data class RechargeRequestPayload(
    val amount: Double,
    val paymentMethod: String,
    val receiptNumber: String,
    val receiptImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RechargeResponse(
    val message: String,
    val request: RechargeRequest
)

@JsonClass(generateAdapter = true)
data class RechargesResponse(val requests: List<RechargeRequest>)

@JsonClass(generateAdapter = true)
data class WalletResponse(
    val balance: Double,
    val transactions: List<BalanceTransaction>
)

@JsonClass(generateAdapter = true)
data class NotificationsResponse(val notifications: List<NotificationItem>)

@JsonClass(generateAdapter = true)
data class FavoriteResponse(val favorited: Boolean)

@JsonClass(generateAdapter = true)
data class SimpleResponse(val message: String? = null, val success: Boolean = true)

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<Map<String, User>>

    @GET("categories")
    suspend fun getCategories(): Response<CategoriesResponse>

    @GET("listings")
    suspend fun getListings(
        @Query("category") category: String? = null,
        @Query("wilaya") wilaya: String? = null,
        @Query("q") query: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("page") page: Int = 1
    ): Response<ListingsResponse>

    @GET("listings/{id}")
    suspend fun getListing(@Path("id") id: String): Response<ListingDetailResponse>

    @POST("listings")
    suspend fun createListing(@Body req: CreateListingRequest): Response<CreateListingResponse>

    @GET("listings/my/all")
    suspend fun getMyListings(): Response<ListingsResponse>

    @DELETE("listings/{id}")
    suspend fun deleteListing(@Path("id") id: String): Response<SimpleResponse>

    @POST("listings/{id}/favorite")
    suspend fun toggleFavorite(@Path("id") id: String): Response<FavoriteResponse>

    @GET("listings/my/favorites")
    suspend fun getFavorites(): Response<ListingsResponse>

    @POST("recharge/request")
    suspend fun submitRecharge(@Body req: RechargeRequestPayload): Response<RechargeResponse>

    @GET("recharge/my-requests")
    suspend fun getMyRecharges(): Response<RechargesResponse>

    @GET("recharge/wallet")
    suspend fun getWallet(): Response<WalletResponse>

    @GET("recharge/notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @PUT("recharge/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<SimpleResponse>
}
