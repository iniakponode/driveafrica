//package com.uoa.core.di
//
//import com.uoa.core.database.daos.EmbeddingDao
//import com.uoa.core.nlg.lngrepositoryimpl.local.EmbeddingUtilsRepositoryImpl
//import com.uoa.core.nlg.repository.EmbeddingUtilsRepository
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object EmbeddingUtilsRepoModule {
//
//    @Provides
//    @Singleton
//    fun provideEmbeddingUtilsRepository(embeddingDao: EmbeddingDao): EmbeddingUtilsRepository {
//        return EmbeddingUtilsRepositoryImpl(embeddingDao)
//    }
//}
