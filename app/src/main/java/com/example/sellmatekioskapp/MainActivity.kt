// MainActivity.kt
package com.example.sellmatekioskapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sellmatekioskapp.ui.theme.SellMateKioskAppTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private sealed class Screen {
    data object Inventory : Screen()
    data class ItemDetails(val slotId: String) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: swap later when you implement machine registration / pairing
        val machineId = "machine_001"

        // Real implementation (no throw -> no "unreachable code")
        val planogramService: PlanogramService =
            FirestorePlanogramService(db = Firebase.firestore)

        setContent {
            SellMateKioskAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var screen by remember { mutableStateOf<Screen>(Screen.Inventory) }

                    val vm: CustomerInventoryViewModel = viewModel(
                        factory = CustomerInventoryViewModelFactory(
                            machineId = machineId,
                            service = planogramService
                        )
                    )

                    when (val s = screen) {
                        Screen.Inventory -> {
                            CustomerInventoryRoute(
                                viewModel = vm,
                                onTileSelected = { slotId ->
                                    screen = Screen.ItemDetails(slotId)
                                }
                            )
                        }

                        is Screen.ItemDetails -> {
                            ItemDetailsPlaceholderScreen(
                                slotId = s.slotId,
                                onBack = { screen = Screen.Inventory }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerInventoryRoute(
    viewModel: CustomerInventoryViewModel,
    onTileSelected: (String) -> Unit
) {
    val slots by viewModel.slots.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorText by viewModel.errorText.collectAsState(initial = null)

    // Load once when the route first appears
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingOverlay()

            errorText != null -> ErrorOverlay(
                message = errorText ?: "Unknown error",
                onRetry = { viewModel.load() }
            )

            else -> {
                // ViewModel gives SlotUi, grid wants ProductTile -> map here
                val tiles = remember(slots) { slots.map { it.toProductTile() } }

                InventoryGridScreen(
                    items = tiles,
                    onItemClicked = { tile ->
                        onTileSelected(tile.slotId)
                    }
                )
            }
        }
    }
}

private fun SlotUi.toProductTile(): ProductTile {
    val p = this.product
    val safeName = p?.name?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unknown"

    return ProductTile(
        slotId = this.slotId,
        name = safeName,
        priceCents = p?.priceCents ?: 0,
        quantity = this.inventory,
        imageUrl = p?.imageUrl
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun ItemDetailsPlaceholderScreen(
    slotId: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Selected slot: $slotId",
            style = MaterialTheme.typography.titleLarge
        )
        Text(text = "Replace this placeholder with your item details / checkout flow.")
        Button(onClick = onBack) { Text("Back") }
    }
}

private class CustomerInventoryViewModelFactory(
    private val machineId: String,
    private val service: PlanogramService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerInventoryViewModel::class.java)) {
            return CustomerInventoryViewModel(machineId, service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
