package khaled.benzo.ui.common

import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout

/** Title for the collapsing bars: big while expanded, small beside the back arrow once collapsed. */
object BarTitle {
    fun set(toolbar: Toolbar, title: CharSequence?) {
        val bar = toolbar.parent as? CollapsingToolbarLayout
        if (bar != null) bar.title = title else toolbar.title = title
    }
}
