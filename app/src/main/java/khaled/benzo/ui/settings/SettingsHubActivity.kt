package khaled.benzo.ui.settings

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R

/** List of settings categories. Each opens [SettingsActivity] showing only that category. */
class SettingsHubActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_hub)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHub).setNavigationOnClickListener { finish() }
        val list = findViewById<LinearLayout>(R.id.hubList)
        val inflater = LayoutInflater.from(this)
        for (section in SettingsSection.entries) {
            val row = inflater.inflate(R.layout.item_settings_category, list, false)
            row.findViewById<TextView>(R.id.catTitle).setText(section.title)
            row.findViewById<TextView>(R.id.catSubtitle).setText(section.subtitle)
            row.findViewById<ImageView>(R.id.catIcon).apply {
                setImageResource(section.icon)
                background = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 12f * resources.displayMetrics.density; setColor(section.tile) }
            }
            row.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_SECTION, section.id))
            }
            list.addView(row)
        }
    }
}
