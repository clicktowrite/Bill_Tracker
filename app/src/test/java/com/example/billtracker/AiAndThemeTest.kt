package com.example.billtracker

import com.example.billtracker.domain.ai.GgufHeaderParser
import com.example.billtracker.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AiAndThemeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testThemeModeFromStringMapping() {
        assertEquals(ThemeMode.FUTURISTIC, ThemeMode.fromString("FUTURISTIC"))
        assertEquals(ThemeMode.LIQUID_GLASS, ThemeMode.fromString("liquid_glass"))
        assertEquals(ThemeMode.CLAYMORPHISM, ThemeMode.fromString("CLAYMORPHISM"))
        assertEquals(ThemeMode.GLASSMORPHISM, ThemeMode.fromString("glassmorphism"))
        assertEquals(ThemeMode.FUTURISTIC, ThemeMode.fromString("DARK"))
        assertEquals(ThemeMode.GLASSMORPHISM, ThemeMode.fromString("LIGHT"))
        assertEquals(ThemeMode.GLASSMORPHISM, ThemeMode.fromString("UNKNOWN"))
    }

    @Test
    fun testGgufHeaderParserValid() {
        val file = tempFolder.newFile("qwen2.5-0.5b-instruct-q4_k_m.gguf")
        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(0x46554747) // GGUF magic
            header.putInt(3)          // version
            header.putLong(120L)      // tensor count
            header.putLong(45L)       // kv count
            fos.write(header.array())
        }

        val metadata = GgufHeaderParser.parse(file)
        assertNotNull(metadata)
        assertEquals(3, metadata?.version)
        assertEquals(120L, metadata?.tensorCount)
        assertEquals(45L, metadata?.kvCount)
        assertEquals("qwen", metadata?.architecture)
        assertEquals("Q4_K_M", metadata?.quantization)
    }

    @Test
    fun testGgufHeaderParserInvalidMagicRejected() {
        val file = tempFolder.newFile("invalid.gguf")
        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(0x12345678) // Invalid magic
            fos.write(header.array())
        }

        val metadata = GgufHeaderParser.parse(file)
        assertNull(metadata)
    }
}
