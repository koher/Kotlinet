package org.koherent.kotlinet

import android.support.v7.app.ActionBarActivity
import android.os.Bundle
import android.os.Debug
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import java.io.File
import android.util.Log



public class MainActivity : ActionBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn = Button(this)
        btn.text = "DOWNLOAD"

        val destination = File.createTempFile("temp","")

        var downloading = false
        var down: Request? = null
        btn.setOnClickListener {
            if(!downloading) {
                runOnUiThread {
                    downloading = true
                    btn.text = "CANCEL"
                    down = download(Method.GET, "http://www.spitzer.caltech.edu/uploaded_files/images/0006/3034/ssc2008-11a12_Huge.jpg", destination)
                            .progress { a, b, c ->
                                Log.d("tag", "${a}/${b}/${c}")
                            }
                            .response { url, urlConnection, bytes, exception ->
                                Toast.makeText(this, "result ${bytes?.size}, ${exception?.message}", Toast.LENGTH_SHORT).show()
                                downloading = false
                                btn.text = "DOWNLOAD"
                            }
                }
            }else{
                runOnUiThread{
                    Toast.makeText(this, "cancelled", Toast.LENGTH_SHORT).show()
                    down?.cancel()
                    btn.text = "DOWNLOAD"
                    downloading = false
                }
            }
        }

        val view = findViewById(android.R.id.content) as ViewGroup;

        view.addView(btn);
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
