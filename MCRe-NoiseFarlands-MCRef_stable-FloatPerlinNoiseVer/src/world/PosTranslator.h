#ifndef NET_MINECRAFT_WORLD_PosTranslator_H__
#define NET_MINECRAFT_WORLD_PosTranslator_H__

//package net.minecraft;

class IPosTranslator {
public:
    virtual ~IPosTranslator() {}
    virtual void to(int& x, int& y, int& z) = 0;
    virtual void to(float& x, float& y, float& z) = 0;
    virtual void to(double& x, double& y, double& z) = 0;   // 🆕 新增 double 接口

    virtual void from(int& x, int& y, int& z) = 0;
    virtual void from(float& x, float& y, float& z) = 0;
    virtual void from(double& x, double& y, double& z) = 0; // 🆕 新增 double 接口
};

class OffsetPosTranslator : public IPosTranslator {
public:
    OffsetPosTranslator()
        : xo(0.0), yo(0.0), zo(0.0)
    {}
    OffsetPosTranslator(double xo, double yo, double zo)
        : xo(xo), yo(yo), zo(zo)
    {}

    // Double 版本（主要使用）
    void to(double& x, double& y, double& z) override {
        x += xo;
        y += yo;
        z += zo;
    }
    void from(double& x, double& y, double& z) override {
        x -= xo;
        y -= yo;
        z -= zo;
    }

    // Float 版本（兼容旧代码，内部转为 double 计算以保证精度）
    void to(float& x, float& y, float& z) override {
        double dx = x, dy = y, dz = z;
        dx += xo; dy += yo; dz += zo;
        x = (float)dx;
        y = (float)dy;
        z = (float)dz;
    }
    void from(float& x, float& y, float& z) override {
        double dx = x, dy = y, dz = z;
        dx -= xo; dy -= yo; dz -= zo;
        x = (float)dx;
        y = (float)dy;
        z = (float)dz;
    }

    // Int 版本（保持原有逻辑）
    void to(int& x, int& y, int& z) override {
        x += (int)xo;
        y += (int)yo;
        z += (int)zo;
    }
    void from(int& x, int& y, int& z) override {
        x -= (int)xo;
        y -= (int)yo;
        z -= (int)zo;
    }

    double xo, yo, zo;   // 成员变量升级为 double
};

#endif /*NET_MINECRAFT_WORLD_PosTranslator_H__*/
