#ifndef NET_MINECRAFT_WORLD_LEVEL_CHUNK_STORAGE__ChunkStorage_H__
#define NET_MINECRAFT_WORLD_LEVEL_CHUNK_STORAGE__ChunkStorage_H__

#include <cstdint>
#include <vector>

class Level;
class LevelChunk;

class ChunkStorage {
public:
    virtual ~ChunkStorage() {}
    virtual LevelChunk* load(Level* level, int64_t x, int64_t z) {
        return NULL;
    }
    virtual void save(Level* level, LevelChunk* levelChunk) {}
    virtual void saveEntities(Level* level, LevelChunk* levelChunk) {}
    virtual void saveAll(Level* level, std::vector<LevelChunk*>& levelChunks) {
        for (unsigned int i = 0; i < levelChunks.size(); ++i) save(level, levelChunks[i]);
    }
    virtual void tick() {}
    virtual void flush() {}
};

#endif
