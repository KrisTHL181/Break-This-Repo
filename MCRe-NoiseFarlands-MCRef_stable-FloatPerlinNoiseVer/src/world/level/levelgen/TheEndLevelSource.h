#ifndef NET_MINECRAFT_WORLD_LEVEL_LEVELGEN__TheEndLevelSource_H__
#define NET_MINECRAFT_WORLD_LEVEL_LEVELGEN__TheEndLevelSource_H__

#include "../chunk/ChunkSource.h"
#include "synth/PerlinNoise.h"
#include "synth/SimplexNoise.h"  // ← 新增
#include <map>  // 文件顶部

class Level;
class LevelChunk;

class TheEndLevelSource : public ChunkSource {
public:
    TheEndLevelSource(Level* level, long seed);
    ~TheEndLevelSource();
bool isEndSource() const override { return true; }

    double getWorldOffsetX() const { return m_worldOffsetX; }
    double getWorldOffsetY() const { return m_worldOffsetY; }
    double getWorldOffsetZ() const { return m_worldOffsetZ; }
    double getWorldScaleX()  const { return m_worldScaleX; }
    double getWorldScaleY()  const { return m_worldScaleY; }
    double getWorldScaleZ()  const { return m_worldScaleZ; }

    PerlinNoise& getPNoise1() { return pNoise1; }
    PerlinNoise& getPNoise2() { return pNoise2; }
    PerlinNoise& getPNoise3() { return pNoise3; }
    SimplexNoise& getSNoise1() { return sNoise1; }

    double sampleDensityAt(double worldX, double worldY, double worldZ);
    double getIslandHeightValue(int64_t chunkX, int64_t chunkZ, int xC, int zC);
    std::map<int64_t, LevelChunk*> chunkMap;
    // 🛡️ 调试面板用 — 包装私有 getIslandHeightValue
double debugIslandHeightValue(int64_t chunkX, int64_t chunkZ) {
    return getIslandHeightValue(chunkX, chunkZ, 1, 1);
}

    virtual bool hasChunk(int64_t x, int64_t z) override;
    virtual LevelChunk* create(int64_t x, int64_t z) override;
    virtual LevelChunk* getChunk(int64_t xOffs, int64_t zOffs) override;
    virtual void postProcess(ChunkSource* parent, int64_t xt, int64_t zt) override;
    virtual bool tick() override;
    virtual Biome::MobList getMobsAt(const MobCategory& mobCategory, int x, int y, int z) override;
    virtual bool shouldSave() override;
    virtual std::string gatherStats() override;

private:
    void prepareHeights(int64_t chunkX, int64_t chunkZ, unsigned char* blocks);
    void generateDensityCells(int64_t chunkX, int64_t chunkZ, double* density);

    double m_worldOffsetX = 0.0;
    double m_worldOffsetY = 0.0;
    double m_worldOffsetZ = 0.0;
    double m_worldScaleX  = 1.0;
    double m_worldScaleY  = 1.0;
    double m_worldScaleZ  = 1.0;

    bool m_spikesGenerated = false;
    bool m_endCircles = false;
void generateEndSpikes();

    Level* level;
    Random random;
    PerlinNoise pNoise1;   // 16 octaves, 3D — 地形低频
    PerlinNoise pNoise2;   // 16 octaves, 3D — 地形高频
    PerlinNoise pNoise3;   // 8 octaves, 3D — 选择器
    SimplexNoise sNoise1;  // ✅ 真正的 SimplexNoise
    double* densityBuffer;
    static const int DENSITY_X = 3;
    static const int DENSITY_Y = 33;  // 128/8 + 1
    static const int DENSITY_Z = 3;
};

#endif
