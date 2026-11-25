package khaled.benzo

import android.app.Application
import kotlinx.coroutines.launch
import khaled.benzo.service.NotificationHelper

class BenzoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        val codec = khaled.benzo.core.security.KeystoreKey.codec()
        khaled.benzo.core.security.Secrets.install(codec)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            khaled.benzo.core.security.SecretsMigrationRunner(this@BenzoApp, codec).run()
        }

        // Initialize notification channels
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()
    }

    companion object {
        lateinit var instance: BenzoApp
            private set
    }
}
