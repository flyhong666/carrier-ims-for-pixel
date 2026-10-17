package io.github.vvb2060.ims.privileged

import java.lang.reflect.InvocationTargetException

internal fun stopShellPermissionDelegation(
    activityManager: Any,
    activityManagerClass: Class<*>,
    uid: Int,
) {
    // Android 17 的部分系统改为按 UID 释放，按真实签名探测以兼容系统回移。
    val stopMethod = try {
        activityManagerClass.getMethod("stopDelegateShellPermissionIdentity", Int::class.javaPrimitiveType)
    } catch (_: NoSuchMethodException) {
        activityManagerClass.getMethod("stopDelegateShellPermissionIdentity")
    }
    try {
        if (stopMethod.parameterCount == 0) {
            stopMethod.invoke(activityManager)
        } else {
            stopMethod.invoke(activityManager, uid)
        }
    } catch (error: InvocationTargetException) {
        // 只有签名不存在才回退；保留真正的服务异常供调用方记录。
        throw error.targetException
    }
}
