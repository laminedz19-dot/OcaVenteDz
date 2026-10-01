package com.example.ui.navigation

import com.example.ui.components.MainTab
import com.example.ui.model.Conversation
import com.example.ui.model.Listing
import com.example.ui.screens.search.SearchFilterState

sealed interface AppScreen {
    object Splash : AppScreen
    object AuthLanding : AppScreen
    object Login : AppScreen
    object Register : AppScreen
    object ForgotPassword : AppScreen
    object ChangePassword : AppScreen

    data class Main(val activeTab: MainTab = MainTab.HOME) : AppScreen

    data class AdDetails(val listingId: String) : AppScreen
    object Categories : AppScreen
    data class SearchResults(val filter: SearchFilterState) : AppScreen
    data class SearchFilter(val initialFilter: SearchFilterState = SearchFilterState()) : AppScreen
    object Favorites : AppScreen
    object MyListings : AppScreen
    data class EditAd(val listingId: String) : AppScreen
    data class Chat(val conversationId: String) : AppScreen
    object Offers : AppScreen
    object Orders : AppScreen
    object Wallet : AppScreen
    object Notifications : AppScreen
    object EditProfile : AppScreen
    data class SellerProfile(val sellerId: String) : AppScreen
    object Security : AppScreen
    object Settings : AppScreen
    object Back4AppSettings : AppScreen
    object Legal : AppScreen

    object AdminDashboard : AppScreen
    object AdminListings : AppScreen
    object AdminRecharge : AppScreen
    object AdminUsers : AppScreen
}
