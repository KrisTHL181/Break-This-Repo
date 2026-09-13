#ifndef NET_MINECRAFT_WORLD_PHYS__Vec3_H__
#define NET_MINECRAFT_WORLD_PHYS__Vec3_H__

#include <cmath>
#include <string>
#include <sstream>

class Vec3
{
public:
    Vec3() : x(0.0), y(0.0), z(0.0) {}

    Vec3(double x, double y, double z) {
        if (x == -0.0) x = 0.0;
        if (y == -0.0) y = 0.0;
        if (z == -0.0) z = 0.0;
        this->x = x;
        this->y = y;
        this->z = z;
    }

    Vec3* set(double x, double y, double z) {
        this->x = x; this->y = y; this->z = z;
        return this;
    }

    // 供 UI 使用的 float 转换（可选，安全）
    float fx() const { return (float)x; }
    float fy() const { return (float)y; }
    float fz() const { return (float)z; }

    // 运算符重载（全部 double）
    Vec3 operator+(const Vec3& rhs) const {
        return Vec3(x + rhs.x, y + rhs.y, z + rhs.z);
    }
    Vec3& operator+=(const Vec3& rhs) {
        x += rhs.x; y += rhs.y; z += rhs.z;
        return *this;
    }
    Vec3 operator-(const Vec3& rhs) const {
        return Vec3(x - rhs.x, y - rhs.y, z - rhs.z);
    }
    Vec3& operator-=(const Vec3& rhs) {
        x -= rhs.x; y -= rhs.y; z -= rhs.z;
        return *this;
    }
    Vec3 operator*(double k) const {
        return Vec3(x * k, y * k, z * k);
    }
    Vec3& operator*=(double k) {
        x *= k; y *= k; z *= k;
        return *this;
    }

    Vec3 normalized() const {
        double dist = sqrt(x*x + y*y + z*z);
        if (dist < 1e-12) return Vec3();
        return Vec3(x / dist, y / dist, z / dist);
    }
    double dot(const Vec3& p) const {
        return x * p.x + y * p.y + z * p.z;
    }
    Vec3 cross(const Vec3& p) const {
        return Vec3(y * p.z - z * p.y,
                    z * p.x - x * p.z,
                    x * p.y - y * p.x);
    }
    Vec3 add(double x, double y, double z) const {
        return Vec3(this->x + x, this->y + y, this->z + z);
    }
    Vec3& addSelf(double x, double y, double z) {
        this->x += x; this->y += y; this->z += z;
        return *this;
    }
    Vec3 sub(double x, double y, double z) const {
        return Vec3(this->x - x, this->y - y, this->z - z);
    }
    Vec3& subSelf(double x, double y, double z) {
        this->x -= x; this->y -= y; this->z -= z;
        return *this;
    }
    void negate() { x = -x; y = -y; z = -z; }
    Vec3 negated() const { return Vec3(-x, -y, -z); }

    double distanceTo(const Vec3& p) const {
        double dx = p.x - x, dy = p.y - y, dz = p.z - z;
        return sqrt(dx*dx + dy*dy + dz*dz);
    }
    double distanceToSqr(const Vec3& p) const {
        double dx = p.x - x, dy = p.y - y, dz = p.z - z;
        return dx*dx + dy*dy + dz*dz;
    }
    double distanceToSqr(double x2, double y2, double z2) const {
        double dx = x2 - x, dy = y2 - y, dz = z2 - z;
        return dx*dx + dy*dy + dz*dz;
    }
    double length() const { return sqrt(x*x + y*y + z*z); }

    bool clipX(const Vec3& b, double xt, Vec3& result) const {
        double dx = b.x - x, dy = b.y - y, dz = b.z - z;
        if (dx*dx < 1e-12) return false;
        double d = (xt - x) / dx;
        if (d < 0 || d > 1) return false;
        result.set(x + dx*d, y + dy*d, z + dz*d);
        return true;
    }
    bool clipY(const Vec3& b, double yt, Vec3& result) const {
        double dx = b.x - x, dy = b.y - y, dz = b.z - z;
        if (dy*dy < 1e-12) return false;
        double d = (yt - y) / dy;
        if (d < 0 || d > 1) return false;
        result.set(x + dx*d, y + dy*d, z + dz*d);
        return true;
    }
    bool clipZ(const Vec3& b, double zt, Vec3& result) const {
        double dx = b.x - x, dy = b.y - y, dz = b.z - z;
        if (dz*dz < 1e-12) return false;
        double d = (zt - z) / dz;
        if (d < 0 || d > 1) return false;
        result.set(x + dx*d, y + dy*d, z + dz*d);
        return true;
    }

    std::string toString() const {
        std::stringstream ss;
        ss << "Vec3(" << x << "," << y << "," << z << ")";
        return ss.str();
    }
    Vec3 lerp(const Vec3& v, double a) const {
        return Vec3(x + (v.x - x)*a, y + (v.y - y)*a, z + (v.z - z)*a);
    }
    void xRot(double degs) {
        double cosv = cos(degs), sinv = sin(degs);
        double yy = y * cosv + z * sinv;
        double zz = z * cosv - y * sinv;
        y = yy; z = zz;
    }
    void yRot(double degs) {
        double cosv = cos(degs), sinv = sin(degs);
        double xx = x * cosv + z * sinv;
        double zz = z * cosv - x * sinv;
        x = xx; z = zz;
    }
    void zRot(double degs) {
        double cosv = cos(degs), sinv = sin(degs);
        double xx = x * cosv + y * sinv;
        double yy = y * cosv - x * sinv;
        x = xx; y = yy;
    }
    static Vec3 fromPolarXY(double angle, double radius) {
        return Vec3(radius * cos(angle), radius * sin(angle), 0);
    }

    double x, y, z;
};

#endif
