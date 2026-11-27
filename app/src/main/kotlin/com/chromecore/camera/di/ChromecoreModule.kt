package com.chromecore.camera.di

import android.content.Context
import com.chromecore.camera.capabilities.DeviceCapabilitiesScanner
import com.chromecore.camera.engine.CameraEngine
import com.chromecore.camera.filters.FilterEngine
import com.chromecore.camera.processing.ImageProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChromecoreModule {

    @Provides
    @Singleton
    fun provideCameraEngine(@ApplicationContext context: Context): CameraEngine {
        return CameraEngine(context)
    }

    @Provides
    @Singleton
    fun provideImageProcessor(): ImageProcessor {
        return ImageProcessor()
    }

    @Provides
    @Singleton
    fun provideFilterEngine(): FilterEngine {
        return FilterEngine()
    }

    @Provides
    @Singleton
    fun provideDeviceCapabilitiesScanner(@ApplicationContext context: Context): DeviceCapabilitiesScanner {
        return DeviceCapabilitiesScanner(context)
    }
}
