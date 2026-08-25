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
import com.stepitacademy.shopledger.data.RoomShopRepository
import com.stepitacademy.shopledger.data.ShopDatabase
import com.stepitacademy.shopledger.data.ShopRepository
import com.stepitacademy.shopledger.orders.AddEditOrderScreen
import com.stepitacademy.shopledger.orders.CustomerDetailScreen
import com.stepitacademy.shopledger.orders.CustomerDetailViewModel
import com.stepitacademy.shopledger.ui.customers.AddEditCustomerScreen
import com.stepitacademy.shopledger.ui.customers.CustomersScreen
import com.stepitacademy.shopledger.ui.customers.CustomersViewModel
import com.stepitacademy.shopledger.ui.theme.ShopLedgerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val applicationScope = CoroutineScope(SupervisorJob())
        val database = ShopDatabase.getDatabase(this, applicationScope)
        val repository = RoomShopRepository(database.shopDao())

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CustomersViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return CustomersViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            ShopLedgerTheme {
                AppNavigation(
                    customersViewModel = viewModel(factory = factory),
                    repository=repository

                )
            }
        }
    }
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
        //customers List
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
                    val encodedName = java.net.URLEncoder.encode(customer.name, "UTF-8")
                    val encodedPhone = java.net.URLEncoder.encode(customer.phone ?: "", "UTF-8")
                    navController.navigate("edit_customer?customerId=${customer.id}&initialName=$encodedName&initialPhone=$encodedPhone")
                }
            )
        }

        //add new customer
        composable("add_customer") {
            AddEditCustomerScreen(
                isEditing = false,
                onSave = { name, phone ->
                    customersViewModel.saveCustomer(
                        id = null,
                        name = name,
                        phone = phone
                    ) {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // edit existing customer
        composable(
            route = "edit_customer?customerId={customerId}&initialName={initialName}&initialPhone={initialPhone}",
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("initialName") { type = NavType.StringType; defaultValue = "" },
                navArgument("initialPhone") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val initialName = backStackEntry.arguments?.getString("initialName").orEmpty()
            val initialPhone = backStackEntry.arguments?.getString("initialPhone").orEmpty()

            AddEditCustomerScreen(
                initialName = initialName,
                initialPhone = initialPhone,
                isEditing = true,
                onSave = { name, phone ->
                    customersViewModel.saveCustomer(
                        id = customerId,
                        name = name,
                        phone = phone
                    ) {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Customer Detail Screen 3
        composable(
            route = "customer_detail/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val detailViewModel = remember { CustomerDetailViewModel(customerId, repository) }

            CustomerDetailScreen(
                viewModel = detailViewModel,
                onAddOrderClick = {
                    navController.navigate("add_order/$customerId")
                },
                onEditOrderClick = { order ->
                    navController.navigate("edit_order/${order.id}?customerId=$customerId")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        //  Add Order Screen Route
        composable(
            route = "add_order/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            val detailViewModel = remember { CustomerDetailViewModel(customerId, repository) }
            val customerDebt by detailViewModel.customerDebt.collectAsState()

            AddEditOrderScreen(
                customerName = customerDebt?.name ?: "",
                orderToEdit = null,
                onSave = { description, amount, currency, timestamp, isPaid ->
                    detailViewModel.addOrder(description, amount, currency, timestamp, isPaid) {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Order Screen Route
        composable(
            route = "edit_order/{orderId}?customerId={customerId}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType },
                navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L

            val detailViewModel = remember { CustomerDetailViewModel(customerId, repository) }
            val orders by detailViewModel.orders.collectAsState()
            val customerDebt by detailViewModel.customerDebt.collectAsState()

            val orderToEdit = remember(orders, orderId) {
                orders.find { it.id == orderId }
            }

            AddEditOrderScreen(
                customerName = customerDebt?.name ?: "",
                orderToEdit = orderToEdit,
                onSave = { description, amount, currency, timestamp, isPaid ->
                    detailViewModel.updateOrder(
                        orderId = orderId,
                        description = description,
                        amount = amount,
                        currency = currency,
                        timestamp = timestamp,
                        isPaid = isPaid
                    ) {
                        navController.popBackStack()
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}