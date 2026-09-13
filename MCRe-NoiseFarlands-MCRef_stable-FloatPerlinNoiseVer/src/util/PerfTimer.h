#ifndef NET_UTIL__PerfTimer_H__
#define NET_UTIL__PerfTimer_H__

#include <map>
#include <vector>
#include "StringUtils.h"

#define PERF_TIMER_SKIP_FRAMES 1       // 最佳平衡点

#ifdef PROFILER
    #define TIMER_PUSH(x) do { \
        if (PerfTimer::enabled && (PerfTimer::s_frameCounter % PERF_TIMER_SKIP_FRAMES == 0 || PerfTimer::s_warmupFrames > 0)) \
            PerfTimer::push(x); \
    } while(0)

    #define TIMER_POP() do { \
        if (PerfTimer::enabled && (PerfTimer::s_frameCounter % PERF_TIMER_SKIP_FRAMES == 0 || PerfTimer::s_warmupFrames > 0)) \
            PerfTimer::pop(); \
    } while(0)

    #define TIMER_POP_PUSH(x) do { \
        if (PerfTimer::enabled && (PerfTimer::s_frameCounter % PERF_TIMER_SKIP_FRAMES == 0 || PerfTimer::s_warmupFrames > 0)) \
            PerfTimer::popPush(x); \
    } while(0)
#else
    #define TIMER_PUSH(x)       ((void*)0)
    #define TIMER_POP()         ((void*)0)
    #define TIMER_POP_PUSH(x)   ((void*)0)
#endif

class PerfTimer
{
    typedef std::map<std::string, float> TimeMap;
public:
    class ResultField {
    public:
        float percentage;
        float globalPercentage;
        std::string name;

        ResultField(const std::string& name, float percentage, float globalPercentage)
            : name(name), percentage(percentage), globalPercentage(globalPercentage) {}

        bool operator<(const ResultField& rf) const {
            if (percentage != rf.percentage) return percentage > rf.percentage;
            return name > rf.name;
        }
        int getColor() const {
            return (Util::hashCode(name) & 0xaaaaaa) + 0x444444;
        }
    };

    static void reset();
    static void push(const std::string& name);
    static void pop();
    static void popPush(const std::string& name);
    static std::vector<ResultField> getLog(const std::string& path, bool forceUpdate = false);
    static void tickFrame();

    static bool enabled;
    static int  s_frameCounter;
    static int  s_warmupFrames;         // 每次 reset 后全速记录 64 帧

private:
    static std::vector<std::string> paths;
    static std::vector<float> startTimes;
    static std::string path;
    static TimeMap times;
};

#endif
