#if !defined(DEMO_MODE) && !defined(APPLE_DEMO_PROMOTION)

#include "LevelData.h"
#include "RegionFile.h"
#include "ExternalFileLevelStorage.h"
#include "FolderMethods.h"
#include "../chunk/LevelChunk.h"
#include "../Level.h"
#include "../LevelConstants.h"
#include "platform/log.h"
#include "../tile/TreeTile.h"
#include "../../entity/EntityFactory.h"
#include "../../../nbt/NbtIo.h"
#include "../../../util/RakDataIO.h"
#include "../../../raknet/GetTime.h"
#include "../tile/entity/TileEntity.h"
#include "../chunk/ChunkCache.h"
#include "util/DebugLog.h"

static const int ChunkVersion_Light = 1;
static const int ChunkVersion_Entity = 2;

const char* const fnLevelDatOld = "level.dat_old";
const char* const fnLevelDatNew = "level.dat_new";
const char* const fnLevelDat    = "level.dat";
const char* const fnPlayerDat   = "player.dat";

class LevelConverters
{
public:
    static bool v1_ClothIdToClothData(LevelChunk* c) {
        bool changed = false;
        unsigned char* blocks = c->getBlockData();
        unsigned char newTile = Tile::cloth->id;
        for (int i = 0; i < 16*16*128; ++i) {
            unsigned char oldTile = blocks[i];
            if (oldTile >= 101 && oldTile <= 115) {
                int color = 0xf - (oldTile - 101);
                blocks[i] = newTile;
                c->data.set(i, color);
                changed = true;
            }
        }
        return changed;
    }

    static bool ReplaceUnavailableBlocks(LevelChunk* c) {
        bool changed = false;
        unsigned char* blocks = c->getBlockData();
        for (int i = 0; i < 16*16*128; ++i) {
            unsigned char oldTile = blocks[i];
            unsigned char newTile = Tile::transformToValidBlockId(oldTile);
            if (oldTile != newTile) {
                blocks[i] = newTile;
                changed = true;
            }
        }
        return changed;
    }
};

ExternalFileLevelStorage::ExternalFileLevelStorage(const std::string& levelId, const std::string& fullPath)
:   levelId(levelId),
    levelPath(fullPath),
    loadedLevelData(NULL),
    regionFile(NULL),
    entitiesFile(NULL),
    tickCount(0),
    lastSavedEntitiesTick(-999999),
    level(NULL),
    loadedStorageVersion(SharedConstants::StorageVersion)
{
if (!DebugLog::instance().isInitialized()) {
    DebugLog::instance().initialize(levelPath);
    DebugLog::instance().setMinLevel(DebugLog::LEVEL_INFO);
}
    createFolderIfNotExists(levelPath.c_str());

    std::string datFileName   = levelPath + "/" + fnLevelDat;
    std::string levelFileName = levelPath + "/" + fnPlayerDat;
    loadedLevelData = new LevelData();
    if (readLevelData(levelPath, *loadedLevelData))
    {
        loadedStorageVersion = loadedLevelData->getStorageVersion();
        // 禁用旧 player.dat 读取
        // readPlayerData(levelFileName, *loadedLevelData);
    } else {
        delete loadedLevelData;
        loadedLevelData = NULL;
    }
}

ExternalFileLevelStorage::~ExternalFileLevelStorage() {
    for (auto& pair : m_regionCache) {
        delete pair.second;
    }
    m_regionCache.clear();
    delete loadedLevelData;
}
RegionFile* ExternalFileLevelStorage::getRegionFile(int64_t chunkX, int64_t chunkZ) {
    // 计算区域坐标（每个区域 32×32 区块）
    int64_t regionX = (chunkX >= 0) ? (chunkX / 32) : ((chunkX - 31) / 32);
    int64_t regionZ = (chunkZ >= 0) ? (chunkZ / 32) : ((chunkZ - 31) / 32);

    auto key = std::make_pair(regionX, regionZ);
    auto it = m_regionCache.find(key);
    if (it != m_regionCache.end()) {
        return it->second;
    }

    // 确保目录存在
    std::string regionDir = levelPath + "/region";
    createFolderIfNotExists(regionDir.c_str());

    char fileName[512];
    snprintf(fileName, sizeof(fileName), "%s/r.%lld.%lld.dat", regionDir.c_str(),
             (long long)regionX, (long long)regionZ);
    RegionFile* rf = new RegionFile(fileName);
    if (!rf->open()) {
        delete rf;
        return nullptr;
    }
    m_regionCache[key] = rf;
    return rf;
}

void ExternalFileLevelStorage::saveLevelData(LevelData& levelData, std::vector<Player*>* players) {
    ExternalFileLevelStorage::saveLevelData(levelPath, levelData, players);
}

void ExternalFileLevelStorage::saveLevelData(const std::string& levelPath, LevelData& levelData, std::vector<Player*>* players)
{
    std::string directory = levelPath + "/";
    std::string tmpFile = directory + fnLevelDatNew;
    std::string datFile = directory + fnLevelDat;
    std::string oldFile = directory + fnLevelDatOld;

    levelData.setStorageVersion(SharedConstants::StorageVersion);
    if (!writeLevelData(tmpFile, levelData, players))
        return;

    remove(oldFile.c_str());
    if (exists(datFile.c_str())) {
        if (rename(datFile.c_str(), oldFile.c_str())) {
            LOGE("Error@saveLevelData: Couldn't move savefile to level.dat_old\n");
            return;
        }
        remove(datFile.c_str());
    }
    if (rename(tmpFile.c_str(), datFile.c_str())) {
        LOGE("Error@saveLevelData: Couldn't move new file to level.dat\n");
        return;
    }
    remove(tmpFile.c_str());
}

LevelData* ExternalFileLevelStorage::prepareLevel(Level* _level)
{
    level = _level;
    return loadedLevelData;
}

bool ExternalFileLevelStorage::readLevelData(const std::string& directory, LevelData& levelData)
{
    std::string datFilename = directory + "/" + fnLevelDat;
    FILE* file = fopen(datFilename.c_str(), "rb");
    if (!file) {
        datFilename = directory + "/" + fnLevelDatOld;
        file = fopen(datFilename.c_str(), "rb");
    }
    if (!file)
        return false;

    int version = 0;
    int size = 0;
    unsigned char* data = NULL;

    do {
        if (fread(&version, sizeof(version), 1, file) != 1) break;
        if (fread(&size, sizeof(size), 1, file) != 1) break;

        int left = getRemainingFileSize(file);
        if (size > left || size <= 0) break;

        data = new unsigned char[size];
        if (fread(data, 1, size, file) != size) break;

        if (version == 1) {
            RakNet::BitStream bitStream(data, size, false);
            levelData.v1_read(bitStream, version);
        } else if (version >= 2) {
            RakNet::BitStream bitStream(data, size, false);
            RakDataInput stream(bitStream);
            CompoundTag* tag = NbtIo::read(&stream);
            if (tag) {
                levelData.getTagData(tag);
                tag->deleteChildren();
                delete tag;
            }
        }
    } while (false);

    fclose(file);
    delete [] data;
    return true;
}

bool ExternalFileLevelStorage::writeLevelData(const std::string& datFileName, LevelData& levelData, const std::vector<Player*>* players)
{
    LOGI("Writing down level seed as: %ld\n", levelData.getSeed());
    FILE* file = fopen(datFileName.c_str(), "wb");
    if (!file)
        return false;

    RakNet::BitStream data;
    if (levelData.getStorageVersion() == 1)
        levelData.v1_write(data);
    else {
        RakDataOutput buf(data);
        CompoundTag* tag = (players && !players->empty()) ? levelData.createTag(*players) : levelData.createTag();
        NbtIo::write(tag, &buf);
        tag->deleteChildren();
        delete tag;
    }

    int version = levelData.getStorageVersion();
    fwrite(&version, sizeof(version), 1, file);
    int size = data.GetNumberOfBytesUsed();
    fwrite(&size, sizeof(size), 1, file);
    fwrite(data.GetData(), 1, size, file);
    fclose(file);
    return true;
}

bool ExternalFileLevelStorage::readPlayerData(const std::string& filename, LevelData& dest)
{
    return false;
}

void ExternalFileLevelStorage::tick() {
    tickCount++;
    
    // 每 1000 tick（约 50 秒）执行一次区块保存检查
    if ((tickCount % 1000) == 0 && level) {
        LOGI("Saving level...\n");
        
        // 从 ChunkCache 获取当前已加载的区块列表
        ChunkCache* cache = dynamic_cast<ChunkCache*>(level->getChunkSource());
        if (cache) {
            std::vector<LevelChunk*> loaded;
            cache->getLoadedChunks(loaded);
            
            int added = 0;
            for (LevelChunk* chunk : loaded) {
                if (!chunk) continue;
                if (chunk->unsaved) {
                    // 构造唯一标识（使用区块坐标）
                    int64_t pos = (chunk->x << 16) ^ chunk->z;
                    
                    // 检查是否已在待保存列表中
                    UnsavedChunkList::iterator prev = unsavedChunkList.begin();
                    bool found = false;
                    for ( ; prev != unsavedChunkList.end(); ++prev) {
                        if ((*prev).pos == pos) {
                            (*prev).addedToList = RakNet::GetTimeMS();
                            found = true;
                            break;
                        }
                    }
                    
                    if (!found) {
                        UnsavedLevelChunk unsaved;
                        unsaved.pos = (int)pos;
                        unsaved.addedToList = RakNet::GetTimeMS();
                        unsaved.chunk = chunk;
                        unsavedChunkList.push_back(unsaved);
                        added++;
                    }
                    // 注意：这里不将 unsaved 置 false，因为我们只是在排队
                }
            }
            LOGI("Added %d chunks to save queue. Total pending: %d\n", added, (int)unsavedChunkList.size());
        }
        
        // 每次 tick 保存最多 4 个最旧的待保存区块
        int saved = savePendingUnsavedChunks(4);
        LOGI("Saved %d chunks.\n", saved);
    }
    
    // 每 60 秒保存一次实体数据
    if (tickCount - lastSavedEntitiesTick > (60 * SharedConstants::TicksPerSecond)) {
        saveEntities(level, NULL);
        lastSavedEntitiesTick = tickCount;
    }
}

void ExternalFileLevelStorage::save(Level* level, LevelChunk* levelChunk) {
    int64_t x = levelChunk->x;
    int64_t z = levelChunk->z;

    RegionFile* rf = getRegionFile(x, z);
    if (!rf) return;

    int64_t regionX = (x >= 0) ? (x / 32) : ((x - 31) / 32);
    int64_t regionZ = (z >= 0) ? (z / 32) : ((z - 31) / 32);
    int localX = (int)(x - regionX * 32);
    int localZ = (int)(z - regionZ * 32);

    RakNet::BitStream chunkData;
    chunkData.Write((const char*)levelChunk->getBlockData(), CHUNK_BLOCK_COUNT);
    chunkData.Write((const char*)levelChunk->data.data, CHUNK_BLOCK_COUNT / 2);
    chunkData.Write((const char*)levelChunk->skyLight.data, CHUNK_BLOCK_COUNT / 2);
    chunkData.Write((const char*)levelChunk->blockLight.data, CHUNK_BLOCK_COUNT / 2);
    chunkData.Write((const char*)levelChunk->updateMap, CHUNK_COLUMNS);

    rf->writeChunk(localX, localZ, chunkData);
}

LevelChunk* ExternalFileLevelStorage::load(Level* level, int64_t x, int64_t z) {
    RegionFile* rf = getRegionFile(x, z);
    if (!rf) return nullptr;

    // 计算区域内的局部坐标 (0~31)
    int64_t regionX = (x >= 0) ? (x / 32) : ((x - 31) / 32);
    int64_t regionZ = (z >= 0) ? (z / 32) : ((z - 31) / 32);
    int localX = (int)(x - regionX * 32);
    int localZ = (int)(z - regionZ * 32);

    RakNet::BitStream* chunkData = nullptr;
    if (!rf->readChunk(localX, localZ, &chunkData)) {
        return nullptr;
    }

    chunkData->ResetReadPointer();
    unsigned char* blockIds = new unsigned char[CHUNK_BLOCK_COUNT];
    chunkData->Read((char*)blockIds, CHUNK_BLOCK_COUNT);

    LevelChunk* levelChunk = new LevelChunk(level, blockIds, x, z);
    chunkData->Read((char*)levelChunk->data.data, CHUNK_BLOCK_COUNT / 2);

    if (loadedStorageVersion >= ChunkVersion_Light) {
        chunkData->Read((char*)levelChunk->skyLight.data, CHUNK_BLOCK_COUNT / 2);
        chunkData->Read((char*)levelChunk->blockLight.data, CHUNK_BLOCK_COUNT / 2);
    }
    chunkData->Read((char*)levelChunk->updateMap, CHUNK_COLUMNS);

    delete[] chunkData->GetData();
    delete chunkData;

    // 版本转换（保留原有逻辑）
    bool changed = false;
    if (loadedStorageVersion == 1)
        changed |= LevelConverters::v1_ClothIdToClothData(levelChunk);
    changed |= LevelConverters::ReplaceUnavailableBlocks(levelChunk);

    levelChunk->recalcHeightmap();
    levelChunk->unsaved = changed;
    levelChunk->terrainPopulated = true;
    levelChunk->createdFromSave = true;
    return levelChunk;
}

void ExternalFileLevelStorage::saveEntities( Level* level, LevelChunk* levelChunk )
{
    lastSavedEntitiesTick = tickCount;
    int count = 0;
    float st = getTimeS();

    EntityList& entities = level->entities;
    ListTag* entityTags = new ListTag();
    for (unsigned int i = 0; i < entities.size(); ++i) {
        Entity* e = entities[i];
        CompoundTag* tag = new CompoundTag();
        if (e->save(tag)) {
            count++;
            entityTags->add(tag);
        } else
            delete tag;
    }

    TileEntityList& tileEntities = level->tileEntities;
    ListTag* tileEntityTags = new ListTag();
    for (unsigned int i = 0; i < tileEntities.size(); ++i) {
        TileEntity* e = tileEntities[i];
        if (!e->shouldSave()) continue;
        CompoundTag* tag = new CompoundTag();
        if (e->save(tag)) {
            count++;
            tileEntityTags->add(tag);
        } else
            delete tag;
    }

    CompoundTag base;
    base.put("Entities", entityTags);
    base.put("TileEntities", tileEntityTags);

    RakNet::BitStream stream;
    RakDataOutput dos(stream);
    NbtIo::write(&base, &dos);
    int numBytes = stream.GetNumberOfBytesUsed();

    FILE* fp = fopen((levelPath + "/entities.dat").c_str(), "wb");
    if (fp) {
        int version = 1;
        fwrite("ENT\0", 1, 4, fp);
        fwrite(&version, sizeof(int), 1, fp);
        fwrite(&numBytes, sizeof(int), 1, fp);
        fwrite(stream.GetData(), 1, numBytes, fp);
        fclose(fp);
    }

    base.deleteChildren();

    float tt = getTimeS() - st;
    LOGI("Time to save %d entities: %f s. Size: %d bytes\n", count, tt, numBytes);
}

void ExternalFileLevelStorage::loadEntities(Level* level, LevelChunk* chunk) {
    lastSavedEntitiesTick = tickCount;
    FILE* fp = fopen((levelPath + "/entities.dat").c_str(), "rb");
    if (fp) {
        char header[5];
        int version, numBytes;
        fread(header, 1, 4, fp);
        fread(&version, sizeof(int), 1, fp);
        fread(&numBytes, sizeof(int), 1, fp);

        int left = getRemainingFileSize(fp);
        if (numBytes <= left && numBytes > 0) {
            unsigned char* buf = new unsigned char[numBytes];
            fread(buf, 1, numBytes, fp);

            RakNet::BitStream stream(buf, numBytes, false);
            RakDataInput dis(stream);
            CompoundTag* tag = NbtIo::read(&dis);

            if (tag->contains("Entities", Tag::TAG_List)) {
                ListTag* entityTags = tag->getList("Entities");
                for (int i = 0; i < entityTags->size(); ++i) {
                    Tag* _et = entityTags->get(i);
                    if (!_et || _et->getId() != Tag::TAG_Compound) continue;
                    CompoundTag* et = (CompoundTag*)_et;
                    if (Entity* e = EntityFactory::loadEntity(et, level)) {
                        level->addEntity(e);
                    }
                }
            }
            if (tag->contains("TileEntities", Tag::TAG_List)) {
                ListTag* tileEntityTags = tag->getList("TileEntities");
                for (int i = 0; i < tileEntityTags->size(); ++i) {
                    Tag* _et = tileEntityTags->get(i);
                    if (!_et || _et->getId() != Tag::TAG_Compound) continue;
                    CompoundTag* et = (CompoundTag*)_et;
                    if (TileEntity* e = TileEntity::loadStatic(et)) {
                        LevelChunk* chunk = level->getChunkAt(e->x, e->z);
                        if (chunk && !chunk->hasTileEntityAt(e)) {
                            chunk->addTileEntity(e);
                        } else {
                            delete e;
                        }
                    }
                }
            }

            tag->deleteChildren();
            delete tag;
            delete[] buf;
        }
        fclose(fp);
    }
}

void ExternalFileLevelStorage::saveGame(Level* level) {
    saveEntities(level, NULL);
}

int ExternalFileLevelStorage::savePendingUnsavedChunks(int maxCount) {
    if (maxCount < 0) {
        maxCount = (int)unsavedChunkList.size();
    }
    int count = 0;
    int safety = maxCount * 3 + 10; // 防止意外死循环
    
    while (count < maxCount && !unsavedChunkList.empty() && --safety > 0) {
        // 找到加入时间最早的待保存区块
        UnsavedChunkList::iterator it = unsavedChunkList.begin();
        UnsavedChunkList::iterator removeIt = it;
        UnsavedLevelChunk* oldest = &(*it);
        
        for ( ; it != unsavedChunkList.end(); ++it) {
            if ((*it).addedToList < oldest->addedToList) {
                oldest = &(*it);
                removeIt = it;
            }
        }
        
        LevelChunk* chunk = oldest->chunk;
        unsavedChunkList.erase(removeIt);
        
        if (chunk) {
            save(level, chunk);
            chunk->unsaved = false;
            count++;
        } else {
            LOGW("Null chunk in unsaved list!\n");
        }
    }
    
    if (safety <= 0) {
        LOGE("savePendingUnsavedChunks safety triggered! Clearing list.\n");
        unsavedChunkList.clear();
    }
    return count;
}
void ExternalFileLevelStorage::saveAll( Level* level, std::vector<LevelChunk*>& levelChunks ) {
    ChunkStorage::saveAll(level, levelChunks);
    int numChunks = savePendingUnsavedChunks(-1);
    LOGI("Saving %d additional chunks.\n", numChunks);
}

#endif /*DEMO_MODE*/
