package app.urgent.call

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SoundSettingsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun the_sound_settings_read_without_do_not_disturb_access() {
        assertNotNull(context.soundSettings())
    }
}
