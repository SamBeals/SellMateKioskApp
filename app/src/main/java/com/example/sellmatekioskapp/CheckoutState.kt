// CheckoutManager.kt
package com.example.sellmatekioskapp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class CheckoutState {
    data object Idle : CheckoutState()
    data object CreatingOrder : CheckoutState()
    data class StartingPayment(val orderId: String) : CheckoutState()
    data class AwaitingPayment(val orderId: String) : CheckoutState()
    data class Vending(val orderId: String) : CheckoutState()
    data class Success(val orderId: String) : CheckoutState()
    data class Failure(val message: String) : CheckoutState()
}

class CheckoutManager(
    private val api: SellMateApi,
    private val machineId: String
) {
    private val _state = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val state: StateFlow<CheckoutState> = _state

    suspend fun startCheckout(
        amountCents: Int,
        items: List<VendSequenceItem>,
        pollEveryMs: Long = 1200,
        timeoutMs: Long = 90_000
    ): Result<OrderStatusResponse> {
        try {
            if (items.isEmpty()) return Result.failure(IllegalStateException("Cart is empty."))

            _state.value = CheckoutState.CreatingOrder
            val order = api.createOrder(
                CreateOrderRequest(
                    machineId = machineId,
                    items = items,
                    amountCents = amountCents
                )
            )

            _state.value = CheckoutState.StartingPayment(order.orderId)
            api.startPayment(order.orderId)

            _state.value = CheckoutState.AwaitingPayment(order.orderId)

            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMs) {
                val status = api.getOrder(order.orderId)

                when (status.status) {
                    "CREATED",
                    "PAYMENT_STARTED",
                    "AWAITING_PAYMENT" -> {
                        _state.value = CheckoutState.AwaitingPayment(order.orderId)
                    }

                    "PAYMENT_SUCCEEDED" -> {
                        _state.value = CheckoutState.Vending(order.orderId)
                    }

                    "VEND_SUCCEEDED",
                    "COMPLETED" -> {
                        _state.value = CheckoutState.Success(order.orderId)
                        return Result.success(status)
                    }

                    "PAYMENT_FAILED",
                    "VEND_FAILED",
                    "ERROR" -> {
                        val msg = status.message ?: "Checkout failed."
                        _state.value = CheckoutState.Failure(msg)
                        return Result.failure(IllegalStateException(msg))
                    }

                    else -> {
                        // Unknown status — keep polling, but don't fail silently.
                        _state.value = CheckoutState.AwaitingPayment(order.orderId)
                    }
                }

                delay(pollEveryMs)
            }

            val msg = "Timed out waiting for payment/vend."
            _state.value = CheckoutState.Failure(msg)
            return Result.failure(IllegalStateException(msg))
        } catch (e: Exception) {
            _state.value = CheckoutState.Failure(e.message ?: "Unknown error")
            return Result.failure(e)
        }
    }

    fun reset() {
        _state.value = CheckoutState.Idle
    }
}
