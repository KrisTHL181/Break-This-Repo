// DebugLog.cpp 修复跨平台编译

#include "DebugLog.h"
#include <cstdarg>
#include <ctime>
#include <chrono>
#include <iomanip>
#include <sstream>
#include <cstring>

#ifdef __ANDROID__
    #include <android/log.h>
#endif

#ifdef _WIN32
    #include <windows.h>
#else
    #include <unistd.h>
    #include <sys/stat.h>
#endif

DebugLog::DebugLog() : m_initialized(false), m_file(nullptr), m_minLevel(LEVEL_DEBUG) {
    m_categoryNames[CAT_GENERAL] = "GENERAL";
    m_categoryNames[CAT_RENDER]  = "RENDER ";
    m_categoryNames[CAT_WORLD]   = "WORLD  ";
    m_categoryNames[CAT_SERVER]  = "SERVER ";
    m_categoryNames[CAT_CLIENT]  = "CLIENT ";
    m_categoryNames[CAT_NETWORK] = "NETWORK";
}

DebugLog::~DebugLog() {
    shutdown();
}

DebugLog& DebugLog::instance() {
    static DebugLog instance;
    return instance;
}

static bool createDirectory(const std::string& path) {
#ifdef _WIN32
    return CreateDirectoryA(path.c_str(), NULL) != 0 || GetLastError() == ERROR_ALREADY_EXISTS;
#else
    return mkdir(path.c_str(), 0755) == 0 || errno == EEXIST;
#endif
}

void DebugLog::initialize(const std::string& logDir) {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_initialized) return;

    if (!createDirectory(logDir)) {
        // 目录创建失败，输出到 stderr 作为降级
        fprintf(stderr, "DebugLog: Failed to create log directory: %s\n", logDir.c_str());
    }

    auto now = std::chrono::system_clock::now();
    std::time_t t = std::chrono::system_clock::to_time_t(now);
    std::tm tm;
#ifdef _WIN32
    localtime_s(&tm, &t);
#else
    localtime_r(&t, &tm);
#endif
    char timeBuf[64];
    std::strftime(timeBuf, sizeof(timeBuf), "%Y%m%d_%H%M%S", &tm);
    m_logPath = logDir + "/debug_" + timeBuf + ".log";

    m_file = fopen(m_logPath.c_str(), "a");
    if (m_file) {
        m_initialized = true;
        info(CAT_GENERAL, "=== DebugLog initialized ===");
        info(CAT_GENERAL, "Log file: %s", m_logPath.c_str());
    } else {
        fprintf(stderr, "DebugLog: FAILED to open log file: %s\n", m_logPath.c_str());
    }
}

void DebugLog::shutdown() {
    std::lock_guard<std::mutex> lock(m_mutex);
    if (m_file) {
        info(CAT_GENERAL, "=== DebugLog shutdown ===");
        fclose(m_file);
        m_file = nullptr;
    }
    m_initialized = false;
}

std::string DebugLog::getTimestamp() {
    auto now = std::chrono::system_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()) % 1000;
    std::time_t t = std::chrono::system_clock::to_time_t(now);
    std::tm tm;
#ifdef _WIN32
    localtime_s(&tm, &t);
#else
    localtime_r(&t, &tm);
#endif
    char buf[32];
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", &tm);
    std::ostringstream oss;
    oss << buf << "." << std::setfill('0') << std::setw(3) << ms.count();
    return oss.str();
}

std::string DebugLog::getLevelString(Level level) {
    switch (level) {
        case LEVEL_DEBUG: return "DEBUG";
        case LEVEL_INFO:  return "INFO ";
        case LEVEL_WARN:  return "WARN ";
        case LEVEL_ERROR: return "ERROR";
        default: return "?????";
    }
}

std::string DebugLog::getCategoryString(Category cat) {
    auto it = m_categoryNames.find(cat);
    if (it != m_categoryNames.end()) return it->second;
    return "UNKNOWN";
}

void DebugLog::log(Category cat, Level level, const char* format, ...) {
    if (level < m_minLevel) return;

    va_list args;
    va_start(args, format);

    if (!m_initialized || !m_file) {
        // 降级输出到 stderr（所有平台通用）
        fprintf(stderr, "[%s] [%s] [%s] ", getTimestamp().c_str(), getCategoryString(cat).c_str(), getLevelString(level).c_str());
        vfprintf(stderr, format, args);
        fprintf(stderr, "\n");
        va_end(args);
        return;
    }

    std::string line = "[" + getTimestamp() + "] [" + getCategoryString(cat) + "] [" + getLevelString(level) + "] ";
    fprintf(m_file, "%s", line.c_str());
    vfprintf(m_file, format, args);
    fprintf(m_file, "\n");
    fflush(m_file);

    // 同时输出到 stderr（方便观察）
    fprintf(stderr, "%s", line.c_str());
    vfprintf(stderr, format, args);
    fprintf(stderr, "\n");

    va_end(args);
}

void DebugLog::debug(Category cat, const char* format, ...) {
    va_list args;
    va_start(args, format);
    log(cat, LEVEL_DEBUG, format, args);
    va_end(args);
}

void DebugLog::info(Category cat, const char* format, ...) {
    va_list args;
    va_start(args, format);
    log(cat, LEVEL_INFO, format, args);
    va_end(args);
}

void DebugLog::warn(Category cat, const char* format, ...) {
    va_list args;
    va_start(args, format);
    log(cat, LEVEL_WARN, format, args);
    va_end(args);
}

void DebugLog::error(Category cat, const char* format, ...) {
    va_list args;
    va_start(args, format);
    log(cat, LEVEL_ERROR, format, args);
    va_end(args);
}
