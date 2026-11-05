package com.jd.core.shared

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample

const val UI_UPDATE_INTERVAL_MS = 500L

/** Limits a data stream to 2Hz for display, keeping the latest value of each window. */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.atUiRate(): Flow<T> = sample(UI_UPDATE_INTERVAL_MS)
