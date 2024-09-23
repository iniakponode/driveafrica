// DI provider class for OnnxModelRunner
package com.uoa.core.di
import android.content.Context
import com.uoa.core.mlclassifier.OnnxModelRunner
import com.uoa.core.mlclassifier.OrtEnvironmentWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OnnxModelRunnerProvider {

    @Provides
    @Singleton
    fun provideOrtEnvironmentWrapper(): OrtEnvironmentWrapper {
        return OrtEnvironmentWrapper()
    }

    @Provides
    @Singleton
    fun provideOnnxModelRunner(@ApplicationContext context: Context, ortEnvironmentWrapper: OrtEnvironmentWrapper): OnnxModelRunner {
        return OnnxModelRunner(context, ortEnvironmentWrapper)
    }
}