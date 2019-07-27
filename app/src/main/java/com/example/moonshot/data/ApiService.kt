package com.example.moonshot.data

import android.util.Log
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {

//    @Multipart
    @POST("enroll")
    fun uploadFingerprint(
        @Body data: HashMap<String, String>
    ): Single<Response>

    companion object {

        private fun provideOkhttp(): OkHttpClient {
            return OkHttpClient.Builder().apply {
                readTimeout(20, TimeUnit.SECONDS)
                writeTimeout(20, TimeUnit.SECONDS)
                connectTimeout(20, TimeUnit.SECONDS)
                addNetworkInterceptor(HttpLoggingInterceptor {
                    Log.i("API Service", it)
                }.setLevel(HttpLoggingInterceptor.Level.BODY))
            }.build()
        }

        fun provideRetrofit(): Retrofit {
            return Retrofit.Builder().apply {
                client(provideOkhttp())
                baseUrl("http://192.168.1.90:8081/v1/biometrics/")
                addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                addConverterFactory(GsonConverterFactory.create())
            }.build()
        }
    }

    data class Response(
        val success: Boolean,
        val message: String?,
        val error: String?
    )
}