package zw.co.byrosolutions.landmarkguide

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import zw.co.byrosolutions.landmarkguide.logic.methods

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // inflate layout
            setContentView(R.layout.activity_settings)

            // below line is to change
            // the title of our action bar.

            // below line is to change
            // the title of our action bar.
            supportActionBar!!.title = "Settings"

            // below line is used to check if
            // frame layout is empty or not.

            // below line is used to check if
            // frame layout is empty or not.
            if (findViewById<View?>(R.id.idFrameLayout) != null) {
                if (savedInstanceState != null) {
                    return
                }
                // below line is to inflate our fragment.
                fragmentManager.beginTransaction().add(R.id.idFrameLayout, SettingsFragment())
                    .commit()
            }
        } catch (e: Exception) {
            methods.alertUser("Error", e.toString(), this)
        }
    }
}