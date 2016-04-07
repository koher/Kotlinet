package org.koherent.kotlinet

import android.support.v7.app.ActionBarActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast


public class MainActivity : ActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = Button(this)
        btn.text = "download"
        var req: Request? = null
        btn.setOnClickListener {
            if(req != null) {
                req!!.cancel()
                btn.text = "download"
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }else {
                btn.text = "cancel"

                req = request(Method.GET, "https://github.com/android/platform_frameworks_base/archive/master.zip")
                        .progress { bytes, bytesRead, totalBytes ->
                            android.util.Log.d("PROGRESS", "${bytesRead}/${totalBytes}")
                        }.response { url, urlConnection, bytes, exception ->
                            btn.text = "download"
                            req = null
                            Toast.makeText(this, "Finish: ${bytes?.size} bytes ${exception}", Toast.LENGTH_SHORT).show()
                        }
            }
        }

        (findViewById(android.R.id.content) as ViewGroup).addView(btn)
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
