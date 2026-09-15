#include "Level.h"
#include "LevelListener.h"
#include "tile/entity/TileEntity.h"
#include "../entity/player/Player.h"
#include "../entity/EntityEvent.h"

#include "biome/BiomeSource.h"
#include "chunk/storage/ChunkStorage.h"
#include "chunk/ChunkCache.h"
#include "storage/LevelStorage.h"
#include "Region.h"
#include "Explosion.h"
#include "LevelConstants.h"
#include "pathfinder/PathFinder.h"

#include "../Facing.h"
#include "../../util/PerfTimer.h"

#include "tile/LiquidTile.h"

#include "biome/Biome.h"
#include "MobSpawner.h"
#include "../../network/packet/SetEntityDataPacket.h"
#include "../../network/RakNetInstance.h"
#include "../../network/packet/EntityEventPacket.h"
#include "../../network/packet/SetTimePacket.h"
#include "../entity/monster/Zombie.h"
#include "../inventory/BaseContainerMenu.h"
#include "../Difficulty.h"
#include "../../network/packet/ExplodePacket.h"
#include "levelgen/TheEndLevelSource.h"
#include "../../client/Minecraft.h"       // 🔧 for Minecraft::instance

Level::Level(LevelStorage* levelStorage, const std::string& levelName, const LevelSettings& settings, int generatorVersion, Dimension* fixedDimension /* = NULL */)
:	levelStorage(levelStorage),
	isClientSide(false),
	isFindingSpawn(false),
	noNeighborUpdate(false),
	skyDarken(0),
	instaTick(false),
	random(1),
	_isNew(false),
	_chunkSource(NULL),
	_randValue(42184323),
	_addend(1013904223),
	_maxRecurse(0),
	_updateLights(true),
	_pathFinder(NULL),
	_spawnFriendlies(true),
	_spawnEnemies(true),
	_lastSavedPlayerTime(0),
	raknetInstance(0),
	updatingTileEntities(false),
	allPlayersAreSleeping(false),
	_nightMode(false)
{
    _init(levelName, settings, generatorVersion, fixedDimension);
}

Level::~Level() {
	LOGI("Erasing chunk source\n");
	delete _chunkSource;
	LOGI("Erasing dimension\n");
	delete dimension;
	delete _pathFinder;

	std::set<Entity*> all;
	all.insert(entities.begin(), entities.end());
	all.insert(players.begin(), players.end());
	PendingList::iterator it = _pendingPlayerRemovals.begin();
	while (it != _pendingPlayerRemovals.end()) {
		all.insert(it->e);
		++it;
	}
	for (std::set<Entity*>::iterator it = all.begin(); it != all.end(); ++it)
		delete *it;

	std::set<TileEntity*> allTileEntities;
	allTileEntities.insert(tileEntities.begin(), tileEntities.end());
	allTileEntities.insert(pendingTileEntities.begin(), pendingTileEntities.end());
	for (std::set<TileEntity*>::iterator it = allTileEntities.begin(); it != allTileEntities.end(); ++it)
		delete *it;
}

void Level::_init(const std::string& levelName, const LevelSettings& settings, int generatorVersion, Dimension* fixedDimension) {

	isGeneratingTerrain = false;

	LevelData* preparedData = levelStorage->prepareLevel(this);
    _isNew = (preparedData == NULL);

    if (preparedData == NULL) {
        levelData = LevelData(settings, levelName, generatorVersion);
    } else {
		levelData = *preparedData;
        levelData.setLevelName(levelName);
    }

	if (fixedDimension != NULL) {
		dimension = fixedDimension;
	} else {
		dimension = DimensionFactory::createDefaultDimension( &levelData );
	}

    dimension->init(this);
    _chunkSource = createChunkSource();

	_pathFinder = new PathFinder();
	_nightMode = false;
    updateSkyBrightness();
}

/*protected*/
ChunkSource* Level::createChunkSource(){
#ifndef STANDALONE_SERVER
    bool useEnd = Minecraft::instance &&
        Minecraft::instance->options.getBooleanValue(OPTIONS_END_GENERATOR);
#else
    bool useEnd = false;
#endif

    if(!levelStorage){
        if(useEnd) return new TheEndLevelSource(this, levelData.getSeed());
        return dimension->createRandomLevelSource();
    }

    ChunkStorage* chunkStorage = levelStorage->createChunkStorage(dimension);
    
    if (useEnd) {
    return new TheEndLevelSource(this, levelData.getSeed());
	}
    
    return new ChunkCache(this, chunkStorage, dimension->createRandomLevelSource());
}
/*public*/
bool Level::checkAndHandleWater(const AABB& box, const Material* material, Entity* e) {
    int x0 = Mth::floor(box.x0);
    int x1 = Mth::floor(box.x1 + 1);

    int y0 = Mth::floor(box.y0);
    int y1 = Mth::floor(box.y1 + 1);

    int z0 = Mth::floor(box.z0);
    int z1 = Mth::floor(box.z1 + 1);

    if (!hasChunksAt(x0, y0, z0, x1, y1, z1)) {
        return false;
    }

    bool ok = false;
    Vec3 current(0,0,0);
    for (int x = x0; x < x1; x++)
        for (int y = y0; y < y1; y++)
            for (int z = z0; z < z1; z++) {
                Tile* tile = Tile::tiles[getTile(x, y, z)];
                if (tile != NULL && tile->material == material) {
                    float yt0 = y + 1 - LiquidTile::getHeight(getData(x, y, z));
                    if (y1 >= yt0) {
                        ok = true;
                        tile->handleEntityInside(this, x, y, z, e, current);
                    }
                }
            }
	float len = current.length();
    if (len > 0) {
        const float pow = 0.004f / len;
        e->xd += current.x * pow;
        e->yd += current.y * pow;
        e->zd += current.z * pow;
    }
    return ok;
}

/*public*/
Player* Level::getNearestPlayer(Entity* source, float maxDist) {
    return getNearestPlayer(source->x, source->y, source->z, maxDist);
}

/*public*/
Player* Level::getNearestPlayer(float x, float y, float z, float maxDist) {
    float maxDistSqr = maxDist * maxDist;
	float best = -1;
    Player* result = NULL;
    for (unsigned int i = 0; i < players.size(); i++) {
        Player* p = players[i];
		if (p->removed) continue;
		float dist = p->distanceToSqr(x, y, z);
        if ((maxDist < 0 || dist < maxDistSqr) && (best == -1 || dist < best)) {
            best = dist;
            result = p;
        }
    }
    return result;
}

/*public*/
void Level::tick() {
	if (!isClientSide && levelData.getSpawnMobs()) {
		static int _mobSpawnTick = 0;
		const int MobSpawnInterval = 2;
		if (++_mobSpawnTick >= MobSpawnInterval) {
			_mobSpawnTick = 0;
			TIMER_PUSH("mobSpawner");
			MobSpawner::tick(this,	_spawnEnemies && difficulty > Difficulty::PEACEFUL,
									_spawnFriendlies && (levelData.getTime() % 400) < MobSpawnInterval);
			TIMER_POP();
		}
	}

	TIMER_PUSH("chunkSource");
	_chunkSource->tick();

	updateSkyDarken();
	if(_nightMode) {
		long curTime = levelData.getTime();
		if(curTime % TICKS_PER_DAY  != MIDDLE_OF_NIGHT_TIME) {
			if(curTime % TICKS_PER_DAY < MIDDLE_OF_NIGHT_TIME && (curTime + 20) % TICKS_PER_DAY > MIDDLE_OF_NIGHT_TIME) {
				curTime = MIDDLE_OF_NIGHT_TIME;
				SetTimePacket packet(curTime);
				raknetInstance->send(packet);
			}
			else {
				curTime += 20;
				if(curTime % 20  == 0) {
					SetTimePacket packet(curTime);
					raknetInstance->send(packet);
				}
			}
			setTime(curTime%TICKS_PER_DAY);
		}
	}
	else {
		long time = levelData.getTime() + 1;
		levelData.setTime(time);
		if ((time & 255) == 0) {
			SetTimePacket packet(time);
			raknetInstance->send(packet);
		}
	}
	TIMER_POP_PUSH("tickPending");
	tickPendingTicks(false);

	TIMER_POP_PUSH("tickTiles");
    tickTiles();

	TIMER_POP_PUSH("sendEntityData");
	for (unsigned int i = 0; i < entities.size(); ++i) {
		Entity* e = entities[i];
		SynchedEntityData* data = e->getEntityData();
		if (data && data->isDirty()) {
			SetEntityDataPacket packet(e->entityId, *data);
			raknetInstance->send(packet);
		}
	}

	TIMER_POP();
}

/*protected*/
void Level::tickTiles() {
    _chunksToPoll.clear();

	static const int pollChunkOffsets[] = {
		-1,-4,  0,-4,  1,-4,  -2,-3,  -1,-3,  0,-3,  1,-3,  2,-3,  -3,-2,
		-2,-2,  -1,-2,  0,-2,  1,-2,  2,-2,  3,-2,  -4,-1,  -3,-1,  -2,-1,
		-1,-1,  0,-1,  1,-1,  2,-1,  3,-1,  4,-1,  -4,0,  -3,0,  -2,0,  -1,0,
		0,0,  1,0,  2,0,  3,0,  4,0,  -4,1,  -3,1,  -2,1,  -1,1,  0,1,  1,1,
		2,1,  3,1,  4,1,  -3,2,  -2,2,  -1,2,  0,2, 1,2,  2,2,  3,2,  -2,3,
		-1,3,  0,3,  1,3,  2,3,  -1,4,  0,4,  1,4
	};
	const int pollChunkOffsetsSize = sizeof(pollChunkOffsets) / sizeof(int);

	TIMER_PUSH("buildList");
    for (size_t i = 0; i < players.size(); i++) {
        Player* player = players[i];
        int xx = Mth::floor(player->x / 16);
        int zz = Mth::floor(player->z / 16);

		for (int i = 0; i < pollChunkOffsetsSize; i += 2) {
			const int xp = xx + pollChunkOffsets[i];
			const int zp = zz + pollChunkOffsets[i+1];
			if (xp >= 0 && xp < CHUNK_CACHE_WIDTH &&
				zp >= 0 && zp < CHUNK_CACHE_WIDTH)
				_chunksToPoll.insert(ChunkPos(xp, zp));
		}
    }
	TIMER_POP();

	TIMER_PUSH("loop");
    for (ChunkPosSet::iterator it = _chunksToPoll.begin(); it != _chunksToPoll.end(); ++it) {
		const ChunkPos& cp = *it;
        int xo = cp.x * 16;
        int zo = cp.z * 16;
		TIMER_PUSH("getChunk");
        LevelChunk* lc = this->getChunk(cp.x, cp.z);
		TIMER_POP_PUSH("tickTiles");
        for (int i = 0; i < 20; i++) {
            _randValue = _randValue * 3 + _addend;
            int val = (_randValue >> 2);
            int x = (val & 15);
            int z = ((val >> 8) & 15);
            int y = ((val >> 16) & 127);

            int id = lc->getTile(x, y, z);
            if (Tile::shouldTick[id]) {
                Tile::tiles[id]->tick(this, x + xo, y, z + zo, &random);
            }
        }
		TIMER_POP();
    }
	TIMER_POP();
}

bool Level::tickPendingTicks(bool force) {
    int count = _tickNextTickSet.size();
    if (count > MAX_TICK_TILES_PER_TICK) count = MAX_TICK_TILES_PER_TICK;
    for (int i = 0; i < count; i++) {
		TickDataSet::iterator td = _tickNextTickSet.begin();
        if (!force && td->delay > levelData.getTime()) {
            break;
        }

		int r = 8;
        if (hasChunksAt(td->x - r, td->y - r, td->z - r, td->x + r, td->y + r, td->z + r)) {
            int id = getTile(td->x, td->y, td->z);
            if (id == td->tileId && id > 0) {
                Tile::tiles[id]->tick(this, td->x, td->y, td->z, &random);
            }
        }
        _tickNextTickSet.erase(td);
    }
    return _tickNextTickSet.size() != 0;
}

/*public*/
void Level::loadPlayer(Player* player, bool doAddPlayer /*= true*/) {
	if (player) {
		CompoundTag* loadedPlayerTag = levelData.getLoadedPlayerTag();
        if (loadedPlayerTag != NULL) {
            player->load(loadedPlayerTag);
            levelData.setLoadedPlayerTag(NULL);
        } else {
			levelData.setLoadedPlayerTo(player);
		}
		if (doAddPlayer)
			addEntity(player);
	}
}

bool Level::findPath(Path* path, Entity* from, Entity* to, float maxDist, bool canOpenDoors, bool avoidWater) {
    TIMER_PUSH("pathfind");
	int x = Mth::floor(from->x);
    int y = Mth::floor(from->y);
    int z = Mth::floor(from->z);

    int r = (int) (maxDist + 16);
    int x1 = x - r;
    int y1 = y - r;
    int z1 = z - r;
    int x2 = x + r;
    int y2 = y + r;
    int z2 = z + r;
    Region region(this, x1, y1, z1, x2, y2, z2);
	_pathFinder->setLevelSource(&region);
	_pathFinder->canOpenDoors = canOpenDoors;
	_pathFinder->avoidWater = avoidWater;
	_pathFinder->findPath(path, from, to, maxDist);
	TIMER_POP();
	return true;
}

bool Level::findPath(Path* path, Entity* from, int xBest, int yBest, int zBest, float maxDist, bool canOpenDoors, bool avoidWater) {
    TIMER_PUSH("pathfind");
	int x = Mth::floor(from->x);
    int y = Mth::floor(from->y);
    int z = Mth::floor(from->z);

    int r = (int) (maxDist + 8);
    int x1 = x - r;
    int y1 = y - r;
    int z1 = z - r;
    int x2 = x + r;
    int y2 = y + r;
    int z2 = z + r;
    Region region(this, x1, y1, z1, x2, y2, z2);
	_pathFinder->setLevelSource(&region);
	_pathFinder->canOpenDoors = canOpenDoors;
	_pathFinder->avoidWater = avoidWater;
    _pathFinder->findPath(path, from, xBest, yBest, zBest, maxDist);
	TIMER_POP();
	return true;
}

/*protected*/
void Level::setInitialSpawn() {
    isFindingSpawn = true;

    int xSpawn = 0;
    int ySpawn = 64;
    int zSpawn = 0;

    // 强制加载出生点所在区块
    getChunk(xSpawn >> 4, zSpawn >> 4);
    
    // 放置基岩
    setTileAndDataNoUpdate(xSpawn, ySpawn - 1, zSpawn, Tile::unbreakable->id, 0);
    setTileNoUpdate(xSpawn, ySpawn, zSpawn, 0);
    setTileNoUpdate(xSpawn, ySpawn + 1, zSpawn, 0);

    levelData.setSpawn(xSpawn, ySpawn, zSpawn);
    isFindingSpawn = false;
}

/*public*/
void Level::validateSpawn() {
    if (levelData.getYSpawn() <= 0) {
        levelData.setYSpawn(64);
    }
    int xSpawn = levelData.getXSpawn();
    int zSpawn = levelData.getZSpawn();
    while (getTopTile(xSpawn, zSpawn) == 0 || getTopTile(xSpawn, zSpawn) == Tile::invisible_bedrock->id) {
        xSpawn += random.nextInt(8) - random.nextInt(8);
        zSpawn += random.nextInt(8) - random.nextInt(8);
    }
    levelData.setXSpawn(xSpawn);
    levelData.setZSpawn(zSpawn);
}

int Level::getTopTile(int64_t x, int64_t z) {
    int y = 63;
    while (!isEmptyTile(x, y + 1, z)) {
        y++;
    }
    return getTile(x, y, z);
}

int Level::getTopTileY(int64_t x, int64_t z) {
    int y = 63;
    while (!isEmptyTile(x, y + 1, z)) {
        y++;
    }
    return y;
}

void Level::saveLevelData() {
	levelStorage->saveLevelData(levelData, &players);
}

int Level::getTile(int64_t x, int y, int64_t z) {
    if (y < 0) return 0;
    if (y >= DEPTH) return 0;
    return getChunk(x >> 4, z >> 4)->getTile((int)(x & 15), y, (int)(z & 15));
}

bool Level::isEmptyTile(int64_t x, int y, int64_t z) {
    return getTile(x, y, z) == 0;
}

bool Level::hasChunkAt(int64_t x, int64_t y, int64_t z) {
    return hasChunk(x >> 4, z >> 4);
}

bool Level::hasChunksAt(int x, int y, int z, int r) {
    return hasChunksAt((int64_t)x - r, (int64_t)y - r, (int64_t)z - r,
                       (int64_t)x + r, (int64_t)y + r, (int64_t)z + r);
}

bool Level::hasChunksAt(int64_t x0, int64_t y0, int64_t z0, int64_t x1, int64_t y1, int64_t z1) {
    int64_t xc0 = x0 >> 4;
    int64_t zc0 = z0 >> 4;
    int64_t xc1 = x1 >> 4;
    int64_t zc1 = z1 >> 4;
    for (int64_t x = xc0; x <= xc1; ++x)
        for (int64_t z = zc0; z <= zc1; ++z)
            if (!hasChunk(x, z)) return false;
    return true;
}

bool Level::hasChunk(int64_t x, int64_t z) {
    return _chunkSource->hasChunk(x, z);
}

LevelChunk* Level::getChunkAt(int64_t x, int64_t z) {
    return getChunk(x >> 4, z >> 4);
}

LevelChunk* Level::getChunk(int64_t x, int64_t z) {
    return _chunkSource->getChunk(x, z);
}

bool Level::setTileAndDataNoUpdate(int64_t x, int y, int64_t z, int tile, int data) {
    if (y < 0) return false;
    if (y >= DEPTH) return false;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->setTileAndData((int)(x & 15), y, (int)(z & 15), tile, data);
}

bool Level::setTileNoUpdate(int64_t x, int y, int64_t z, int tile) {
    if (y < 0) return false;
    if (y >= DEPTH) return false;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->setTile((int)(x & 15), y, (int)(z & 15), tile);
}

const Material* Level::getMaterial(int64_t x, int y, int64_t z) {
    int t = getTile(x, y, z);
    if (t == 0) return Material::air;
    return Tile::tiles[t]->material;
}

int Level::getData(int64_t x, int y, int64_t z) {
    if (y < 0) return 0;
    if (y >= DEPTH) return 0;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->getData((int)(x & 15), y, (int)(z & 15));
}

void Level::setData(int64_t x, int y, int64_t z, int data) {
    if (setDataNoUpdate(x, y, z, data)) {
        tileUpdated(x, y, z, getTile(x, y, z));
    }
}

bool Level::setDataNoUpdate(int64_t x, int y, int64_t z, int data) {
    if (y < 0) return false;
    if (y >= DEPTH) return false;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    int xi = (int)(x & 15);
    int zi = (int)(z & 15);
    if (c->getData(xi, y, zi) != data) {
        c->setData(xi, y, zi, data);
        return true;
    }
    return false;
}

bool Level::setTile(int64_t x, int y, int64_t z, int tile) {
    if (setTileNoUpdate(x, y, z, tile)) {
        tileUpdated(x, y, z, tile);
        return true;
    }
    return false;
}

bool Level::setTileAndData(int64_t x, int y, int64_t z, int tile, int data) {
    if (setTileAndDataNoUpdate(x, y, z, tile, data)) {
        tileUpdated(x, y, z, tile);
        return true;
    }
    return false;
}

void Level::sendTileUpdated(int64_t x, int y, int64_t z) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->tileChanged(x, y, z);
    }
}

/*protected*/
void Level::tileUpdated(int64_t x, int y, int64_t z, int tile) {
    sendTileUpdated(x, y, z);
    this->updateNeighborsAt(x, y, z, tile);
}

void Level::lightColumnChanged(int64_t x, int64_t z, int y0, int y1) {
    if (y0 > y1) {
        int tmp = y1;
        y1 = y0;
        y0 = tmp;
    }
    setTilesDirty(x, y0, z, x, y1, z);
}

void Level::setTileDirty(int64_t x, int y, int64_t z) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->setTilesDirty(x, y, z, x, y, z);
    }
}

void Level::setTilesDirty(int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->setTilesDirty(x0, y0, z0, x1, y1, z1);
    }
}

void Level::swap(int64_t x1, int y1, int64_t z1, int64_t x2, int y2, int64_t z2) {
    int t1 = getTile(x1, y1, z1);
    int d1 = getData(x1, y1, z1);
    int t2 = getTile(x2, y2, z2);
    int d2 = getData(x2, y2, z2);

    setTileAndDataNoUpdate(x1, y1, z1, t2, d2);
    setTileAndDataNoUpdate(x2, y2, z2, t1, d1);

    updateNeighborsAt(x1, y1, z1, t2);
    updateNeighborsAt(x2, y2, z2, t1);
}

void Level::updateNeighborsAt(int64_t x, int y, int64_t z, int tile) {
    neighborChanged(x - 1, y, z, tile);
    neighborChanged(x + 1, y, z, tile);
    neighborChanged(x, y - 1, z, tile);
    neighborChanged(x, y + 1, z, tile);
    neighborChanged(x, y, z - 1, tile);
    neighborChanged(x, y, z + 1, tile);
}

/*private*/ void Level::neighborChanged(int64_t x, int y, int64_t z, int type) {
    if (noNeighborUpdate || isClientSide) return;
    Tile* tile = Tile::tiles[getTile(x, y, z)];
    if (tile != NULL) tile->neighborChanged(this, x, y, z, type);
}

bool Level::canSeeSky(int64_t x, int y, int64_t z) {
    return getChunk(x >> 4, z >> 4)->isSkyLit((int)(x & 15), y, (int)(z & 15));
}

int Level::getRawBrightness(int64_t x, int y, int64_t z) {
    return getRawBrightness(x, y, z, true);
}

int Level::getRawBrightness(int64_t x, int y, int64_t z, bool propagate) {
    if (propagate) {
        int id = getTile(x, y, z);
        if (id == Tile::stoneSlabHalf->id || id == Tile::farmland->id) {
            int br = getRawBrightness(x, y + 1, z, false);
            int br1 = getRawBrightness(x + 1, y, z, false);
            int br2 = getRawBrightness(x - 1, y, z, false);
            int br3 = getRawBrightness(x, y, z + 1, false);
            int br4 = getRawBrightness(x, y, z - 1, false);
            if (br1 > br) br = br1;
            if (br2 > br) br = br2;
            if (br3 > br) br = br3;
            if (br4 > br) br = br4;
            return br;
        }
    }
    if (y < 0) return 0;
    if (y >= DEPTH) {
        int br = MAX_BRIGHTNESS - skyDarken;
        if (br < 0) br = 0;
        return br;
    }
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->getRawBrightness((int)(x & 15), y, (int)(z & 15), skyDarken);
}

bool Level::isSkyLit(int64_t x, int y, int64_t z) {
    if (y < 0) return false;
    if (y >= DEPTH) return true;
    if (!hasChunk(x >> 4, z >> 4)) return false;

    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->isSkyLit((int)(x & 15), y, (int)(z & 15));
}

int Level::getHeightmap(int64_t x, int64_t z) {
    if (!hasChunk(x >> 4, z >> 4)) return 0;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    return c->getHeightmap((int)(x & 15), (int)(z & 15));
}

BiomeSource* Level::getBiomeSource() {
	return dimension->biomeSource;
}
Biome* Level::getBiome(int64_t x, int64_t z) {
	return dimension->biomeSource->getBiome(x, z);
}

void Level::updateLightIfOtherThan(const LightLayer& layer, int64_t x, int y, int64_t z, int expected) {
    if (dimension->hasCeiling && (&layer) == &LightLayer::Sky) return;

    if (!hasChunkAt(x, y, z)) return;

    if (&layer == &LightLayer::Sky) {
        if (isSkyLit(x, y, z)) expected = 15;
    } else if (&layer == &LightLayer::Block) {
        int t = getTile(x, y, z);
        if (Tile::lightEmission[t] > expected) expected = Tile::lightEmission[t];
    }

    if (getBrightness(layer, x, y, z) != expected) {
        updateLight(layer, x, y, z, x, y, z);
    }
}

int Level::getBrightness(const LightLayer& layer, int64_t x, int y, int64_t z) {
    if (y < 0 || y >= DEPTH) {
        return layer.surrounding;
    }
    int64_t xc = x >> 4;
    int64_t zc = z >> 4;
    if (!hasChunk(xc, zc)) return 0;
    LevelChunk* c = getChunk(xc, zc);
    return c->getBrightness(layer, (int)(x & 15), y, (int)(z & 15));
}

void Level::setBrightness(const LightLayer& layer, int64_t x, int y, int64_t z, int brightness) {
    if (y < 0) return;
    if (y >= DEPTH) return;
    if (!hasChunk(x >> 4, z >> 4)) return;
    LevelChunk* c = getChunk(x >> 4, z >> 4);
    c->setBrightness(layer, (int)(x & 15), y, (int)(z & 15), brightness);
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->tileBrightnessChanged(x, y, z);
    }
}

float Level::getBrightness(int64_t x, int y, int64_t z) {
	return dimension->brightnessRamp[getRawBrightness(x, y, z)];
}

bool Level::isDay() {
    return this->skyDarken < 4;
}

HitResult Level::clip(const Vec3& A, const Vec3& b, bool liquid /*= false*/, bool solidOnly /*= false*/) {
    static Stopwatch sw;
    SwStartStopper w(sw);

    if (A.x != A.x || A.y != A.y || A.z != A.z) return HitResult();
    if (b.x != b.x || b.y != b.y || b.z != b.z) return HitResult();

    Vec3 a(A);

    // 🔧 使用 int64_t 存储方块坐标，避免溢出
    int64_t xTile1 = Mth::floor64(b.x);
    int64_t yTile1 = Mth::floor64(b.y);
    int64_t zTile1 = Mth::floor64(b.z);

    int64_t xTile0 = Mth::floor64(a.x);
    int64_t yTile0 = Mth::floor64(a.y);
    int64_t zTile0 = Mth::floor64(a.z);

    int maxIterations = 200;
    while (maxIterations-- >= 0) {
        if (a.x != a.x || a.y != a.y || a.z != a.z)
            return HitResult();
        if (xTile0 == xTile1 && yTile0 == yTile1 && zTile0 == zTile1)
            return HitResult();

        float xClip = 999;
        float yClip = 999;
        float zClip = 999;

        if (xTile1 > xTile0) xClip = (float)(xTile0 + 1.0f);
        if (xTile1 < xTile0) xClip = (float)(xTile0 + 0.0f);

        if (yTile1 > yTile0) yClip = (float)(yTile0 + 1.0f);
        if (yTile1 < yTile0) yClip = (float)(yTile0 + 0.0f);

        if (zTile1 > zTile0) zClip = (float)(zTile0 + 1.0f);
        if (zTile1 < zTile0) zClip = (float)(zTile0 + 0.0f);

        float xDist = 999;
        float yDist = 999;
        float zDist = 999;

        float xd = b.x - a.x;
        float yd = b.y - a.y;
        float zd = b.z - a.z;

        if (xClip != 999) xDist = (xClip - (float)a.x) / xd;
        if (yClip != 999) yDist = (yClip - (float)a.y) / yd;
        if (zClip != 999) zDist = (zClip - (float)a.z) / zd;

        int face = 0;
        if (xDist < yDist && xDist < zDist) {
            if (xTile1 > xTile0) face = 4;
            else face = 5;

            a.x = xClip;
            a.y += yd * xDist;
            a.z += zd * xDist;
        } else if (yDist < zDist) {
            if (yTile1 > yTile0) face = 0;
            else face = 1;

            a.x += xd * yDist;
            a.y = yClip;
            a.z += zd * yDist;
        } else {
            if (zTile1 > zTile0) face = 2;
            else face = 3;

            a.x += xd * zDist;
            a.y += yd * zDist;
            a.z = zClip;
        }

        Vec3 tPos(a.x, a.y, a.z);
        xTile0 = Mth::floor64(a.x);
        if (face == 5) {
            xTile0--;
            tPos.x++;
        }
        yTile0 = Mth::floor64(a.y);
        if (face == 1) {
            yTile0--;
            tPos.y++;
        }
        zTile0 = Mth::floor64(a.z);
        if (face == 3) {
            zTile0--;
            tPos.z++;
        }

        int t = getTile(xTile0, (int)yTile0, zTile0);
        int data = getData(xTile0, (int)yTile0, zTile0);
        Tile* tile = Tile::tiles[t];

        if (solidOnly && tile != NULL && tile->getAABB(this, (int)xTile0, (int)yTile0, (int)zTile0) == NULL) {
            // No collision
        } else if (t > 0 && tile->mayPick(data, liquid)) {
            // 🔧 移除原版的 LEVEL_WIDTH 边界检查（世界是无限的）
            HitResult r = tile->clip(this, (int)xTile0, (int)yTile0, (int)zTile0, a, b);
            if (r.isHit()) return r;
        }
    }
    return HitResult();
}

void Level::playSound(Entity* entity, const std::string& name, float volume, float pitch) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->playSound(name, entity->x, entity->y - entity->heightOffset, entity->z, volume, pitch);
    }
}

void Level::playSound(float x, float y, float z, const std::string& name, float volume, float pitch) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->playSound(name, x, y, z, volume, pitch);
    }
}

void Level::levelEvent(Player* source, int type, int64_t x, int y, int64_t z, int data) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->levelEvent(source, type, x, y, z, data);
    }
}

void Level::tileEvent(int64_t x, int y, int64_t z, int b0, int b1) {
	int t = getTile(x, y, z);
	if (t > 0) Tile::tiles[t]->triggerEvent(this, x, y, z, b0, b1);

	for (unsigned int i = 0; i < _listeners.size(); i++) {
		_listeners[i]->tileEvent(x, y, z, b0, b1);
	}
}

void Level::addParticle(const std::string& id, float x, float y, float z, float xd, float yd, float zd, int data /* = 0 */) {
	for (unsigned int i = 0; i < _listeners.size(); i++)
        _listeners[i]->addParticle(id, x, y, z, xd, yd, zd, data);
}

void Level::addParticle(ParticleType::Id id, float x, float y, float z, float xd, float yd, float zd, int data /* = 0 */) {
	for (unsigned int i = 0; i < _listeners.size(); i++)
		_listeners[i]->addParticle(id, x, y, z, xd, yd, zd, data);
}

bool Level::addEntity(Entity* e) {
	Entity* prev = getEntity(e->entityId);
	if (prev)
		removeEntity(prev);

    int xc = Mth::floor(e->x / 16.f);
    int zc = Mth::floor(e->z / 16.f);

	bool forced = true;

	if (forced) {
		if (e->isPlayer() && std::find(players.begin(), players.end(), e) == players.end()) {
			players.push_back( (Player*) e );
		}
		getChunk(xc, zc)->addEntity(e);
		entities.push_back(e);
		entityIdLookup[e->entityId] = e;
		entityAdded(e);
		return true;
	}

	return false;
}

Entity* Level::getEntity(int entityId)
{
	EntityMap::const_iterator cit = entityIdLookup.find(entityId);
	return (cit != entityIdLookup.end())? cit->second : NULL;
}

/*protected*/
void Level::entityAdded(Entity* e) {
    for (unsigned int i = 0; i < _listeners.size(); i++) {
        _listeners[i]->entityAdded(e);
    }
}

/*protected*/
void Level::entityRemoved(Entity* e) {
    for (unsigned int j = 0; j < _listeners.size(); j++) {
        _listeners[j]->entityRemoved(e);
    }
}

Mob* Level::getMob(int entityId)
{
	Entity* entity = getEntity(entityId);
	return (entity && entity->isMob())? (Mob*)entity : NULL;
}

void Level::removeEntity(Entity* e) {
    e->remove();
	if (e->isPlayer() && e->reallyRemoveIfPlayer) {
		Util::remove(players, (Player*) e);
    }
}

void Level::tileEntityChanged(int64_t x, int y, int64_t z, TileEntity* te) {
	if (this->hasChunkAt(x, y, z)) {
		getChunkAt(x, z)->markUnsaved();
	}
	for (unsigned int i = 0; i < _listeners.size(); i++) {
		_listeners[i]->tileEntityChanged(x, y, z, te);
	}
}

Biome::MobSpawnerData Level::getRandomMobSpawnAt(const MobCategory& mobCategory, int x, int y, int z) {
	Biome::MobList mobList = _chunkSource->getMobsAt(mobCategory, x, y, z);
    if (mobList.empty()) return Biome::MobSpawnerData();

	Biome::MobSpawnerData* data = (Biome::MobSpawnerData*) WeighedRandom::getRandomItem(&random, mobList);
	if (!data)
		return Biome::MobSpawnerData();
    return *data;
}

void Level::addListener(LevelListener* listener) {
    _listeners.push_back(listener);
}

void Level::removeListener(LevelListener* listener) {
	ListenerList::iterator it = std::find(_listeners.begin(), _listeners.end(), listener);
	_listeners.erase(it);
}

std::vector<AABB>& Level::getCubes(const Entity* source, const AABB& box_) {
	boxes.clear();
	const AABB* box = &box_;
    int x0 = Mth::floor(box->x0);
    int x1 = Mth::floor(box->x1 + 1);
    int y0 = Mth::floor(box->y0);
    int y1 = Mth::floor(box->y1 + 1);
    int z0 = Mth::floor(box->z0);
    int z1 = Mth::floor(box->z1 + 1);

    for (int x = x0; x < x1; x++)
        for (int z = z0; z < z1; z++) {
            if (hasChunkAt(x, Level::DEPTH / 2, z)) {
                for (int y = y0 - 1; y < y1; y++) {
                    Tile* tile = Tile::tiles[getTile(x, y, z)];
                    if (tile != NULL) {
                        tile->addAABBs(this, x, y, z, box, boxes);
                    }
                }
            }
        }

    return boxes;
}

int Level::getSkyDarken(float a) {
    float td = getTimeOfDay(a);

    float br = 1 - (Mth::cos(td * Mth::PI * 2) * 2 + 0.5f);
    if (br < 0.0f) br = 0.0f;
    if (br > 0.80f) br = 0.80f;
    return ((int) (br * 11));
}

Vec3 Level::getSkyColor(Entity* source, float a) {
    float td = getTimeOfDay(a);

	float br = Mth::cos(td * Mth::PI * 2) * 2 + 0.5f;
    if (br < 0.0f) br = 0.0f;
    if (br > 0.75f) br = 0.75f;

    int skyColor = 0x3070ff;

    float r = ((skyColor >> 16) & 0xff) / 255.0f;
    float g = ((skyColor >> 8) & 0xff) / 255.0f;
    float b = ((skyColor) & 0xff) / 255.0f;
    r *= br;
    g *= br;
    b *= br;

    return Vec3(r, g, b);
}

Vec3 Level::getCloudColor( float a ) {
	float td = getTimeOfDay(a);

	float br = Mth::cos(td * Mth::PI * 2) * 2.0f + 0.5f;
	if (br < 0.0f) br = 0.0f;
	if (br > 1.0f) br = 1.0f;
	long cloudColor = 0xffffff; 
	float r = ((cloudColor >> 16) & 0xff) / 255.0f;
	float g = ((cloudColor >> 8) & 0xff) / 255.0f;
	float b = ((cloudColor) & 0xff) / 255.0f;

	float rainLevel = 0;
	if (rainLevel > 0) {
		float mid = (r * 0.30f + g * 0.59f + b * 0.11f) * 0.6f;

		float ba = 1 - rainLevel * 0.95f;
		r = r * ba + mid * (1 - ba);
		g = g * ba + mid * (1 - ba);
		b = b * ba + mid * (1 - ba);
	}

	r *= br * 0.90f + 0.10f;
	g *= br * 0.90f + 0.10f;
	b *= br * 0.85f + 0.15f;

	float thunderLevel = 0;
	if (thunderLevel > 0) {
		float mid = (r * 0.30f + g * 0.59f + b * 0.11f) * 0.2f;

		float ba = 1 - thunderLevel * 0.95f;
		r = r * ba + mid * (1 - ba);
		g = g * ba + mid * (1 - ba);
		b = b * ba + mid * (1 - ba);
	}

	return Vec3(r, g, b);
}

float Level::getTimeOfDay(float a) {
	return dimension->getTimeOfDay(levelData.getTime(), a);
}

float Level::getSunAngle(float a) {
    float td = getTimeOfDay(a);
    return td * Mth::PI * 2;
}

Vec3 Level::getFogColor(float a) {
    float td = getTimeOfDay(a);
    return dimension->getFogColor(td, a);
}

int Level::getTopSolidBlock(int64_t x, int64_t z) {
    LevelChunk* levelChunk = getChunkAt(x, z);

    int y = Level::DEPTH - 1;

    while (getMaterial(x, y, z)->blocksMotion() && y > 0) {
        y--;
    }

    int xi = (int)(x & 15);
    int zi = (int)(z & 15);

    while (y > 0) {
        int t = levelChunk->getTile(xi, y, zi);
        if (t == 0 || !(Tile::tiles[t]->material->blocksMotion()) || Tile::tiles[t]->material == Material::leaves) {
            y--;
        } else {
            return y + 1;
        }
    }
    return -1;
}

int Level::getLightDepth(int64_t x, int64_t z) {
    return getChunkAt(x, z)->getHeightmap((int)(x & 15), (int)(z & 15));
}

float Level::getStarBrightness(float a) {
    float td = getTimeOfDay(a);

    float br = 1 - (Mth::cos(td * Mth::PI * 2) * 2 + 0.75f);
    if (br < 0.f) br = 0;
    if (br > 1.f) br = 1;

    return br * br * 0.5f;
}

void Level::addToTickNextTick(int64_t x, int y, int64_t z, int tileId, int tickDelay) {
	TickNextTickData td((int)x, y, (int)z, tileId);
    int r = 8;
    if (instaTick) {
        if (hasChunksAt(td.x - r, td.y - r, td.z - r, td.x + r, td.y + r, td.z + r)) {
            int id = getTile(td.x, td.y, td.z);
            if (id == td.tileId && id > 0) {
                Tile::tiles[id]->tick(this, td.x, td.y, td.z, &random);
            }
        }
        return;
    }

    if (hasChunksAt(x - r, y - r, z - r, x + r, y + r, z + r)) {
        if (tileId > 0) {
            td.setDelay(tickDelay + levelData.getTime());
        }
		if (_tickNextTickSet.find(td) == _tickNextTickSet.end()) {
			_tickNextTickSet.insert(td);
        }
    }
}

void Level::tickEntities() {
    TIMER_PUSH("entities");

    TIMER_PUSH("remove");
    EntityList pendingRemovedEntities;
    std::vector<Zombie*> zombies;

    // 参考玩家（取列表第一个），用于距离判断
    Player* refPlayer = players.empty() ? nullptr : players[0];

    TIMER_POP_PUSH("regular");
    for (unsigned int i = 0; i < entities.size(); i++) {
        Entity* e = entities[i];

        // 对存活实体进行降频更新
        if (!e->removed) {
            bool shouldTick = true;
            if (refPlayer) {
                float distSqr = e->distanceToSqr(refPlayer);
                long now = levelData.getTime();          // 世界时间，代替全局 ticks
                // 先判断更远的距离，避免永远进不了更长的间隔
                if (distSqr > 256 * 256) {
                    if ((now + e->entityId) % 10 != 0) shouldTick = false;
                } else if (distSqr > 128 * 128) {
                    if ((now + e->entityId) % 4 != 0) shouldTick = false;
                }
            }

            if (shouldTick) {
                tick(e);
                if (e->getEntityTypeId() == MobTypes::Zombie) {
                    zombies.push_back((Zombie*)e);
                    ((Zombie*)e)->setUseNewAi(false);
                }
            }
        }

        // 移除逻辑：无论实体是否被 tick，都必须执行，防止遗留
        TIMER_PUSH("remove");
        if (e->removed && (!e->isPlayer() || e->reallyRemoveIfPlayer)) {
            int xc = e->xChunk;
            int zc = e->zChunk;
            if (e->inChunk && hasChunk(xc, zc)) {
                getChunk(xc, zc)->removeEntity(e);
            }
            entityIdLookup.erase(e->entityId);
            entities.erase(entities.begin() + (i--));
            entityRemoved(e);
            pendingRemovedEntities.push_back(e);
        }
        TIMER_POP();
    }

    // 延迟销毁待移除实体
    TIMER_POP_PUSH("remove");
    for (unsigned int i = 0; i < pendingRemovedEntities.size(); ++i) {
        Entity* e = pendingRemovedEntities[i];
        if (e->isPlayer())
            _pendingPlayerRemovals.push_back( PRInfo(e, 16) );
        else
            delete e;
    }
    pendingRemovedEntities.clear();

    for (int i = (int)_pendingPlayerRemovals.size()-1; i >= 0; --i) {
        PRInfo& pr = _pendingPlayerRemovals[i];
        if (--pr.ticks <= 0) {
            LOGI("deleting: %p\n", pr.e);
            delete pr.e;
            _pendingPlayerRemovals.erase(_pendingPlayerRemovals.begin() + i);
        }
    }

    // 方块实体更新
    TIMER_POP_PUSH("tileEntities");
    updatingTileEntities = true;
    for (unsigned int i = 0; i < tileEntities.size(); ++i) {
        TileEntity* te = tileEntities[i];
        if (!te->isRemoved() && te->level != NULL) {
            if (hasChunkAt(te->x, te->y, te->z)) {
                te->tick();
            }
        }

        if (te->isRemoved()) {
            tileEntities.erase(tileEntities.begin() + (i--));

            if (hasChunk(te->x >> 4, te->z >> 4)) {
                LevelChunk* lc = getChunk(te->x >> 4, te->z >> 4);
                if (lc != NULL) {
                    lc->removeTileEntity(te->x & 15, te->y, te->z & 15);
                }
            }
            delete te;
        }
    }
    updatingTileEntities = false;

    // 待处理的方块实体
    TIMER_POP_PUSH("pendingTileEntities");
    if (!pendingTileEntities.empty()) {
        for (unsigned int i = 0; i < pendingTileEntities.size(); ++i) {
            TileEntity* e = pendingTileEntities[i];
            if (!e->isRemoved()) {
                bool found = false;
                for (unsigned int j = 0; j < tileEntities.size(); ++j) {
                    if (tileEntities[j] == e) {
                        found = true;
                        break;
                    }
                }
                int xx = e->x, yy = e->y, zz = e->z;

                LevelChunk* lc = getChunk(xx >> 4, zz >> 4);
                bool has = lc && lc->hasTileEntityAt(e);
                if (!found) {
                    if (!has) tileEntities.push_back(e);
                    else {
                        delete e;
                        e = NULL;
                    }
                }

                if (e && lc) lc->setTileEntity(xx & 15, yy, zz & 15, e);
                sendTileUpdated(xx, yy, zz);
            }
        }
        pendingTileEntities.clear();
    }

    TIMER_POP();
    TIMER_POP();
}

class DistanceEntitySorter {
	Vec3 c;
public:
	DistanceEntitySorter(float x, float y, float z) : c(x, y, z) {}
	bool operator() (const Entity* c0, const Entity* c1) {
		const float d0 = c.distanceToSqr(c0->x, c0->y, c0->z);
		const float d1 = c.distanceToSqr(c1->x, c1->y, c1->z);
		return d0 < d1;
	}
};

void Level::setZombieAi(std::vector<Zombie*>& zombies) {
	unsigned int size = zombies.size();
	const int NumSmartZombiesPerPlayer = 0;
	if (size <= NumSmartZombiesPerPlayer) {
		for (unsigned int i = 0; i < size; ++i)
			zombies[i]->setUseNewAi(true);
		return;
	}

	for (unsigned int i = 0; i < players.size(); ++i) {
		Player* p = players[i];
		DistanceEntitySorter sorter(p->x, p->y, p->z);
		std::nth_element(zombies.begin(), zombies.begin() + NumSmartZombiesPerPlayer, zombies.end(), sorter);
		for (int j = 0; j < NumSmartZombiesPerPlayer; ++j)
			if (zombies[j]->distanceToSqr(p) < 32*32)
				zombies[j]->setUseNewAi(true);
	}
}

void Level::tick(Entity* e) {
    tick(e, true);
}

void Level::tick(Entity* e, bool actual) {
    int64_t xc = Mth::floor64(e->x);
int64_t zc = Mth::floor64(e->z);
    int r = 32;
    if (actual && !hasChunksAt(xc - r, 0, zc - r, xc + r, 128, zc + r)) {
        return;
    }

    e->xOld = e->x;
    e->yOld = e->y;
    e->zOld = e->z;
    e->yRotO = e->yRot;
    e->xRotO = e->xRot;

    if (actual && e->inChunk) {
        e->tick();
    }

	TIMER_PUSH("chunkCheck");
	if (e->x != e->x) e->x = e->xOld;
	if (e->y != e->y) e->y = e->yOld;
	if (e->z != e->z) e->z = e->zOld;
	if (e->xRot != e->xRot) e->xRot = e->xRotO;
	if (e->yRot != e->yRot) e->yRot = e->yRotO;

    // 用 Big 精确计算区块坐标 (e->getChunkX() 已通过 Entity.h 添加)
int64_t xcn = static_cast<int64_t>(
    e->getBigAbsX().convert_to<BigWorldCoordinate_Integer>()) >> 4;
int64_t ycn = Mth::floor64(e->y / 16.0);
int64_t zcn = static_cast<int64_t>(
    e->getBigAbsZ().convert_to<BigWorldCoordinate_Integer>()) >> 4;

    if (!e->inChunk || (e->xChunk != xcn || e->yChunk != ycn || e->zChunk != zcn)) {
        if (e->inChunk && hasChunk(e->xChunk, e->zChunk)) {
            getChunk(e->xChunk, e->zChunk)->removeEntity(e, e->yChunk);
        }

        if (hasChunk(xcn, zcn)) {
            e->inChunk = true;
            getChunk(xcn, zcn)->addEntity(e);
        } else {
            e->inChunk = false;
        }
    }
	TIMER_POP();

	const float now = getTimeS();
	if (now - _lastSavedPlayerTime >= 30) {
		saveLevelData();
		_lastSavedPlayerTime = now;
	}
}

bool Level::isUnobstructed(const AABB& aabb) {
    EntityList& entities = getEntities(NULL, aabb);
    for (unsigned int i = 0; i < entities.size(); i++) {
        Entity* e = entities[i];
        if (!e->removed && e->blocksBuilding) return false;
    }
    return true;
}

bool Level::containsAnyLiquid(const AABB& box) {
    int x0 = Mth::floor(box.x0);
    int x1 = Mth::floor(box.x1 + 1);
    int y0 = Mth::floor(box.y0);
    int y1 = Mth::floor(box.y1 + 1);
    int z0 = Mth::floor(box.z0);
    int z1 = Mth::floor(box.z1 + 1);

    if (box.x0 < 0) x0--;
    if (box.y0 < 0) y0--;
    if (box.z0 < 0) z0--;

    for (int x = x0; x < x1; x++)
        for (int y = y0; y < y1; y++)
            for (int z = z0; z < z1; z++) {
                Tile* tile = Tile::tiles[getTile(x, y, z)];
                if (tile != NULL && tile->material->isLiquid()) {
                    return true;
                }
            }
    return false;
}

bool Level::containsFireTile(const AABB& box) {
    int x0 = Mth::floor(box.x0);
    int x1 = Mth::floor(box.x1 + 1);
    int y0 = Mth::floor(box.y0);
    int y1 = Mth::floor(box.y1 + 1);
    int z0 = Mth::floor(box.z0);
    int z1 = Mth::floor(box.z1 + 1);

    if (hasChunksAt(x0, y0, z0, x1, y1, z1)) {
        for (int x = x0; x < x1; x++)
            for (int y = y0; y < y1; y++)
                for (int z = z0; z < z1; z++) {
                    int t = getTile(x, y, z);
                    if (t == Tile::lava->id || t == Tile::calmLava->id) {
 						 return true;
					}
                }
    }
    return false;
}

bool Level::containsMaterial(const AABB& box, const Material* material) {
    int x0 = Mth::floor(box.x0);
    int x1 = Mth::floor(box.x1 + 1);
    int y0 = Mth::floor(box.y0);
    int y1 = Mth::floor(box.y1 + 1);
    int z0 = Mth::floor(box.z0);
    int z1 = Mth::floor(box.z1 + 1);

    for (int x = x0; x < x1; x++)
        for (int y = y0; y < y1; y++)
            for (int z = z0; z < z1; z++) {
                Tile* tile = Tile::tiles[getTile(x, y, z)];
                if (tile != NULL && tile->material == material) {
                    return true;
                }
            }
    return false;
}

bool Level::containsLiquid(const AABB& box, const Material* material) {
    int x0 = Mth::floor(box.x0);
    int x1 = Mth::floor(box.x1 + 1);
    int y0 = Mth::floor(box.y0);
    int y1 = Mth::floor(box.y1 + 1);
    int z0 = Mth::floor(box.z0);
    int z1 = Mth::floor(box.z1 + 1);

    for (int x = x0; x < x1; x++)
        for (int y = y0; y < y1; y++)
            for (int z = z0; z < z1; z++) {
                Tile* tile = Tile::tiles[getTile(x, y, z)];
                if (tile != NULL && tile->material == material) {
                    int data = getData(x, y, z);
                    float yh1 = (float)(y + 1);
                    if (data < 8) {
                        yh1 = (float)y + 1.0f - (float)data / 8.0f;
                    }
                    if (yh1 >= box.y0) {
                        return true;
                    }
                }
            }
    return false;
}

/* in java, this returns an Explosion */
void Level::explode(Entity* source, float x, float y, float z, float r) {
    explode(source, x, y, z, r, false);
}

/* in java, this returns an Explosion */
void Level::explode(Entity* source, float x, float y, float z, float r, bool fire) {
	if (!isClientSide) {
		Explosion explosion(this, source, x, y, z, r);
		explosion.fire = fire;
		explosion.explode();
		explosion.finalizeExplosion();
		ExplodePacket packet(x, y, z, r, explosion.toBlow);
		raknetInstance->send(packet);
	}
}

float Level::getSeenPercent(const Vec3& center, const AABB& bb) {
    float xs = 1.0f / ((bb.x1 - bb.x0) * 2 + 1);
    float ys = 1.0f / ((bb.y1 - bb.y0) * 2 + 1);
    float zs = 1.0f / ((bb.z1 - bb.z0) * 2 + 1);
    int hits = 0;
    int count = 0;
    for (float xx = 0; xx <= 1; xx += xs)
        for (float yy = 0; yy <= 1; yy += ys)
            for (float zz = 0; zz <= 1; zz += zs) {
                float x = bb.x0 + (bb.x1 - bb.x0) * xx;
                float y = bb.y0 + (bb.y1 - bb.y0) * yy;
                float z = bb.z0 + (bb.z1 - bb.z0) * zz;
				if (!clip(Vec3(x, y, z), center).isHit()) hits++;
                count++;
            }

    return hits / (float) count;
}

bool Level::isSolidBlockingTile(int64_t x, int y, int64_t z)
{
    Tile* tile = Tile::tiles[getTile(x, y, z)];
    if (tile == NULL) return false;
    return tile->material->isSolidBlocking() && tile->isCubeShaped();
}

bool Level::isSolidRenderTile(int64_t x, int y, int64_t z) {
	Tile* tile = Tile::tiles[getTile(x, y, z)];
	if (tile == NULL) return false;
	return tile->isSolidRender();
}

void Level::extinguishFire(int64_t x, int y, int64_t z, int face) {
	switch (face) {
		case Facing::DOWN : y--; break;
		case Facing::UP   : y++; break;
		case Facing::NORTH: z--; break;
		case Facing::SOUTH: z++; break;
		case Facing::WEST : x--; break;
		case Facing::EAST : x++; break;
	}

    if (getTile(x, y, z) == ((Tile*)Tile::fire)->id) {
        setTile(x, y, z, 0);
    }
}

TileEntity* Level::getTileEntity(int64_t x, int y, int64_t z) {
	LevelChunk* lc = getChunk(x >> 4, z >> 4);
	if (!lc) return NULL;

	if (TileEntity* tileEntity = lc->getTileEntity((int)(x & 15), y, (int)(z & 15)))
		return tileEntity;

	for (unsigned int i = 0; i < pendingTileEntities.size(); ++i) {
		TileEntity* e = pendingTileEntities[i];
		if (!e->isRemoved() && e->x == x && e->y == y && e->z == z)
			return e;
	}
	return NULL;
}

void Level::setTileEntity(int64_t x, int y, int64_t z, TileEntity* tileEntity) {
	if (!tileEntity || tileEntity->isRemoved())
		return;

	if (updatingTileEntities) {
		tileEntity->x = x;
		tileEntity->y = y;
		tileEntity->z = z;
		pendingTileEntities.push_back(tileEntity);
	} else {
		tileEntities.push_back(tileEntity);

		LevelChunk* lc = getChunk(x >> 4, z >> 4);
		if (lc != NULL) lc->setTileEntity((int)(x & 15), y, (int)(z & 15), tileEntity);
	}
}

void Level::removeTileEntity(int64_t x, int y, int64_t z) {
	TileEntity* te = getTileEntity(x, y, z);
	if (te && updatingTileEntities) {
		te->setRemoved();
		Util::remove(pendingTileEntities, te);
	} else {
		LevelChunk* lc = getChunk(x >> 4, z >> 4);
		if (lc) lc->removeTileEntity((int)(x & 15), y, (int)(z & 15));

		if (te) {
			Util::remove(pendingTileEntities, te);
			Util::remove(tileEntities, te);
			delete te;
		}
	}
}

int Level::getLightsToUpdate() {
    return _lightUpdates.size();
}

bool Level::updateLights() {
    if (_maxRecurse >= 50) return false;
    _maxRecurse++;
    int max = 50;   // 从 500 降至 50，大幅降低峰值
    while ((int)_lightUpdates.size() > 0) {
        if (--max <= 0) {
            _maxRecurse--;
            return true;   // 没处理完，下帧继续
        }
        LightUpdate l = _lightUpdates.back();
        _lightUpdates.pop_back();
        l.update(this);
    }
    _maxRecurse--;
    return false;
}

void Level::setUpdateLights(bool doUpdate) {
	_updateLights = doUpdate;
}

void Level::updateLight(const LightLayer& layer, int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1) {
    updateLight(layer, x0, y0, z0, x1, y1, z1, true);
}

static int maxLoop = 0;

void Level::updateLight(const LightLayer& layer, int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1, bool join) {
    if ((dimension->hasCeiling && &layer == &LightLayer::Sky) || !_updateLights) return;

    maxLoop++;
    if (maxLoop == 50) {
        maxLoop--;
        return;
    }
    int64_t xm = (x1 + x0) / 2;
    int64_t zm = (z1 + z0) / 2;
    if (!hasChunkAt(xm, Level::DEPTH / 2, zm)) {
        maxLoop--;
        return;
    }
    if (getChunkAt(xm, zm)->isEmpty()) {
        maxLoop--;
        return;
    }
    int count = _lightUpdates.size();
    if (join) {
        int toCheck = 5;
        if (toCheck > count) toCheck = count;
        for (int i = 0; i < toCheck; i++) {
            LightUpdate& last = _lightUpdates[_lightUpdates.size() - i - 1];
            if (last.layer == &layer && last.expandToContain((int)x0, y0, (int)z0, (int)x1, y1, (int)z1)) {
                maxLoop--;
                return;
            }
        }
    }
    _lightUpdates.push_back(LightUpdate(layer, (int)x0, y0, (int)z0, (int)x1, y1, (int)z1));
    int max = 1000000;
    if ((int)_lightUpdates.size() > max) {
        LOGI("More than %d updates, aborting lighting updates\n", max);
        _lightUpdates.clear();
    }
    maxLoop--;
}

bool Level::updateSkyBrightness() {
    if (!adventureSettings.doTickTime) {
        return false; // 时间冻结时不更新天空亮度
    }
    int newDark = this->getSkyDarken(1);
    if (newDark != skyDarken) {
        skyDarken = newDark;
        return true;
    }
    return false;
}

void Level::setSpawnSettings(bool spawnEnemies, bool spawnFriendlies) {
}

void Level::animateTick(int64_t xt, int yt, int64_t zt) {
    int r = 16;
    Random animateRandom;

    for (int i = 0; i < 100; i++) {
        int x = (int)(xt + random.nextInt(r) - random.nextInt(r));
        int y = yt + random.nextInt(r) - random.nextInt(r);
        int z = (int)(zt + random.nextInt(r) - random.nextInt(r));
        int t = getTile(x, y, z);
         if (t > 0 && t < Tile::NUM_BLOCK_TYPES && Tile::tiles[t] != nullptr) {
            Tile::tiles[t]->animateTick(this, x, y, z, &animateRandom);
		 }
    }
}

EntityList& Level::getEntities(Entity* except, const AABB& bb) {
    _es.clear();
    int xc0 = Mth::floor((bb.x0 - 2) / 16);
    int xc1 = Mth::floor((bb.x1 + 2) / 16);
    int zc0 = Mth::floor((bb.z0 - 2) / 16);
    int zc1 = Mth::floor((bb.z1 + 2) / 16);
    for (int xc = xc0; xc <= xc1; xc++)
        for (int zc = zc0; zc <= zc1; zc++) {
            if (hasChunk(xc, zc)) {
                getChunk(xc, zc)->getEntities(except, bb, _es);
            }
        }
    return _es;
}

const EntityList& Level::getAllEntities() {
    return entities;
}

void Level::prepare() {
    while (_chunkSource->tick())
        ;
}

bool Level::mayPlace(int tileId, int64_t x, int y, int64_t z, bool ignoreEntities,unsigned char face) {
    int targetType = getTile(x, y, z);
    const Tile* targetTile = Tile::tiles[targetType];
    Tile* tile = Tile::tiles[tileId];

    AABB* aabb = tile->getAABB(this, x, y, z);
    if (ignoreEntities) aabb = NULL;
    if (aabb != NULL && !isUnobstructed(*aabb)) return false;
    if (targetTile == Tile::water || targetTile == Tile::calmWater || targetTile == Tile::lava || targetTile == Tile::calmLava || targetTile == (Tile*)(Tile::fire) || targetTile == Tile::topSnow) targetTile = NULL;
    if (tileId > 0 && targetTile == NULL) {
        if (tile->mayPlace(this, x, y, z, face)) {
            return true;
        }
    }

    return false;
}

int Level::getSeaLevel() {
    return SEA_LEVEL;
}

bool Level::getDirectSignal(int64_t x, int y, int64_t z, int dir) {
    int t = getTile(x, y, z);
    if (t == 0) return false;
    return Tile::tiles[t]->getDirectSignal(this, x, y, z, dir);
}

bool Level::hasDirectSignal(int64_t x, int y, int64_t z) {
    if (getDirectSignal(x, y - 1, z, 0)) return true;
    if (getDirectSignal(x, y + 1, z, 1)) return true;
    if (getDirectSignal(x, y, z - 1, 2)) return true;
    if (getDirectSignal(x, y, z + 1, 3)) return true;
    if (getDirectSignal(x - 1, y, z, 4)) return true;
    if (getDirectSignal(x + 1, y, z, 5)) return true;
    return false;
}

bool Level::getSignal(int64_t x, int y, int64_t z, int dir) {
    if (isSolidBlockingTile(x, y, z)) {
        return hasDirectSignal(x, y, z);
    }
    int t = getTile(x, y, z);
    if (t == 0) return false;
    return Tile::tiles[t]->getSignal(this, x, y, z, dir);
}

bool Level::hasNeighborSignal(int64_t x, int y, int64_t z) {
    if (getSignal(x, y - 1, z, 0)) return true;
    if (getSignal(x, y + 1, z, 1)) return true;
    if (getSignal(x, y, z - 1, 2)) return true;
    if (getSignal(x, y, z + 1, 3)) return true;
    if (getSignal(x - 1, y, z, 4)) return true;
    if (getSignal(x + 1, y, z, 5)) return true;
    return false;
}

void Level::setTime(long time) {
    this->levelData.setTime(time);
}

long Level::getSeed() {
    return levelData.getSeed();
}

long Level::getTime() {
    return levelData.getTime();
}

Pos Level::getSharedSpawnPos() {
    return Pos(levelData.getXSpawn(), levelData.getYSpawn(), levelData.getZSpawn());
}

void Level::setSpawnPos(Pos spawnPos) {
    levelData.setSpawn(spawnPos.x, spawnPos.y, spawnPos.z);
}

bool Level::mayInteract(Player* player, int64_t xt, int yt, int64_t zt) {
    return true;
}

void Level::broadcastEntityEvent(Entity* e, char eventId) {
	if (isClientSide) return;

	EntityEventPacket packet(e->entityId, eventId);
	raknetInstance->send(packet);
}

ChunkSource* Level::getChunkSource() {
    return _chunkSource;
}

LevelStorage* Level::getLevelStorage() {
    return levelStorage;
}

LevelData* Level::getLevelData() {
    return &levelData;
}

void Level::takePicture( TripodCamera* cam, Entity* e )
{
	for (unsigned int i = 0; i < _listeners.size(); ++i)
		_listeners[i]->takePicture(cam, e);
}

int Level::getEntitiesOfType( int entityType, const AABB& bb, EntityList& list )
{
	int xc0 = Mth::floor((bb.x0 - 2) / 16);
	int xc1 = Mth::floor((bb.x1 + 2) / 16);
	int zc0 = Mth::floor((bb.z0 - 2) / 16);
	int zc1 = Mth::floor((bb.z1 + 2) / 16);
	int count = -(int)list.size();
	for (int xc = xc0; xc <= xc1; xc++)
		for (int zc = zc0; zc <= zc1; zc++) {
			if (hasChunk(xc, zc)) {
				getChunk(xc, zc)->getEntitiesOfType(entityType, bb, list);
			}
		}
	return list.size() - count;
}

int Level::getEntitiesOfClass( int type, const AABB& bb, EntityList& list ) {
	int xc0 = Mth::floor((bb.x0 - 2) / 16);
	int xc1 = Mth::floor((bb.x1 + 2) / 16);
	int zc0 = Mth::floor((bb.z0 - 2) / 16);
	int zc1 = Mth::floor((bb.z1 + 2) / 16);
	int count = -(int)list.size();
	for (int xc = xc0; xc <= xc1; xc++)
		for (int zc = zc0; zc <= zc1; zc++) {
			if (hasChunk(xc, zc)) {
				getChunk(xc, zc)->getEntitiesOfClass(type, bb, list);
			}
		}
		return list.size() - count;
}

int Level::countInstanceOfType( int typeId ) {
	int n = 0;
	for (unsigned int i = 0; i < entities.size(); ++i)
		if (typeId == entities[i]->getEntityTypeId())
			++n;
	return n;
}

int Level::countInstanceOfBaseType( int baseTypeId ) {
	if (baseTypeId < MobTypes::BaseEnemy || baseTypeId > MobTypes::BaseWaterCreature) {
		LOGE("Bad typeId sent to Level::countInstanceOf. Note that only Base types (MobTypes::Base*) are supported for now.\n");
		return -1;
	}

	int n = 0;
	for (unsigned int i = 0; i < entities.size(); ++i) 
		if (baseTypeId == entities[i]->getCreatureBaseType())
			++n;
	return n;
}

void Level::dispatchEntityData( Entity* e )
{
	if (isClientSide) return;
	_pendingEntityData.insert(std::make_pair(e->entityId, e));
}

void Level::saveGame()
{
	if (levelStorage) {
		levelStorage->saveGame(this);
		saveLevelData();
	}
}

void Level::loadEntities()
{
	if (levelStorage)
		levelStorage->loadEntities(this, NULL);
}

void Level::updateSkyDarken()
{
	if (updateSkyBrightness())
		for (unsigned i = 0; i < _listeners.size(); i++) {
			_listeners[i]->skyColorChanged();
		}
}

void Level::removePlayer( Player* player )
{
	for (unsigned int i = 0; i < players.size(); ++i)
	if (players[i] == player) {
		players.erase( players.begin() + i );
	}
}

int Level::isNightMode() {
	return _nightMode;
}

void Level::setNightMode( bool isNightMode ) {
	_nightMode = isNightMode;
}

bool Level::inRange( int64_t x, int y, int64_t z ) {
    return y >= 0 && y < LEVEL_HEIGHT;
}

//
// AdventureSettings
//
AdventureSettings::AdventureSettings()
:	doTickTime(true),
	noPvP(false),
	noPvM(false),
	noMvP(false),
	immutableWorld(false),
	showNameTags(true)
{
}
