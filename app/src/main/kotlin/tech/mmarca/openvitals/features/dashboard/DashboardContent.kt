package tech.mmarca.openvitals.features.dashboard

import tech.mmarca.openvitals.domain.model.DashboardMetric
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import tech.mmarca.openvitals.features.assistant.AssistantBriefCard
import tech.mmarca.openvitals.ui.components.staggeredEntrance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.features.activity.DeleteActivityConfirmationDialog
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.HealthConnectSyncStatusBanner
import java.time.LocalDate
import java.time.ZoneId

/**
 * The order the dashboard renders. Outside edit mode, carousel tiles with
 * no data sink below the ones with some, in a stable partition. Edit mode
 * keeps the saved order, so a drag lands where the card is.
 */
internal fun dashboardVisibleWidgetIds(
    dashboardWidgets: List<DashboardWidgetId>,
    specIds: Set<DashboardWidgetId>,
    display: DashboardDisplayState,
    isEditingDashboard: Boolean,
    placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
    sortEmptyTilesLast: Boolean = true,
    /** Metrics with records in the recent window; null until the first probe answers. */
    presentMetrics: Set<DashboardMetric>? = null,
): List<DashboardWidgetId> {
    val ordered = dashboardWidgets
        .filter { it in specIds }
        .filterNot { it in UnmeasuredWidgets }
        // Only what the wearable records through Google Health; editing shows every placed tile
        // so it can be dragged or removed.
        .filter { isEditingDashboard || it.isRecordedBy(presentMetrics, display) }
        // While editing, an unsupported metric the user never placed goes to the tray.
        .filterNot { isEditingDashboard && it in display.unsupportedIds && it !in placedWidgetIds }
    if (isEditingDashboard || !sortEmptyTilesLast) return ordered
    val fixed = dashboardWidgetIdsThatFitRows(ordered, DashboardFixedWidgetRows).toSet()
    val rest = ordered.filterNot { it in fixed }
    // On first load, hold the saved order until every tile has answered once.
    // Otherwise the grid would move on every arrival.
    if (rest.any { id -> display.widgets[id]?.isLoading == true && display.isDemoted(id) == null }) {
        return ordered
    }
    // An empty tile goes to the back; a setup offer does not. See [isDemoted].
    val (empty, withData) = rest.partition { id -> display.isDemoted(id) == true }
    return ordered.filter { it in fixed } + withData + empty
}

/**
 * Whether the tile has anything to show from the user's sources. Medical records are
 * not something a wearable writes, and the watch tile needs a paired device.
 */
internal fun DashboardWidgetId.isRecordedBy(
    presentMetrics: Set<DashboardMetric>?,
    display: DashboardDisplayState,
): Boolean = when (this) {
    DashboardWidgetId.MEDICAL_RECORDS -> false
    DashboardWidgetId.WATCH -> display.widgets[this]?.watch != null
    DashboardWidgetId.BODY_ENERGY ->
        presentMetrics == null || DashboardMetric.AVG_HEART_RATE in presentMetrics
    else -> presentMetrics == null || toDashboardMetricOrNull()?.let { it in presentMetrics } ?: true
}

/**
 * What a smartwatch or Fitbit Air can neither measure nor estimate: food and caffeine intake,
 * body composition a scale would give, and readings that need a cuff, a meter or a thermometer.
 * These never show, not even in the add tray. BMR stays, since Fitbit estimates it from the
 * profile and the wearable's data; water stays, for its reminders and quick manual entry.
 */
internal val UnmeasuredWidgets = setOf(
    DashboardWidgetId.CALORIES_IN,
    DashboardWidgetId.PROTEIN,
    DashboardWidgetId.CARBS,
    DashboardWidgetId.FAT,
    DashboardWidgetId.CAFFEINE,
    DashboardWidgetId.BODY_FAT,
    DashboardWidgetId.LEAN_MASS,
    DashboardWidgetId.BONE_MASS,
    DashboardWidgetId.BODY_WATER_MASS,
    DashboardWidgetId.FFMI,
    DashboardWidgetId.BLOOD_PRESSURE,
    DashboardWidgetId.BLOOD_GLUCOSE,
    DashboardWidgetId.BODY_TEMPERATURE,
    DashboardWidgetId.MEDICAL_RECORDS,
)

/** The edit-mode add tray: everything the grid does not show, in layout order. */
internal fun dashboardTrayWidgetIds(
    specIds: List<DashboardWidgetId>,
    visibleIds: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
): List<DashboardWidgetId> =
    if (isEditingDashboard) {
        specIds.filterNot { it in visibleIds || it in UnmeasuredWidgets }
    } else {
        emptyList()
    }

/** Today's activities: the workout list wins; the lone workout is the fallback. */
internal fun dashboardActivitiesForDay(data: DashboardData): List<ExerciseData> =
    data.workouts.ifEmpty { data.workout?.let(::listOf).orEmpty() }

private val DashboardScrollTopPadding = 4.dp
private val DashboardScrollBottomPadding = 24.dp
private val DashboardQuickActionsTopPadding = 14.dp

@Composable
internal fun DashboardContent(
    data: DashboardData,
    /** The day the user asked for, ahead of [data] while a switch loads. */
    selectedDate: LocalDate = data.date,
    display: DashboardDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    isRefreshing: Boolean,
    syncPaused: Boolean = false,
    /** When Health Connect's refusal ends; the banner counts down to it. Null while it answers. */
    rateLimitDeadlineEpochMillis: Long? = null,
    dashboardWidgets: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    sortEmptyTilesLast: Boolean = true,
    presentMetrics: Set<DashboardMetric>? = null,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onSelectDate: (LocalDate) -> Unit = {},
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onAddWidget: (DashboardWidgetId) -> Unit,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onSyncWatch: () -> Unit = {},
    onOpenActivities: () -> Unit,
    onOpenAssistant: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    /** The widgets the user placed. Only these keep an unsupported metric in the edit grid. */
    placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
) {
    val zone = ZoneId.systemDefault()
    val specWidgetIds = remember(dashboardWidgets, isEditingDashboard) {
        if (isEditingDashboard) {
            DashboardWidgetId.entries.toList()
        } else {
            dashboardWidgets
        }
    }
    // While a switch loads, the tiles read "loading" rather than the old day's numbers.
    // Ordering keeps the old day's partition until the new day lands.
    val awaitingSelectedDay = data.date != selectedDate
    val widgetDisplay = remember(display, awaitingSelectedDay) {
        if (awaitingSelectedDay) {
            display.copy(
                widgets = display.widgets.mapValues { (_, model) -> model.copy(isLoading = true) },
            )
        } else {
            display
        }
    }
    val specs = dashboardWidgetSpecs(
        display = widgetDisplay,
        unitFormatter = unitFormatter,
        widgetIds = specWidgetIds,
        isEditingDashboard = isEditingDashboard,
        onOpenMetric = onOpenMetric,
        onSyncWatch = onSyncWatch,
    )
    val specsById = remember(specs) { specs.associateBy { it.id } }
    val visibleIds = remember(
        dashboardWidgets,
        placedWidgetIds,
        specsById,
        display,
        isEditingDashboard,
        sortEmptyTilesLast,
        presentMetrics,
    ) {
        dashboardVisibleWidgetIds(
            dashboardWidgets = dashboardWidgets,
            specIds = specsById.keys,
            display = display,
            isEditingDashboard = isEditingDashboard,
            placedWidgetIds = placedWidgetIds,
            sortEmptyTilesLast = sortEmptyTilesLast,
            presentMetrics = presentMetrics,
        )
    }
    val hiddenSpecs = remember(isEditingDashboard, specs, visibleIds) {
        val trayIds = dashboardTrayWidgetIds(
            specIds = specs.map { it.id },
            visibleIds = visibleIds,
            isEditingDashboard = isEditingDashboard,
        ).toSet()
        specs.filter { it.id in trayIds }
    }
    var activityPendingDelete by remember { mutableStateOf<ExerciseData?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
        ) {
            if (syncPaused || rateLimitDeadlineEpochMillis != null) {
                HealthConnectSyncStatusBanner(
                    syncPaused = syncPaused,
                    syncInProgress = false,
                    rateLimitDeadlineEpochMillis = rateLimitDeadlineEpochMillis,
                    modifier = Modifier.padding(
                        start = DashboardScreenPadding,
                        end = DashboardScreenPadding,
                        top = DashboardScrollTopPadding,
                    ),
                )
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // No top bar on the summary: the list starts below the status bar and scrolls under it.
                contentPadding = PaddingValues(
                    top = DashboardScrollTopPadding +
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = DashboardScrollBottomPadding,
                ),
            ) {
                // Explicit keys, or a change above re-creates every sibling below.
                item(key = "day_navigator") {
                    DayNavigator(
                        date = selectedDate,
                        canGoForward = canGoForward,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onOpenCalendar = onOpenCalendar,
                        onOpenSettings = onOpenSettings,
                        onSelectDate = onSelectDate,
                        modifier = Modifier.staggeredEntrance(0).padding(
                            horizontal = DashboardScreenPadding,
                            vertical = 4.dp,
                        ),
                    )
                }

                // Claude speaks first: its latest check-in, or its greeting until there is one.
                item(key = "assistant_brief") {
                    AssistantBriefCard(
                        onOpenAssistant = onOpenAssistant,
                        modifier = Modifier
                            .staggeredEntrance(1)
                            .animateItem()
                            .padding(horizontal = DashboardScreenPadding)
                            .padding(top = 4.dp, bottom = 12.dp),
                    )
                }

                item(key = "hero") {
                    DashboardHeroCard(
                        data = data,
                        display = display,
                        modifier = Modifier
                            .staggeredEntrance(2)
                            .animateItem()
                            .padding(horizontal = DashboardScreenPadding)
                            .padding(bottom = 12.dp),
                    )
                }

                item(key = "widgets") {
                    // Reveal animations play once per day on display.
                    key(data.date) {
                        DashboardWidgetCarousel(
                            visibleIds = visibleIds,
                            specsById = specsById,
                            isEditingDashboard = isEditingDashboard,
                            onMoveWidgetToTarget = onMoveWidgetToTarget,
                            onRemoveWidget = onRemoveWidget,
                            actionContent = {
                                DashboardQuickActions(
                                    isEditingDashboard = isEditingDashboard,
                                    onOpenLog = onOpenLog,
                                    onStartActivity = onStartActivity,
                                    onToggleDashboardEdit = onToggleDashboardEdit,
                                    // 14dp under the rings; the divider carries its own air.
                                    modifier = Modifier.padding(
                                        start = DashboardScreenPadding,
                                        end = DashboardScreenPadding,
                                        top = DashboardQuickActionsTopPadding,
                                    ),
                                )
                            },
                            hiddenContent = {
                                if (isEditingDashboard) {
                                    DashboardHiddenWidgets(
                                        hiddenSpecs = hiddenSpecs,
                                        onAddWidget = onAddWidget,
                                    )
                                }
                            },
                        )
                    }
                }

                // The sensor-status card is absent: the top-bar battery action is the entry point.

                dashboardActivitiesToday(
                    // The old day's workouts must not sit under the new day's date.
                    workouts = if (awaitingSelectedDay) emptyList() else dashboardActivitiesForDay(data),
                    isLoading = awaitingSelectedDay,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivities = onOpenActivities,
                    onOpenActivity = onOpenActivity,
                    onEditActivity = onEditActivity,
                    onRequestDeleteActivity = { workout -> activityPendingDelete = workout },
                )

                item(key = "bottom_spacer") { Spacer(Modifier.height(10.dp)) }
            }
        }

        activityPendingDelete?.let { workout ->
            DeleteActivityConfirmationDialog(
                workout = workout,
                onDismiss = { activityPendingDelete = null },
                onConfirm = {
                    activityPendingDelete = null
                    onDeleteActivity(workout.id)
                },
            )
        }
    }
}
