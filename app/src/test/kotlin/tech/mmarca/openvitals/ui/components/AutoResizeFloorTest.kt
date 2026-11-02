package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Shrinking keeps most of the size the user chose. The floor scales with the text, in sp. */
class AutoResizeFloorTest {

    @Test
    fun `the floor keeps three fifths of the size`() {
        assertEquals(19.2.sp, autoResizeFloor(32.sp))
        assertEquals(10.sp, autoResizeFloor(16.sp))
    }

    @Test
    fun `small text does not shrink below the smallest size`() {
        // Under the 10 sp floor nothing shrinks further.
        assertEquals(10.sp, autoResizeFloor(11.sp))
    }

    @Test
    fun `a size that is not in sp is left alone`() {
        assertEquals(1.em, autoResizeFloor(1.em))
    }
}
