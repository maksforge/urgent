package app.urgent.call

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.telephony.TelephonyManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What Android reads off the installed manifest. Nothing in Kotlin references the receiver — the OS
 * finds it by its intent filter — so a renamed class or a dropped filter compiles cleanly and leaves
 * the app deaf to every call.
 */
@RunWith(AndroidJUnit4::class)
class ManifestWiringTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun the_receiver_hears_phone_state() {
        val receivers =
            context.packageManager.queryBroadcastReceivers(
                Intent(TelephonyManager.ACTION_PHONE_STATE_CHANGED).setPackage(context.packageName),
                0,
            )
        val names = receivers.map { it.activityInfo.name }
        assertTrue("PHONE_STATE reaches $names", PhoneStateReceiver::class.java.name in names)
        assertTrue("the telephony process is another app", receivers.single().activityInfo.exported)
    }

    @Test
    fun the_ringing_service_is_a_media_playback_foreground_service() {
        val info =
            context.packageManager.getServiceInfo(
                ComponentName(context, RingThroughService::class.java),
                PackageManager.GET_META_DATA,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType)
        }
    }
}
