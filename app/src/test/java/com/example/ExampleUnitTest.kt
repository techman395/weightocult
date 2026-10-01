package com.example

import com.example.ble.CultScaleParser
import com.example.metrics.BodyComp
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testBodyCompositionEquations() {
        assertTrue("Body composition peer-reviewed assertions must pass", BodyComp.bodyCompSelfTest())
    }

    @Test
    fun testCultScaleBleFrameParsing() {
        assertTrue("Cult smart scale BLE frame parser assertions must pass", CultScaleParser.cultSelfTest())
    }
}
