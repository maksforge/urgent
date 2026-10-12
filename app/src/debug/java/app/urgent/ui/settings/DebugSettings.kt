package app.urgent.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.urgent.R
import app.urgent.call.RingThroughService
import app.urgent.theme.Spacing
import app.urgent.ui.common.SectionHeader

/**
 * The developer-only section of Settings. **This file exists only in the debug source set** — see
 * the release half for why that is a source set and not a `BuildConfig.DEBUG` branch.
 */
@Composable
fun DebugSettings() {
    val context = LocalContext.current

    SectionHeader(stringResource(R.string.debug_section))
    DebugRow(stringResource(R.string.debug_ring)) { RingThroughService.start(context) }
    DebugRow(stringResource(R.string.debug_stop)) { RingThroughService.stop(context) }
}

@Composable
private fun DebugRow(
    title: String,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.base, vertical = Spacing.base),
    )
}
