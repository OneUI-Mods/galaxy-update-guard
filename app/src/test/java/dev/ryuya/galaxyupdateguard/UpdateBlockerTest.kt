package dev.ryuya.galaxyupdateguard

import java.io.IOException
import java.util.LinkedHashMap
import java.util.Map
import kotlin.jvm.JvmField
import org.junit.Assert.*
import org.junit.Test

open class UpdateBlockerTest {
    open class Fake : UpdateBlocker.Backend, UpdateBlocker.Store {
        @JvmField
        var values: MutableMap<String?, Int?> =
            LinkedHashMap<String?, Int?>(
                Map.of<String?, Int?>(UpdateBlockerTest.FIRST, 0, UpdateBlockerTest.SECOND, 1)
            )
        @JvmField var journal: UpdateBlocker.Journal? = null
        @JvmField var fail: String? = null
        @JvmField var persistFails: Boolean = false
        @JvmField var refuseChange: Boolean = false
        @JvmField var writes: Int = 0

        override fun states(): MutableMap<String?, Int?>? {
            return Map.copyOf<String?, Int?>(values)
        }

        @Throws(Exception::class)
        override fun set(name: String?, state: Int) {
            writes++
            if (fail == name) {
                fail = null
                throw IOException("One write failed")
            }
            if (!refuseChange) values.put(name, state)
        }

        override fun load(): UpdateBlocker.Journal? {
            return journal
        }

        @Throws(Exception::class)
        override fun save(next: UpdateBlocker.Journal) {
            if (persistFails) throw IOException("Disk full")
            journal = next
        }

        override fun clear() {
            journal = null
        }

        open fun controller(): UpdateBlocker {
            return UpdateBlocker(this, this)
        }
    }

    @Test
    @Throws(Exception::class)
    open fun roundTripRestoresDefaultAndExplicitEnabled() {
        val fake = UpdateBlockerTest.Fake()
        val original = Map.copyOf<String?, Int?>(fake.values)
        fake.controller().block()
        assertEquals(
            Map.of<String?, Int?>(UpdateBlockerTest.FIRST, 3, UpdateBlockerTest.SECOND, 3),
            fake.values,
        )
        assertTrue(fake.journal!!.requested)
        fake.controller().restore()
        assertEquals(original, fake.values)
        assertNull(fake.journal)
    }

    @Test
    @Throws(Exception::class)
    open fun initiallyDisabledServicesStayDisabledAfterRestore() {
        val fake = UpdateBlockerTest.Fake()
        fake.values.put(UpdateBlockerTest.SECOND, 2)
        fake.controller().block()
        fake.controller().restore()
        assertEquals(2, fake.values.get(UpdateBlockerTest.SECOND))
    }

    @Test
    @Throws(Exception::class)
    open fun repeatedBlockNeverOverwritesOriginalState() {
        val fake = UpdateBlockerTest.Fake()
        fake.controller().block()
        fake.controller().block()
        fake.controller().restore()
        assertEquals(0, fake.values.get(UpdateBlockerTest.FIRST))
    }

    @Test
    @Throws(Exception::class)
    open fun failedBlockRollsBackEveryTarget() {
        val fake = UpdateBlockerTest.Fake()
        val original = Map.copyOf<String?, Int?>(fake.values)
        fake.fail = UpdateBlockerTest.SECOND
        assertThrows(IOException::class.java, { fake.controller().block() })
        assertEquals(original, fake.values)
        assertNull(fake.journal)
    }

    @Test
    open fun failedSaveCannotChangePackages() {
        val fake = UpdateBlockerTest.Fake()
        fake.persistFails = true
        assertThrows(IOException::class.java, { fake.controller().block() })
        assertEquals(0, fake.writes)
    }

    @Test
    @Throws(Exception::class)
    open fun restoreFailureRetainsOriginalStateForRetry() {
        val fake = UpdateBlockerTest.Fake()
        fake.controller().block()
        fake.fail = UpdateBlockerTest.FIRST
        assertThrows(IOException::class.java, { fake.controller().restore() })
        assertNotNull(fake.journal)
        assertFalse(fake.journal!!.requested)
        fake.controller().restore()
        assertNull(fake.journal)
        assertEquals(0, fake.values.get(UpdateBlockerTest.FIRST))
    }

    @Test
    @Throws(Exception::class)
    open fun processDeathDuringBlockCanBeRecovered() {
        val fake = UpdateBlockerTest.Fake()
        val original = Map.copyOf<String?, Int?>(fake.values)
        fake.save(UpdateBlocker.Journal(original, true))
        fake.values.put(UpdateBlockerTest.FIRST, 3)
        fake.controller().restore()
        assertEquals(original, fake.values)
        assertNull(fake.journal)
    }

    @Test
    @Throws(Exception::class)
    open fun unownedDisableCannotBeUndone() {
        val fake = UpdateBlockerTest.Fake()
        fake.values.put(UpdateBlockerTest.FIRST, 3)
        fake.controller().restore()
        assertEquals(0, fake.writes)
        assertEquals(3, fake.values.get(UpdateBlockerTest.FIRST))
    }

    @Test
    open fun verificationFailureIsNotSuccess() {
        val fake = UpdateBlockerTest.Fake()
        fake.refuseChange = true
        assertThrows(IllegalStateException::class.java, { fake.controller().block() })
        assertNull(fake.journal)
    }

    @Test
    open fun commandScopeRejectsUnrelatedPackagesAndInvalidStates() {
        assertThrows(
            IllegalArgumentException::class.java,
            { UpdatePackages.command("com.google.android.modulemetadata", 3) },
        )
        assertThrows(
            IllegalArgumentException::class.java,
            { UpdatePackages.command(UpdateBlockerTest.FIRST + "; reboot", 3) },
        )
        assertThrows(
            IllegalArgumentException::class.java,
            { UpdatePackages.command(UpdateBlockerTest.FIRST, 5) },
        )
        assertTrue(
            UpdatePackages.command(UpdateBlockerTest.FIRST, 0).contains("default-state --user 0")
        )
    }

    @Test
    @Throws(Exception::class)
    open fun allFiveOriginalStatesRoundTrip() {
        for (state in 0..4) {
            val fake = UpdateBlockerTest.Fake()
            fake.values.put(UpdateBlockerTest.FIRST, state)
            fake.controller().block()
            fake.controller().restore()
            assertEquals(state, fake.values.get(UpdateBlockerTest.FIRST))
        }
    }

    @Test
    @Throws(Exception::class)
    open fun missingUpdaterDoesNotEraseRestoreJournal() {
        val fake = UpdateBlockerTest.Fake()
        fake.controller().block()
        fake.values.remove(UpdateBlockerTest.FIRST)
        fake.refuseChange = true
        assertThrows(IllegalStateException::class.java, { fake.controller().restore() })
        assertNotNull(fake.journal)
    }

    companion object {
        private val FIRST: String = UpdatePackages.ALL.get(0)
        private val SECOND: String = UpdatePackages.ALL.get(1)
    }
}
