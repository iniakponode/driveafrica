package com.uoa.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.PerformanceMetricsState.*
import androidx.metrics.performance.PerformanceMetricsState
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberMetricsStateHolder(): Holder {
    val localView= LocalView.current
    return remember(localView) {
        PerformanceMetricsState.getHolderForHierarchy(localView)
    }
}

@Composable
fun TrackJank(varargs: Any,
              reportMetrics: suspend  CoroutineScope.(state: Holder)->Unit,
              ) {
    val metrics= rememberMetricsStateHolder()
    LaunchedEffect(metrics, reportMetrics){
        reportMetrics(metrics)
    }
}

@Composable
fun TrackDisposableJank(vararg keys: Any, reportMetric: DisposableEffectScope.(state: Holder)->DisposableEffectResult, ) {
    val metrics= rememberMetricsStateHolder()
    DisposableEffect(metrics, *keys){
       reportMetric(this, metrics)
    }
}

@Composable
fun TrackScrollJank(scrollState: ScrollState, stateName: String) {
    TrackJank(scrollState) { metricsHolder->
       snapshotFlow {scrollState.isScrollInProgress}.collect{
           isScrollInProgress->
           metricsHolder.state?.apply {
                if(isScrollInProgress){
                     putState(stateName, "Scrolling=true")
                }else{
                     removeState(stateName)
                }
           }
       }
    }
}
