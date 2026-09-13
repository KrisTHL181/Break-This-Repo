#ifndef NET_MINECRAFT_CLIENT__Options_H__
#define NET_MINECRAFT_CLIENT__Options_H__

#define SOUND_MIN_VALUE 0.0f
#define SOUND_MAX_VALUE 1.0f
#define MUSIC_MIN_VALUE 0.0f
#define MUSIC_MAX_VALUE 1.0f
#define SENSITIVITY_MIN_VALUE 0.0f
#define SENSITIVITY_MAX_VALUE 1.0f
#define PIXELS_PER_MILLIMETER_MIN_VALUE 3.0f
#define PIXELS_PER_MILLIMETER_MAX_VALUE 4.0f
#define OPTIONS_FARLANDS_SCALE farlands_scale
#define OPTIONS_WORLD_OFFSET_X world_offset_x
#define OPTIONS_WORLD_OFFSET_Z world_offset_z
#define OPTIONS_POSTPONED_FRINGE postponed_fringe
#define OPTIONS_DEBUG_SCREEN_SIZE debug_screen_size
#define OPTIONS_SEA_LEVEL sea_level
//package net.minecraft.client;

//#include "locale/Language.h"

#include <string>
#include <cstdio>
#include "../platform/input/Keyboard.h"
#include "../util/StringUtils.h"
#include "OptionsFile.h"
#include "Option.h"
#include <array>

enum OptionId {
    // General
    OPTIONS_DIFFICULTY,
    OPTIONS_HIDEGUI,
    OPTIONS_THIRD_PERSON_VIEW,
    OPTIONS_GUI_SCALE,
    OPTIONS_DESTROY_VIBRATION,
    OPTIONS_MUSIC_VOLUME,
    OPTIONS_SOUND_VOLUME,
    OPTIONS_SKIN,
    OPTIONS_USERNAME,
    OPTIONS_SERVER_VISIBLE,
    OPTIONS_BAR_ON_TOP,
    OPTIONS_ALLOW_SPRINT,
    OPTIONS_AUTOJUMP,

    // Graphics
    OPTIONS_RENDER_DEBUG,
    OPTIONS_SMOOTH_CAMERA,
    OPTIONS_FIXED_CAMERA,
    OPTIONS_VIEW_DISTANCE,
    OPTIONS_VIEW_BOBBING,
    OPTIONS_AMBIENT_OCCLUSION,
    OPTIONS_ANAGLYPH_3D,
    OPTIONS_LIMIT_FRAMERATE,
    OPTIONS_VSYNC,
    OPTIONS_FANCY_GRAPHICS,

    // Cheats / debug
    OPTIONS_FLY_SPEED,
    OPTIONS_CAMERA_SPEED,
    OPTIONS_IS_FLYING,

    // Control
    OPTIONS_USE_MOUSE_FOR_DIGGING,
    OPTIONS_IS_LEFT_HANDED,
    OPTIONS_IS_JOY_TOUCH_AREA,
    OPTIONS_SENSITIVITY,
    OPTIONS_INVERT_Y_MOUSE,
    OPTIONS_USE_TOUCHSCREEN,

    OPTIONS_KEY_FORWARD,
    OPTIONS_KEY_LEFT,
    OPTIONS_KEY_BACK,
    OPTIONS_KEY_RIGHT,
    OPTIONS_KEY_JUMP,
    OPTIONS_KEY_INVENTORY,
    OPTIONS_KEY_SNEAK,
    OPTIONS_KEY_DROP,
    OPTIONS_KEY_CHAT,
    OPTIONS_KEY_FOG,
    OPTIONS_KEY_USE,

    OPTIONS_KEY_MENU_NEXT,
    OPTIONS_KEY_MENU_PREV,
    OPTIONS_KEY_MENU_OK,
    OPTIONS_KEY_MENU_CANCEL,

    OPTIONS_FIRST_LAUNCH,
    OPTIONS_LAST_IP,
    OPTIONS_FARLANDS_SCALE,
    OPTIONS_WORLD_OFFSET_X,
    OPTIONS_WORLD_OFFSET_Z,
    OPTIONS_POSTPONED_FRINGE,
    OPTIONS_DEBUG_SCREEN_SIZE,
    OPTIONS_SEA_LEVEL,
    OPTIONS_TELEPORT,           // 新增
    OPTIONS_STRIPE_REPAIR,   // 新增
    OPTIONS_WORLD_SCALE,
    OPTIONS_PROGRESSIVE_FARLANDS,
    OPTIONS_SIXTYFOUR_FARLANDS,
    OPTIONS_DOUBLE_FARLANDS,
    OPTIONS_WORLD_SCALE_X,   // 新增：X 轴缩放
    OPTIONS_WORLD_SCALE_Z,   // 新增：Z 轴缩放
    OPTIONS_DISABLE_SKYGRID,
    OPTIONS_WORLD_SCALE_Y,   // 新增：Y 轴缩放
    OPTIONS_WORLD_OFFSET_Y,  // 新增：Y 轴偏移
    OPTIONS_END_GENERATOR,
    OPTIONS_END_CIRCLES,  // 🛡️ 新增：末地环开关

    OPTIONS_RPI_CURSOR,
	// Should be last!
	OPTIONS_COUNT
};

class Minecraft;
typedef std::vector<std::string> StringVector;

class Options
{
public:
    float sound;
    float music;
    static bool debugGl;

    Options(Minecraft* minecraft, const std::string& workingDirectory = "") 
	: minecraft(minecraft),
      sound(1.0f),    // ★ 默认音量 1.0
      music(1.0f)     // ★ 默认音量 1.0
    {
        // elements werent initialized so i was getting a garbage pointer and a crash
        m_options.fill(nullptr);
        initTable();
	    // load() is deferred to init() where path is configured correctly
    }

    void initTable();

    int getIntValue(OptionId key) {
        auto option = opt<OptionInt>(key);
        return (option)? option->get() : 0;
    }

    std::string getStringValue(OptionId key) {
        auto option = opt<OptionString>(key);
        return (option)? option->get() : "";
    }

    float getProgressValue(OptionId key) {
        auto option = opt<OptionFloat>(key);
        return (option)? option->get() : 0.f;
    }

    bool getBooleanValue(OptionId key) {
        auto option = opt<OptionBool>(key);
        return (option)? option->get() : false;
    }

    float getProgrssMin(OptionId key) {
        auto option = opt<OptionFloat>(key);
        return (option)? option->getMin() : 0.f;
    }

    float getProgrssMax(OptionId key) {
        auto option = opt<OptionFloat>(key);
        return (option)? option->getMax() : 0.f;
    }

    Option* getOpt(OptionId id) { return m_options[id]; }

    void load();
    void save();
    void set(OptionId key, int value);
void set(OptionId key, float value);
void set(OptionId key, const std::string& value);
void set(OptionId key, bool value);   // 🆕 新增
	void setOptionsFilePath(const std::string& path);
	void toggle(OptionId key);

	void notifyOptionUpdate(OptionId key, bool value);
	void notifyOptionUpdate(OptionId key, float value);
	void notifyOptionUpdate(OptionId key, int value);
    void notifyOptionUpdate(OptionId key, const std::string& value) {}

private:
    template<typename T>
    T* opt(OptionId key) { 
        if (m_options[key] == nullptr) return nullptr;
        return dynamic_cast<T*>(m_options[key]); 
    }

	std::array<Option*, OPTIONS_COUNT> m_options;
	OptionsFile optionsFile;

	Minecraft* minecraft;
};

#endif /*NET_MINECRAFT_CLIENT__Options_H__*/
