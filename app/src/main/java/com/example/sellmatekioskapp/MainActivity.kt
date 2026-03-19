package com.example.sellmatekioskapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sellmatekioskapp.ui.theme.SellMateKioskAppTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private sealed class Screen {
    data object Inventory : Screen()
    data object Checkout : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val machineId = "machine_001"
        val baseUrl = "https://sellmatecloud-1002770348452.us-west4.run.app/"
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

                    val order = remember { OrderDraft() }

                    val api = remember {
                        Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(SellMateApi::class.java)
                    }

                    val checkoutManager = remember {
                        CheckoutManager(
                            api = api,
                            machineId = machineId,
                            baseUrl = baseUrl
                        )
                    }

                    when (screen) {
                        Screen.Inventory -> {
                            CustomerInventoryScreen(
                                vm = vm,
                                order = order,
                                onOpenCheckout = {
                                    if (order.totalCents() > 0) {
                                        checkoutManager.reset()
                                        screen = Screen.Checkout
                                    }
                                }
                            )
                        }

                        Screen.Checkout -> {
                            CheckoutScreen(
                                order = order,
                                checkout = checkoutManager,
                                onBack = {
                                    screen = Screen.Inventory
                                },
                                onFinished = {
                                    checkoutManager.reset()

                                    // If your OrderDraft has a clear() method, keep this.
                                    // If yours uses a different name, swap it here.
                                    order.clear()

                                    screen = Screen.Inventory
                                },
                                onReturnHome = {
                                    screen = Screen.Inventory
                                }
                            )
                        }
                    }
                }
            }
        }
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