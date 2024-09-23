//package com.uoa.ml.presentation
//
//import com.uoa.core.database.daos.CauseDao
//import com.uoa.core.database.repository.CauseRepository
//import com.uoa.dbda.repository.CauseRepositoryImpl
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//
//@Module
//@InstallIn(SingletonComponent::class)
//object RepositoryModule {
//
//    @Provides
//    fun providesCauseRepository(causeDao: CauseDao): CauseRepository {
//        return com.uoa.dbda.repository.CauseRepositoryImpl(causeDao)
//    }
//}
