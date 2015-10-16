package org.koherent.kotlinet

import android.support.v7.app.ActionBarActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import java.io.File


public class MainActivity : ActionBarActivity() {

    var request: Request? = null

    var progress: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val p = findViewById(R.id.progress)
        if(p is ProgressBar) {
            progress = p
        }

        findViewById(R.id.download).setOnClickListener {
            println("start downloading")
            this.request = download(Method.GET, "http://circos.ca/guide/visual/img/circos-visualguide-med.png", File.createTempFile("prf","sf"))
            .progress { bytes, totalBytes, totalBytesExpectedToRead ->
                this.progress!!.max = totalBytesExpectedToRead.toInt()
                this.progress!!.progress = totalBytes.toInt()
                println("progress: ${this.progress!!.progress}/${this.progress!!.max}")
            }.response { url, httpURLConnection, bytes, exception ->
                println("finished")
                println("StatusCode: ${httpURLConnection?.responseCode}")
                println("error: ${exception}")
            }
        }

        findViewById(R.id.cancel).setOnClickListener {
            this.request?.cancel()
            this.progress!!.progress = 0
            println("cancelled")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.getItemId()

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
