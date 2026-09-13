#ifndef NET_MINECRAFT_CLIENT_RENDERER_CULLING__FrustumData_H__
#define NET_MINECRAFT_CLIENT_RENDERER_CULLING__FrustumData_H__

//package net.minecraft.client.renderer.culling;

#include "../../../world/phys/AABB.h"

// We create an enum of the sides so we don't have to call each side 0 or 1.
// This way it makes it more understandable and readable when dealing with frustum sides.
class FrustumData
{
public:
    //enum FrustumSide
    static const int RIGHT = 0; // The RIGHT side of the frustum
    static const int LEFT = 1; // The LEFT    side of the frustum
    static const int BOTTOM = 2; // The BOTTOM side of the frustum
    static const int TOP = 3; // The TOP side of the frustum
    static const int BACK = 4; // The BACK   side of the frustum
    static const int FRONT = 5; // The FRONT side of the frustum

    // Like above, instead of saying a number for the ABC and D of the plane, we
    // want to be more descriptive.
    static const int A = 0; // The X value of the plane's normal
    static const int B = 1; // The Y value of the plane's normal
    static const int C = 2; // The Z value of the plane's normal
    static const int D = 3; // The distance the plane is from the origin

    double m_Frustum[6][4];   // 从 float 改为 double，并修正尺寸为 6x4
    double proj[16];
    double modl[16];
    double clip[16];

    // 所有方法参数从 float 改为 double

	bool pointInFrustum(double x, double y, double z) const
    {
        for (int i = 0; i < 6; i++)
        {
            if (m_Frustum[i][A] * x + m_Frustum[i][B] * y + m_Frustum[i][C] * z + m_Frustum[i][D] <= 0)
            {
                return false;
            }
        }
    
        return true;
    }
    bool sphereInFrustum(double x, double y, double z, double radius) const
    {
        for (int i = 0; i < 6; i++)
        {
            if (m_Frustum[i][A] * x + m_Frustum[i][B] * y + m_Frustum[i][C] * z + m_Frustum[i][D] <= -radius)
            {
                return false;
            }
        }
    
        return true;
    }
    bool cubeFullyInFrustum(double x1, double y1, double z1, double x2, double y2, double z2) const
    {
        for (int i = 0; i < 6; i++)
        {
            if (!(m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0)) return false;
            if (!(m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0)) return false;
        }
    
        return true;
    }
    
    bool cubeInFrustum(double x1, double y1, double z1, double x2, double y2, double z2) const
    {
        for (int i = 0; i < 6; i++)
        {
            if (m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z1) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y1) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x1) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0) continue;
            if (m_Frustum[i][A] * (x2) + m_Frustum[i][B] * (y2) + m_Frustum[i][C] * (z2) + m_Frustum[i][D] > 0) continue;
    
            return false;
        }
    
        return true;
    }
    bool isVisible(const AABB& aabb) const
    {
        return cubeInFrustum(aabb.x0, aabb.y0, aabb.z0, aabb.x1, aabb.y1, aabb.z1);
    }
};
#endif /*NET_MINECRAFT_CLIENT_RENDERER_CULLING__FrustumData_H__*/
