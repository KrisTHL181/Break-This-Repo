#ifndef NET_MINECRAFT_WORLD_LEVEL__Region_H__
#define NET_MINECRAFT_WORLD_LEVEL__Region_H__

#include "LevelSource.h"
#include <cstdint>

class Level;
class Material;
class LevelChunk;

class Region: public LevelSource
{
public:
    Region(Level* level, int64_t x1, int y1, int64_t z1, int64_t x2, int y2, int64_t z2);
    ~Region();

    // 所有坐标参数保持 int64_t
    virtual int getTile(int64_t x, int y, int64_t z) override;
    virtual bool isEmptyTile(int64_t x, int y, int64_t z) override;
    virtual float getBrightness(int64_t x, int y, int64_t z) override;
    virtual int getData(int64_t x, int y, int64_t z) override;
    virtual const Material* getMaterial(int64_t x, int y, int64_t z) override;
    virtual bool isSolidRenderTile(int64_t x, int y, int64_t z) override;
    virtual bool isSolidBlockingTile(int64_t x, int y, int64_t z) override;
    virtual Biome* getBiome(int64_t x, int64_t z) override;

    int getRawBrightness(int64_t x, int y, int64_t z);
    int getRawBrightness(int64_t x, int y, int64_t z, bool propagate);

private:
    int64_t xc1, zc1;          // 起始区块坐标（单位：区块）
    LevelChunk*** chunks;      // 二维数组 [size_x][size_z]
    Level* level;
    int64_t size_x;                // 区块范围大小（足够用 int，因为区域通常几个区块）
    int64_t size_z;
};

#endif
