#ifndef NET_MINECRAFT_WORLD__WorldCoordinate_H__
#define NET_MINECRAFT_WORLD__WorldCoordinate_H__

#include <cstdint>
#include <string>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <limits>
// 第 10 行附近，cpp_dec_float.hpp 之后
#include <boost/multiprecision/cpp_dec_float.hpp>
#include <boost/multiprecision/cpp_int.hpp>   // <-- 加这一行

// =====================================================================
//  类型定义
// =====================================================================

/// 日常存储/计算使用的双精度坐标（快速）
typedef double WorldCoordinate;
/// 日常使用的 64 位整数坐标（快速）
typedef int64_t WorldCoordinate_Integer;

/// 中间安全计算使用的任意精度十进制浮点（防溢出/防 inf，算完即丢）
typedef boost::multiprecision::number<
    boost::multiprecision::cpp_dec_float<50>,
    boost::multiprecision::et_off
> BigWorldCoordinate;

/// 中间安全计算使用的任意精度整数（防溢出，算完即丢）
typedef boost::multiprecision::number<
    boost::multiprecision::cpp_int_backend<>,
    boost::multiprecision::et_off
> BigWorldCoordinate_Integer;

// =====================================================================
//  阈值常量
// =====================================================================

/// |坐标| ≥ 2^48 时切换 BigWorldCoordinate 精确路径
static constexpr double BIG_THRESHOLD = 281474976710656.0;

// =====================================================================
//  阈值判断
// =====================================================================

inline bool needsBigWorldCoord(double v) {
    return !std::isfinite(v) || std::abs(v) >= BIG_THRESHOLD;
}

// ── 阈值判断（复用 2^48） ──

inline bool needsBigWorldCoord_Integer(int64_t v) {
    return (v >= BIG_THRESHOLD) || (v <= -BIG_THRESHOLD);
}

// =====================================================================
//  BigWorldCoordinate 计算
// =====================================================================

/// double → BigWorldCoordinate
inline BigWorldCoordinate worldCoordToBig(double v) {
    return BigWorldCoordinate(v);
}

// ── 转换 ──

inline BigWorldCoordinate_Integer worldCoord_IntegerToBig(int64_t v) {
    return BigWorldCoordinate_Integer(v);
}

/// 精确计算 pxo = px * scale + offset * scale
inline BigWorldCoordinate computeWorldCoordBig(
    double px,
    WorldCoordinate scale,
    WorldCoordinate offset)
{
    BigWorldCoordinate bpx(px);
    BigWorldCoordinate bs(scale);
    BigWorldCoordinate bo(offset);
    return bpx * bs + bo * bs;
}

/// BigWorldCoordinate → double
inline double worldCoordBigToDouble(const BigWorldCoordinate& v) {
    return v.convert_to<double>();
}

inline int64_t worldCoord_BigToInteger(const BigWorldCoordinate_Integer& v) {
    // clamp 到 int64_t 范围
    const BigWorldCoordinate_Integer maxV = (std::numeric_limits<int64_t>::max)();
    const BigWorldCoordinate_Integer minV = (std::numeric_limits<int64_t>::min)();
    if (v > maxV) return (std::numeric_limits<int64_t>::max)();
    if (v < minV) return (std::numeric_limits<int64_t>::min)();
    return static_cast<int64_t>(v);
}

/// BigWorldCoordinate → 字符串
inline std::string worldCoordBigToString(const BigWorldCoordinate& v) {
    return v.str();
}

inline std::string worldCoord_IntegerBigToString(const BigWorldCoordinate_Integer& v) {
    return v.str();
}

// ── 计算：用 cpp_int 做超大整数运算，防溢出 ──

inline BigWorldCoordinate_Integer computeBlockCoordBig(
    int64_t px, int64_t py, int64_t pz,
    int64_t scale, int64_t offset)
{
    BigWorldCoordinate_Integer bpx(px);
    BigWorldCoordinate_Integer bpy(py);
    BigWorldCoordinate_Integer bpz(pz);
    BigWorldCoordinate_Integer bs(scale);
    BigWorldCoordinate_Integer bo(offset);
    return bpx * bs + bo * bs;
}

// =====================================================================
//  整数坐标工具（区块/方块坐标相关计算）
// =====================================================================

inline bool canConvertWorldCoordinateToInt(WorldCoordinate_Integer value) {
    return value >= static_cast<WorldCoordinate_Integer>((std::numeric_limits<int>::min)())
        && value <= static_cast<WorldCoordinate_Integer>((std::numeric_limits<int>::max)());
}

inline int clampWorldCoordinateToInt(WorldCoordinate_Integer value) {
    if (value > static_cast<WorldCoordinate_Integer>((std::numeric_limits<int>::max)())) {
        return (std::numeric_limits<int>::max)();
    }
    if (value < static_cast<WorldCoordinate_Integer>((std::numeric_limits<int>::min)())) {
        return (std::numeric_limits<int>::min)();
    }
    return static_cast<int>(value);
}

inline WorldCoordinate_Integer floorDivWorldCoordinate(WorldCoordinate_Integer value, WorldCoordinate_Integer divisor) {
    WorldCoordinate_Integer quotient = value / divisor;
    WorldCoordinate_Integer remainder = value % divisor;
    if (remainder != 0 && ((remainder > 0) != (divisor > 0))) {
        --quotient;
    }
    return quotient;
}

inline WorldCoordinate_Integer blockToChunkCoordinate(WorldCoordinate_Integer blockCoordinate) {
    return floorDivWorldCoordinate(blockCoordinate, 16);
}

inline int localBlockCoordinate(WorldCoordinate_Integer blockCoordinate) {
    WorldCoordinate_Integer local = blockCoordinate % 16;
    if (local < 0) {
        local += 16;
    }
    return static_cast<int>(local);
}

inline WorldCoordinate_Integer clampBigWorldCoordinateToInt(const BigWorldCoordinate_Integer& value) {
    const BigWorldCoordinate_Integer maxValue = (std::numeric_limits<WorldCoordinate_Integer>::max)();
    const BigWorldCoordinate_Integer minValue = (std::numeric_limits<WorldCoordinate_Integer>::min)();
    if (value > maxValue) return (std::numeric_limits<WorldCoordinate_Integer>::max)();
    if (value < minValue) return (std::numeric_limits<WorldCoordinate_Integer>::min)();
    return static_cast<WorldCoordinate_Integer>(value.convert_to<long long>());
}

#endif /* NET_MINECRAFT_WORLD__WorldCoordinate_H__ */
