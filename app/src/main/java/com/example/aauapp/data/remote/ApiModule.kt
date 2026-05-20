package com.example.aauapp.data.remote

import com.example.aauapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object ApiModule {

    private val ngrokInterceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder()
            .addHeader("ngrok-skip-browser-warning", "true")
            .build()

        chain.proceed(request)
    }

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request()
            .newBuilder()
            .addHeader("X-Api-Key", BuildConfig.BACKEND_API_KEY)

        AuthTokenStore.token?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(ngrokInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val backendApi: BackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}