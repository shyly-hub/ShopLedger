package com.stepitacademy.shopledger.orders

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stepitacademy.shopledger.data.Currency
import com.stepitacademy.shopledger.data.Order
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOrderScreen(
    customerName: String = "",
    orderToEdit: Order? = null,
    onSave: (description: String, amount: Long, currency: Currency, timestamp: Long, isPaid: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var description by remember(orderToEdit) { mutableStateOf(orderToEdit?.description ?: "") }

    var amountText by remember(orderToEdit) {
        mutableStateOf(
            if (orderToEdit != null) {
                if (orderToEdit.currency == Currency.USD) (orderToEdit.amount / 100.0).toString()
                else orderToEdit.amount.toString()
            } else ""
        )
    }

    var selectedCurrency by remember(orderToEdit) { mutableStateOf(orderToEdit?.currency ?: Currency.KHR) }
    var isPaid by remember(orderToEdit) { mutableStateOf(orderToEdit?.isPaid ?: false) }
    var selectedTimestamp by remember(orderToEdit) { mutableStateOf(orderToEdit?.timestamp ?: System.currentTimeMillis()) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedTimestamp = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (orderToEdit == null) {
                        if (customerName.isNotBlank()) "Add Order for $customerName" else "Add Order"
                    } else {
                        "Edit Order"
                    }
                    Text(titleText)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(if (selectedCurrency == Currency.USD) "Amount ($)" else "Amount (Riel)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = dateFormatter.format(Date(selectedTimestamp)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Order Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                enabled = false
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedCurrency = Currency.KHR },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCurrency == Currency.KHR)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("KHR")
                }
                Button(
                    onClick = { selectedCurrency = Currency.USD },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCurrency == Currency.USD)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("USD")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isPaid,
                    onClick = { isPaid = false },
                    label = { Text("Unpaid (Debt)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isPaid,
                    onClick = { isPaid = true },
                    label = { Text("Paid") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val parsedDouble = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && parsedDouble > 0.0) {
                        val finalAmount = if (selectedCurrency == Currency.USD) {
                            Math.round(parsedDouble * 100)
                        } else {
                            parsedDouble.toLong()
                        }
                        onSave(description.trim(), finalAmount, selectedCurrency,
                            selectedTimestamp, isPaid)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (orderToEdit == null) "Save Order" else "Update Order")
            }
        }
    }
}