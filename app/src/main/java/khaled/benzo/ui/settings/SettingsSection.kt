package khaled.benzo.ui.settings

import khaled.benzo.R

/** One entry of the settings hub. [id] matches the section container in activity_settings.xml (section_<id>). */
enum class SettingsSection(val id: String, val title: Int, val subtitle: Int, val icon: Int, val tile: Int) {
    DOWNLOADING("downloading", R.string.cat_downloading, R.string.cat_downloading_sub, R.drawable.ic_cat_downloading, 0xFF4F8EF7.toInt()),
    TORRENT("torrent", R.string.cat_torrent, R.string.cat_torrent_sub, R.drawable.ic_cat_torrent, 0xFF5BB85D.toInt()),
    NOTIFICATIONS("notifications", R.string.cat_notifications, R.string.cat_notifications_sub, R.drawable.ic_cat_notifications, 0xFF8E6CF0.toInt()),
    AUTOMATION("automation", R.string.cat_automation, R.string.cat_automation_sub, R.drawable.ic_cat_automation, 0xFFF0715F.toInt()),
    CATALOGING("cataloging", R.string.cat_cataloging, R.string.cat_cataloging_sub, R.drawable.ic_cat_cataloging, 0xFF2BA69A.toInt()),
    SCHEDULER("scheduler", R.string.cat_scheduler, R.string.cat_scheduler_sub, R.drawable.ic_cat_scheduler, 0xFFE0568C.toInt()),
    BROWSER("browser", R.string.cat_browser, R.string.cat_browser_sub, R.drawable.ic_cat_browser, 0xFF6C7AE0.toInt());

    companion object {
        fun fromId(id: String?): SettingsSection? = entries.firstOrNull { it.id == id }
    }
}
