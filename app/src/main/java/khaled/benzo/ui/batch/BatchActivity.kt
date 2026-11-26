package khaled.benzo.ui.batch

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import khaled.benzo.R
import khaled.benzo.core.batch.BatchPolicy
import khaled.benzo.data.repository.SettingsRepository
import khaled.benzo.ui.editor.EditorViewModel

/**
 * Batch loading dialog (written, unbuilt, unrendered). Layout order follows the reference video: Link, numeric range,
 * letter range, then Cancel and Generate. Built in code, so there is no XML to review.
 * UNFINISHED: the reference's "Advanced (path, speed, etc.)" section is not here; generated items use the default
 * folder and thread count. The items are stored PAUSED, because what the reference does after Generate was not seen.
 */
class BatchActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var link: EditText
    private lateinit var numbers: RadioButton
    private lateinit var letters: RadioButton
    private lateinit var from: EditText
    private lateinit var to: EditText
    private lateinit var step: EditText
    private lateinit var fromL: EditText
    private lateinit var toL: EditText
    private lateinit var preview: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun field(hint: String, type: Int, text: String, w: Int = 64) = EditText(this).apply {
        this.hint = hint; inputType = type; setText(text); minWidth = dp(w); gravity = android.view.Gravity.CENTER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(12)) }
        val titleRow = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        titleRow.addView(TextView(this).apply { setText(R.string.batch_title); textSize = 20f }, LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply {
            text = "?"; contentDescription = getString(R.string.batch_help_title)
            setOnClickListener { AlertDialog.Builder(this@BatchActivity).setTitle(R.string.batch_help_title).setMessage(R.string.batch_help).setPositiveButton(android.R.string.ok, null).show() }
        })
        root.addView(titleRow)
        link = field(getString(R.string.batch_link_hint), InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_CLASS_TEXT, "", 200)
        link.gravity = android.view.Gravity.START
        root.addView(link, LinearLayout.LayoutParams(-1, -2))

        numbers = RadioButton(this).apply { setText(R.string.batch_numbers); isChecked = true }
        letters = RadioButton(this).apply { setText(R.string.batch_letters) }
        val group = RadioGroup(this)
        group.addView(numbers); group.addView(letters)
        root.addView(group)
        numbers.setOnCheckedChangeListener { _, c -> if (c) letters.isChecked = false; updatePreview() }
        letters.setOnCheckedChangeListener { _, c -> if (c) numbers.isChecked = false; updatePreview() }

        val num = InputType.TYPE_CLASS_NUMBER
        val txt = InputType.TYPE_CLASS_TEXT
        from = field("0", num, "0"); to = field("9", num, "9"); step = field("1", num, "1")
        fromL = field("A", txt, "A", 48); toL = field("Z", txt, "Z", 48)
        root.addView(row(getString(R.string.batch_from) to from, getString(R.string.batch_to) to to, getString(R.string.batch_step) to step))
        root.addView(row(getString(R.string.batch_from) to fromL, getString(R.string.batch_to) to toL))
        preview = TextView(this).apply { setPadding(0, dp(12), 0, dp(4)) }
        root.addView(preview)
        root.addView(TextView(this).apply { setText(R.string.batch_paused_note); textSize = 12f })

        val buttons = LinearLayout(this).apply { gravity = android.view.Gravity.END }
        buttons.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply { setText(R.string.dialog_cancel); setOnClickListener { finish() } })
        buttons.addView(Button(this, null, android.R.attr.borderlessButtonStyle).apply { setText(R.string.batch_generate); setOnClickListener { generate() } })
        root.addView(buttons)
        setContentView(ScrollView(this).apply { addView(root) })

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = updatePreview()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        }
        listOf(link, from, to, step, fromL, toL).forEach { it.addTextChangedListener(watcher) }
        updatePreview()
    }

    private fun row(vararg cells: Pair<String, EditText>) = LinearLayout(this).apply {
        setPadding(dp(32), 0, 0, 0); gravity = android.view.Gravity.CENTER_VERTICAL
        cells.forEach { (label, edit) ->
            addView(TextView(this@BatchActivity).apply { text = label; setPadding(dp(6), 0, dp(6), 0) })
            addView(edit)
        }
    }

    private fun current(): BatchPolicy.Result {
        val range: BatchPolicy.Range = if (numbers.isChecked) {
            val f = from.text.toString().toLongOrNull(); val t = to.text.toString().toLongOrNull(); val s = step.text.toString().toLongOrNull()
            if (f == null || t == null) return BatchPolicy.Result.Error.BAD_NUMBER
            BatchPolicy.Range.Numbers(f, t, s ?: 0L)
        } else {
            val f = fromL.text.toString().singleOrNull(); val t = toL.text.toString().singleOrNull()
            if (f == null || t == null) return BatchPolicy.Result.Error.BAD_LETTER
            BatchPolicy.Range.Letters(f, t)
        }
        return BatchPolicy.expand(link.text.toString(), range)
    }

    private fun updatePreview() {
        if (!::preview.isInitialized) return
        preview.text = when (val r = current()) {
            is BatchPolicy.Result.Ok -> getString(R.string.batch_preview, r.links.size, r.links.first(), r.links.last())
            is BatchPolicy.Result.Error -> getString(when (r) {
                BatchPolicy.Result.Error.NO_PLACE -> R.string.batch_err_place
                BatchPolicy.Result.Error.NOT_WEB_LINK -> R.string.batch_err_web
                BatchPolicy.Result.Error.BAD_NUMBER -> R.string.batch_err_number
                BatchPolicy.Result.Error.BAD_STEP -> R.string.batch_err_step
                BatchPolicy.Result.Error.BAD_LETTER -> R.string.batch_err_letter
                BatchPolicy.Result.Error.EMPTY_RANGE -> R.string.batch_err_range
                BatchPolicy.Result.Error.TOO_MANY -> R.string.batch_err_many
            }, BatchPolicy.MAX_LINKS)
        }
    }

    private fun generate() {
        val r = current() as? BatchPolicy.Result.Ok
        if (r == null) { updatePreview(); return }
        val settings = SettingsRepository(this)
        val folder = settings.defaultDownloadPath.ifEmpty {
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath
        }
        val total = r.links.size
        // One at a time: each add checks the names already queued, so same-named files get distinct names.
        fun next(i: Int) {
            if (i >= total) {
                Toast.makeText(this, getString(R.string.batch_added, total), Toast.LENGTH_LONG).show(); finish(); return
            }
            val url = r.links[i]
            val name = runCatching { android.net.Uri.parse(url).path.orEmpty().substringAfterLast('/') }.getOrDefault("").ifEmpty { "download_" + System.currentTimeMillis() + "_" + i }
            viewModel.addDownload(
                url = url, fileName = name, savePath = folder, parts = settings.defaultThreadsCount, isWifiOnly = false,
                userAgent = null, referer = null, cookies = null, startNow = false,
                onError = { Toast.makeText(this, getString(R.string.batch_stopped, i, total), Toast.LENGTH_LONG).show(); finish() }
            ) { next(i + 1) }
        }
        findViewById<View>(android.R.id.content).isEnabled = false
        next(0)
    }
}
