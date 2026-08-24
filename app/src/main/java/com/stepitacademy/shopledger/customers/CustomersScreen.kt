package com.stepitacademy.shopledger.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stepitacademy.shopledger.data.CustomerWithDebt
import com.stepitacademy.shopledger.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onCustomerClick: (Long) -> Unit,
    onAddCustomerClick: () -> Unit,
    onEditCustomerClick: (CustomerWithDebt) -> Unit
) {
    val customers by viewModel.customersWithDebt.collectAsState()
    val total by viewModel.shopTotal.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Shop Ledger") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCustomerClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Shop Totals Banner (KHR & USD side-by-side)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Owed to Shop", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMoney(total.totalKhr, "KHR"), style = MaterialTheme.typography.titleMedium)
                        Text(formatMoney(total.totalUsd, "USD"), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            //search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search by name or phone") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            //customer List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customers, key = { it.id }) { customerWithDebt ->
                    CustomerRow(
                        item = customerWithDebt,
                        onClick = { onCustomerClick(customerWithDebt.id) },
                        onEdit = { onEditCustomerClick(customerWithDebt) },
                        onDelete = { viewModel.deleteCustomer(customerWithDebt) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerRow(
    item: CustomerWithDebt,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                item.phone?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Owes: ${formatMoney(item.owedKhr, "KHR")} | ${formatMoney(item.owedUsd, "USD")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Customer")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Customer")
                }
            }
        }
    }
}