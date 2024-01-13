package com.example.kanbun

import android.os.Build
import android.util.Log

object EmulatorDetector {

    /**
     * Checks if the app is running on an emulator.
     *
     * @return `true` if running on an emulator, `false` otherwise.
     */
    fun isEmulator(): Boolean {
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT.contains("sdk") || Build.PRODUCT.contains("vbox") ||
                Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu") ||
                Build.HARDWARE.contains("vbox") || Build.HARDWARE.contains("emulator")
        Log.d("EmuDetector", "isEmulator: $isEmulator")
        return isEmulator
    }
}