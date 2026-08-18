package com.stepitacademy.shopledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Customer::class, Order::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ShopDatabase : RoomDatabase() {

    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: ShopDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShopDatabase::class.java,
                    "shop_ledger.db"
                )
                    .addCallback(SeedCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /** Populates a few customers + orders the first time the database is created. */
    private class SeedCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.shopDao()

                    val daraId = dao.insertCustomer(Customer(name = "Sok Dara", phone = "012 345 678"))
                    val vicheaId = dao.insertCustomer(Customer(name = "Chan Vichea", phone = "011 222 333"))
                    val noPhoneId = dao.insertCustomer(Customer(name = "No Phone Guy", phone = null))

                    val now = System.currentTimeMillis()

                    dao.insertOrder(
                        Order(
                            customerId = daraId,
                            description = "Rice, 5kg",
                            amount = 20_000,
                            currency = Currency.KHR,
                            timestamp = now,
                            isPaid = false
                        )
                    )
                    dao.insertOrder(
                        Order(
                            customerId = daraId,
                            description = "Cooking oil",
                            amount = 25_000,
                            currency = Currency.KHR,
                            timestamp = now - 86_400_000,
                            isPaid = false
                        )
                    )
                    dao.insertOrder(
                        Order(
                            customerId = vicheaId,
                            description = "Phone credit",
                            amount = 1250, // $12.50 in cents
                            currency = Currency.USD,
                            timestamp = now,
                            isPaid = false
                        )
                    )
                    dao.insertOrder(
                        Order(
                            customerId = noPhoneId,
                            description = "Instant noodles, case",
                            amount = 8_000,
                            currency = Currency.KHR,
                            timestamp = now,
                            isPaid = false
                        )
                    )
                    dao.insertOrder(
                        Order(
                            customerId = noPhoneId,
                            description = "Bottled water",
                            amount = 500,
                            currency = Currency.USD,
                            timestamp = now,
                            isPaid = false
                        )
                    )
                }
            }
        }
    }
}