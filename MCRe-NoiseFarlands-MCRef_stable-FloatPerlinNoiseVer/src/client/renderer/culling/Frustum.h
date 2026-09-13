// 文件: src/client/renderer/culling/Frustum.h
#ifndef NET_MINECRAFT_CLIENT_RENDERER_CULLING__Frustum_H__
#define NET_MINECRAFT_CLIENT_RENDERER_CULLING__Frustum_H__

#include "FrustumData.h"
#include "../gles.h"
#include <cmath>

class Frustum : public FrustumData {
public:
    static Frustum frustum;

    static FrustumData& getFrustum() {
        frustum.calculateFrustum();
        return frustum;
    }
    

    // src/client/renderer/culling/Frustum.h

void calculateFrustum() {
    float projF[16], modlF[16];
    glGetFloatv(GL_PROJECTION_MATRIX, projF);
    glGetFloatv(GL_MODELVIEW_MATRIX, modlF);
    
    double proj[16], modl[16];
    for (int i = 0; i < 16; i++) {
        proj[i] = (double)projF[i];
        modl[i] = (double)modlF[i];
    }
    
    // 关键修复：将模型视图矩阵的平移分量清零（索引 12,13,14 = 平移）
    // 因为剔除时顶点已经减去了相机坐标，这里必须用纯旋转矩阵
    modl[12] = 0.0;
    modl[13] = 0.0;
    modl[14] = 0.0;
    modl[15] = 1.0;
    
    double clip[16];
    multiplyMatrices(proj, modl, clip);  // 列主序乘法（已有）
    extractPlanes(clip);
    normalizePlanes();
}

private:
    void multiplyMatrices(const double* a, const double* b, double* result) {
    // a 和 b 都是 OpenGL 列主序 4x4 矩阵
    // result = a * b，也是列主序输出
    // 对于列主序矩阵，元素 (i,j) 的索引是 j*4 + i
    for (int i = 0; i < 4; i++) {          // 结果行
        for (int j = 0; j < 4; j++) {      // 结果列
            double sum = 0.0;
            for (int k = 0; k < 4; k++) {
                // a 的元素 (i,k) 索引为 k*4 + i
                // b 的元素 (k,j) 索引为 j*4 + k
                sum += a[k * 4 + i] * b[j * 4 + k];
            }
            result[j * 4 + i] = sum;        // 结果 (i,j) 存储到 j*4 + i
        }
    }
    }
    void extractPlanes(const double* m) {
        // 右平面
        m_Frustum[0][0] = m[3]  - m[0];
        m_Frustum[0][1] = m[7]  - m[4];
        m_Frustum[0][2] = m[11] - m[8];
        m_Frustum[0][3] = m[15] - m[12];
        // 左平面
        m_Frustum[1][0] = m[3]  + m[0];
        m_Frustum[1][1] = m[7]  + m[4];
        m_Frustum[1][2] = m[11] + m[8];
        m_Frustum[1][3] = m[15] + m[12];
        // 下平面
        m_Frustum[2][0] = m[3]  + m[1];
        m_Frustum[2][1] = m[7]  + m[5];
        m_Frustum[2][2] = m[11] + m[9];
        m_Frustum[2][3] = m[15] + m[13];
        // 上平面
        m_Frustum[3][0] = m[3]  - m[1];
        m_Frustum[3][1] = m[7]  - m[5];
        m_Frustum[3][2] = m[11] - m[9];
        m_Frustum[3][3] = m[15] - m[13];
        // 远平面
        m_Frustum[4][0] = m[3]  - m[2];
        m_Frustum[4][1] = m[7]  - m[6];
        m_Frustum[4][2] = m[11] - m[10];
        m_Frustum[4][3] = m[15] - m[14];
        // 近平面
        m_Frustum[5][0] = m[3]  + m[2];
        m_Frustum[5][1] = m[7]  + m[6];
        m_Frustum[5][2] = m[11] + m[10];
        m_Frustum[5][3] = m[15] + m[14];
    }

    void normalizePlanes() {
        for (int i = 0; i < 6; i++) {
            double length = std::sqrt(m_Frustum[i][0] * m_Frustum[i][0] +
                                      m_Frustum[i][1] * m_Frustum[i][1] +
                                      m_Frustum[i][2] * m_Frustum[i][2]);
            if (length > 0.0) {
                m_Frustum[i][0] /= length;
                m_Frustum[i][1] /= length;
                m_Frustum[i][2] /= length;
                m_Frustum[i][3] /= length;
            }
        }
    }
};

#endif
