#ifndef NET_MINECRAFT_WORLD_LEVEL_CHUNK__ChunkSource_H__
#define NET_MINECRAFT_WORLD_LEVEL_CHUNK__ChunkSource_H__

#include <string>
#include "../biome/Biome.h"

class ProgressListener;
class LevelChunk;

class ChunkSource
{
public:
    bool isChunkCache;

    ChunkSource() : isChunkCache(false) {}
    virtual ~ChunkSource() {}
virtual bool isEndSource() const { return false; }

    virtual bool hasChunk(int64_t x, int64_t y) = 0;
    virtual LevelChunk* getChunk(int64_t x, int64_t z) = 0;
    virtual LevelChunk* create(int64_t x, int64_t z) = 0;
    virtual void postProcess(ChunkSource* parent, int64_t x, int64_t z) = 0;
    virtual bool tick() = 0;
    virtual bool shouldSave() = 0;
    virtual void saveAll(bool onlyUnsaved) {}
    virtual Biome::MobList getMobsAt(const MobCategory& mobCategory, int x, int y, int z) = 0;
    virtual std::string gatherStats() = 0;
};

#endif
