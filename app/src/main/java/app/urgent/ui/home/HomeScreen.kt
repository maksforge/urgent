package app.urgent.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.urgent.R
import app.urgent.call.CALL_PERMISSIONS
import app.urgent.call.hasCallPermissions
import app.urgent.data.REPEAT_WINDOW_CHOICES
import app.urgent.theme.Spacing
import app.urgent.ui.appViewModelExtras
import app.urgent.ui.common.SectionHeader
import app.urgent.ui.common.SwitchRow
import app.urgent.ui.common.WarningBanner
import app.urgent.work.hasAutostartSettings
import app.urgent.work.isIgnoringBatteryOptimisations
import app.urgent.work.openAutostartSettings
import app.urgent.work.openBatteryOptimisationSettings
import app.urgent.work.startActivitySafely

/**
 * The whole app, really: the switch, the window, and everything Android or the ROM must allow before
 * the switch can do anything.
 *
 * Every grant below is a **live read re-taken on each resume**, because the fix for each one is a
 * system screen this one hands the user to, and they come back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory, extras = appViewModelExtras()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.pickMissed.collect {
            Toast.makeText(context, R.string.home_callers_no_number, Toast.LENGTH_LONG).show()
        }
    }

    var callPermissions by remember { mutableStateOf(context.hasCallPermissions()) }
    var batteryExempt by remember { mutableStateOf(context.isIgnoringBatteryOptimisations()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    callPermissions = context.hasCallPermissions()
                    batteryExempt = context.isIgnoringBatteryOptimisations()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val askPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            callPermissions = context.hasCallPermissions()
        }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { insets ->
        Column(modifier = Modifier.padding(insets).verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.home_intro),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = Spacing.base, vertical = Spacing.tight),
            )

            SwitchRow(
                title = stringResource(R.string.home_ring_through),
                subtitle = stringResource(R.string.home_ring_through_hint),
                checked = state.ringThrough,
                onChange = { enabled ->
                    viewModel.setRingThrough(enabled)
                    // The ask rides on the moment the feature is switched on, never at launch:
                    // Android allows two denials before the dialog stops appearing for good.
                    if (enabled && !callPermissions) askPermissions.launch(permissionsToAsk())
                },
            )

            SectionHeader(stringResource(R.string.home_window))
            FlowRow(modifier = Modifier.padding(horizontal = Spacing.base)) {
                for (minutes in REPEAT_WINDOW_CHOICES) {
                    FilterChip(
                        selected = state.windowMinutes == minutes,
                        onClick = { viewModel.setWindowMinutes(minutes) },
                        label = { Text(pluralStringResource(R.plurals.home_window_minutes, minutes, minutes)) },
                        modifier = Modifier.padding(end = Spacing.tight),
                    )
                }
            }

            SwitchRow(
                title = stringResource(R.string.home_count_declined),
                subtitle = stringResource(R.string.home_count_declined_hint),
                checked = state.countDeclined,
                onChange = viewModel::setCountDeclined,
            )

            // Only while the switch is on: a warning about a grant a disabled feature would need is
            // the app asking for something it is not using.
            if (state.ringThrough) {
                if (!callPermissions) {
                    WarningBanner(
                        title = stringResource(R.string.home_permissions_missing_title),
                        body = stringResource(R.string.home_permissions_missing_body),
                        actionLabel = stringResource(R.string.home_permissions_missing_action),
                        onAct = { context.openAppDetails() },
                    )
                }
                if (!batteryExempt) {
                    WarningBanner(
                        title = stringResource(R.string.home_battery_title),
                        body = stringResource(R.string.home_battery_body),
                        actionLabel = stringResource(R.string.home_battery_action),
                        onAct = { context.openBatteryOptimisationSettings() },
                    )
                }
                // Xiaomi's grant has no readable state, so it is a permanent row that claims nothing
                // rather than a banner that could never clear (work/Autostart.kt).
                if (context.hasAutostartSettings()) {
                    SectionHeader(stringResource(R.string.home_xiaomi))
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { context.openAutostartSettings() }
                                .padding(horizontal = Spacing.base, vertical = Spacing.base),
                    ) {
                        Text(stringResource(R.string.home_autostart), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(R.string.home_autostart_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            CallersSection(
                chosenCallers = state.chosenCallers,
                onAddTyped = viewModel::addChosenCaller,
                onAddFromContacts = viewModel::addChosenCallerFromContacts,
                onRemove = viewModel::removeChosenCaller,
            )
        }
    }
}

@Composable
private fun CallersSection(
    chosenCallers: Set<String>,
    onAddTyped: (String) -> Unit,
    onAddFromContacts: (Uri) -> Unit,
    onRemove: (String) -> Unit,
) {
    var typed by rememberSaveable { mutableStateOf("") }
    // The picker answers through a callback the Activity registered, not a return value.
    val pickContact =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let(onAddFromContacts)
        }
    val status = if (chosenCallers.isEmpty()) R.string.home_callers_anyone else R.string.home_callers_chosen

    SectionHeader(stringResource(R.string.home_callers))
    Text(
        text = stringResource(status),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.base),
    )
    for (number in chosenCallers.sorted()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base),
        ) {
            Text(
                number.split(' ').joinToString(" / "),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onRemove(number) }) {
                Text(stringResource(R.string.home_callers_remove))
            }
        }
    }
    OutlinedButton(
        onClick = {
            try {
                pickContact.launch(pickCallerIntent())
            } catch (e: ActivityNotFoundException) {
                // No Contacts app on this phone; the typed field below still works.
            }
        },
        modifier = Modifier.padding(horizontal = Spacing.base),
    ) {
        Text(stringResource(R.string.home_callers_from_contacts))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.base),
    ) {
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text(stringResource(R.string.home_callers_type)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
        )
        TextButton(
            enabled = storedCallerNumber(typed) != null,
            onClick = {
                onAddTyped(typed)
                typed = ""
            },
        ) {
            Text(stringResource(R.string.home_callers_add))
        }
    }
}

/**
 * The notification permission rides along on Android 13+: without it the ringing still happens, but
 * the notification with its Silence button is hidden.
 */
private fun permissionsToAsk(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        CALL_PERMISSIONS + Manifest.permission.POST_NOTIFICATIONS
    } else {
        CALL_PERMISSIONS
    }

private fun Context.openAppDetails(): Boolean =
    startActivitySafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
