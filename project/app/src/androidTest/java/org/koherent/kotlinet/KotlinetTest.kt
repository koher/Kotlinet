package org.koherent.kotlinet

import android.test.ActivityInstrumentationTestCase2
import android.test.RenamingDelegatingContext
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

public class KotlinetTest : ActivityInstrumentationTestCase2<MainActivity>(MainActivity::class.java) {
    public fun testBasic() {
        val signal = CountDownLatch(1)

        var result: String? = null

        runTestOnUiThread {
            request(Method.GET, "https://raw.githubusercontent.com/koher/Kotlinet/master/test-resources/basic.txt").response { url, urlConnection, bytes, exception ->
                if (bytes != null) {
                    result = String(bytes, Charsets.UTF_8)
                }

                signal.countDown()
            }
        }

        try {
            signal.await(5L, TimeUnit.SECONDS)
        } catch(e: InterruptedException) {
            fail(e.getMessage())
            e.printStackTrace()
        }

        if (result != null) {
            assertEquals("ABCDEFG\n", result)
        } else {
            fail()
        }
    }

    public fun testDownload() {
        val context = RenamingDelegatingContext(activity, "__kotlinet__")

        val signal = CountDownLatch(1)

        val destination = File(context.filesDir, "master.zip")
        destination.delete()

        var result: ByteArray? = null

        runTestOnUiThread {
            try {
                download(Method.GET, "https://github.com/koher/Kotlinet/archive/master.zip", destination).response { url, urlConnection, bytes, exception ->
                    result = bytes
                    signal.countDown()
                }
            } catch(e: Exception) {
                signal.countDown()
            }
        }

        try {
            signal.await(30L, TimeUnit.SECONDS)
        } catch(e: InterruptedException) {
            destination.delete()
            fail(e.getMessage())
            e.printStackTrace()
        }

        val bytes = result
        if (bytes != null) {
            assertBytes(bytes, destination.readBytes())
        } else {
            fail()
        }

        destination.delete()
    }

    private fun assertBytes(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.size(), actual.size(), "Different size")

        for (i in 0..(Math.min(expected.size(), actual.size()) - 1)) {
            assertEquals(expected[i], actual[i], "At <$i> / <${expected.size()}>")
        }
    }
}