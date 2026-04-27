package com.ssafy.mobile.feature.sample.di

import android.content.Context
import com.ssafy.mobile.BuildConfig
import com.ssafy.mobile.feature.sample.data.local.db.SampleDatabase
import com.ssafy.mobile.feature.sample.data.local.preference.SamplePreferenceDataSource
import com.ssafy.mobile.feature.sample.data.remote.SampleApiService
import com.ssafy.mobile.feature.sample.data.repository.DefaultSampleRepository
import com.ssafy.mobile.feature.sample.domain.repository.SampleRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SampleDataModule {
    private const val SAMPLE_NETWORK_TIMEOUT_SEC = 15L

    @Volatile
    private var sampleRepository: SampleRepository? = null

    fun provideSampleRepository(context: Context): SampleRepository =
        sampleRepository ?: synchronized(this) {
            sampleRepository ?: createSampleRepository(context.applicationContext).also {
                sampleRepository = it
            }
        }

    private fun createSampleRepository(context: Context): SampleRepository {
        val database = SampleDatabase.getInstance(context)
        val apiService = createSampleApiService()
        val preferenceDataSource = SamplePreferenceDataSource.create(context)

        return DefaultSampleRepository(
            apiService = apiService,
            sampleTodoDao = database.sampleTodoDao(),
            preferenceDataSource = preferenceDataSource,
        )
    }

    private fun createSampleApiService(): SampleApiService {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        val okHttpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(SAMPLE_NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(SAMPLE_NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(SAMPLE_NETWORK_TIMEOUT_SEC, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

        return Retrofit
            .Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SampleApiService::class.java)
    }
}
