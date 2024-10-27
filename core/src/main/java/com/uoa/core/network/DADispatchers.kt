package com.uoa.core.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val daDispatcher: DaDispatchers)

enum class DaDispatchers {
    IO, Default
}