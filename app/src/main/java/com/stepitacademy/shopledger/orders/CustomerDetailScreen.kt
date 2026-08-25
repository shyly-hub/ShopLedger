package com.stepitacademy.shopledger.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stepitacademy.shopledger.data.Order
import com.stepitacademy.shopledger.util.formatMoney
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onAddOrderClick: () -> Unit,
    onEditOrderClick: (Order) -> Unit,
    onBackClick: () -> Unit
) {
    val customer by viewModel.customerDebt.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer?.name ?: "Customer Detail") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddOrderClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Order")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            customer?.let { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Owes Right Now", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatMoney(c.owedKhr, "KHR"), style = MaterialTheme.typography.titleLarge)
                            Text(formatMoney(c.owedUsd, "USD"), style = MaterialTheme.typography.titleLarge)
                        }

                        if (!c.phone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))
                                context.startActivity(intent)
                            }) {
                                Text("Call: ${c.phone}")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Orders (Newest First)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderRow(
                        order = order,
                        onMarkPaid = { viewModel.markOrderPaid(order.id) },
                        onEditClick = { onEditOrderClick(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderRow(
    order: Order,
    onMarkPaid: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dateString = remember(order.timestamp) { dateFormatter.format(Date(order.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Order details and date
            Column(modifier = Modifier.weight(1f)) {
                Text(order.description, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Date: $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatMoney(order.amount, order.currency.name)} - ${if (order.isPaid) "Paid" else "Unpaid"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Right side: Mark Paid button (if unpaid) and Edit icon aligned cleanly
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!order.isPaid) {
                    Button(
                        onClick = onMarkPaid,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Mark Paid")
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Order",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}