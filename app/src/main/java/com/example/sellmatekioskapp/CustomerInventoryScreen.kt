// CustomerInventoryScreen.kt
package com.example.sellmatekioskapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerInventoryScreen(
    vm: CustomerInventoryViewModel,
    order: OrderDraft,
    onOpenCheckout: () -> Unit
) {
    val slots by vm.slots.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorText by vm.errorText.collectAsState()
    val linesBySlot by order.linesBySlot.collectAsState()

    LaunchedEffect(Unit) {
        if (slots.isEmpty() && !isLoading) vm.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Items") },
                actions = {
                    TextButton(onClick = { vm.load() }) { Text("Reload") }
                }
            )
        },
        floatingActionButton = {
            if (linesBySlot.isNotEmpty()) {
                FloatingActionButton(onClick = onOpenCheckout) {
                    Text(formatUsd(order.totalCents()))
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                errorText != null -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(errorText!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.load() }) { Text("Retry") }
                    }
                }
                slots.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No items available.")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.load() }) { Text("Reload") }
                    }
                }
                else -> {
                    LazyColumn {
                        items(slots) { slot ->
                            val product = slot.product
                            val disabled = !(slot.enabled && slot.inventory > 0 && (product?.name?.trim()?.isNotEmpty() == true))

                            ListItem(
                                headlineContent = { Text(product?.name?.takeIf { it.isNotBlank() } ?: "Empty") },
                                supportingContent = { Text(formatUsd(product?.priceCents ?: 0)) },
                                trailingContent = {
                                    Button(
                                        onClick = { order.add(slot) },
                                        enabled = !disabled
                                    ) { Text("Add to cart") }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}
