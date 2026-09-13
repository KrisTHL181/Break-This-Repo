#ifndef NET_MINECRAFT_WORLD_PHYS__AABB_H__
#define NET_MINECRAFT_WORLD_PHYS__AABB_H__

#include <vector>
#include "Vec3.h"
#include "HitResult.h"
#include <sstream>

class AABB
{
public:
    AABB()
    :   x0(0), y0(0), z0(0),
        x1(1), y1(1), z1(1)
    {}

    AABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        this->x0 = x0; this->y0 = y0; this->z0 = z0;
        this->x1 = x1; this->y1 = y1; this->z1 = z1;
    }

    AABB* set(double x0, double y0, double z0, double x1, double y1, double z1) {
        this->x0 = x0; this->y0 = y0; this->z0 = z0;
        this->x1 = x1; this->y1 = y1; this->z1 = z1;
        return this;
    }

    AABB expand(double xa, double ya, double za) {
        double _x0 = x0, _y0 = y0, _z0 = z0;
        double _x1 = x1, _y1 = y1, _z1 = z1;
        if (xa < 0) _x0 += xa;
        if (xa > 0) _x1 += xa;
        if (ya < 0) _y0 += ya;
        if (ya > 0) _y1 += ya;
        if (za < 0) _z0 += za;
        if (za > 0) _z1 += za;
        return AABB(_x0, _y0, _z0, _x1, _y1, _z1);
    }

    AABB grow(double xa, double ya, double za) const {
        return AABB(x0 - xa, y0 - ya, z0 - za, x1 + xa, y1 + ya, z1 + za);
    }

    AABB cloneMove(double xa, double ya, double za) const {
        return AABB(x0 + xa, y0 + ya, z0 + za, x1 + xa, y1 + ya, z1 + za);
    }

    double clipXCollide(const AABB& c, double xa) const {
        if (c.y1 <= y0 || c.y0 >= y1) return xa;
        if (c.z1 <= z0 || c.z0 >= z1) return xa;
        if (xa > 0 && c.x1 <= x0) {
            double max = x0 - c.x1;
            if (max < xa) xa = max;
        }
        if (xa < 0 && c.x0 >= x1) {
            double max = x1 - c.x0;
            if (max > xa) xa = max;
        }
        return xa;
    }

    double clipYCollide(const AABB& c, double ya) const {
        if (c.x1 <= x0 || c.x0 >= x1) return ya;
        if (c.z1 <= z0 || c.z0 >= z1) return ya;
        if (ya > 0 && c.y1 <= y0) {
            double max = y0 - c.y1;
            if (max < ya) ya = max;
        }
        if (ya < 0 && c.y0 >= y1) {
            double max = y1 - c.y0;
            if (max > ya) ya = max;
        }
        return ya;
    }

    double clipZCollide(const AABB& c, double za) const {
        if (c.x1 <= x0 || c.x0 >= x1) return za;
        if (c.y1 <= y0 || c.y0 >= y1) return za;
        if (za > 0 && c.z1 <= z0) {
            double max = z0 - c.z1;
            if (max < za) za = max;
        }
        if (za < 0 && c.z0 >= z1) {
            double max = z1 - c.z0;
            if (max > za) za = max;
        }
        return za;
    }

    bool intersects(const AABB& c) const {
        if (c.x1 <= x0 || c.x0 >= x1) return false;
        if (c.y1 <= y0 || c.y0 >= y1) return false;
        if (c.z1 <= z0 || c.z0 >= z1) return false;
        return true;
    }

    bool intersectsInner(const AABB& c) const {
        if (c.x1 < x0 || c.x0 > x1) return false;
        if (c.y1 < y0 || c.y0 > y1) return false;
        if (c.z1 < z0 || c.z0 > z1) return false;
        return true;
    }

    AABB* move(double xa, double ya, double za) {
        x0 += xa; y0 += ya; z0 += za;
        x1 += xa; y1 += ya; z1 += za;
        return this;
    }

    bool intersects(double x02, double y02, double z02, double x12, double y12, double z12) const {
        if (x12 <= x0 || x02 >= x1) return false;
        if (y12 <= y0 || y02 >= y1) return false;
        if (z12 <= z0 || z02 >= z1) return false;
        return true;
    }

    bool contains(const Vec3& p) const {
        return p.x > x0 && p.x < x1 && p.y > y0 && p.y < y1 && p.z > z0 && p.z < z1;
    }

    double getSize() const {
        double xs = x1 - x0;
        double ys = y1 - y0;
        double zs = z1 - z0;
        return (xs + ys + zs) / 3.0;
    }

    AABB shrink(double xa, double ya, double za) {
        return AABB(x0 + xa, y0 + ya, z0 + za, x1 - xa, y1 - ya, z1 - za);
    }

    AABB copy() {
        return AABB(x0, y0, z0, x1, y1, z1);
    }

    HitResult clip(const Vec3& a, const Vec3& b) {
        Vec3 xh0, xh1, yh0, yh1, zh0, zh1;

        bool bxh0 = a.clipX(b, x0, xh0);
        bool bxh1 = a.clipX(b, x1, xh1);
        bool byh0 = a.clipY(b, y0, yh0);
        bool byh1 = a.clipY(b, y1, yh1);
        bool bzh0 = a.clipZ(b, z0, zh0);
        bool bzh1 = a.clipZ(b, z1, zh1);

        if (!bxh0 || !containsX(&xh0)) bxh0 = false;
        if (!bxh1 || !containsX(&xh1)) bxh1 = false;
        if (!byh0 || !containsY(&yh0)) byh0 = false;
        if (!byh1 || !containsY(&yh1)) byh1 = false;
        if (!bzh0 || !containsZ(&zh0)) bzh0 = false;
        if (!bzh1 || !containsZ(&zh1)) bzh1 = false;

        Vec3* closest = NULL;

        if (bxh0 && (closest == NULL || a.distanceToSqr(xh0) < a.distanceToSqr(*closest))) closest = &xh0;
        if (bxh1 && (closest == NULL || a.distanceToSqr(xh1) < a.distanceToSqr(*closest))) closest = &xh1;
        if (byh0 && (closest == NULL || a.distanceToSqr(yh0) < a.distanceToSqr(*closest))) closest = &yh0;
        if (byh1 && (closest == NULL || a.distanceToSqr(yh1) < a.distanceToSqr(*closest))) closest = &yh1;
        if (bzh0 && (closest == NULL || a.distanceToSqr(zh0) < a.distanceToSqr(*closest))) closest = &zh0;
        if (bzh1 && (closest == NULL || a.distanceToSqr(zh1) < a.distanceToSqr(*closest))) closest = &zh1;

        if (closest == NULL)
            return HitResult();

        int face = -1;
        if (closest == &xh0) face = 4;
        if (closest == &xh1) face = 5;
        if (closest == &yh0) face = 0;
        if (closest == &yh1) face = 1;
        if (closest == &zh0) face = 2;
        if (closest == &zh1) face = 3;

        return HitResult(0, 0, 0, face, *closest);
    }

    void set(const AABB& b) {
        this->x0 = b.x0; this->y0 = b.y0; this->z0 = b.z0;
        this->x1 = b.x1; this->y1 = b.y1; this->z1 = b.z1;
    }

    std::string toString() const {
        std::stringstream ss;
        ss << "AABB(" << x0 << "," << y0 << "," << z0 << " to " << x1 << "," << y1 << "," << z1 << ")";
        return ss.str();
    }

    double x0, y0, z0;
    double x1, y1, z1;

private:
    bool containsX(Vec3* v) {
        return v->y >= y0 && v->y <= y1 && v->z >= z0 && v->z <= z1;
    }
    bool containsY(Vec3* v) {
        return v->x >= x0 && v->x <= x1 && v->z >= z0 && v->z <= z1;
    }
    bool containsZ(Vec3* v) {
        return v->x >= x0 && v->x <= x1 && v->y >= y0 && v->y <= y1;
    }
};

#endif
