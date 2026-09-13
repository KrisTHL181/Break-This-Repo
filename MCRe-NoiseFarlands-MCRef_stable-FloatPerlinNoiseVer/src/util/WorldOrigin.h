// src/util/WorldOrigin.h
#ifndef WORLDORIGIN_H__
#define WORLDORIGIN_H__

#include "WorldCoordinate.h"

class WorldOrigin {
    BigWorldCoordinate m_originX = 0;
    BigWorldCoordinate m_originY = 0;
    BigWorldCoordinate m_originZ = 0;

    double m_localX = 0.0;
    double m_localY = 0.0;
    double m_localZ = 0.0;

public:
    // ═══════════════════════════════════════════
    // 每帧调用 — 用 Big 精度重算 local，超阈值自动切换原点
    // entityX/Y/Z 是 Entity 当前的绝对坐标 (double)
    // ═══════════════════════════════════════════
    // BigWorldCoordinate 版 tick — 无限精度
    // ====== BigWorldCoordinate 版 tick — 无限精度 ======
    void tickBig(const BigWorldCoordinate& absX,
                 const BigWorldCoordinate& absY,
                 const BigWorldCoordinate& absZ) {
        recomputeLocalBig(absX, m_localX, m_originX);
        recomputeLocalBig(absY, m_localY, m_originY);
        recomputeLocalBig(absZ, m_localZ, m_originZ);
    }

    // 获取精确的绝对坐标 (Big)
    BigWorldCoordinate absX() const { return m_originX + BigWorldCoordinate(m_localX); }
    BigWorldCoordinate absY() const { return m_originY + BigWorldCoordinate(m_localY); }
    BigWorldCoordinate absZ() const { return m_originZ + BigWorldCoordinate(m_localZ); }

    // 转回 double (供调试/网络/存档)
    double absXAsDouble() const { return absX().convert_to<double>(); }
    double absYAsDouble() const { return absY().convert_to<double>(); }
    double absZAsDouble() const { return absZ().convert_to<double>(); }

    // 原点分量 (供 RandomLevelSource 更新偏移)
    const BigWorldCoordinate& originX() const { return m_originX; }
    const BigWorldCoordinate& originY() const { return m_originY; }
    const BigWorldCoordinate& originZ() const { return m_originZ; }

    // 原来的 teleport 接口保留
    void onTeleport(double oldLocal, double newLocal, BigWorldCoordinate& origin) {
        double delta = newLocal - oldLocal;
        if (std::abs(delta) >= BIG_THRESHOLD) {
            int64_t offset = (int64_t)(delta / (double)BIG_THRESHOLD);
            origin += BigWorldCoordinate((double)offset * BIG_THRESHOLD);
        }
    }

    void setOrigin(double ox, double oy, double oz) {
        m_originX = BigWorldCoordinate(ox);
        m_originY = BigWorldCoordinate(oy);
        m_originZ = BigWorldCoordinate(oz);
    }

private:
    static constexpr double ORIGIN_SHIFT = 281474976710656.0; // 2^48

    // Big 版 recalculate — 无精度损失
    void recomputeLocalBig(const BigWorldCoordinate& absCoord,
                           double& local,
                           BigWorldCoordinate& origin) {
        BigWorldCoordinate bloc = absCoord - origin;
        local = bloc.convert_to<double>();
        if (std::abs(local) >= ORIGIN_SHIFT) {
            double sign = (local > 0.0) ? 1.0 : -1.0;
            double steps = std::floor(std::abs(local) / ORIGIN_SHIFT);
            double shift = steps * ORIGIN_SHIFT * sign;
            BigWorldCoordinate bshift(shift);
            BigWorldCoordinate bnewLocal = bloc - bshift;
            local = bnewLocal.convert_to<double>();
            origin += bshift;
        }
    }
};

#endif
