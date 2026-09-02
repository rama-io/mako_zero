#include <android/native_activity.h>
#include <android/native_window.h>
#include <android/log.h>
#include <cstdint>
#include <cstring>

#define LOG_TAG "Mako Forever"

struct Glyph {
    char c;
    uint8_t rows[7];
};

static const Glyph kFont[] = {
    {'M', {0b10001, 0b11011, 0b10101, 0b10001, 0b10001, 0b10001, 0b10001}},
    {'A', {0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001}},
    {'K', {0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001}},
    {'O', {0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110}},
    {'F', {0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000}},
    {'R', {0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001}},
    {'E', {0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111}},
    {'V', {0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100}},
    {' ', {0, 0, 0, 0, 0, 0, 0}},
};

static const uint8_t* glyphFor(char c) {
    for (const auto& g : kFont) {
        if (g.c == c) return g.rows;
    }
    return kFont[8].rows; // fall back to space
}

static void drawGlyph(
    ANativeWindow_Buffer* buffer,
    const uint8_t* rows,
    int originX,
    int originY,
    int scale,
    uint32_t color
) {
    auto* pixels = static_cast<uint32_t*>(buffer->bits);

    for (int row = 0; row < 7; row++) {
        for (int col = 0; col < 5; col++) {
            bool on = (rows[row] >> (4 - col)) & 1;
            if (!on) continue;

            for (int sy = 0; sy < scale; sy++) {
                int py = originY + row * scale + sy;
                if (py < 0 || py >= buffer->height) continue;

                for (int sx = 0; sx < scale; sx++) {
                    int px = originX + col * scale + sx;
                    if (px < 0 || px >= buffer->width) continue;

                    pixels[py * buffer->stride + px] = color;
                }
            }
        }
    }
}

static void drawText(
    ANativeWindow_Buffer* buffer,
    const char* text,
    int scale,
    uint32_t textColor,
    uint32_t backgroundColor
) {
    // Fill the background first.
    auto* pixels = static_cast<uint32_t*>(buffer->bits);
    for (int y = 0; y < buffer->height; y++) {
        for (int x = 0; x < buffer->width; x++) {
            pixels[y * buffer->stride + x] = backgroundColor;
        }
    }

    const int glyphWidth = 5 * scale;
    const int glyphHeight = 7 * scale;
    const int spacing = scale;

    int len = static_cast<int>(strlen(text));
    int totalWidth = len * glyphWidth + (len - 1) * spacing;

    int x = (buffer->width - totalWidth) / 2;
    int y = (buffer->height - glyphHeight) / 2;

    for (int i = 0; i < len; i++) {
        drawGlyph(buffer, glyphFor(text[i]), x, y, scale, textColor);
        x += glyphWidth + spacing;
    }
}

static void renderMessage(ANativeWindow* window) {
    if (window == nullptr) return;

    ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888);

    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(window, &buffer, nullptr) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Failed to lock window buffer");
        return;
    }

    const uint32_t backgroundColor = 0xFF2B2B3A; // dark slate
    const uint32_t textColor = 0xFF7FE0FF;       // light cyan

    int scale = buffer.width / 120;
    if (scale < 4) scale = 4;

    drawText(&buffer, "MAKO FOREVER", scale, textColor, backgroundColor);

    ANativeWindow_unlockAndPost(window);
}

static void onNativeWindowCreated(ANativeActivity* activity, ANativeWindow* window) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Native window created");
    renderMessage(window);
}

static void onNativeWindowRedrawNeeded(ANativeActivity* activity, ANativeWindow* window) {
    renderMessage(window);
}

static void onCreate(
    ANativeActivity* activity,
    void* savedState,
    size_t savedStateSize
) {
    __android_log_print(
        ANDROID_LOG_INFO,
        LOG_TAG,
        "Mako Forever started"
    );
}

extern "C"
void ANativeActivity_onCreate(
    ANativeActivity* activity,
    void* savedState,
    size_t savedStateSize
) {
    activity->callbacks->onStart = nullptr;
    activity->callbacks->onNativeWindowCreated = onNativeWindowCreated;
    activity->callbacks->onNativeWindowRedrawNeeded = onNativeWindowRedrawNeeded;

    onCreate(
        activity,
        savedState,
        savedStateSize
    );
}
