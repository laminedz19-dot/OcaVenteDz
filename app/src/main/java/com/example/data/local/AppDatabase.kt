package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ListingEntity::class,
        PaymentOrderEntity::class,
        WalletEntity::class,
        WalletTransactionEntity::class,
        ChatMessageEntity::class,
        ReviewEntity::class,
        ReportEntity::class,
        FavoriteEntity::class,
        PlatformSettingsEntity::class,
        TopUpRequestEntity::class,
        OrderEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun userDao(): UserDao
    abstract fun paymentDao(): PaymentDao
    abstract fun walletDao(): WalletDao
    abstract fun topUpRequestDao(): TopUpRequestDao
    abstract fun chatDao(): ChatDao
    abstract fun reviewDao(): ReviewDao
    abstract fun reportDao(): ReportDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun settingsDao(): PlatformSettingsDao
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ocavente_dz.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initial data seeded in background coroutine
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        InitialDataSeeder.seed(database)
                    }
                }
            }
        }
    }
}
