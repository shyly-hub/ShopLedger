package com.stepitacademy.shopledger.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stepitacademy.shopledger.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CustomersViewModel(private val repository: ShopRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val customersWithDebt: StateFlow<List<CustomerWithDebt>> = _searchQuery
        .flatMapLatest { query -> repository.customersWithDebt(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val shopTotal: StateFlow<ShopTotal> = repository.shopTotal()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ShopTotal(0L, 0L)
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteCustomer(customerWithDebt: CustomerWithDebt) {
        viewModelScope.launch {
            val customer = Customer(
                id = customerWithDebt.id,
                name = customerWithDebt.name,
                phone = customerWithDebt.phone
            )
            val result = repository.deleteCustomer(customer)
            result.onFailure { exception ->
                if (exception is CustomerHasDebtException) {
                    _errorMessage.value = "${customerWithDebt.name} still owes money and cannot be deleted!"
                } else {
                    _errorMessage.value = "Failed to delete customer."
                }
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun saveCustomer(id: Long? = null, name: String, phone: String?, onSuccess: () -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _errorMessage.value = "Customer name is required."
            return
        }

        val formattedPhone = phone?.trim()?.ifBlank { null }

        viewModelScope.launch {
            if (id == null || id == 0L) {
                // New Customer
                repository.addCustomer(trimmedName, formattedPhone)
            } else {
                // Edit Existing Customer
                val customerToUpdate = Customer(id = id, name = trimmedName, phone = formattedPhone)
                repository.updateCustomer(customerToUpdate)
            }
            onSuccess()
        }
    }
}