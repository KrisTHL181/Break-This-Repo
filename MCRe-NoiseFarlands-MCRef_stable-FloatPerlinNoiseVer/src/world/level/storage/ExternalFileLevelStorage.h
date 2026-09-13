#if !defined(DEMO_MODE) && !defined(APPLE_DEMO_PROMOTION)

#ifndef NET_MINECRAFT_WORLD_LEVEL_STORAGE__ExternalFileLevelStorage_H__
#define NET_MINECRAFT_WORLD_LEVEL_STORAGE__ExternalFileLevelStorage_H__

#include <vector>
#include <list>
#include "LevelStorage.h"
#include "../chunk/storage/ChunkStorage.h"

class Player;
class Dimension;
class RegionFile;

typedef struct UnsavedLevelChunk
{
    int             pos;
    RakNet::TimeMS  addedToList;
    LevelChunk*     chunk;
} UnsavedLevelChunk;

typedef std::list<UnsavedLevelChunk> UnsavedChunkList;

class ExternalFileLevelStorage : public LevelStorage, public ChunkStorage
{
public:
    ExternalFileLevelStorage(const std::string& levelId, const std::string& fullPath);
    virtual ~ExternalFileLevelStorage();

    LevelData* prepareLevel(Level* level) override;
    void checkSession() {}   // 不是虚函数，不能加 override

    ChunkStorage* createChunkStorage(Dimension* dimension) override { return this; }

    void saveLevelData(LevelData& levelData, std::vector<Player*>* players) override;
    void closeAll() override {}

    static bool readLevelData(const std::string& directory, LevelData& dest);
    static bool readPlayerData(const std::string& filename, LevelData& dest);
    static bool writeLevelData(const std::string& datFileName, LevelData& dest, const std::vector<Player*>* players);
    static void saveLevelData(const std::string& directory, LevelData& levelData, std::vector<Player*>* players);

    int savePendingUnsavedChunks(int maxCount);

    // ChunkStorage 方法（参数改为 int64_t）
    virtual LevelChunk* load(Level* level, int64_t x, int64_t z) override;
    void save(Level* level, LevelChunk* levelChunk) override;
    void loadEntities(Level* level, LevelChunk* levelChunk) override;
    void saveEntities(Level* level, LevelChunk* levelChunk) override;
    void saveGame(Level* level) override;
    void saveAll(Level* level, std::vector<LevelChunk*>& levelChunks) override;

    virtual void tick() override;
    virtual void flush() override {}

private:
    std::map<std::pair<int64_t, int64_t>, RegionFile*> m_regionCache;
    RegionFile* getRegionFile(int64_t chunkX, int64_t chunkZ);
    std::string levelId;
    std::string levelPath;
    LevelData* loadedLevelData;
    RegionFile* regionFile;
    RegionFile* entitiesFile;

    Level* level;
    int tickCount;
    int loadedStorageVersion;
    UnsavedChunkList unsavedChunkList;
    int lastSavedEntitiesTick;
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_STORAGE__ExternalFileLevelStorage_H__*/

#endif /*DEMO_MODE*/
