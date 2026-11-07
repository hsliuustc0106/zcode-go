package com.zcodego.app.camera

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class LuminanceRotationTest {
    private val source = byteArrayOf(1, 2, 3, 4)

    @Test
    fun zeroAndFullTurnsStayInPlace() {
        val same = LuminanceRotation.rotate(source, 2, 2, 0)
        assertArrayEquals(source, same.bytes)
        assertEquals(2, same.width)
        val full = LuminanceRotation.rotate(source, 2, 2, 360)
        assertArrayEquals(source, full.bytes)
    }

    @Test
    fun clockwiseRightAngles() {
        val turned90 = LuminanceRotation.rotate(source, 2, 2, 90)
        assertEquals(2, turned90.width)
        assertEquals(2, turned90.height)
        assertArrayEquals(byteArrayOf(3, 1, 4, 2), turned90.bytes)

        val turned180 = LuminanceRotation.rotate(source, 2, 2, 180)
        assertArrayEquals(byteArrayOf(4, 3, 2, 1), turned180.bytes)

        val turned270 = LuminanceRotation.rotate(source, 2, 2, -90)
        assertArrayEquals(byteArrayOf(2, 4, 1, 3), turned270.bytes)
    }
}
