package khaled.benzo.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class PathPickerActivity : AppCompatActivity() {

    private lateinit var currentFolder: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listView = ListView(this)
        setContentView(listView)

        currentFolder = Environment.getExternalStorageDirectory()
        updateList(listView)

        listView.setOnItemClickListener { _, _, position, _ ->
            val files = currentFolder.listFiles()?.filter { it.isDirectory } ?: emptyList()
            if (position < files.size) {
                currentFolder = files[position]
                updateList(listView)
            }
        }
    }

    private fun updateList(listView: ListView) {
        title = currentFolder.absolutePath
        val subFolders = currentFolder.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subFolders)
        listView.adapter = adapter
    }

    override fun onBackPressed() {
        if (currentFolder.parentFile != null && currentFolder != Environment.getExternalStorageDirectory()) {
            currentFolder = currentFolder.parentFile!!
            val listView = findViewById<ListView>(android.R.id.list)
            if (listView != null) {
                updateList(listView)
            } else {
                finishWithResult()
            }
        } else {
            finishWithResult()
        }
    }

    private fun finishWithResult() {
        val intent = Intent().apply {
            putExtra("selected_path", currentFolder.absolutePath)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
