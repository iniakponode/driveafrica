//package com.uoa.core.di
//
//import android.content.Context
//import com.uoa.core.mlclassifier.OrtEnvironmentWrapper
////import com.uoa.core.nlg.JsonContentBasedRAGEngine
////import com.uoa.core.nlg.RAGEngine
//import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object RAGEngineProviderModule {
//
////    @Provides
////    @Singleton
////    fun provideRAGEngine(
////        @ApplicationContext context: Context,
////        ortEnvironmentWrapper: OrtEnvironmentWrapper,
////        embeddingUtilsRepository: EmbeddingUtilsRepository
////    ): RAGEngine {
////        val modelPath = "path/to/your/model" // Replace with the actual model path
////        return RAGEngine(modelPath, ortEnvironmentWrapper.ortEnvironment, embeddingUtilsRepository)
////    }
//
////    @Provides
////    @Singleton
////    fun provideJsonContentBasedRageEngine(
////        @ApplicationContext context: Context,
////        ortEnvironmentWrapper: OrtEnvironmentWrapper,
////        embeddingUtilsRepository: EmbeddingUtilsRepository
////    ): JsonContentBasedRAGEngine {
////        val modelPath = "path/to/your/model" // Replace with the actual model path
////        return JsonContentBasedRAGEngine(modelPath, ortEnvironmentWrapper.ortEnvironment, embeddingUtilsRepository)
////    }
//}