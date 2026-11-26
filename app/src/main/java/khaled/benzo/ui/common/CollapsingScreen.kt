package khaled.benzo.ui.common

import android.app.Activity
import android.util.TypedValue
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import khaled.benzo.R

/** Wraps a scrolling screen body in the collapsing top bar: big title expanded, small pinned bar with a back arrow when collapsed. */
object CollapsingScreen {
    private fun attr(a: Activity, id: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (a.theme.resolveAttribute(id, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) tv.data else fallback
    }

    fun wrap(a: Activity, title: CharSequence, content: View): CoordinatorLayout {
        val dp = a.resources.displayMetrics.density
        val bar = attr(a, R.attr.barBackground, 0xFF212121.toInt())
        val on = attr(a, R.attr.onBar, 0xFFFFFFFF.toInt())
        val root = CoordinatorLayout(a)
        val appBar = AppBarLayout(a).apply { setBackgroundColor(bar) }
        val toolbar = MaterialToolbar(a).apply {
            setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            navigationIcon?.setTint(on)
            setNavigationOnClickListener { a.finish() }
        }
        val ctl = CollapsingToolbarLayout(a).apply {
            this.title = title
            setContentScrimColor(bar)
            setCollapsedTitleTextColor(on)
            setExpandedTitleColor(on)
            expandedTitleMarginStart = (20 * dp).toInt()
            expandedTitleMarginBottom = (16 * dp).toInt()
            addView(toolbar, CollapsingToolbarLayout.LayoutParams(-1, (56 * dp).toInt()).apply {
                collapseMode = CollapsingToolbarLayout.LayoutParams.COLLAPSE_MODE_PIN
            })
        }
        appBar.addView(ctl, AppBarLayout.LayoutParams(-1, (144 * dp).toInt()).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
        })
        root.addView(appBar, CoordinatorLayout.LayoutParams(-1, -2))
        root.addView(content, CoordinatorLayout.LayoutParams(-1, -1).apply { behavior = AppBarLayout.ScrollingViewBehavior() })
        root.tag = ctl
        return root
    }

    fun setTitle(root: View, title: CharSequence) { ((root.tag) as? CollapsingToolbarLayout)?.title = title }
}
