#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <array>
#include <memory>
#include <sstream>

#define TAG "AutoroidNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_autoroid_app_core_native_NativeEngine_getNativeCoreVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "Autoroid-Native-Core v1.0.0 (ARM64/x86_64 Optimized)";
    return env->NewStringUTF(version.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_autoroid_app_core_native_NativeEngine_isDirectRootAvailable(
        JNIEnv* env,
        jobject /* this */) {
    // Check common root binary locations natively without JVM overhead
    const char* suPaths[] = {
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/system/sd/xbin/su"
    };

    for (const char* path : suPaths) {
        if (access(path, X_OK) == 0) {
            return JNI_TRUE;
        }
    }

    // Check KernelSU / APatch / Magisk paths
    if (access("/system/bin/ksu", X_OK) == 0 ||
        access("/data/adb/ksu/bin/su", X_OK) == 0 ||
        access("/data/adb/ap/bin/su", X_OK) == 0 ||
        access("/data/adb/magisk/su", X_OK) == 0) {
        return JNI_TRUE;
    }

    return JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_autoroid_app_core_native_NativeEngine_executeNativeCommand(
        JNIEnv* env,
        jobject /* this */,
        jstring jcommand) {
    const char* cmd = env->GetStringUTFChars(jcommand, nullptr);
    if (!cmd) {
        return env->NewStringUTF("");
    }

    std::array<char, 256> buffer;
    std::string result;
    std::unique_ptr<FILE, decltype(&pclose)> pipe(popen(cmd, "r"), pclose);

    if (pipe) {
        while (fgets(buffer.data(), buffer.size(), pipe.get()) != nullptr) {
            result += buffer.data();
        }
    } else {
        LOGE("Failed to execute native popen for command: %s", cmd);
    }

    env->ReleaseStringUTFChars(jcommand, cmd);
    return env->NewStringUTF(result.c_str());
}
