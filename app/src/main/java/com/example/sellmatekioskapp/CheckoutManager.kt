package com.example.sellmatekioskapp

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CheckoutManager(
    private val api: SellMateApi,
    private val machineId: String,
    private val baseUrl: String
) {
    var activeOrderId by mutableStateOf<String?>(null)
        private set

    var activeTotalPriceCents by mutableStateOf(0)
        private set

    var lastKnownStatus by mutableStateOf<String?>(null)
        private set

    fun setActiveOrder(orderId: String, totalPriceCents: Int) {
        activeOrderId = orderId
        activeTotalPriceCents = totalPriceCents
        lastKnownStatus = "CREATED"
    }

    fun reset() {
        activeOrderId = null
        activeTotalPriceCents = 0
        lastKnownStatus = null
    }

    suspend fun startCheckout(order: OrderDraft): Result<String> = withContext(Dispatchers.IO) {
        try {
            val items = order.toVendItems()

            if (items.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Order is empty")
                )
            }

            val totalCents = order.totalCents()

            val createResponse = api.createOrder(
                CreateOrderRequest(
                    machineId = machineId,
                    items = items,
                    amountCents = totalCents
                )
            )

            setActiveOrder(
                orderId = createResponse.orderId,
                totalPriceCents = totalCents
            )

            lastKnownStatus = createResponse.status

            val paymentResponse = api.startPayment(createResponse.orderId)

            lastKnownStatus = paymentResponse.status

            Result.success(createResponse.orderId)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string().orEmpty()

            Log.e(
                "CheckoutManager",
                "HTTP ${e.code()} during checkout. body=$errorBody",
                e
            )

            Result.failure(
                IllegalStateException(
                    "Backend returned HTTP ${e.code()}: $errorBody"
                )
            )
        } catch (e: Exception) {
            Log.e("CheckoutManager", "startCheckout failed", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCurrentOrderStatus(): OrderStatusResponse? {
        val id = activeOrderId ?: return null
        return fetchOrderStatus(id)
    }

    suspend fun cancelCurrentOrder(): Boolean {
        val id = activeOrderId ?: return false
        return cancelOrder(id)
    }

    suspend fun fetchOrderStatus(orderId: String): OrderStatusResponse? = withContext(Dispatchers.IO) {
        try {
            val response = api.getOrder(orderId)

            lastKnownStatus = response.status

            OrderStatusResponse(
                orderId = response.orderId,
                status = response.status,
                paymentIntentId = response.paymentIntentId,
                amountCents = response.amountCents ?: activeTotalPriceCents
            )
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string().orEmpty()

            Log.e(
                "CheckoutManager",
                "fetchOrderStatus HTTP ${e.code()} body=$errorBody",
                e
            )

            null
        } catch (e: Exception) {
            Log.e("CheckoutManager", "fetchOrderStatus failed", e)
            null
        }
    }

    suspend fun cancelOrder(orderId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${baseUrl.trimEnd('/')}/orders/$orderId/cancel")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject()
                .put("reason", "Cancelled from kiosk before payment")
                .toString()

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val code = conn.responseCode

            try {
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            } finally {
                conn.disconnect()
            }

            val success = code in 200..299

            if (success) {
                lastKnownStatus = "CANCELLED"
            } else {
                Log.e("CheckoutManager", "cancelOrder failed HTTP $code")
            }

            success
        } catch (e: Exception) {
            Log.e("CheckoutManager", "cancelOrder failed", e)
            false
        }
    }
}
