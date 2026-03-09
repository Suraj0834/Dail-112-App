package com.dial112.di

import android.content.Context
import androidx.room.Room
import com.dial112.BuildConfig
import com.dial112.data.local.database.Dial112Database
import com.dial112.data.remote.api.*
import com.dial112.data.repository.*
import com.dial112.domain.repository.*import com.dial112.utils.AuthInterceptor
import com.dial112.utils.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * AppModule - Hilt DI Module for application-level dependencies
 *
 * Provides:
 * - Room Database
 * - OkHttp Client (with auth interceptor + logging)
 * - Retrofit instance (for Node.js backend)
 * - All Repository bindings
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =========================================================================
    // DATABASE
    // =========================================================================

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): Dial112Database {
        return Room.databaseBuilder(
            context,
            Dial112Database::class.java,
            Dial112Database.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides @Singleton
    fun provideUserDao(db: Dial112Database) = db.userDao()

    @Provides @Singleton
    fun provideCaseDao(db: Dial112Database) = db.caseDao()

    @Provides @Singleton
    fun provideSosLogDao(db: Dial112Database) = db.sosLogDao()

    @Provides @Singleton
    fun provideHotspotDao(db: Dial112Database) = db.hotspotDao()

    // =========================================================================
    // NETWORK - OKHTTP CLIENT
    // =========================================================================

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)       // Attach JWT to all requests
            .addInterceptor(loggingInterceptor)    // Log request/response in debug
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // =========================================================================
    // RETROFIT - MAIN BACKEND (Node.js)
    // =========================================================================

    @Provides
    @Singleton
    @Named("MainRetrofit")
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // =========================================================================
    // API SERVICES
    // =========================================================================

    @Provides @Singleton
    fun provideAuthApiService(@Named("MainRetrofit") retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)

    @Provides @Singleton
    fun provideSosApiService(@Named("MainRetrofit") retrofit: Retrofit): SosApiService =
        retrofit.create(SosApiService::class.java)

    @Provides @Singleton
    fun provideCasesApiService(@Named("MainRetrofit") retrofit: Retrofit): CasesApiService =
        retrofit.create(CasesApiService::class.java)

    @Provides @Singleton
    fun provideVehicleApiService(@Named("MainRetrofit") retrofit: Retrofit): VehicleApiService =
        retrofit.create(VehicleApiService::class.java)

    @Provides @Singleton
    fun provideAiApiService(@Named("MainRetrofit") retrofit: Retrofit): AiApiService =
        retrofit.create(AiApiService::class.java)

    @Provides @Singleton
    fun providePcrVanApiService(@Named("MainRetrofit") retrofit: Retrofit): PcrVanApiService =
        retrofit.create(PcrVanApiService::class.java)

    @Provides @Singleton
    fun provideProfileApiService(@Named("MainRetrofit") retrofit: Retrofit): ProfileApiService =
        retrofit.create(ProfileApiService::class.java)

    @Provides @Singleton
    fun provideSosManagementApiService(@Named("MainRetrofit") retrofit: Retrofit): SosManagementApiService =
        retrofit.create(SosManagementApiService::class.java)

    @Provides @Singleton
    fun provideCriminalApiService(@Named("MainRetrofit") retrofit: Retrofit): CriminalApiService =
        retrofit.create(CriminalApiService::class.java)

    // =========================================================================
    // REPOSITORIES - Bind interface → implementation
    // =========================================================================

    @Provides @Singleton
    fun provideAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl

    @Provides @Singleton
    fun provideSosRepository(impl: SosRepositoryImpl): SosRepository = impl

    @Provides @Singleton
    fun provideCasesRepository(impl: CasesRepositoryImpl): CasesRepository = impl

    @Provides @Singleton
    fun provideVehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository = impl

    @Provides @Singleton
    fun provideAiRepository(impl: AiRepositoryImpl): AiRepository = impl

    @Provides @Singleton
    fun providePcrVanRepository(impl: PcrVanRepositoryImpl): PcrVanRepository = impl

    @Provides @Singleton
    fun provideProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository = impl

    @Provides @Singleton
    fun provideSosManagementRepository(impl: SosManagementRepositoryImpl): SosManagementRepository = impl

    @Provides @Singleton
    fun provideCriminalRepository(impl: CriminalRepositoryImpl): CriminalRepository = impl
}
