package com.stepitacademy.shopledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stepitacademy.shopledger.customers.AddEditCustomerScreen
import com.stepitacademy.shopledger.customers.CustomersScreen
import com.stepitacademy.shopledger.data.Currency
import com.stepitacademy.shopledger.data.RoomShopRepository
import com.stepitacademy.shopledger.data.ShopDatabase
import com.stepitacademy.shopledger.data.ShopRepository
import com.stepitacademy.shopledger.orders.AddEditOrderScreen
import com.stepitacademy.shopledger.orders.CustomerDetailScreen
import com.stepitacademy.shopledger.orders.CustomerDetailViewModel
import com.stepitacademy.shopledger.ui.customers.CustomersViewModel
import com.stepitacademy.shopledger.ui.theme.ShopLedgerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val applicationScope = CoroutineScope(SupervisorJob())
        val database = ShopDatabase.getDatabase(this, applicationScope)
        val repository = RoomShopRepository(database.shopDao())

        val customersViewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CustomersViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return CustomersViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
            }
        }

        setContent {
            ShopLedgerTheme {
                AppNavigation(
                    customersViewModel = viewModel(factory = customersViewModelFactory),
                    repository = repository
                )
            }
        }
    }
}

/**
 * CustomerDetailViewModel's constructor is (repository, customerId) — in
 * that order. This factory is built fresh per navigation entry so the
 * ViewModel is registered with that entry's ViewModelStore and its
 * viewModelScope is actually cancelled (onCleared fires) when you
 * navigate away, instead of leaking its Flow collectors.
 */
private fun customerDetailViewModelFactory(
    repository: ShopRepository,
    customerId: Long
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerDetailViewModel(repository, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

/** Converts a stored minor-unit amount back into the text a user would type. */
private fun minorUnitsToDisplayText(amount: Long, currency: Currency): String = when (currency) {
    Currency.KHR -> amount.toString()
    Currency.USD -> "%.2f".format(amount / 100.0)
}

@Composable
fun AppNavigation(
    customersViewModel: CustomersViewModel,
    repository: ShopRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "customers_list"
    ) {
        // Customers List
        composable("customers_list") {
            CustomersScreen(
                viewModel = customersViewModel,
                onCustomerClick = { customerId ->
                    navController.navigate("customer_detail/$customerId")
                },
                onAddCustomerClick = {
                    navController.navigate("add_customer")
                },
                onEditCustomerClick = { customer ->
                    val encodedName = URLEncoder.encode(customer.name, "UTF-8")
                    val encodedPhone = URLEncoder.encode(customer.phone ?: "", "UTF-8")
                    navController.navigate(
                        "edit_customer?customerId=${customer.id}&initialName=$encodedName&initialPhone=$encodedPhone"
                    )
                }
            )
        }

        // Add New Customer
        composable("add_customer") {
            AddEditCustomerScreen(
                isEditing = false,
                onSave = { name, phone ->
                    customersViewModel.saveCustomer(id = null, name = name, phone = phone) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Edit Existing Customer
        composable(
            route = "edit_customer?customerId={customerId}&initialName={initialName}&initialPhone={initialPhone}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("initialName") { type = NavType.StringType; defaultValue = "" },
                navArgument("initialPhone") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val initialName = URLDecoder.decode(
                backStackEntry.arguments?.getString("initialName").orEmpty(), "UTF-8"
            )
            val initialPhone = URLDecoder.decode(
                backStackEntry.arguments?.getString("initialPhone").orEmpty(), "UTF-8"
            )

            AddEditCustomerScreen(
                initialName = initialName,
                initialPhone = initialPhone,
                isEditing = true,
                onSave = { name, phone ->
                    customersViewModel.saveCustomer(id = customerId, name = name, phone = phone) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Customer Detail
        composable(
            route = "customer_detail/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val detailViewModel: CustomerDetailViewModel = viewModel(
                factory = customerDetailViewModelFactory(repository, customerId)
            )

            CustomerDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onAddOrderClick = {
                    navController.navigate("add_order/$customerId")
                },
                onEditOrderClick = { order ->
                    navController.navigate("edit_order/${order.id}?customerId=$customerId")
                }
            )
        }

        // Add Order
        composable(
            route = "add_order/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val detailViewModel: CustomerDetailViewModel = viewModel(
                factory = customerDetailViewModelFactory(repository, customerId)
            )
            val customer by detailViewModel.customer.collectAsState()

            AddEditOrderScreen(
                customerName = customer?.name ?: "",
                isEditing = false,
                onSave = { description, amountMinorUnits, currency, timestamp, isPaid ->
                    detailViewModel.addOrder(description, amountMinorUnits, currency, timestamp, isPaid) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Edit Order
        composable(
            route = "edit_order/{orderId}?customerId={customerId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType },
                navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L

            val detailViewModel: CustomerDetailViewModel = viewModel(
                factory = customerDetailViewModelFactory(repository, customerId)
            )
            val orders by detailViewModel.orders.collectAsState()
            val customer by detailViewModel.customer.collectAsState()

            val orderToEdit = remember(orders, orderId) { orders.find { it.id == orderId } }

            // While `orders` hasn't loaded yet, orderToEdit is briefly null.
            // Fall back to blank/default initial values rather than crashing;
            // once the Flow emits, this recomposes with the real order data.
            AddEditOrderScreen(
                customerName = customer?.name ?: "",
                initialDescription = orderToEdit?.description ?: "",
                initialAmountMajorUnitsText = orderToEdit?.let {
                    minorUnitsToDisplayText(it.amount, it.currency)
                } ?: "",
                initialCurrency = orderToEdit?.currency ?: Currency.KHR,
                initialTimestamp = orderToEdit?.timestamp ?: System.currentTimeMillis(),
                initialIsPaid = orderToEdit?.isPaid ?: false,
                isEditing = true,
                onSave = { description, amountMinorUnits, currency, timestamp, isPaid ->
                    detailViewModel.updateOrder(
                        orderId = orderId,
                        description = description,
                        amount = amountMinorUnits,
                        currency = currency,
                        timestamp = timestamp,
                        isPaid = isPaid
                    ) {
                        navController.popBackStack()
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}