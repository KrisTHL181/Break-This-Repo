#ifndef UTIL_DEBUGLOG_H
#define UTIL_DEBUGLOG_H

#include <string>
#include <map>
#include <mutex>
#include <cstdio>

class DebugLog {
public:
    enum Level {
        LEVEL_DEBUG,
        LEVEL_INFO,
        LEVEL_WARN,
        LEVEL_ERROR
    };

    enum Category {
        CAT_GENERAL,
        CAT_RENDER,
        CAT_WORLD,
        CAT_SERVER,
        CAT_CLIENT,
        CAT_NETWORK,
        CAT_COUNT
    };

    static DebugLog& instance();

    void initialize(const std::string& logDir);
    void shutdown();

    void setMinLevel(Level level) { m_minLevel = level; }

    void log(Category cat, Level level, const char* format, ...);

    void debug(Category cat, const char* format, ...);
    void info(Category cat, const char* format, ...);
    void warn(Category cat, const char* format, ...);
    void error(Category cat, const char* format, ...);

    bool isInitialized() const { return m_initialized; }

    std::string getCategoryString(Category cat);
    std::string getLevelString(Level level);

private:
    DebugLog();
    ~DebugLog();
    DebugLog(const DebugLog&) = delete;
    DebugLog& operator=(const DebugLog&) = delete;

    std::string getTimestamp();

    FILE* m_file;                       // 改为 FILE*
    std::string m_logPath;
    bool m_initialized;
    std::mutex m_mutex;
    std::map<Category, std::string> m_categoryNames;
    Level m_minLevel;                   // 新增最低级别
};

// 便捷宏
#define DLOG_DEBUG(cat, format, ...) DebugLog::instance().debug(DebugLog::CAT_##cat, format, ##__VA_ARGS__)
#define DLOG_INFO(cat, format, ...)  DebugLog::instance().info(DebugLog::CAT_##cat, format, ##__VA_ARGS__)
#define DLOG_WARN(cat, format, ...)  DebugLog::instance().warn(DebugLog::CAT_##cat, format, ##__VA_ARGS__)
#define DLOG_ERROR(cat, format, ...) DebugLog::instance().error(DebugLog::CAT_##cat, format, ##__VA_ARGS__)

// 简写
#define DLOG_G(fmt, ...) DLOG_INFO(GENERAL, fmt, ##__VA_ARGS__)
#define DLOG_R(fmt, ...) DLOG_INFO(RENDER, fmt, ##__VA_ARGS__)
#define DLOG_W(fmt, ...) DLOG_INFO(WORLD, fmt, ##__VA_ARGS__)
#define DLOG_S(fmt, ...) DLOG_INFO(SERVER, fmt, ##__VA_ARGS__)
#define DLOG_C(fmt, ...) DLOG_INFO(CLIENT, fmt, ##__VA_ARGS__)
#define DLOG_N(fmt, ...) DLOG_INFO(NETWORK, fmt, ##__VA_ARGS__)

#endif
