package io.github.vvb2060.ims.privileged

import android.app.IActivityManager
import android.system.Os
import rikka.shizuku.Shizuku

internal fun IActivityManager.stopDelegateShellPermissionIdentityCompat() {
    stopShellPermissionDelegation(this, IActivityManager::class.java, Os.getuid())
}

internal fun waitForShizukuBinderReady(
    maxRetries: Int = 20,
    intervalMs: Long = 100L,
): Boolean {
    var retries = 0
    while (!Shizuku.pingBinder()) {
        retries++
        if (retries > maxRetries) return false
        try {
            Thread.sleep(intervalMs)
        } catch (_: InterruptedException) {
            return false
        }
    }
    return true
}
