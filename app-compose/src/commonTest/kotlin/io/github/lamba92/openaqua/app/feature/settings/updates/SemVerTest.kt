package io.github.lamba92.openaqua.app.feature.settings.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {
    @Test
    fun testParse() {
        val v1 = SemVer.parseOrNull("1.2.3")
        assertNotNull(v1)
        assertEquals(1, v1.major)
        assertEquals(2, v1.minor)
        assertEquals(3, v1.patch)
        assertNull(v1.preRelease)

        val v2 = SemVer.parseOrNull("v2.0.4-beta.1")
        assertNotNull(v2)
        assertEquals(2, v2.major)
        assertEquals(0, v2.minor)
        assertEquals(4, v2.patch)
        assertEquals("beta.1", v2.preRelease)

        val v3 = SemVer.parseOrNull("1.0.0-SNAPSHOT")
        assertNotNull(v3)
        assertEquals(1, v3.major)
        assertEquals(0, v3.minor)
        assertEquals(0, v3.patch)
        assertEquals("SNAPSHOT", v3.preRelease)

        assertNull(SemVer.parseOrNull("invalid"))
    }

    @Test
    fun testComparison() {
        val v100 = SemVer.parseOrNull("1.0.0")!!
        val v101 = SemVer.parseOrNull("1.0.1")!!
        val v110 = SemVer.parseOrNull("1.1.0")!!
        val v200 = SemVer.parseOrNull("2.0.0")!!
        val v100Snapshot = SemVer.parseOrNull("1.0.0-SNAPSHOT")!!

        assertTrue(v101 > v100)
        assertTrue(v110 > v101)
        assertTrue(v200 > v110)
        assertTrue(v100 > v100Snapshot)
        assertEquals(0, v100.compareTo(SemVer.parseOrNull("v1.0.0")!!))
    }
}
