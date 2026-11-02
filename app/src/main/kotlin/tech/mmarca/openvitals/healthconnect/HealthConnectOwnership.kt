package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.Record

internal fun isOpenVitalsRecord(sourcePackageName: String, appPackageName: String): Boolean =
    sourcePackageName == appPackageName

internal fun Record.requireOpenVitalsOrigin(appPackageName: String) {
    val sourcePackageName = metadata.dataOrigin.packageName
    require(isOpenVitalsRecord(sourcePackageName, appPackageName)) {
        "Only records created by NCHealth can be edited."
    }
}
