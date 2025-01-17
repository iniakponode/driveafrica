package com.uoa.alcoholquestionnaire.presentation.di

import com.uoa.alcoholquestionnaire.data.repository.AlcoholQuestionnaireRepoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.uoa.core.database.daos.AlcoholQuestionnaireResponseDao
import com.uoa.core.database.repository.QuestionnaireRepository

@Module
@InstallIn(SingletonComponent::class)
object ModulesProvider {


    @Provides
    @Singleton
    fun provideAlcoholQuestionnaireRepository(alcoholQDao: AlcoholQuestionnaireResponseDao): QuestionnaireRepository {
        return AlcoholQuestionnaireRepoImpl(alcoholQDao)
    }
}