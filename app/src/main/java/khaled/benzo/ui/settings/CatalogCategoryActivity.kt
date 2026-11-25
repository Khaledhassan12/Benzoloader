package khaled.benzo.ui.settings

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import khaled.benzo.R
import khaled.benzo.core.catalog.FolderNames
import khaled.benzo.data.model.FileCategory
import khaled.benzo.data.repository.SettingsRepository
import java.io.File

/**
 * One cataloging category: the folder name (editable), the built-in extensions (shown, not editable yet) and the resulting folder.
 * The name is saved when the screen closes. A blank or unusable name goes back to the default name.
 */
class CatalogCategoryActivity : AppCompatActivity() {
    private lateinit var category: FileCategory
    private lateinit var repo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog_category)
        repo = SettingsRepository(this)
        category = FileCategory.entries.firstOrNull { it.key == intent.getStringExtra(EXTRA_CATEGORY) } ?: run { finish(); return }
        val names = repo.catalogFolderNames
        val toolbar = findViewById<Toolbar>(R.id.toolbarCategory)
        toolbar.title = names.nameFor(category)
        toolbar.setNavigationOnClickListener { finish() }
        val edit = findViewById<EditText>(R.id.editFolderName)
        edit.setText(names.nameFor(category))
        findViewById<TextView>(R.id.textExtensions).text =
            FileCategory.EXTENSIONS[category]?.joinToString(", ") ?: getString(R.string.cat_extensions_other)
        edit.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = showPath(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        showPath(edit.text.toString())
    }

    private fun showPath(name: String) {
        val clean = FolderNames.clean(name) ?: FolderNames.defaults[category].orEmpty()
        findViewById<TextView>(R.id.textFolderPath).text = File(repo.defaultDownloadPath, clean).path
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        if (!::category.isInitialized) return
        val current = repo.catalogFolderNames
        val all = FolderNames.editable.associateWith { current.nameFor(it).orEmpty() }.toMutableMap()
        all[category] = findViewById<EditText>(R.id.editFolderName).text.toString()
        repo.catalogFolderNames = FolderNames.fromInput(all)
    }

    companion object { const val EXTRA_CATEGORY = "category" }
}
