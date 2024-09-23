//package com.uoa.core.network.di
//
//import android.content.Context
//import com.uoa.core.network.TimeZoneMonitorImpl
//import com.uoa.core.utils.TimeZoneMonitor
//import dagger.Provides
//import dagger.hilt.android.qualifiers.ApplicationContext
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object TimeZoneModulProvider {
//    @Provides
//    @Singleton
//    fun provideTimeZoneMonitor(@ApplicationContext context: Context, ): TimeZoneMonitor = TimeZoneMonitorImpl(context, timeZoneFlow)
//}