package com.stepitacademy.shopledger.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stepitacademy.shopledger.data.Currency
import com.stepitacademy.shopledger.data.CustomerWithDebt
import com.stepitacademy.shopledger.data.Order
import com.stepitacademy.shopledger.data.ShopRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomerDetailViewModel(
    private val repository: ShopRepository,
    private val customerId: Long
) : ViewModel() {

    val customer: StateFlow<CustomerWithDebt?> = repository.customerDebt(customerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val orders: StateFlow<List<Order>> = repository.ordersForCustomer(customerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun markOrderPaid(orderId: Long) {
        viewModelScope.launch {
            repository.markOrderPaid(orderId)
        }
    }

    /** Inserts a new order for this customer. amount is already in minor units. */
    fun addOrder(
        description: String,
        amount: Long,
        currency: Currency,
        timestamp: Long,
        isPaid: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addOrder(
                customerId = customerId,
                description = description,
                amount = amount,
                currency = currency,
                timestamp = timestamp,
                isPaid = isPaid
            )
            onSuccess()
        }
    }

    /** Updates an existing order. amount is already in minor units. */
    fun updateOrder(
        orderId: Long,
        description: String,
        amount: Long,
        currency: Currency,
        timestamp: Long,
        isPaid: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateOrder(
                Order(
                    id = orderId,
                    customerId = customerId,
                    description = description,
                    amount = amount,
                    currency = currency,
                    timestamp = timestamp,
                    isPaid = isPaid
                )
            )
            onSuccess()
        }
    }
}