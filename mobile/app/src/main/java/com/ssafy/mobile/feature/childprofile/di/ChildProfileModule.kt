package com.ssafy.mobile.feature.childprofile.di

import com.ssafy.mobile.feature.childprofile.data.repository.FakeChildProfileRepository
import com.ssafy.mobile.feature.childprofile.domain.repository.ChildProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChildProfileModule {
    @Binds
    @Singleton
    abstract fun bindChildProfileRepository(
        fakeChildProfileRepository: FakeChildProfileRepository,
    ): ChildProfileRepository
}
