package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Whether Health Connect holds any record of a type in a window: one read of one
 * record per type. A type that cannot be read (no permission, not supported by the
 * provider, refused) counts as absent.
 */
internal class RecordPresenceHealthReader(
    private val support: HealthConnectReaderSupport,
) {
    suspend fun presentTypes(
        types: Collection<KClass<out Record>>,
        start: Instant,
        end: Instant,
    ): Set<KClass<out Record>> = types.filterTo(mutableSetOf()) { type ->
        support.withLogging("hasRecords[${type.simpleName}]", false) {
            support.client().readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 1,
                ),
            ).records.isNotEmpty()
        }
    }
}
