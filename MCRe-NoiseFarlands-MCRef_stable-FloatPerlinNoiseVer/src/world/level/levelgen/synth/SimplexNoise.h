// ====== src/world/level/levelgen/synth/SimplexNoise.h ======
#ifndef NET_MINECRAFT_WORLD_LEVEL_LEVELGEN_SYNTH__SimplexNoise_H__
#define NET_MINECRAFT_WORLD_LEVEL_LEVELGEN_SYNTH__SimplexNoise_H__

#include "Synth.h"
#include "../../../../util/Random.h"

namespace {
    const int SIMPLEX_GRADIENT[16][3] = {
        { 1, 1, 0}, {-1, 1, 0}, { 1,-1, 0}, {-1,-1, 0},
        { 1, 0, 1}, {-1, 0, 1}, { 1, 0,-1}, {-1, 0,-1},
        { 0, 1, 1}, { 0,-1, 1}, { 0, 1,-1}, { 0,-1,-1},
        { 1, 1, 0}, { 0,-1, 1}, {-1, 1, 0}, { 0,-1,-1}
    };
}

class SimplexNoise : public Synth {
    static constexpr float SQRT3  = 1.7320508075688772;
    static constexpr float F2     = 0.5 * (SQRT3 - 1.0);       // 0.3660254  skew
    static constexpr float G2     = (3.0 - SQRT3) / 6.0;       // 0.2113249  unskew
    static constexpr float G2x2   = 2.0 * G2;                  // 0.4226498

public:
    SimplexNoise(Random* random) {
        float xo = random->nextDouble() * 256.0;
        float yo = random->nextDouble() * 256.0;
        float zo = random->nextDouble() * 256.0;
        m_xo = xo;
        m_yo = yo;
        m_zo = zo;

        // 初始化排列表（仅 256，不翻倍）
        for (int i = 0; i < 256; ++i) m_p[i] = (unsigned char)i;
        for (int i = 0; i < 256; ++i) {
            int j = random->nextInt(256 - i) + i;
            unsigned char tmp = m_p[i];
            m_p[i] = m_p[j];
            m_p[j] = tmp;
        }
    }

    // 2D Simplex Noise（主接口）
    float getValue(float x, float z) override {

        // Skew 到单形空间
        float s = (x + z) * F2;
        int i = (int)std::floor(x + s);
        int j = (int)std::floor(z + s);

        // Unskew 回笛卡尔空间 → 第一个角的原点偏移
        float t = (i + j) * G2;
        float X0 = x - (i - t);
        float Z0 = z - (j - t);

        // 判断上下三角
        int i1, j1;  // 第二个角的偏移
        if (X0 > Z0) { i1 = 1; j1 = 0; }  // 下三角
        else         { i1 = 0; j1 = 1; }  // 上三角

        float x1 = X0 - i1 + G2;         // 第二个角的局部坐标
        float z1 = Z0 - j1 + G2;
        float x2 = X0 - 1.0 + G2x2;      // 第三个角的局部坐标
        float z2 = Z0 - 1.0 + G2x2;

        // Hashing
        int ii = i & 255;
        int jj = j & 255;

        int gi0 = pf(ii + pf(jj)) % 12;
        int gi1 = pf(ii + i1 + pf(jj + j1)) % 12;
        int gi2 = pf(ii + 1 + pf(jj + 1)) % 12;

        // 三个角的贡献（z=0 的 3D kernel，t=0.5）
        float n0 = cornerNoise(gi0, X0, Z0);
        float n1 = cornerNoise(gi1, x1, z1);
        float n2 = cornerNoise(gi2, x2, z2);

        // 标准缩放因子：70.0
        return 70.0 * (n0 + n1 + n2);
    }

private:
    float m_xo, m_yo, m_zo;
    unsigned char m_p[256];

    // 排列表查值
    unsigned char pf(int n) const { return m_p[n & 255]; }

    // 单角贡献：t = 0.5 - x² - z²，t⁴ × gradDot
    static float cornerNoise(int gi, float x, float z) {
        double t = 0.5 - x * x - z * z;
        if (t < 0.0) return 0.0;
        t *= t;  // t²
        return t * t * (SIMPLEX_GRADIENT[gi][0] * x + SIMPLEX_GRADIENT[gi][1] * z);
    }
};

#endif
