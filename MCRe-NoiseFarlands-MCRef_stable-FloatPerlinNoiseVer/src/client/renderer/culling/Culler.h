#ifndef NET_MINECRAFT_CLIENT_RENDERER_CULLING__Culler_H__
#define NET_MINECRAFT_CLIENT_RENDERER_CULLING__Culler_H__

class AABB;

class Culler {
public:
    virtual ~Culler() {}
    virtual bool isVisible(const AABB& bb) = 0;
    // 参数全部改为 double
    virtual bool cubeInFrustum(double x0, double y0, double z0, 
                               double x1, double y1, double z1) = 0;
    virtual bool cubeFullyInFrustum(double x0, double y0, double z0, 
                                    double x1, double y1, double z1) = 0;
    virtual void prepare(double xOff, double yOff, double zOff) {}
};

#endif
