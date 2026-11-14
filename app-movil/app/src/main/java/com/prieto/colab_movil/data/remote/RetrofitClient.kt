package com.prieto.colab_movil.data.remote

import com.prieto.colab_movil.BuildConfig
import com.prieto.colab_movil.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = SessionManager.token
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val perfilApiService: PerfilApiService by lazy {
        retrofit.create(PerfilApiService::class.java)
    }

    val proyectoApiService: ProyectoApiService by lazy {
        retrofit.create(ProyectoApiService::class.java)
    }

    val planificacionApiService: PlanificacionApiService by lazy {
        retrofit.create(PlanificacionApiService::class.java)
    }

    val generacionPlanificacionApiService: PlanificacionApiService by lazy {
        retrofit.newBuilder()
            .client(clienteGeneracionPlanificacion(okHttpClient))
            .build()
            .create(PlanificacionApiService::class.java)
    }

    val misTareasApiService: MisTareasApiService by lazy {
        retrofit.create(MisTareasApiService::class.java)
    }

    val avanceApiService: AvanceApiService by lazy {
        // Un POST de avance no es idempotente. Evitar reintentos de conexión automáticos.
        retrofit.newBuilder()
            .client(okHttpClient.newBuilder().retryOnConnectionFailure(false).build())
            .build()
            .create(AvanceApiService::class.java)
    }
}

/** La IA no transmite datos mientras genera; no debe usar la espera corta del resto de la API. */
internal fun clienteGeneracionPlanificacion(base: OkHttpClient): OkHttpClient = base.newBuilder()
    .readTimeout(180, TimeUnit.SECONDS)
    .callTimeout(210, TimeUnit.SECONDS)
    // Un POST puede seguir ejecutándose en el servidor aunque se pierda la respuesta.
    .retryOnConnectionFailure(false)
    .build()
