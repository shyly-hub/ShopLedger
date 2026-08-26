package com.stepitacademy.shopledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

// ---------------------------------------------------------------------------
// ENTITIES
// ---------------------------------------------------------------------------

enum class Currency { KHR, USD }

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("customerId")]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val description: String,
    val amount: Long,          // minor units: whole riel for KHR, cents for USD. Always positive.
    val currency: Currency,
    val timestamp: Long,       // epoch millis — source of truth for ordering + date shown
    val isPaid: Boolean = false
)

// ---------------------------------------------------------------------------
// TYPE CONVERTERS — Room can't store the Currency enum natively, so we
// convert it to/from its name ("KHR" / "USD") as TEXT.
// ---------------------------------------------------------------------------

class Converters {
    @TypeConverter
    fun fromCurrency(value: Currency): String = value.name

    @TypeConverter
    fun toCurrency(value: String): Currency = Currency.valueOf(value)
}

// ---------------------------------------------------------------------------
// QUERY RESULT SHAPES
// ---------------------------------------------------------------------------

data class CustomerWithDebt(
    val id: Long,
    val name: String,
    val phone: String?,
    val owedKhr: Long,   // sum of unpaid KHR orders, minor units
    val owedUsd: Long    // sum of unpaid USD orders, minor units
)

data class ShopTotal(
    val totalKhr: Long,
    val totalUsd: Long
)

/** Thrown (or wrapped) when a delete is blocked because the customer still owes money. */
class CustomerHasDebtException(val customerId: Long) : Exception(
    "Customer $customerId still has unpaid orders and cannot be deleted."
)

// ---------------------------------------------------------------------------
// DAO
// ---------------------------------------------------------------------------

@Dao
interface ShopDao {

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    // Only safe to call once the customer's orders are gone / all paid off.
    // Left in place for direct use / testing; the repository is what
    // orchestrates the debt check + cleanup below.
    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Insert
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("UPDATE orders SET isPaid = 1 WHERE id = :orderId")
    suspend fun markOrderPaid(orderId: Long)

    // Deletes a customer's order history. Only ever called once we've
    // confirmed the customer has no outstanding debt — this clears their
    // paid orders so the RESTRICT foreign key doesn't block the delete.
    @Query("DELETE FROM orders WHERE customerId = :customerId")
    suspend fun deleteOrdersForCustomer(customerId: Long)

    // Debt-aware delete: wipes the (paid) order history for this customer,
    // then deletes the customer, atomically. Caller MUST have already
    // verified debt == 0 — this does not re-check.
    @Transaction
    suspend fun deleteCustomerAndOrders(customer: Customer) {
        deleteOrdersForCustomer(customer.id)
        deleteCustomer(customer)
    }

    @Query("""
        SELECT c.id AS id, c.name AS name, c.phone AS phone,
            COALESCE(SUM(CASE WHEN o.currency = 'KHR' AND o.isPaid = 0 THEN o.amount ELSE 0 END), 0) AS owedKhr,
            COALESCE(SUM(CASE WHEN o.currency = 'USD' AND o.isPaid = 0 THEN o.amount ELSE 0 END), 0) AS owedUsd
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.id
        WHERE (:query = '' OR c.name LIKE '%' || :query || '%' OR c.phone LIKE '%' || :query || '%')
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun getCustomersWithDebt(query: String = ""): Flow<List<CustomerWithDebt>>

    @Query("""
        SELECT c.id AS id, c.name AS name, c.phone AS phone,
            COALESCE(SUM(CASE WHEN o.currency = 'KHR' AND o.isPaid = 0 THEN o.amount ELSE 0 END), 0) AS owedKhr,
            COALESCE(SUM(CASE WHEN o.currency = 'USD' AND o.isPaid = 0 THEN o.amount ELSE 0 END), 0) AS owedUsd
        FROM customers c
        LEFT JOIN orders o ON o.customerId = c.id
        WHERE c.id = :customerId
        GROUP BY c.id
    """)
    fun getCustomerDebt(customerId: Long): Flow<CustomerWithDebt?>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN currency = 'KHR' AND isPaid = 0 THEN amount ELSE 0 END), 0) AS totalKhr,
            COALESCE(SUM(CASE WHEN currency = 'USD' AND isPaid = 0 THEN amount ELSE 0 END), 0) AS totalUsd
        FROM orders
    """)
    fun getShopTotal(): Flow<ShopTotal>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getOrdersForCustomer(customerId: Long): Flow<List<Order>>
}

// ---------------------------------------------------------------------------
// REPOSITORY
// ---------------------------------------------------------------------------

interface ShopRepository {
    suspend fun addCustomer(name: String, phone: String?): Long
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customer: Customer): Result<Unit> // failure = CustomerHasDebtException

    suspend fun addOrder(customerId: Long, description: String, amount: Long, currency: Currency, timestamp: Long, isPaid: Boolean): Long
    suspend fun updateOrder(order: Order)
    suspend fun markOrderPaid(orderId: Long)

    fun customersWithDebt(query: String = ""): Flow<List<CustomerWithDebt>>
    fun customerDebt(customerId: Long): Flow<CustomerWithDebt?>
    fun shopTotal(): Flow<ShopTotal>
    fun ordersForCustomer(customerId: Long): Flow<List<Order>>
}

/**
 * Fake, in-memory repository with hardcoded data. Persons 2 & 3: point your
 * ViewModels at this today so you're not blocked on the real Room queries.
 * Swap the constructor arg for RoomShopRepository(dao) once it's ready —
 * nothing in your UI code should need to change.
 */
class FakeShopRepository : ShopRepository {

    private val fakeCustomers = listOf(
        CustomerWithDebt(1, "Sok Dara", "012 345 678", owedKhr = 45_000, owedUsd = 0),
        CustomerWithDebt(2, "Chan Vichea", "011 222 333", owedKhr = 0, owedUsd = 1250), // $12.50
        CustomerWithDebt(3, "No Phone Guy", null, owedKhr = 8_000, owedUsd = 500)
    )

    private val fakeOrders = listOf(
        Order(1, 1, "Rice, 5kg", 20_000, Currency.KHR, System.currentTimeMillis(), isPaid = false),
        Order(2, 1, "Cooking oil", 25_000, Currency.KHR, System.currentTimeMillis() - 86_400_000, isPaid = false),
        Order(3, 2, "Phone credit", 1250, Currency.USD, System.currentTimeMillis(), isPaid = false)
    )

    override suspend fun addCustomer(name: String, phone: String?): Long = 0
    override suspend fun updateCustomer(customer: Customer) {}
    override suspend fun deleteCustomer(customer: Customer): Result<Unit> = Result.success(Unit)

    override suspend fun addOrder(customerId: Long, description: String, amount: Long, currency: Currency, timestamp: Long, isPaid: Boolean): Long = 0
    override suspend fun updateOrder(order: Order) {}
    override suspend fun markOrderPaid(orderId: Long) {}

    override fun customersWithDebt(query: String): Flow<List<CustomerWithDebt>> =
        flowOf(fakeCustomers.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || (it.phone?.contains(query) == true)
        })

    override fun customerDebt(customerId: Long): Flow<CustomerWithDebt?> =
        flowOf(fakeCustomers.find { it.id == customerId })

    override fun shopTotal(): Flow<ShopTotal> =
        flowOf(ShopTotal(
            totalKhr = fakeCustomers.sumOf { it.owedKhr },
            totalUsd = fakeCustomers.sumOf { it.owedUsd }
        ))

    override fun ordersForCustomer(customerId: Long): Flow<List<Order>> =
        flowOf(fakeOrders.filter { it.customerId == customerId }.sortedByDescending { it.timestamp })
}

// ---------------------------------------------------------------------------
// RoomShopRepository — the real implementation, backed by the DAO.
// ---------------------------------------------------------------------------

class RoomShopRepository(private val dao: ShopDao) : ShopRepository {
    override suspend fun addCustomer(name: String, phone: String?): Long =
        dao.insertCustomer(Customer(name = name, phone = phone))

    override suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)

    /**
     * A customer can be deleted only once they owe nothing.
     *
     * The `RESTRICT` foreign key on orders.customerId blocks deletion as long
     * as ANY order row references the customer — paid or not. So checking
     * only "does debt == 0" isn't enough on its own; we also have to clear
     * out the (now irrelevant) paid order history in the same transaction,
     * or the delete still throws SQLiteConstraintException even with zero debt.
     */
    override suspend fun deleteCustomer(customer: Customer): Result<Unit> {
        val debt = dao.getCustomerDebt(customer.id).first()
        val stillOwes = debt != null && (debt.owedKhr > 0 || debt.owedUsd > 0)

        if (stillOwes) {
            return Result.failure(CustomerHasDebtException(customer.id))
        }

        return try {
            dao.deleteCustomerAndOrders(customer)
            Result.success(Unit)
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // Defensive fallback — shouldn't normally hit this once debt == 0,
            // but never surface a raw DB exception to the UI.
            Result.failure(CustomerHasDebtException(customer.id))
        }
    }

    override suspend fun addOrder(customerId: Long, description: String, amount: Long, currency: Currency, timestamp: Long, isPaid: Boolean): Long =
        dao.insertOrder(Order(customerId = customerId, description = description, amount = amount, currency = currency, timestamp = timestamp, isPaid = isPaid))

    override suspend fun updateOrder(order: Order) = dao.updateOrder(order)
    override suspend fun markOrderPaid(orderId: Long) = dao.markOrderPaid(orderId)

    override fun customersWithDebt(query: String): Flow<List<CustomerWithDebt>> = dao.getCustomersWithDebt(query)
    override fun customerDebt(customerId: Long): Flow<CustomerWithDebt?> = dao.getCustomerDebt(customerId)
    override fun shopTotal(): Flow<ShopTotal> = dao.getShopTotal()
    override fun ordersForCustomer(customerId: Long): Flow<List<Order>> = dao.getOrdersForCustomer(customerId)
}