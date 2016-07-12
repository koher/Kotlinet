package org.koherent.kotlinet

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class KotlinetTest {
    @Rule @JvmField
    val folder = TemporaryFolder()

    @Test
    fun testBasic() {
        val signal = CountDownLatch(1)

        var result: String? = null

        request(Method.GET, "https://raw.githubusercontent.com/koher/Kotlinet/master/test-resources/basic.txt").response { url, urlConnection, bytes, exception ->
            if (bytes != null) {
                result = String(bytes, Charsets.UTF_8)
            }

            signal.countDown()
        }

        try {
            signal.await(5L, TimeUnit.SECONDS)
        } catch(e: InterruptedException) {
            fail(e.message)
            e.printStackTrace()
        }

        assertEquals("ABCDEFG\n", result)
    }

    @Test
    fun testDownload() {
        val signal = CountDownLatch(1)

        val destination = folder.newFile("master.zip")

        var result: ByteArray? = null

        try {
            download(Method.GET, "https://github.com/koher/Kotlinet/archive/master.zip", destination).response { url, urlConnection, bytes, exception ->
                result = bytes
                signal.countDown()
            }
        } catch(e: Exception) {
            signal.countDown()
        }

        try {
            signal.await(30L, TimeUnit.SECONDS)
        } catch(e: InterruptedException) {
            destination.delete()
            fail(e.message)
            e.printStackTrace()
        }

        val bytes = result
        if (bytes != null) {
            assertBytes(bytes, destination.readBytes())
        } else {
            fail()
        }
    }

    @Test
    fun testCancel(){
        val signal = CountDownLatch(1)

        var result: ByteArray? = null

        request(Method.GET, "https://github.com/android/platform_frameworks_base/archive/master.zip").response { url, urlConnection, bytes, exception ->
            result = bytes
            signal.countDown()
        }.cancel()

        try {
            signal.await(5L, TimeUnit.SECONDS)
        } catch(e: InterruptedException) {
            fail(e.message)
            e.printStackTrace()
        }

        assertNull(result)
    }

    private fun assertBytes(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.size, actual.size)

        for (i in 0..(Math.min(expected.size, actual.size) - 1)) {
            assertEquals(expected[i], actual[i])
        }
    }
}