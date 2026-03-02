// Network.kt
package com.example.sellmatekioskapp

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object Network {

    // MUST end with /
    private const val BASE_URL = "https://sellmatecloud-1002770348452.us-west4.run.app/"

    private val json = Json {
        ignoreUnknownKeys = true   // your /orders/{id} returns extra fields (created_at, updated_at, etc.)
        isLenient = true
        explicitNulls = false
    }

    private fun okHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun createSellMateApi(): SellMateApi {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SellMateApi::class.java)
    }
}
