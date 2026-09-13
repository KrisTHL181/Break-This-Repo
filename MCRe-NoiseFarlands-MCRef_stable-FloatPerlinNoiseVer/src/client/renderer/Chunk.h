#ifndef NET_MINECRAFT_CLIENT_RENDERER__Chunk_H__
#define NET_MINECRAFT_CLIENT_RENDERER__Chunk_H__

#include "RenderChunk.h"
#include "../../world/phys/AABB.h"
#include <cstdint>

class Level;
class Entity;
class Culler;
class Tesselator;

class Chunk
{
    static const int NumLayers = 3;
public:
    Chunk(Level* level_, int64_t x, int64_t y, int64_t z, int size, int lists_, GLuint* ptrBuf = NULL);

    void setPos(int64_t x, int64_t y, int64_t z);

    void rebuild();
    void setDirty();
    void setClean();
    bool isDirty();
    void reset();

    double distanceToSqr(const Entity* player) const;
    double distanceToSqr(double px, double py, double pz) const;
    float squishedDistanceToSqr(const Entity* player) const;

    int getAllLists(int displayLists[], int p, int layer);
    int getList(int layer);

    RenderChunk& getRenderChunk(int layer);

    bool isEmpty();
    void cull(Culler* culler);
    void renderBB();

    static void resetUpdates();

private:
    void translateToPos();

public:
    Level* level;

    static int updates;

    int64_t x, y, z;          // 世界方块坐标（改为 int64_t）
    int xs, ys, zs;           // 尺寸（int 足够）
    bool empty[NumLayers];
    int64_t xm, ym, zm;       // 中心点坐标（int64_t）
    float radius;
    AABB bb;

    int id;
    bool visible;
    bool occlusion_visible;
    bool occlusion_querying;
    int occlusion_id;
    bool skyLit;

    RenderChunk renderChunk[NumLayers];

private:
    Tesselator& t;
    int lists;
    GLuint* vboBuffers;
    bool compiled;
    bool dirty;
    bool _empty;
};

#endif
