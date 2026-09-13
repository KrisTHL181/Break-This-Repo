#ifndef NET_MINECRAFT_WORLD_LEVEL_CHUNK__ChunkCache_H__
#define NET_MINECRAFT_WORLD_LEVEL_CHUNK__ChunkCache_H__

#include "ChunkSource.h"
#include "storage/ChunkStorage.h"
#include "EmptyLevelChunk.h"
#include "../Level.h"
#include "../LevelConstants.h"
#include <unordered_map>
#include <set>
// 在 ChunkCache.h 中，将 emptyChunk 改为 std::shared_ptr<LevelChunk>
#include <memory>
// 在 cpp 
struct pair_hash {
    std::size_t operator()(const std::pair<int64_t, int64_t>& p) const {
        return std::hash<int64_t>()(p.first) ^ (std::hash<int64_t>()(p.second) << 1);
    }
};

class ChunkCache: public ChunkSource {
    static const int MAX_SAVES = 2;
public:
    ChunkSource* getSource() const { return source; }

    ChunkCache(Level* level_, ChunkStorage* storage_, ChunkSource* source_)
        : xLast(-999999999), zLast(-999999999), last(NULL),
          level(level_), storage(storage_), source(source_)
    {
        isChunkCache = true;
        emptyChunk = getEmptyChunk(level_);   // 静态单例
        // ... 其余初始化代码不变
    }

    ~ChunkCache(){
    delete source;
    std::set<LevelChunk*> deleted;
    for(auto& pair : chunks){
        LevelChunk* p = pair.second;
        if(p && p != emptyChunk && deleted.find(p) == deleted.end()){
            deleted.insert(p);
            p->deleteBlockData();
            delete p;
        }
    }
}

    bool fits(int64_t x, int64_t z) { return true; }

    bool hasChunk(int64_t x, int64_t z) {
        auto key = std::make_pair(x, z);
        return chunks.find(key) != chunks.end();
    }

    LevelChunk* create(int64_t x, int64_t z) {
        return getChunk(x, z);
    }

    // 完全保留原始生成流程：同步生成、光照、后处理
    LevelChunk* getChunk(int64_t x, int64_t z) {
        if (x == xLast && z == zLast && last != NULL) {
            return last;
        }
        auto key = std::make_pair(x, z);
        auto it = chunks.find(key);
        if (it != chunks.end()) {
            xLast = x;
            zLast = z;
            last = it->second;
            return last;
        }

        LevelChunk* newChunk = load(x, z);
        bool updateLights = false;
        if (newChunk == NULL) {
        if (source == NULL) {
            return emptyChunk;
        } else {
            newChunk = source->getChunk(x, z);
        }
        } else {
            updateLights = true;
        }
        chunks[key] = newChunk;
        newChunk->lightLava();

        if (updateLights) {
            for (int cx = 0; cx < 16; cx++) {
                for (int cz = 0; cz < 16; cz++) {
                    int height = level->getHeightmap((int)(cx + x * 16), (int)(cz + z * 16));
                    for (int cy = height; cy >= 0; cy--) {
                        level->updateLight(LightLayer::Sky, (int)(cx + x * 16), cy, (int)(cz + z * 16), (int)(cx + x * 16), cy, (int)(cz + z * 16));
                        level->updateLight(LightLayer::Block, (int)(cx + x * 16 - 1), cy, (int)(cz + z * 16 - 1), (int)(cx + x * 16 + 1), cy, (int)(cz + z * 16 + 1));
                    }
                }
            }
        }

        if (newChunk != NULL) {
            newChunk->load();
        }

        if (!newChunk->terrainPopulated && hasChunk(x + 1, z + 1) && hasChunk(x, z + 1) && hasChunk(x + 1, z))
            postProcess(this, x, z);
        if (hasChunk(x - 1, z) && !getChunk(x - 1, z)->terrainPopulated && hasChunk(x - 1, z + 1) && hasChunk(x, z + 1) && hasChunk(x - 1, z))
            postProcess(this, x - 1, z);
        if (hasChunk(x, z - 1) && !getChunk(x, z - 1)->terrainPopulated && hasChunk(x + 1, z - 1) && hasChunk(x, z - 1) && hasChunk(x + 1, z))
            postProcess(this, x, z - 1);
        if (hasChunk(x - 1, z - 1) && !getChunk(x - 1, z - 1)->terrainPopulated && hasChunk(x - 1, z - 1) && hasChunk(x, z - 1) && hasChunk(x - 1, z))
            postProcess(this, x - 1, z - 1);

        xLast = x;
        zLast = z;
        last = newChunk;
        return newChunk;
    }

    Biome::MobList getMobsAt(const MobCategory& mobCategory, int x, int y, int z) {
        return source->getMobsAt(mobCategory, x, y, z);
    }

    void postProcess(ChunkSource* parent, int64_t x, int64_t z) {
        static int depth = 0;
        if (depth > 20) return;
        depth++;
        if (!fits(x, z)) {
            depth--;
            return;
        }
        LevelChunk* chunk = getChunk(x, z);
        if (!chunk->terrainPopulated) {
            chunk->terrainPopulated = true;
            if (source != NULL) {
                source->postProcess(parent, x, z);
            }
            chunk->clearUpdateMap();
        }
        depth--;
    }

    // 每帧调用，进行少量预生成以平滑突发卡顿
    bool tick() {
    // 动态预生成
    static float lastFrameTime = 0.016f;  // 前一帧耗时
    int maxPregen = 2;
    if (lastFrameTime < 0.010f) maxPregen = 4;   // 上一帧很快，多干点
    else if (lastFrameTime > 0.030f) maxPregen = 1; // 上一帧卡，少干

    pregenerateNearbyChunks(maxPregen);

    if (storage != NULL) storage->tick();
    bool result = source->tick();

    // 记录本帧耗时（粗略：可结合 PerfTimer）
    // 实际使用时在外部记录帧间隔，这里直接假设可用 getTimeS()
    static float prevTime = getTimeS();
    float now = getTimeS();
    lastFrameTime = now - prevTime;
    prevTime = now;

    return result;
    }

    void getLoadedChunks(std::vector<LevelChunk*>& out) const {
        out.clear();
        for (const auto& pair : chunks) {
            if (pair.second && pair.second != emptyChunk) {
                out.push_back(pair.second);
            }
        }
    }

    bool shouldSave() { return true; }
    std::string gatherStats() { return "ChunkCache: safe pregen"; }

    void saveAll(bool onlyUnsaved) {
        if (storage != NULL) {
            std::vector<LevelChunk*> chunksToSave;
            for (auto& pair : chunks) {
                LevelChunk* chunk = pair.second;
                if (!onlyUnsaved || chunk->shouldSave(false))
                    chunksToSave.push_back(chunk);
            }
            storage->saveAll(level, chunksToSave);
        }
    }

private:
    // 安全预生成：以玩家为中心，按螺旋半径生成未加载区块
    void pregenerateNearbyChunks(int maxPerFrame) {
        int generated = 0;
        for (size_t pi = 0; pi < level->players.size() && generated < maxPerFrame; ++pi) {
            Player* player = level->players[pi];
            int cx = Mth::floor(player->x / 16.0);
            int cz = Mth::floor(player->z / 16.0);
            for (int r = 1; r <= 4 && generated < maxPerFrame; ++r) {   // 最多到半径4
                // 上下两边
                for (int dx = -r; dx <= r && generated < maxPerFrame; ++dx) {
                    int nx = cx + dx, nz = cz + r;
                    if (!hasChunk(nx, nz)) { getChunk(nx, nz); generated++; }
                    nz = cz - r;
                    if (!hasChunk(nx, nz)) { getChunk(nx, nz); generated++; }
                }
                // 左右两边（不含角）
                for (int dz = -r+1; dz <= r-1 && generated < maxPerFrame; ++dz) {
                    int nx = cx + r, nz = cz + dz;
                    if (!hasChunk(nx, nz)) { getChunk(nx, nz); generated++; }
                    nx = cx - r;
                    if (!hasChunk(nx, nz)) { getChunk(nx, nz); generated++; }
                }
            }
        }
    }

    LevelChunk* load(int64_t x, int64_t z) {
        if (storage == NULL) return emptyChunk;
        LevelChunk* levelChunk = storage->load(level, x, z);
        if (levelChunk != NULL) {
            levelChunk->lastSaveTime = level->getTime();
        }
        return levelChunk;
    }

public:
    int64_t xLast;
    int64_t zLast;
private:
    static EmptyLevelChunk* getEmptyChunk(Level* level) {
        static EmptyLevelChunk* s_empty = nullptr;
        if (!s_empty) {
            // 只创建一次，注意参数：EmptyLevelChunk(Level* level, int x, int z)
            s_empty = new EmptyLevelChunk(level, 0, 0);
        }
        return s_empty;
    }

    LevelChunk* emptyChunk;   // 普通指针，指向静态单例
    ChunkSource* source;
    ChunkStorage* storage;
    std::unordered_map<std::pair<int64_t, int64_t>, LevelChunk*, pair_hash> chunks;
    Level* level;
    LevelChunk* last;
};

#endif
