package com.jd.core.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * OneHudModelTest (SIMP-NOV6.3-S1, Nov22.0): HudState is the only HUD model; the unused second model
 * HudViewState was deleted and must not come back.
 */
class OneHudModelTest {

    @Test
    fun hudStateIsTheOnlyHudModel() {
        assertNotNull(Class.forName("com.jd.core.ui.HudState"))
        assertThrows("HudViewState is a second HUD model; use HudState", ClassNotFoundException::class.java) {
            Class.forName("com.jd.core.ui.HudViewState")
        }
    }
}
