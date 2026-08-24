package com.stepitacademy.shopledger.ui.customers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerScreen(
    initialName: String = "",
    initialPhone: String = "",
    isEditing: Boolean = false,
    onSave: (name: String, phone: String) -> Unit,
    onBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var showError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) } // Prevent double click

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Customer" else "Add Customer") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) showError = false
                },
                label = { Text("Customer Name *") },
                isError = showError,
                supportingText = { if (showError) Text("Name is required") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else if (!isSaving) {
                        isSaving = true // Lock button
                        onSave(name.trim(), phone.trim())
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Update Customer" else "Save Customer")
            }
        }
    }
}