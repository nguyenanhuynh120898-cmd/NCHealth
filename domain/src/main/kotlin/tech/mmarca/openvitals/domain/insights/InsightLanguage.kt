package tech.mmarca.openvitals.domain.insights

/**
 * The language the readiness and stress engines write their sentences in. The app sets it
 * from the chosen locale at start-up; anything that never sets it (tests, tools) gets English.
 */
object InsightLanguage {
    @Volatile
    var vietnamese: Boolean = false
}

/** The English sentence, or its Vietnamese twin when the app runs in Vietnamese. */
internal fun tr(en: String, vi: String): String = if (InsightLanguage.vietnamese) vi else en
