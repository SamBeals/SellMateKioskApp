//// FakeSellMateApi.kt
//package com.example.sellmatekioskapp
//
//import kotlinx.coroutines.delay
//import java.util.UUID
//import java.util.concurrent.ConcurrentHashMap
//
///**
// * Dev-only fake server so your UI flow is runnable without Cloud Run.
// * Swap this to Retrofit when you're ready.
// */
//class FakeSellMateApi : SellMateApi {
//
//    private data class Order(
//        val orderId: String,
//        var status: String,
//        var polls: Int = 0,
//        val amountCents: Int,
//        val items: List<VendSequenceItem>
//    )
//
//    private val orders = ConcurrentHashMap<String, Order>()
//
//    override suspend fun createOrder(req: CreateOrderRequest): CreateOrderResponse {
//        delay(300)
//        val id = "ord_" + UUID.randomUUID().toString().take(8)
//        orders[id] = Order(
//            orderId = id,
//            status = "CREATED",
//            polls = 0,
//            amountCents = req.amountCents,
//            items = req.items
//        )
//        return CreateOrderResponse(orderId = id, status = "CREATED")
//    }
//
//    override suspend fun startPayment(orderId: String): StartPaymentResponse {
//        delay(250)
//        val o = orders[orderId] ?: return StartPaymentResponse(status = "ERROR", paymentIntentId = null)
//        o.status = "PAYMENT_STARTED"
//        return StartPaymentResponse(status = "PAYMENT_STARTED", paymentIntentId = "pi_fake_${o.orderId}")
//    }
//
//    override suspend fun getOrder(orderId: String): OrderStatusResponse {
//        delay(200)
//        val o = orders[orderId] ?: return OrderStatusResponse(orderId = orderId, status = "ERROR", message = "Order not found")
//
//        // Simple scripted progression:
//        // PAYMENT_STARTED -> AWAITING_PAYMENT -> PAYMENT_SUCCEEDED -> VEND_SUCCEEDED -> COMPLETED
//        o.polls += 1
//        o.status = when (o.status) {
//            "CREATED" -> "PAYMENT_STARTED"
//            "PAYMENT_STARTED" -> if (o.polls < 2) "AWAITING_PAYMENT" else "PAYMENT_SUCCEEDED"
//            "AWAITING_PAYMENT" -> if (o.polls < 3) "AWAITING_PAYMENT" else "PAYMENT_SUCCEEDED"
//            "PAYMENT_SUCCEEDED" -> if (o.polls < 4) "PAYMENT_SUCCEEDED" else "VEND_SUCCEEDED"
//            "VEND_SUCCEEDED" -> "COMPLETED"
//            else -> o.status
//        }
//
//        return OrderStatusResponse(orderId = orderId, status = o.status, message = null)
//    }
//}
