package khaled.benzo

import android.app.Application
import khaled.benzo.service.NotificationHelper

class BenzoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize notification channels
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()
    }

    companion object {
        lateinit var instance: BenzoApp
            private set
    }
}
