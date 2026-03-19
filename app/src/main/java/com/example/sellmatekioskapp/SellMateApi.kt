package com.example.sellmatekioskapp

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SellMateApi {
    @POST("orders")
    suspend fun createOrder(@Body req: CreateOrderRequest): CreateOrderResponse

    @POST("orders/{orderId}/start_payment")
    suspend fun startPayment(@Path("orderId") orderId: String): StartPaymentResponse

    @GET("orders/{orderId}")
    suspend fun getOrder(@Path("orderId") orderId: String): OrderStatusResponse
}