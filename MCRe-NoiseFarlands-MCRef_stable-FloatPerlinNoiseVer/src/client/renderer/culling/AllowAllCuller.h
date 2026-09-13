#ifndef NET_MINECRAFT_CLIENT_RENDERER_CULLING__AllowAllCuller_H__
#define NET_MINECRAFT_CLIENT_RENDERER_CULLING__AllowAllCuller_H__

#include "Culler.h"

class AllowAllCuller : public Culler {
public:
    bool isVisible(const AABB& bb) override {
        return true;
    }
    bool cubeFullyInFrustum(double x1, double y1, double z1,
                            double x2, double y2, double z2) override {
        return true;
    }
    bool cubeInFrustum(double x1, double y1, double z1,
                       double x2, double y2, double z2) override {
        return true;
    }
    void prepare(double xOff, double yOff, double zOff) override {}
};

#endif
