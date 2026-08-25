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
    private val customerId: Long,
    private val repository: ShopRepository
) : ViewModel() {

    // Customer debt details stream
    val customerDebt: StateFlow<CustomerWithDebt?> = repository.customerDebt(customerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Orders list stream for this customer
    val orders: StateFlow<List<Order>> = repository.ordersForCustomer(customerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Mark an order as paid
    fun markOrderPaid(orderId: Long) {
        viewModelScope.launch {
            repository.markOrderPaid(orderId)
        }
    }

    // Add a new order
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

    // Update an existing order (Edit)
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
            val updatedOrder = Order(
                id = orderId,
                customerId = customerId,
                description = description,
                amount = amount,
                currency = currency,
                timestamp = timestamp,
                isPaid = isPaid
            )
            repository.updateOrder(updatedOrder)
            onSuccess()
        }
    }
}