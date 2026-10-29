package com.example.focusfortvs.di

import android.content.Context
import androidx.room.Room
import com.example.focusfortvs.data.local.FocusFortDatabase
import com.example.focusfortvs.data.local.dao.BlockedAppDao
import com.example.focusfortvs.data.local.dao.ProfileDao
import com.example.focusfortvs.data.local.dao.SessionDao
import com.example.focusfortvs.data.network.AzureOpenAIApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFocusFortDatabase(
        @ApplicationContext context: Context,
    ): FocusFortDatabase =
        Room.databaseBuilder(
            context,
            FocusFortDatabase::class.java,
            FocusFortDatabase.DB_NAME,
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProfileDao(database: FocusFortDatabase): ProfileDao = database.profileDao()

    @Provides
    fun provideBlockedAppDao(database: FocusFortDatabase): BlockedAppDao = database.blockedAppDao()

    @Provides
    fun provideSessionDao(database: FocusFortDatabase): SessionDao = database.sessionDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("https://placeholder.url/") // Dynamic URL passed in call
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAzureOpenAIApi(retrofit: Retrofit): AzureOpenAIApi {
        return retrofit.create(AzureOpenAIApi::class.java)
    }
}
