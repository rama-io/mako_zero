#include <android_native_app_glue.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/input.h>
#include <android/asset_manager.h>

#include <jni.h>

#include <algorithm>
#include <string>
#include <vector>

#define LOG_TAG "mako_zero"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct App {
    std::string packageName;
    std::string label;
};

static std::vector<App> apps;

static ANativeWindow* window = nullptr;

static bool touching = false;
static float touchX = 0.0f;
static float touchY = 0.0f;

static JavaVM* javaVM = nullptr;
static jobject nativeActivity = nullptr;

// ============================================================
// Simple bitmap font
// ============================================================
//
// 5x7 font. Each character is represented by 7 rows of 5 bits.
// This is intentionally tiny and dependency-free.
//

struct Glyph {
    char c;
    uint8_t rows[7];
};

static const Glyph FONT[] = {

        {'A', {0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}},
        {'B', {0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}},
        {'C', {0x0F,0x10,0x10,0x10,0x10,0x10,0x0F}},
        {'D', {0x1E,0x11,0x11,0x11,0x11,0x11,0x1E}},
        {'E', {0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}},
        {'F', {0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}},
        {'G', {0x0F,0x10,0x10,0x17,0x11,0x11,0x0F}},
        {'H', {0x11,0x11,0x11,0x1F,0x11,0x11,0x11}},
        {'I', {0x1F,0x04,0x04,0x04,0x04,0x04,0x1F}},
        {'J', {0x01,0x01,0x01,0x01,0x11,0x11,0x0E}},
        {'K', {0x11,0x12,0x14,0x18,0x14,0x12,0x11}},
        {'L', {0x10,0x10,0x10,0x10,0x10,0x10,0x1F}},
        {'M', {0x11,0x1B,0x15,0x15,0x11,0x11,0x11}},
        {'N', {0x11,0x19,0x15,0x13,0x11,0x11,0x11}},
        {'O', {0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}},
        {'P', {0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}},
        {'Q', {0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}},
        {'R', {0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}},
        {'S', {0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E}},
        {'T', {0x1F,0x04,0x04,0x04,0x04,0x04,0x04}},
        {'U', {0x11,0x11,0x11,0x11,0x11,0x11,0x0E}},
        {'V', {0x11,0x11,0x11,0x11,0x11,0x0A,0x04}},
        {'W', {0x11,0x11,0x11,0x15,0x15,0x15,0x0A}},
        {'X', {0x11,0x11,0x0A,0x04,0x0A,0x11,0x11}},
        {'Y', {0x11,0x11,0x0A,0x04,0x04,0x04,0x04}},
        {'Z', {0x1F,0x01,0x02,0x04,0x08,0x10,0x1F}},

        {'a', {0x00,0x00,0x0E,0x01,0x0F,0x11,0x0F}},
        {'0', {0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}},
        {'1', {0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}},
        {'2', {0x0E,0x11,0x01,0x02,0x04,0x08,0x1F}},
        {'3', {0x1E,0x01,0x01,0x0E,0x01,0x01,0x1E}},
        {'4', {0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}},
        {'5', {0x1F,0x10,0x10,0x1E,0x01,0x01,0x1E}},
        {'6', {0x0E,0x10,0x10,0x1E,0x11,0x11,0x0E}},
        {'7', {0x1F,0x01,0x02,0x04,0x08,0x08,0x08}},
        {'8', {0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}},
        {'9', {0x0E,0x11,0x11,0x0F,0x01,0x01,0x0E}},

        {' ', {0,0,0,0,0,0,0}},
        {'.', {0,0,0,0,0,0x0C,0x0C}},
        {'-', {0,0,0,0x1F,0,0,0}},
        {'_', {0,0,0,0,0,0,0x1F}},
        {':', {0x00, 0x0C, 0x0C, 0x00, 0x0C, 0x0C, 0x00}},
        {'/', {0x01, 0x02, 0x04, 0x08, 0x10, 0x00, 0x00}},

};

static const Glyph* findGlyph(char c)
{
    for (const auto& glyph : FONT) {
        if (glyph.c == c)
            return &glyph;
    }

    return nullptr;
}


// ============================================================
// Primitive drawing
// ============================================================

static void fillRect(
        ANativeWindow_Buffer& buffer,
        int x,
        int y,
        int width,
        int height,
        uint32_t color)
{
    const int screenWidth = buffer.width;
    const int screenHeight = buffer.height;

    int x0 = std::max(0, x);
    int y0 = std::max(0, y);
    int x1 = std::min(screenWidth, x + width);
    int y1 = std::min(screenHeight, y + height);

    if (x0 >= x1 || y0 >= y1)
        return;

    uint32_t* pixels =
            static_cast<uint32_t*>(buffer.bits);

    for (int py = y0; py < y1; ++py) {

        uint32_t* row =
                pixels + py * buffer.stride;

        for (int px = x0; px < x1; ++px)
            row[px] = color;
    }
}


static void drawText(
        ANativeWindow_Buffer& buffer,
        const std::string& text,
        int x,
        int y,
        int scale,
        uint32_t color)
{
    int cursorX = x;

    for (char c : text) {

        // Convert character to uppercase
        c = static_cast<char>(
                std::toupper(static_cast<unsigned char>(c))
        );

        const Glyph* glyph =
                findGlyph(c);

        if (!glyph) {
            cursorX += 8 * scale;
            continue;
        }

        for (int row = 0; row < 7; ++row) {

            uint8_t bits =
                    glyph->rows[row];

            for (int col = 0; col < 5; ++col) {

                if (bits & (1 << (4 - col))) {

                    fillRect(
                            buffer,
                            cursorX + col * scale,
                            y + row * scale,
                            scale,
                            scale,
                            color
                    );
                }
            }
        }

        cursorX += 7 * scale;
    }
}


// ============================================================
// JNI helpers
// ============================================================

static JNIEnv* getJNIEnv()
{
    if (!javaVM)
        return nullptr;

    JNIEnv* env = nullptr;

    jint result = javaVM->GetEnv(
            reinterpret_cast<void**>(&env),
            JNI_VERSION_1_6
    );

    if (result == JNI_OK)
        return env;

    if (result == JNI_EDETACHED) {
        if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK)
            return nullptr;

        return env;
    }

    return nullptr;
}


// ============================================================
// Get launchable applications
// ============================================================

static void loadApps()
{
    JNIEnv* env = getJNIEnv();

    if (!env || !nativeActivity) {
        LOGE("Could not get JNI environment");
        return;
    }

    jobject activity = nativeActivity;

    // Activity.getPackageManager()
    jclass activityClass = env->GetObjectClass(activity);

    jmethodID getPackageManager =
            env->GetMethodID(
                    activityClass,
                    "getPackageManager",
                    "()Landroid/content/pm/PackageManager;"
            );

    if (!getPackageManager) {
        LOGE("getPackageManager not found");
        env->DeleteLocalRef(activityClass);
        return;
    }

    jobject packageManager =
            env->CallObjectMethod(
                    activity,
                    getPackageManager
            );

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        env->DeleteLocalRef(activityClass);
        return;
    }

    // Intent("android.intent.action.MAIN")
    jclass intentClass = env->FindClass(
            "android/content/Intent"
    );

    jmethodID intentConstructor =
            env->GetMethodID(
                    intentClass,
                    "<init>",
                    "(Ljava/lang/String;)V"
            );

    jstring mainAction =
            env->NewStringUTF(
                    "android.intent.action.MAIN"
            );

    jobject intent =
            env->NewObject(
                    intentClass,
                    intentConstructor,
                    mainAction
            );

    // Intent.addCategory("android.intent.category.LAUNCHER")
    jmethodID addCategory =
            env->GetMethodID(
                    intentClass,
                    "addCategory",
                    "(Ljava/lang/String;)Landroid/content/Intent;"
            );

    jstring launcherCategory =
            env->NewStringUTF(
                    "android.intent.category.LAUNCHER"
            );

    env->CallObjectMethod(
            intent,
            addCategory,
            launcherCategory
    );

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    // PackageManager.queryIntentActivities(...)
    jclass pmClass =
            env->GetObjectClass(packageManager);

    jmethodID queryIntentActivities =
            env->GetMethodID(
                    pmClass,
                    "queryIntentActivities",
                    "(Landroid/content/Intent;I)Ljava/util/List;"
            );

    if (!queryIntentActivities) {
        LOGE("queryIntentActivities not found");

        env->DeleteLocalRef(pmClass);
        env->DeleteLocalRef(packageManager);
        env->DeleteLocalRef(intent);
        env->DeleteLocalRef(intentClass);
        env->DeleteLocalRef(mainAction);
        env->DeleteLocalRef(launcherCategory);
        env->DeleteLocalRef(activityClass);

        return;
    }

    // 0 = no special flags
    jobject result =
            env->CallObjectMethod(
                    packageManager,
                    queryIntentActivities,
                    intent,
                    0
            );

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();

        LOGE("queryIntentActivities failed");

        return;
    }

    // java.util.List.size()
    jclass listClass =
            env->GetObjectClass(result);

    jmethodID listSize =
            env->GetMethodID(
                    listClass,
                    "size",
                    "()I"
            );

    jmethodID listGet =
            env->GetMethodID(
                    listClass,
                    "get",
                    "(I)Ljava/lang/Object;"
            );

    const jint count =
            env->CallIntMethod(result, listSize);

    LOGI("Found %d launchable applications", count);

    apps.clear();

    // ResolveInfo
    jclass resolveInfoClass =
            env->FindClass(
                    "android/content/pm/ResolveInfo"
            );

    jfieldID activityInfoField =
            env->GetFieldID(
                    resolveInfoClass,
                    "activityInfo",
                    "Landroid/content/pm/ActivityInfo;"
            );

    for (jint i = 0; i < count; ++i) {

        jobject resolveInfo =
                env->CallObjectMethod(
                        result,
                        listGet,
                        i
                );

        if (!resolveInfo)
            continue;

        jobject activityInfo =
                env->GetObjectField(
                        resolveInfo,
                        activityInfoField
                );

        if (!activityInfo) {
            env->DeleteLocalRef(resolveInfo);
            continue;
        }

        jclass activityInfoClass =
                env->GetObjectClass(activityInfo);

        // ActivityInfo.packageName
        jfieldID packageNameField =
                env->GetFieldID(
                        activityInfoClass,
                        "packageName",
                        "Ljava/lang/String;"
                );

        jstring packageName =
                static_cast<jstring>(
                        env->GetObjectField(
                                activityInfo,
                                packageNameField
                        )
                );

        // ActivityInfo.loadLabel(PackageManager)
        jmethodID loadLabel =
                env->GetMethodID(
                        activityInfoClass,
                        "loadLabel",
                        "(Landroid/content/pm/PackageManager;)"
                        "Ljava/lang/CharSequence;"
                );

        jobject labelObject =
                env->CallObjectMethod(
                        activityInfo,
                        loadLabel,
                        packageManager
                );

        std::string package;
        std::string label;

        if (packageName) {
            const char* chars =
                    env->GetStringUTFChars(
                            packageName,
                            nullptr
                    );

            if (chars) {
                package = chars;
                env->ReleaseStringUTFChars(
                        packageName,
                        chars
                );
            }
        }

        if (labelObject) {
            jclass stringClass =
                    env->FindClass("java/lang/Object");

            jmethodID toString =
                    env->GetMethodID(
                            stringClass,
                            "toString",
                            "()Ljava/lang/String;"
                    );

            jstring labelString =
                    static_cast<jstring>(
                            env->CallObjectMethod(
                                    labelObject,
                                    toString
                            )
                    );

            if (labelString) {
                const char* chars =
                        env->GetStringUTFChars(
                                labelString,
                                nullptr
                        );

                if (chars) {
                    label = chars;

                    env->ReleaseStringUTFChars(
                            labelString,
                            chars
                    );
                }

                env->DeleteLocalRef(labelString);
            }

            env->DeleteLocalRef(stringClass);
            env->DeleteLocalRef(labelObject);
        }

        if (!package.empty()) {

            if (label.empty())
                label = package;

            apps.push_back({
                                   package,
                                   label
                           });

            LOGI(
                    "App: %s (%s)",
                    label.c_str(),
                    package.c_str()
            );
        }

        env->DeleteLocalRef(activityInfoClass);
        env->DeleteLocalRef(activityInfo);
        env->DeleteLocalRef(resolveInfo);

        if (packageName)
            env->DeleteLocalRef(packageName);
    }

    std::sort(
            apps.begin(),
            apps.end(),
            [](const App& a, const App& b) {
                return a.label < b.label;
            }
    );

    LOGI(
            "Loaded %zu applications",
            apps.size()
    );

    // Cleanup
    env->DeleteLocalRef(resolveInfoClass);
    env->DeleteLocalRef(listClass);
    env->DeleteLocalRef(result);
    env->DeleteLocalRef(pmClass);
    env->DeleteLocalRef(packageManager);
    env->DeleteLocalRef(intent);
    env->DeleteLocalRef(intentClass);
    env->DeleteLocalRef(mainAction);
    env->DeleteLocalRef(launcherCategory);
    env->DeleteLocalRef(activityClass);
}


// ============================================================
// Launch an application
// ============================================================

static void launchApp(const App& app)
{
    JNIEnv* env = getJNIEnv();

    if (!env || !nativeActivity)
        return;

    LOGI(
            "Launching %s (%s)",
            app.label.c_str(),
            app.packageName.c_str()
    );

    jclass activityClass =
            env->GetObjectClass(nativeActivity);

    jmethodID getPackageManager =
            env->GetMethodID(
                    activityClass,
                    "getPackageManager",
                    "()Landroid/content/pm/PackageManager;"
            );

    jobject packageManager =
            env->CallObjectMethod(
                    nativeActivity,
                    getPackageManager
            );

    jclass pmClass =
            env->GetObjectClass(packageManager);

    // getLaunchIntentForPackage(String)
    jmethodID getLaunchIntent =
            env->GetMethodID(
                    pmClass,
                    "getLaunchIntentForPackage",
                    "(Ljava/lang/String;)"
                    "Landroid/content/Intent;"
            );

    jstring packageName =
            env->NewStringUTF(
                    app.packageName.c_str()
            );

    jobject intent =
            env->CallObjectMethod(
                    packageManager,
                    getLaunchIntent,
                    packageName
            );

    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    if (!intent) {
        LOGE(
                "No launch intent for %s",
                app.packageName.c_str()
        );
    } else {

        jmethodID startActivity =
                env->GetMethodID(
                        activityClass,
                        "startActivity",
                        "(Landroid/content/Intent;)V"
                );

        env->CallVoidMethod(
                nativeActivity,
                startActivity,
                intent
        );

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    }

    if (intent)
        env->DeleteLocalRef(intent);

    env->DeleteLocalRef(packageName);
    env->DeleteLocalRef(pmClass);
    env->DeleteLocalRef(packageManager);
    env->DeleteLocalRef(activityClass);
}


// ============================================================
// Rendering
// ============================================================

static void drawAppList()
{
    if (!window)
        return;

    ANativeWindow_Buffer buffer;

    if (ANativeWindow_lock(
            window,
            &buffer,
            nullptr) != 0) {
        return;
    }

    const int width = buffer.width;
    const int height = buffer.height;

    // --------------------------------------------------------
    // Colors
    // --------------------------------------------------------

    constexpr uint32_t BACKGROUND_COLOR = 0xFF2E1E1E;
    constexpr uint32_t TEXT_COLOR = 0xFFF4D6CD;
    constexpr uint32_t ACCENT_COLOR = 0xFFF7A6CB;

    // --------------------------------------------------------
    // Background
    // --------------------------------------------------------

    fillRect(
            buffer,
            0,
            0,
            width,
            height,
            BACKGROUND_COLOR
    );

    // --------------------------------------------------------
    // Header
    // --------------------------------------------------------

    constexpr int SCREEN_PADDING_Y = 110;
    constexpr int SCREEN_PADDING = 32;
    constexpr int HEADER_HEIGHT = 110;
    constexpr int SPACE_BLOCK = 42;
    constexpr int DATE_HEIGHT = 32;

    drawText(
            buffer,
            "22:00",
            SCREEN_PADDING,
            SCREEN_PADDING_Y,
            16,
            ACCENT_COLOR
    );

    drawText(
            buffer,
            "WEDNESDAY::2026-09-02::245/365",
            SCREEN_PADDING,
            SCREEN_PADDING_Y + HEADER_HEIGHT + SPACE_BLOCK,
            4,
            TEXT_COLOR
    );

    // --------------------------------------------------------
    // App rows
    // --------------------------------------------------------

    constexpr int ROW_HEIGHT = 100;

    for (size_t i = 0; i < apps.size(); ++i) {

        const int y =
                SCREEN_PADDING_Y + HEADER_HEIGHT + SPACE_BLOCK + DATE_HEIGHT + 10 +
                static_cast<int>(i) * ROW_HEIGHT;

        if (y >= height)
            break;

        // Application name
        drawText(
                buffer,
                apps[i].label,
                SCREEN_PADDING,
                y + SCREEN_PADDING,
                6,
                TEXT_COLOR
        );
    }

    ANativeWindow_unlockAndPost(window);
}


// ============================================================
// Touch handling
// ============================================================

static void handleTouch(
        float x,
        float y)
{
    if (apps.empty())
        return;

    constexpr float ROW_HEIGHT = 140.0f;

    const int index =
            static_cast<int>((y - 30.0f) / ROW_HEIGHT);

    if (index < 0 ||
        index >= static_cast<int>(apps.size())) {
        return;
    }

    LOGI(
            "Selected app %d: %s",
            index,
            apps[index].label.c_str()
    );

    launchApp(apps[index]);
}


static int32_t handleInput(
        struct android_app* app,
        AInputEvent* event)
{
    if (AInputEvent_getType(event) !=
        AINPUT_EVENT_TYPE_MOTION) {
        return 0;
    }

    const int32_t action =
            AMotionEvent_getAction(event);

    const int32_t maskedAction =
            action & AMOTION_EVENT_ACTION_MASK;

    const float x =
            AMotionEvent_getX(event, 0);

    const float y =
            AMotionEvent_getY(event, 0);

    switch (maskedAction) {

        case AMOTION_EVENT_ACTION_DOWN:

            touching = true;

            touchX = x;
            touchY = y;

            LOGI(
                    "TOUCH DOWN %.1f %.1f",
                    x,
                    y
            );

            return 1;

        case AMOTION_EVENT_ACTION_MOVE:

            if (touching) {
                touchX = x;
                touchY = y;
            }

            return 1;

        case AMOTION_EVENT_ACTION_UP:

            if (touching) {

                touchX = x;
                touchY = y;

                handleTouch(
                        touchX,
                        touchY
                );
            }

            touching = false;

            return 1;

        case AMOTION_EVENT_ACTION_CANCEL:

            touching = false;

            return 1;

        default:
            return 0;
    }
}


// ============================================================
// Android lifecycle
// ============================================================

static void handleAppCmd(
        struct android_app* app,
        int32_t cmd)
{
    switch (cmd) {

        case APP_CMD_INIT_WINDOW:

            LOGI("INIT_WINDOW");

            window = app->window;

            if (window) {

                ANativeWindow_setBuffersGeometry(
                        window,
                        ANativeWindow_getWidth(window),
                        ANativeWindow_getHeight(window),
                        WINDOW_FORMAT_RGBA_8888
                );

                drawAppList();
            }

            break;


        case APP_CMD_TERM_WINDOW:

            LOGI("TERM_WINDOW");

            window = nullptr;

            break;


        case APP_CMD_WINDOW_RESIZED:

            LOGI("WINDOW_RESIZED");

            if (window)
                drawAppList();

            break;


        case APP_CMD_GAINED_FOCUS:

            LOGI("GAINED_FOCUS");

            if (window)
                drawAppList();

            break;


        case APP_CMD_LOST_FOCUS:

            LOGI("LOST_FOCUS");

            break;


        default:
            break;
    }
}


// ============================================================
// Native entry point
// ============================================================

void android_main(
        struct android_app* app)
{
    LOGI("Mako launcher starting");

    app_dummy();

    // --------------------------------------------------------
    // Obtain JavaVM / Activity
    // --------------------------------------------------------

    ANativeActivity* activity =
            app->activity;

    javaVM =
            activity->vm;

    JNIEnv* env = getJNIEnv();

    if (env) {
        nativeActivity =
                env->NewGlobalRef(
                        activity->clazz
                );
    }

    // --------------------------------------------------------
    // Install callbacks
    // --------------------------------------------------------

    app->onAppCmd =
            handleAppCmd;

    app->onInputEvent =
            handleInput;

    // --------------------------------------------------------
    // Load installed apps
    // --------------------------------------------------------

    loadApps();

    // --------------------------------------------------------
    // Main Android event loop
    // --------------------------------------------------------

    while (true) {

        int events;

        struct android_poll_source* source;

        /*
         * timeout = -1 means sleep until Android actually
         * has something to process.
         *
         * This is important: we're NOT spinning at 100% CPU.
         */

        const int ident =
                ALooper_pollOnce(
                        -1,
                        nullptr,
                        &events,
                        reinterpret_cast<void**>(&source)
                );

        if (source)
            source->process(
                    app,
                    source
            );

        /*
         * APP_CMD_DESTROY is Android telling us the Activity
         * is going away.
         */
        if (app->destroyRequested) {

            LOGI(
                    "Destroy requested"
            );

            break;
        }
    }

    if (env && nativeActivity) {
        env->DeleteGlobalRef(
                nativeActivity
        );

        nativeActivity = nullptr;
    }

    LOGI("Mako launcher stopped");
}