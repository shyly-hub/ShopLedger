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

    val customerDebt: StateFlow<CustomerWithDebt?> = repository.customerDebt(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed
            (5000), null)

    val orders: StateFlow<List<Order>> = repository.ordersForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed
            (5000), emptyList())

    fun markOrderPaid(orderId: Long) {
        viewModelScope.launch {
            repository.markOrderPaid(orderId)
        }
    }

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
}