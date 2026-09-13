#include "LevelChunk.h"
#include "../LightLayer.h"
#include "../tile/Tile.h"
#include "../Level.h"
#include "../dimension/Dimension.h"
#include "../../phys/AABB.h"
#include "../../entity/Entity.h"
#include "../TilePos.h"
#include "../tile/entity/TileEntity.h"
#include "../tile/EntityTile.h"
#include "../LevelConstants.h"

/*static*/ bool LevelChunk::touchedSky = false;

LevelChunk::LevelChunk(Level* level, int64_t x, int64_t z)
    : level(level), x(x), z(z), xt(x * CHUNK_WIDTH), zt(z * CHUNK_DEPTH)
{
    blocks = nullptr;  // ← 🛡️ 显式置空，让崩溃更早暴露
    init();
}

LevelChunk::LevelChunk(Level* level, unsigned char* blocks, int64_t x, int64_t z)
    : level(level), x(x), z(z), xt(x * CHUNK_WIDTH), zt(z * CHUNK_DEPTH),
      blocks(blocks), data(ChunkBlockCount), skyLight(ChunkBlockCount),
      blockLight(ChunkBlockCount), blocksLength(ChunkBlockCount)
{
    init();
}

LevelChunk::~LevelChunk() { }

void LevelChunk::init() {
    terrainPopulated = false;
    dontSave = false;
    unsaved = false;
    lastSaveHadEntities = false;
    createdFromSave = false;
    lastSaveTime = 0;
    memset(heightmap, 0, 256);
    memset(updateMap, 0, 256);
}

bool LevelChunk::setTileAndData(int x, int y, int z, int tile_, int data_) {
    int tile = tile_ & 0xff;
    int oldHeight = heightmap[z << 4 | x];
    int old = blocks[x << 11 | z << 7 | y];
    if (old == tile && data.get(x, y, z) == data_) return false;
    int64_t xOffs = xt + x;
    int64_t zOffs = zt + z;
    blocks[x << 11 | z << 7 | y] = (unsigned char)tile;
    if (old != 0) {
        if (!level->isClientSide) {
            Tile::tiles[old]->onRemove(level, (int)xOffs, y, (int)zOffs);
        } else if (old != tile && Tile::isEntityTile[old]) {
            level->removeTileEntity((int)xOffs, y, (int)zOffs);
        }
    }
    data.set(x, y, z, data_);

    if (!level->dimension->hasCeiling) {
        if (Tile::lightBlock[tile] != 0) {
            if (y >= oldHeight) recalcHeight(x, y + 1, z);
        } else {
            if (y == oldHeight - 1) recalcHeight(x, y, z);
        }
        level->updateLight(LightLayer::Sky, (int)xOffs, y, (int)zOffs, (int)xOffs, y, (int)zOffs);
    }
    level->updateLight(LightLayer::Block, (int)xOffs, y, (int)zOffs, (int)xOffs, y, (int)zOffs);
    lightGaps(x, z);

    if (!level->isClientSide && tile != 0 && Tile::tiles[tile] != nullptr) {
        Tile::tiles[tile]->onPlace(level, (int)xOffs, y, (int)zOffs);
    }

    unsaved = true;
    updateMap[x | (z << 4)] |= (1 << (y >> UpdateMapBitShift));
    return true;
}

bool LevelChunk::setTile(int x, int y, int z, int tile_) {
    int tile = tile_ & 0xff;
    int oldHeight = heightmap[z << 4 | x] & 0xff;
    int old = blocks[x << 11 | z << 7 | y] & 0xff;
    if (old == tile_) return false;
    int64_t xOffs = xt + x;
    int64_t zOffs = zt + z;
    blocks[x << 11 | z << 7 | y] = (unsigned char)tile;
    if (old != 0) {
        Tile::tiles[old]->onRemove(level, (int)xOffs, y, (int)zOffs);
    }
    data.set(x, y, z, 0);

    if (Tile::lightBlock[tile] != 0) {
        if (y >= oldHeight) recalcHeight(x, y + 1, z);
    } else {
        if (y == oldHeight - 1) recalcHeight(x, y, z);
    }

    level->updateLight(LightLayer::Sky, (int)xOffs, y, (int)zOffs, (int)xOffs, y, (int)zOffs);
    level->updateLight(LightLayer::Block, (int)xOffs, y, (int)zOffs, (int)xOffs, y, (int)zOffs);
    lightGaps(x, z);

    if (tile_ != 0 && Tile::tiles[tile_] != nullptr) {
        if (!level->isClientSide) Tile::tiles[tile_]->onPlace(level, (int)xOffs, y, (int)zOffs);
    }
    
    unsaved = true;
    updateMap[x | (z << 4)] |= (1 << (y >> UpdateMapBitShift));
    return true;
}

void LevelChunk::setTileRaw(int x, int y, int z, int tile) {
    blocks[x << 11 | z << 7 | y] = tile;
}

int LevelChunk::getData(int x, int y, int z) {
    return data.get(x, y, z);
}

void LevelChunk::recalcHeightmapOnly() {
    int min = Level::DEPTH - 1;
    for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++) {
            int y = Level::DEPTH - 1;
            int p = x << 11 | z << 7;
            while (y > 0 && Tile::lightBlock[blocks[p + y - 1] & 0xff] == 0) y--;
            heightmap[z << 4 | x] = (char)y;
            if (y < min) min = y;
        }
    minHeight = min;
}

void LevelChunk::recalcHeightmap() {
    int min = Level::DEPTH - 1;
    for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++) {
            int y = Level::DEPTH - 1;
            int p = x << 11 | z << 7;
            while (y > 0 && Tile::lightBlock[blocks[p + y - 1] & 0xff] == 0) y--;
            heightmap[z << 4 | x] = (char)y;
            if (y < min) min = y;

            if (!level->dimension->hasCeiling) {
                int br = Level::MAX_BRIGHTNESS;
                int yy = Level::DEPTH - 1;
                do {
                    br -= Tile::lightBlock[blocks[p + yy] & 0xff];
                    if (br > 0) skyLight.set(x, yy, z, br);
                    yy--;
                } while (yy > 0 && br > 0);
            }
        }
    minHeight = min;
    for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++)
            lightGaps(x, z);
}

void LevelChunk::recalcHeight(int x, int yStart, int z) {
    int yOld = heightmap[z << 4 | x] & 0xff;
    int y = yOld;
    if (yStart > yOld) y = yStart;
    int p = x << 11 | z << 7;
    while (y > 0 && Tile::lightBlock[blocks[p + y - 1] & 0xff] == 0) y--;
    if (y == yOld) return;
    int64_t xOffs = xt + x;
    int64_t zOffs = zt + z;
    level->lightColumnChanged((int)xOffs, (int)zOffs, y, yOld);
    heightmap[z << 4 | x] = (char)y;
    if (y < minHeight) {
        minHeight = y;
    } else {
        int min = Level::DEPTH - 1;
        for (int _x = 0; _x < 16; _x++)
            for (int _z = 0; _z < 16; _z++)
                if ((heightmap[_z << 4 | _x] & 0xff) < min) min = (heightmap[_z << 4 | _x] & 0xff);
        minHeight = min;
    }
    if (y < yOld) {
        for (int yy = y; yy < yOld; yy++) skyLight.set(x, yy, z, 15);
    } else {
        level->updateLight(LightLayer::Sky, (int)xOffs, yOld, (int)zOffs, (int)xOffs, y, (int)zOffs);
        for (int yy = yOld; yy < y; yy++) skyLight.set(x, yy, z, 0);
    }
    int br = 15;
    int y0 = y;
    while (y > 0 && br > 0) {
        y--;
        int block = Tile::lightBlock[getTile(x, y, z)];
        if (block == 0) block = 1;
        br -= block;
        if (br < 0) br = 0;
        skyLight.set(x, y, z, br);
    }
    while (y > 0 && Tile::lightBlock[getTile(x, y - 1, z)] == 0) y--;
    if (y != y0) {
        level->updateLight(LightLayer::Sky, (int)xOffs - 1, y, (int)zOffs - 1, (int)xOffs + 1, y0, (int)zOffs + 1);
    }
}

void LevelChunk::skyBrightnessChanged() { }

bool LevelChunk::shouldSave(bool force) {
    if (dontSave) return false;
    return unsaved;
}

void LevelChunk::setBlocks(unsigned char* newBlocks, int sub) {
    LOGI("LevelChunk::setBlocks\n");
    for (int i = 0; i < 128 * 16 * 4; i++) {
        blocks[sub * 128 * 16 * 4 + i] = newBlocks[i];
    }
    for (int x = sub * 4; x < sub * 4 + 4; x++) {
        for (int z = 0; z < 16; z++) recalcHeight(x, 0, z);
    }
    level->updateLight(LightLayer::Sky, (int)(xt + sub * 4), 0, (int)zt, (int)(xt + sub * 4 + 4), 128, (int)(zt + 16));
    level->updateLight(LightLayer::Block, (int)(xt + sub * 4), 0, (int)zt, (int)(xt + sub * 4 + 4), 128, (int)(zt + 16));
    level->setTilesDirty((int)(xt + sub * 4), 0, (int)zt, (int)(xt + sub * 4 + 4), 128, (int)zt);
}

Random LevelChunk::getRandom(long l) {
    return Random((level->getSeed() + x * x * 4987142 + x * 5947611 + z * z * 4392871l + z * 389711) ^ l);
}

void LevelChunk::lightGap(int x, int z, int source) {
    int height = level->getHeightmap(x, z);
    if (height > source) {
        level->updateLight(LightLayer::Sky, x, source, z, x, height, z);
    } else if (height < source) {
        level->updateLight(LightLayer::Sky, x, height, z, x, source, z);
    }
}

void LevelChunk::clearUpdateMap() {
    memset(updateMap, 0x0, 256);
    unsaved = false;
}

void LevelChunk::deleteBlockData() {
    delete[] blocks;
    blocks = NULL;
}

bool LevelChunk::isAt(int64_t _x, int64_t _z) {
    return _x == x && _z == z;
}

int LevelChunk::getHeightmap(int x, int z) {
    return heightmap[z << 4 | x] & 0xff;
}

void LevelChunk::recalcBlockLights() { }

void LevelChunk::lightGaps(int x, int z) {
    int height = getHeightmap(x, z);
    int xOffs = (int)(xt + x);
    int zOffs = (int)(zt + z);
    lightGap(xOffs - 1, zOffs, height);
    lightGap(xOffs + 1, zOffs, height);
    lightGap(xOffs, zOffs - 1, height);
    lightGap(xOffs, zOffs + 1, height);
}

int LevelChunk::getTile(int x, int y, int z) {
    return blocks[x << 11 | z << 7 | y] & 0xff;
}

void LevelChunk::setData(int x, int y, int z, int val) {
    data.set(x, y, z, val);
}

int LevelChunk::getBrightness(const LightLayer& layer, int x, int y, int z) {
    if (&layer == &LightLayer::Sky) return skyLight.get(x, y, z);
    else if (&layer == &LightLayer::Block) return blockLight.get(x, y, z);
    else return 0;
}

void LevelChunk::setBrightness(const LightLayer& layer, int x, int y, int z, int brightness) {
    if (&layer == &LightLayer::Sky) skyLight.set(x, y, z, brightness);
    else if (&layer == &LightLayer::Block) blockLight.set(x, y, z, brightness);
}

int LevelChunk::getRawBrightness(int x, int y, int z, int skyDampen) {
    int light = skyLight.get(x, y, z);
    if (light > 0) LevelChunk::touchedSky = true;
    light -= skyDampen;
    int block = blockLight.get(x, y, z);
    if (block > light) light = block;
    return light;
}

void LevelChunk::addEntity(Entity* e) {
    lastSaveHadEntities = true;
    int64_t xc = Mth::floor64(e->x / 16.0);
    int64_t zc = Mth::floor64(e->z / 16.0);
    if (xc != x || zc != z) {
        LOGE("ERR: WRONG LOCATION : %lld, %lld, %lld, %lld", (long long)xc, (long long)x, (long long)zc, (long long)z);
    }
    int yc = Mth::floor(e->y / 16);
    if (yc < 0) yc = 0;
    if (yc >= EntityBlocksArraySize) yc = EntityBlocksArraySize - 1;
    e->inChunk = true;
    e->xChunk = (int)x;   // 注意：Entity 中 xChunk 目前是 int，需要改为 int64_t，但暂时强转
    e->yChunk = yc;
    e->zChunk = (int)z;
    entityBlocks[yc].push_back(e);
}

void LevelChunk::removeEntity(Entity* e) {
    removeEntity(e, e->yChunk);
}

void LevelChunk::removeEntity(Entity* e, int yc) {
    if (yc < 0) yc = 0;
    if (yc >= EntityBlocksArraySize) yc = EntityBlocksArraySize - 1;
    EntityList::iterator newEnd = std::remove(entityBlocks[yc].begin(), entityBlocks[yc].end(), e);
    entityBlocks[yc].erase(newEnd, entityBlocks[yc].end());
}

bool LevelChunk::isSkyLit(int x, int y, int z) {
    return y >= (heightmap[z << 4 | x] & 0xff);
}

void LevelChunk::load() {
    loaded = true;
}

void LevelChunk::unload() {
    loaded = false;
}

void LevelChunk::markUnsaved() {
    unsaved = true;
}

void LevelChunk::getEntities(Entity* except, const AABB& bb, std::vector<Entity*>& es) {
    int yc0 = Mth::floor((bb.y0 - 2) / 16);
    int yc1 = Mth::floor((bb.y1 + 2) / 16);
    if (yc0 < 0) yc0 = 0;
    if (yc1 >= EntityBlocksArraySize) yc1 = EntityBlocksArraySize - 1;
    for (int yc = yc0; yc <= yc1; yc++) {
        std::vector<Entity*>& entities = entityBlocks[yc];
        for (unsigned int i = 0; i < entities.size(); i++) {
            Entity* e = entities[i];
            if (e != except && e->bb.intersects(bb)) es.push_back(e);
        }
    }
}

void LevelChunk::getEntitiesOfType(int entityType, const AABB& bb, EntityList& list) {
    int yc0 = Mth::floor((bb.y0 - 2) / 16);
    int yc1 = Mth::floor((bb.y1 + 2) / 16);
    if (yc0 < 0) yc0 = 0;
    if (yc0 >= EntityBlocksArraySize) yc0 = EntityBlocksArraySize - 1;
    if (yc1 >= EntityBlocksArraySize) yc1 = EntityBlocksArraySize - 1;
    if (yc1 < 0) yc1 = 0;
    for (int yc = yc0; yc <= yc1; yc++) {
        std::vector<Entity*>& entities = entityBlocks[yc];
        for (unsigned int i = 0; i < entities.size(); i++) {
            Entity* e = entities[i];
            if (e->getEntityTypeId() == entityType) list.push_back(e);
        }
    }
}

void LevelChunk::getEntitiesOfClass(int type, const AABB& bb, EntityList& list) {
    int yc0 = Mth::floor((bb.y0 - 2) / 16);
    int yc1 = Mth::floor((bb.y1 + 2) / 16);
    if (yc0 < 0) yc0 = 0;
    if (yc0 >= EntityBlocksArraySize) yc0 = EntityBlocksArraySize - 1;
    if (yc1 >= EntityBlocksArraySize) yc1 = EntityBlocksArraySize - 1;
    if (yc1 < 0) yc1 = 0;
    for (int yc = yc0; yc <= yc1; yc++) {
        std::vector<Entity*>& entities = entityBlocks[yc];
        for (unsigned int i = 0; i < entities.size(); i++) {
            Entity* e = entities[i];
            if (e->getCreatureBaseType() == type) list.push_back(e);
        }
    }
}

int LevelChunk::countEntities() {
    int entityCount = 0;
    for (int yc = 0; yc < EntityBlocksArraySize; yc++) entityCount += entityBlocks[yc].size();
    return entityCount;
}

TileEntity* LevelChunk::getTileEntity(int x, int y, int z) {
    TilePos pos(x, y, z);
    TEMap::iterator cit = tileEntities.find(pos);
    TileEntity* tileEntity = NULL;
    if (cit == tileEntities.end()) {
        int t = getTile(x, y, z);
        if (t <= 0 || !Tile::isEntityTile[t]) return NULL;
        if (tileEntity == NULL) {
            tileEntity = ((EntityTile*)Tile::tiles[t])->newTileEntity();
            level->setTileEntity((int)(xt + x), y, (int)(zt + z), tileEntity);
        }
    } else {
        tileEntity = cit->second;
    }
    if (tileEntity != NULL && tileEntity->isRemoved()) {
        tileEntities.erase(cit);
        return NULL;
    }
    return tileEntity;
}

bool LevelChunk::hasTileEntityAt(int x, int y, int z) {
    return tileEntities.find(TilePos(x, y, z)) != tileEntities.end();
}

bool LevelChunk::hasTileEntityAt(TileEntity* te) {
    return tileEntities.find(TilePos(te->x & 15, te->y, te->z & 15)) != tileEntities.end();
}

void LevelChunk::addTileEntity(TileEntity* te) {
    int xx = te->x - (int)xt;
    int yy = te->y;
    int zz = te->z - (int)zt;
    setTileEntity(xx, yy, zz, te);
    if (loaded) level->tileEntities.push_back(te);
}

void LevelChunk::setTileEntity(int x, int y, int z, TileEntity* tileEntity) {
    tileEntity->setLevelAndPos(level, (int)(xt + x), y, (int)(zt + z));
    int t = getTile(x, y, z);
    if (t == 0 || !Tile::isEntityTile[t]) {
        LOGW("Attempted to place a tile entity where there was no entity tile! %d, %d, %d\n", tileEntity->x, tileEntity->y, tileEntity->z);
        return;
    }
    tileEntity->clearRemoved();
    tileEntities.insert(std::make_pair(TilePos(x, y, z), tileEntity));
}

void LevelChunk::removeTileEntity(int x, int y, int z) {
    if (loaded) {
        TilePos pos(x, y, z);
        TEMap::iterator cit = tileEntities.find(pos);
        if (cit != tileEntities.end()) {
            cit->second->setRemoved();
            if (!level->isClientSide) {
                for (unsigned int i = 0; i < level->players.size(); ++i) {
                    level->players[i]->tileEntityDestroyed(cit->second->runningId);
                }
            }
            tileEntities.erase(cit);
        }
    }
}

int LevelChunk::getBlocksAndData(unsigned char* data, int x0, int y0, int z0, int x1, int y1, int z1, int p) {
    int len = y1 - y0;
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = x << 11 | z << 7 | y0;
            memcpy(data + p, blocks + slot, len);
            p += len;
        }
    len = (y1 - y0) / 2;
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(data + p, this->data.data + slot, len);
            p += len;
        }
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(data + p, blockLight.data + slot, len);
            p += len;
        }
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(data + p, skyLight.data + slot, len);
            p += len;
        }
    return p;
}

int LevelChunk::setBlocksAndData(unsigned char* data, int x0, int y0, int z0, int x1, int y1, int z1, int p) {
    int len = y1 - y0;
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = x << 11 | z << 7 | y0;
            memcpy(blocks + slot, data + p, len);
            p += len;
        }
    recalcHeightmapOnly();
    len = (y1 - y0) / 2;
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(this->data.data + slot, data + p, len);
            p += len;
        }
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(blockLight.data + slot, data + p, len);
            p += len;
        }
    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            int slot = (x << 11 | z << 7 | y0) >> 1;
            memcpy(skyLight.data + slot, data + p, len);
            p += len;
        }
    return p;
}

bool LevelChunk::isEmpty() { return false; }

const LevelChunk::TEMap& LevelChunk::getTileEntityMap() const { return tileEntities; }
