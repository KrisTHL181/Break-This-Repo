#ifndef NET_MINECRAFT_WORLD_LEVEL_CHUNK__LevelChunk_H__
#define NET_MINECRAFT_WORLD_LEVEL_CHUNK__LevelChunk_H__

#include <algorithm>
#include <vector>
#include <map>
#include <cstdint>
#include "DataLayer.h"
#include "../LevelConstants.h"
#include "../../../util/Random.h"
#include "../TilePos.h"

class Level;
class Entity;
class LightLayer;
class AABB;
class TileEntity;
typedef std::vector<Entity*> EntityList;

class LevelChunk
{
public:
    typedef std::map<TilePos, TileEntity*> TEMap;
    typedef TEMap::const_iterator TEMapCIterator;

    LevelChunk(Level* level, int64_t x, int64_t z);
    LevelChunk(Level* level, unsigned char* blocks, int64_t x, int64_t z);
    virtual ~LevelChunk();

    void init();
    void clearUpdateMap();
    void deleteBlockData();

    virtual bool isAt(int64_t x, int64_t z);
    virtual int getHeightmap(int x, int z);   // 局部坐标 (0-15)

    virtual void recalcHeightmap();
    virtual void recalcHeightmapOnly();

    unsigned char* getBlockData() { return blocks; }

    virtual int getBrightness(const LightLayer& layer, int x, int y, int z);
    virtual void setBrightness(const LightLayer& layer, int x, int y, int z, int brightness);
    virtual int getRawBrightness(int x, int y, int z, int skyDampen);

    virtual void addEntity(Entity* e);
    virtual void removeEntity(Entity* e);
    virtual void removeEntity(Entity* e, int yc);

    virtual void getEntitiesOfClass(int type, const AABB& bb, EntityList& list);
    virtual void getEntitiesOfType(int entityType, const AABB& bb, EntityList& list);

    TileEntity* getTileEntity(int x, int y, int z);
    bool hasTileEntityAt(int x, int y, int z);
    bool hasTileEntityAt(TileEntity* te);
    void addTileEntity(TileEntity* te);
    void setTileEntity(int x, int y, int z, TileEntity* tileEntity);
    void removeTileEntity(int x, int y, int z);

    virtual bool isSkyLit(int x, int y, int z);
    virtual void lightLava() {}
    virtual void recalcBlockLights();
    virtual void skyBrightnessChanged();

    virtual void load();
    virtual void unload();

    virtual bool shouldSave(bool force);
    virtual void markUnsaved();

    virtual int countEntities();
    virtual void getEntities(Entity* except, const AABB& bb, std::vector<Entity*>& es);

    virtual int getTile(int x, int y, int z);
    virtual bool setTile(int x, int y, int z, int tile_);
    void setTileRaw(int x, int y, int z, int tile);
    virtual bool setTileAndData(int x, int y, int z, int tile_, int data_);

    virtual int getData(int x, int y, int z);
    virtual void setData(int x, int y, int z, int val);

    virtual void setBlocks(unsigned char* newBlocks, int sub);

    virtual int getBlocksAndData(unsigned char* data, int x0, int y0, int z0, int x1, int y1, int z1, int p);
    virtual int setBlocksAndData(unsigned char* data, int x0, int y0, int z0, int x1, int y1, int z1, int p);

    virtual Random getRandom(long l);

    virtual bool isEmpty();
    const TEMap& getTileEntityMap() const;

private:
    void lightGap(int x, int z, int source);
    void lightGaps(int x, int z);
    void recalcHeight(int x, int yStart, int z);

public:
    static bool touchedSky;
    static const int ChunkBlockCount = CHUNK_BLOCK_COUNT;
    static const int ChunkSize = ChunkBlockCount;
    static const int UpdateMapBitShift = 4;

    int blocksLength;
    bool loaded;
    Level* level;
    DataLayer data;
    DataLayer skyLight;
    DataLayer blockLight;

    char heightmap[CHUNK_COLUMNS];
    unsigned char updateMap[CHUNK_COLUMNS];
    int minHeight;

    const int64_t x, z;
    const int64_t xt, zt;

    bool terrainPopulated;
    bool unsaved;
    bool dontSave;
    bool createdFromSave;
    bool lastSaveHadEntities;
    long lastSaveTime;

protected:
    unsigned char* blocks;
    static const int EntityBlocksArraySize = 128/16;
    std::vector<Entity*> entityBlocks[EntityBlocksArraySize];
    TEMap tileEntities;
};

#endif
