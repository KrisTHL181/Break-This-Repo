#ifndef NET_MINECRAFT_CLIENT_RENDERER_CULLING__FrustumCuller_H__
#define NET_MINECRAFT_CLIENT_RENDERER_CULLING__FrustumCuller_H__

#include "Culler.h"
#include "FrustumData.h"
#include "Frustum.h"          // 使用统一的视锥体提取
#include "../../../world/phys/AABB.h"

class FrustumCuller : public Culler {
    double xOff, yOff, zOff;
    FrustumData frustum;      // 用 FrustumData 对象，内部是 double[6][4]

public:
    FrustumCuller() {
        frustum = Frustum::getFrustum();   // 初始时提取一次
    }

    void prepare(double xOff, double yOff, double zOff) override {
        this->xOff = xOff;
        this->yOff = yOff;
        this->zOff = zOff;
        // 每帧必须更新视锥体，因为相机视角和位置可能改变
        frustum = Frustum::getFrustum();
    }

    bool cubeFullyInFrustum(double x0, double y0, double z0,
                            double x1, double y1, double z1) override {
        return frustum.cubeFullyInFrustum(x0 - xOff, y0 - yOff, z0 - zOff,
                                          x1 - xOff, y1 - yOff, z1 - zOff);
    }

    bool cubeInFrustum(double x0, double y0, double z0,
                       double x1, double y1, double z1) override {
        return frustum.cubeInFrustum(x0 - xOff, y0 - yOff, z0 - zOff,
                                     x1 - xOff, y1 - yOff, z1 - zOff);
    }

    bool isVisible(const AABB& bb) override {
        return cubeInFrustum(bb.x0, bb.y0, bb.z0, bb.x1, bb.y1, bb.z1);
    }
};

#endif
