package io.github.vvb2060.ims.privileged

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ShellPermissionDelegationTest {
    @Test
    fun releasesDelegationOnLegacySystem() {
        val manager = LegacyManager()

        stopShellPermissionDelegation(manager, LegacyApi::class.java, 10465)

        assertTrue(manager.stopped)
    }

    @Test
    fun releasesOnlyTheGivenUidOnModernSystem() {
        val manager = ModernManager()

        stopShellPermissionDelegation(manager, ModernApi::class.java, 10465)

        assertEquals(10465, manager.stoppedUid)
    }

    @Test
    fun prefersUidScopedReleaseWhenBothSignaturesExist() {
        val manager = BothManager()

        stopShellPermissionDelegation(manager, BothApi::class.java, 10465)

        assertEquals(10465, manager.stoppedUid)
        assertFalse(manager.legacyCalled)
    }

    @Test
    fun preservesPermissionErrorWithoutRetryingGlobalRelease() {
        val failure = SecurityException("permission denied")
        val manager = BothManager(failure)

        val actual = assertFailsWith<SecurityException> {
            stopShellPermissionDelegation(manager, BothApi::class.java, 10465)
        }

        assertSame(failure, actual)
        assertFalse(manager.legacyCalled)
    }

    @Test
    fun preservesLegacyServiceFailure() {
        val failure = IllegalStateException("service unavailable")
        val manager = LegacyManager(failure)

        val actual = assertFailsWith<IllegalStateException> {
            stopShellPermissionDelegation(manager, LegacyApi::class.java, 10465)
        }

        assertSame(failure, actual)
    }

    @Test
    fun reportsUnsupportedInterfaceInsteadOfPretendingReleaseSucceeded() {
        assertFailsWith<NoSuchMethodException> {
            stopShellPermissionDelegation(Any(), Any::class.java, 10465)
        }
    }

    interface LegacyApi {
        fun stopDelegateShellPermissionIdentity()
    }

    interface ModernApi {
        fun stopDelegateShellPermissionIdentity(uid: Int)
    }

    interface BothApi : LegacyApi, ModernApi

    private class LegacyManager(private val failure: Throwable? = null) : LegacyApi {
        var stopped = false

        override fun stopDelegateShellPermissionIdentity() {
            failure?.let { throw it }
            stopped = true
        }
    }

    private class ModernManager : ModernApi {
        var stoppedUid: Int? = null

        override fun stopDelegateShellPermissionIdentity(uid: Int) {
            stoppedUid = uid
        }
    }

    private class BothManager(private val failure: Throwable? = null) : BothApi {
        var stoppedUid: Int? = null
        var legacyCalled = false

        override fun stopDelegateShellPermissionIdentity() {
            legacyCalled = true
        }

        override fun stopDelegateShellPermissionIdentity(uid: Int) {
            failure?.let { throw it }
            stoppedUid = uid
        }
    }
}
