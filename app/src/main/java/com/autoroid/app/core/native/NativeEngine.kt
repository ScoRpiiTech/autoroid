package com.autoroid.app.core.native

object NativeEngine {
    init {
        try {
            System.loadLibrary("autoroid_native")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    external fun getNativeCoreVersion(): String
    external fun isDirectRootAvailable(): Boolean
    external fun executeNativeCommand(command: String): String
}
