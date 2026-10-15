package app.urgent.call

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallerContactLookupTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun grant_read_contacts() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.READ_CONTACTS)
    }

    @Test
    fun an_unknown_number_is_no_contact() {
        val unknown = "+48000000000"
        // callerContact turns a rejected query into NONE, so the query itself is asserted first.
        assertEquals(CallerContact.NONE, context.queryCallerContact(unknown))
        assertEquals(CallerContact.NONE, context.callerContact(unknown))
    }
}
