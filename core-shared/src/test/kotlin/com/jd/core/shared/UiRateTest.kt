package com.jd.core.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies Chapter 120.4: a 10Hz stream reaches the UI at no more than 2Hz. */
@OptIn(ExperimentalCoroutinesApi::class)
class UiRateTest {

    @Test
    fun tenHertzStreamIsReducedToTwoHertzAndKeepsTheLatestValue() = runTest {
        val source = flow {
            repeat(20) {
                emit(it)
                delay(100)
            }
        }

        val received = source.atUiRate().toList()

        assertEquals(listOf(4, 9, 14, 19), received)
    }
}
