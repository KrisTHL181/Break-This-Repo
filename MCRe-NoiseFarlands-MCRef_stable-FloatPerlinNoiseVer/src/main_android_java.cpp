#include "App.h"
#include "AppPlatform_android.h"

// JNI keycode constants
#include <android/keycodes.h>

//#include "main_android_java.h"
#include "platform/input/Multitouch.h"
#include <unistd.h>
#include <pthread.h>

// Horrible, I know. / A
#ifndef MAIN_CLASS
#include "main.cpp"
#endif


// References for JNI
static jobject g_pActivity  = 0;
static AppPlatform_android appPlatform;

static void setupExternalPath(JNIEnv* env, MAIN_CLASS* app)
{
    //JVMAttacher ta(vm);
    //JNIEnv* env = ta.getEnv();

    LOGI("setupExternalPath");

    if (env)
    {
        LOGI("Environment exists");
    }
    // try appspecific external directory first
    jobject activity = g_pActivity;
    jclass activityClass = env->GetObjectClass(activity);
    jmethodID getExternalFilesDir = env->GetMethodID(activityClass, "getExternalFilesDir", "(Ljava/lang/String;)Ljava/io/File;");

    jobject file = NULL;
    if (getExternalFilesDir != NULL) {
        file = env->CallObjectMethod(activity, getExternalFilesDir, NULL);
    }

    if (file == NULL) {
        // Fallback to the legacy shared storage directory
        jclass clazz = env->FindClass("android/os/Environment");
        jmethodID method = env->GetStaticMethodID(clazz, "getExternalStorageDirectory", "()Ljava/io/File;");
        if (env->ExceptionOccurred()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
        file = env->CallStaticObjectMethod(clazz, method);
    }

    if (!file) {
        LOGI("Failed to get external storage file object, using current working dir");
        app->externalStoragePath = ".";
        app->externalCacheStoragePath = ".";
        return;
    }

    jclass fileClass = env->GetObjectClass(file);
    jmethodID fileMethod = env->GetMethodID(fileClass, "getAbsolutePath", "()Ljava/lang/String;");
    jobject pathString = env->CallObjectMethod(file, fileMethod);

    const char* str = env->GetStringUTFChars((jstring) pathString, NULL);
    app->externalStoragePath = str;
    app->externalCacheStoragePath = str;
    LOGI("%s", str);

    // same fix as the native entry point: make sure cwd is writable
    if (chdir(str) != 0) {
        LOGI("chdir to %s failed: %s", str, strerror(errno));
    }

    env->ReleaseStringUTFChars((jstring)pathString, str);
}

static void pointerDown(int pointerId, int x, int y) {
    Multitouch::feed(1, 1, x, y, pointerId);
}
static void pointerUp(int pointerId, int x, int y) {
    Multitouch::feed(1, 0, x, y, pointerId);
}
static void pointerMove(int pointerId, int x, int y) {
    Multitouch::feed(0, 0, x, y, pointerId);
}


static App* gApp = 0;
static AppContext gContext;
static bool g_inNativeOnCreate = false;

extern "C" {
JNIEXPORT jint JNICALL
JNI_OnLoad( JavaVM * vm, void * reserved )
{
    LOGI("Entering OnLoad\n");
    return appPlatform.init(vm);
}

// Register/save a reference to the java main activity instance
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeRegisterThis(JNIEnv* env, jobject clazz) {
    LOGI("@RegisterThis\n");
    g_pActivity = (jobject)env->NewGlobalRef( clazz );
}

// Unregister/delete the reference to the java main activity instance
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeUnregisterThis(JNIEnv* env, jobject clazz) {
    LOGI("@UnregisterThis\n");
    env->DeleteGlobalRef( g_pActivity );
}

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeOnCreate(JNIEnv* env, jobject thiz, jint screenWidth, jint screenHeight) {
    LOGI("@nativeOnCreate w=%d h=%d\n", (int)screenWidth, (int)screenHeight);
    g_inNativeOnCreate = true;

    appPlatform.instance = g_pActivity;
    appPlatform.setScreenDimensions((int)screenWidth, (int)screenHeight);
    LOGI("nativeOnCreate: screen set, no initConsts needed\n");
    gContext.doRender = false;
    gContext.platform = &appPlatform;

    LOGI("nativeOnCreate: creating gApp\n");
    gApp = new MAIN_CLASS();
    LOGI("nativeOnCreate: gApp=%p\n", gApp);
    setupExternalPath(env, (MAIN_CLASS*)gApp);
    if (env->ExceptionOccurred()) {
        LOGI("nativeOnCreate: exception after setupExternalPath!\n");
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    LOGI("nativeOnCreate: done\n");
    g_inNativeOnCreate = false;
    //gApp->init(gContext);
}

static int s_surfaceCreatedCount = 0;

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_GLRenderer_nativeOnSurfaceCreated(JNIEnv* env) {
    s_surfaceCreatedCount++;
    if (g_inNativeOnCreate) {
        // Skip re-entrant surface callbacks that fire during nativeOnCreate
        return;
    }
    LOGI("@nativeOnSurfaceCreated #%d tid=%d\n", s_surfaceCreatedCount, (int)gettid());

     if (gApp) {
         // Don't call onGraphicsReset the first time
        if (gApp->isInited()) {
            LOGI("nativeOnSurfaceCreated: calling onGraphicsReset\n");
            gApp->onGraphicsReset(gContext);
        }

        if (!gApp->isInited()) {
            LOGI("nativeOnSurfaceCreated: calling init\n");
            gApp->init(gContext);
            LOGI("nativeOnSurfaceCreated: init done, isInited=%d\n", (int)gApp->isInited());
        }
     }
}

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_GLRenderer_nativeOnSurfaceChanged(JNIEnv* env, jclass cls, jint w, jint h) {
    LOGI("@nativeOnSurfaceChanged: %lu\n", (unsigned long)pthread_self());

    if (gApp) {
        gApp->setSize(w, h);

        if (!gApp->isInited())
            gApp->init(gContext);

        if (!gApp->isInited())
            LOGI("nativeOnSurfaceChanged: NOT INITED!\n");
    }
}

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeOnDestroy(JNIEnv* env) {
    LOGI("@nativeOnDestroy\n");

    delete gApp;
    gApp = 0;
    //gApp->onGraphicsReset(gContext);
}

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_GLRenderer_nativeUpdate(JNIEnv* env) {
    //LOGI("@nativeUpdate: %p\n", pthread_self());
    if (gApp) {
        if (!gApp->isInited())
            gApp->init(gContext);

        gApp->update();

        if (gApp->wantToQuit())
            appPlatform.finish();
    }
}

//
// Keyboard events
//
// helper to convert Android keycodes to our internal Keyboard constants
// ===== main_android_java.cpp: androidKeyToInternal 修正版 =====
static int androidKeyToInternal(int androidKey){
    // 🧊 数字键 0-9：AKEYCODE_0=7 到 AKEYCODE_9=16
    // 映射到 ASCII '0'~'9' (48~57)，彻底避开 ASCII 控制字符区 (0~31)
    if(androidKey >= AKEYCODE_0 && androidKey <= AKEYCODE_9){
        return '0' + (androidKey - AKEYCODE_0);
    }
    
    switch(androidKey){
        case AKEYCODE_DEL:          return Keyboard::KEY_BACKSPACE; // 67→8
        case AKEYCODE_ENTER:
        case AKEYCODE_NUMPAD_ENTER: return Keyboard::KEY_RETURN;    // 66→13
        case AKEYCODE_SPACE:        return Keyboard::KEY_SPACE;     // 62
        case AKEYCODE_TAB:          return Keyboard::KEY_TAB;       // 61
        case AKEYCODE_ESCAPE:       return Keyboard::KEY_ESCAPE;    // 111
        default:
            // 其他未知键码如果在控制字符区 (0~31)，偏移到 256+
            // 避免被下游 TextBox/Screen 误认为 BACKSPACE(8)/RETURN(13) 等
            if(androidKey >= 0 && androidKey <= 31){
                return 256 + androidKey;
            }
            return androidKey;
    }
}

JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeOnKeyDown(JNIEnv* env, jclass cls, jint keyCode) {
    LOGI("@nativeOnKeyDown: %d\n", keyCode);
    int mapped = androidKeyToInternal(keyCode);
    Keyboard::feed(mapped, true);
}
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeTextChar(JNIEnv* env, jclass cls, jint unicodeChar) {
    // soft-keyboards may send a backspace as a character code
    if (unicodeChar == 8) {
        Keyboard::feed(Keyboard::KEY_BACKSPACE, true);
        Keyboard::feed(Keyboard::KEY_BACKSPACE, false);
    } else if (unicodeChar > 0 && unicodeChar < 128) {
        Keyboard::feedText((char)unicodeChar);
    }
}
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeOnKeyUp(JNIEnv* env, jclass cls, jint keyCode) {
    LOGI("@nativeOnKeyUp: %d\n", (int)keyCode);
    int mapped = androidKeyToInternal(keyCode);
    Keyboard::feed(mapped, false);
}

JNIEXPORT jboolean JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeHandleBack(JNIEnv* env, jclass cls, jboolean isDown) {
    LOGI("@nativeHandleBack: %d\n", isDown);
    if (gApp) return gApp->handleBack(isDown)? JNI_TRUE : JNI_FALSE;
    return JNI_FALSE;
}

//
// Mouse events
//
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeMouseDown(JNIEnv* env, jclass cls, jint pointerId, jint buttonId, jfloat x, jfloat y) {
    //LOGI("@nativeMouseDown: %f %f\n", x, y);
    mouseDown(1, x, y);
    pointerDown(pointerId, x, y);
}
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeMouseUp(JNIEnv* env, jclass cls, jint pointerId, jint buttonId, jfloat x, jfloat y) {
    //LOGI("@nativeMouseUp: %f %f\n", x, y);
    mouseUp(1, x, y);
    pointerUp(pointerId, x, y);
}
JNIEXPORT void JNICALL
Java_com_mojang_minecraftpe_MainActivity_nativeMouseMove(JNIEnv* env, jclass cls, jint pointerId, jfloat x, jfloat y) {
    //LOGI("@nativeMouseMove: %f %f\n", x, y);
    mouseMove(x, y);
    pointerMove(pointerId, x, y);
}
}
