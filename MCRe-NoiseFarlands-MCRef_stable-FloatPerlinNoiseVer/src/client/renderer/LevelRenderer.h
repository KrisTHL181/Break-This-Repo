#ifndef NET_MINECRAFT_CLIENT_RENDERER__LevelRenderer_H__
#define NET_MINECRAFT_CLIENT_RENDERER__LevelRenderer_H__

#include "../../world/level/LevelListener.h"
#include "../renderer/Textures.h"   // 提供 TextureId 定义
#include "../../world/phys/Vec3.h"
#include "RenderList.h"
#include "../renderer/RenderChunk.h"
#include "gles.h"
#include <vector>

class Minecraft;
class Textures;
class Culler;
class Chunk;
class TileRenderer;
class Level;
class Mob;
class Player;
class HitResult;
class AABB;
class TripodCamera;

class LevelRenderer: public LevelListener
{
public:
    static const int CHUNK_SIZE;
    static const int MAX_VISIBLE_REBUILDS_PER_FRAME = 3;
    static const int MAX_INVISIBLE_REBUILDS_PER_FRAME = 1;
    virtual void entityRemoved(Entity* entity);
    void renderChunkVBO(const RenderChunk& rc);
    
    // 改为 double 以支持远距离精度
    double xOld;
    double yOld;
    double zOld;
    float destroyProgress;

    LevelRenderer(Minecraft* mc);
    ~LevelRenderer();

    void setLevel(Level* level);
    void allChanged();

    int  render(Mob* player, int layer, float alpha);
    void renderDebug(const AABB& b, float a) const;

    void renderSky(float alpha);
    void renderClouds(float alpha);
    void renderEntities(Vec3 cam, Culler* culler, float a);
    void renderSameAsLast(int layer, float alpha);
    void renderHit(Player* player, const HitResult& h, int mode, void* inventoryItem, float a);
    void renderHitOutline(Player* player, const HitResult& h, int mode, void* inventoryItem, float a);
    void renderHitSelect(Player* player, const HitResult& h, int mode, void* inventoryItem, float a);
    void entityAdded(Entity* entity);

    void tick();
    bool updateDirtyChunks(Mob* player, bool force);
    void setDirty(int x0, int y0, int z0, int x1, int y1, int z1);
    void tileChanged(int x, int y, int z);
    void setTilesDirty(int x0, int y0, int z0, int x1, int y1, int z1);
    void cull(Culler* culler, float a);
    void skyColorChanged();

    void addParticle(const std::string& name, float x, float y, float z, float xa, float ya, float za, int data);
    void playSound(const std::string& name, float x, float y, float z, float volume, float pitch);
    void takePicture(TripodCamera* cam, Entity* entity);
    void levelEvent(Player* source, int type, int x, int y, int z, int data);
    std::string gatherStats1();
    void render(const AABB& b) const;
    void onGraphicsReset();

// 在类定义中添加
// 新增
void renderSunriseSunset(float a);
void renderSun(float a);
void renderMoon(float a);
// 在 public 区添加
void ensureStarsGenerated();

private:
    void generateStars();            // 生成星星显示列表
    RenderChunk m_starsChunk;
RenderChunk m_skyChunk;
RenderChunk m_skyChunk2;
bool m_starsGenerated;
    TextureId m_moonTexture;
    TextureId m_sunTexture;
    TextureId m_starsTexture;
    void generateSky();
    int  renderChunks(int from, int to, int layer, float alpha);
    // LevelRenderer.h
void resortChunks(int64_t xc, int64_t yc, int64_t zc);  // 原来是 int
    void deleteChunks();
    __inline int getLinearCoord(int x, int y, int z) {
        return (z * yChunks + y) * xChunks + x;
    }
    int noEntityRenderFrames;
    int totalEntities;
// 视锥体剔除缓存
double lastCullCamX, lastCullCamY, lastCullCamZ;
float lastCullYRot, lastCullXRot;
bool cullCacheValid;
int cullSkipTimer;
    int renderedEntities;
    int culledEntities;

    std::vector<Chunk*> _renderChunks;
    int cullStep;
    RenderList renderList;

    int totalChunks, offscreenChunks, occludedChunks, renderedChunks, emptyChunks;
    int chunkFixOffs;
    int64_t xMinChunk, yMinChunk, zMinChunk;
    int64_t xMaxChunk, yMaxChunk, zMaxChunk;

    Level* level;
    std::vector<Chunk*> dirtyChunks;
    Chunk** chunks;
    Chunk** sortedChunks;
    int chunksLength;

public:
    TileRenderer* tileRenderer;

private:
    int xChunks, yChunks, zChunks;
    int chunkLists;
    Minecraft* mc;
    bool occlusionCheck;
    int lastViewDistance;
    int ticks;
    int starList, skyList, darkList;
    int numListsOrBuffers;
    GLuint* chunkBuffers;
    Textures* textures;
};

#endif
