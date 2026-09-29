package com.gps19.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ByteBufferTest {
    @Test
    fun testBufferOffsets() {
        val entryWriteBuffer = ByteBuffer.allocate(128).order(ByteOrder.nativeOrder())
        entryWriteBuffer.clear()
        
        entryWriteBuffer.putLong(1L)
        entryWriteBuffer.putDouble(2.0)
        entryWriteBuffer.putDouble(3.0)
        
        entryWriteBuffer.putFloat(4.0f)
        entryWriteBuffer.putFloat(5.0f)
        entryWriteBuffer.putFloat(6.0f)
        entryWriteBuffer.putFloat(7.0f)
        entryWriteBuffer.putFloat(8.0f)
        entryWriteBuffer.putFloat(9.0f)
        entryWriteBuffer.putFloat(10.0f)
        
        val posBeforeFlags = entryWriteBuffer.position()
        
        entryWriteBuffer.put(1.toByte()) // flags
        entryWriteBuffer.put(2.toByte()) // batt
        entryWriteBuffer.put(35.toByte()) // msgLen
        entryWriteBuffer.put(0.toByte()) // padding
        
        val posAfterPadding = entryWriteBuffer.position()
        
        assertEquals("Position before flags should be 52", 52, posBeforeFlags)
        assertEquals("Position after padding should be 56", 56, posAfterPadding)
        
        entryWriteBuffer.position(0)
        
        entryWriteBuffer.getLong()
        entryWriteBuffer.getDouble()
        entryWriteBuffer.getDouble()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        entryWriteBuffer.getFloat()
        
        val readFlags = entryWriteBuffer.get().toInt()
        val readBatt = entryWriteBuffer.get().toInt() and 0xFF
        val readMsgLen = entryWriteBuffer.get().toInt() and 0xFF
        entryWriteBuffer.get() // padding
        
        assertEquals(1, readFlags)
        assertEquals(2, readBatt)
        assertEquals(35, readMsgLen)
    }
}
