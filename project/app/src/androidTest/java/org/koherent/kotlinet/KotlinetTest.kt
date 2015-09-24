package org.koherent.kotlinet

import android.test.ActivityInstrumentationTestCase2
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
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
}