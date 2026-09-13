#ifndef NET_MINECRAFT_WORLD_LEVEL__LevelSource_H__
#define NET_MINECRAFT_WORLD_LEVEL__LevelSource_H__

#include <cstdint>

class Material;
class Biome;

class LevelSource
{
public:
    virtual ~LevelSource() {}

    virtual int getTile(int64_t x, int y, int64_t z) = 0;
    virtual bool isEmptyTile(int64_t x, int y, int64_t z) = 0;

    virtual float getBrightness(int64_t x, int y, int64_t z) = 0;

    virtual int getData(int64_t x, int y, int64_t z) = 0;

    virtual const Material* getMaterial(int64_t x, int y, int64_t z) = 0;

    virtual bool isSolidRenderTile(int64_t x, int y, int64_t z) = 0;
    virtual bool isSolidBlockingTile(int64_t x, int y, int64_t z) = 0;

    virtual Biome* getBiome(int64_t x, int64_t z) = 0;
};

#endif
