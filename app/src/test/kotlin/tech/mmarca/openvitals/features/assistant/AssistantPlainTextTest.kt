package tech.mmarca.openvitals.features.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantPlainTextTest {
    @Test
    fun `bold markers, headings and dash bullets become plain text`() {
        val raw = "## Sáng nay\n**Ngủ tốt**, bạn nhé.\n- Uống nước\n* Đi bộ 20 phút"

        assertEquals("Sáng nay\nNgủ tốt, bạn nhé.\n• Uống nước\n• Đi bộ 20 phút", raw.asPlainText())
    }
}
