#ifndef NET_MINECRAFT_WORLD_LEVEL__Level_H__
#define NET_MINECRAFT_WORLD_LEVEL__Level_H__

#include <set>
#include <string>
#include <cstdint>

#include "LevelSettings.h"
#include "LevelConstants.h"
#include "ChunkPos.h"
#include "TickNextTickData.h"
#include "storage/LevelData.h"
#include "LightUpdate.h"
#include "LevelSource.h"
#include "../Pos.h"
#include "../../SharedConstants.h"

#include "biome/Biome.h"

class BiomeSource;
class Dimension;
class ChunkSource;
class LevelStorage;
class LightLayer;
class Path;
class TripodCamera;
class Player;
class PathFinder;
class LevelListener;
class IRakNetInstance;
class Zombie;
class TileEntity;

typedef std::vector<Entity*> EntityList;
typedef std::map<int, Entity*> EntityMap;
typedef std::vector<TileEntity*> TileEntityList;
typedef std::vector<Player*> PlayerList;
typedef std::vector<LevelListener*> ListenerList;
typedef std::set<ChunkPos> ChunkPosSet;
typedef std::set<TickNextTickData> TickDataSet;
typedef std::vector<LightUpdate> LightUpdateList;

typedef struct PRInfo {
    PRInfo(Entity* e, int ticks): e(e), ticks(ticks) {}
    Entity* e;
    int ticks;
} PRInfo;
typedef std::vector<PRInfo> PendingList;

typedef struct AdventureSettings {
    AdventureSettings();
    bool doTickTime;
    bool noPvP;
    bool noPvM;
    bool noMvP;
    bool immutableWorld;
    bool showNameTags;
} AdventureSettings;

class Level: public LevelSource
{
    static const int MAX_TICK_TILES_PER_TICK = 100;

public:
    static const int MAX_LEVEL_SIZE = 32000000;
    static const short DEPTH = LEVEL_HEIGHT;
    static const short SEA_LEVEL = DEPTH / 2 - 1;

    static const int MAX_BRIGHTNESS = 15;
    static const int TICKS_PER_DAY = SharedConstants::TicksPerSecond * 60 * 16;
    static const int MIDDLE_OF_NIGHT_TIME = 12000;
    static const int genDepthBits = 7;
    static const int genDepthBitsPlusFour = genDepthBits + 4;
    static const int genDepth = 1 << genDepthBits;
    static const int genDepthMinusOne = genDepth - 1;

    Level(LevelStorage* levelStorage, const std::string& levelName, const LevelSettings& settings, int generatorVersion, Dimension* fixedDimension = NULL);
    virtual ~Level();

    void _init(const std::string& levelName, const LevelSettings& settings, int levelVersion, Dimension* fixedDimension);
    void validateSpawn();
    int getTopTile(int64_t x, int64_t z);
    int getTopTileY(int64_t x, int64_t z);
    int getTopSolidBlock(int64_t x, int64_t z);
    bool isEmptyTile(int64_t x, int y, int64_t z);
    bool isSolidRenderTile(int64_t x, int y, int64_t z);
    bool isSolidBlockingTile(int64_t x, int y, int64_t z);
    int getTile(int64_t x, int y, int64_t z);
    bool setTile(int64_t x, int y, int64_t z, int tile);
    bool hasChunkAt(int64_t x, int64_t y, int64_t z);
    bool hasChunksAt(int x, int y, int z, int r);  // 保留局部坐标版本
    bool hasChunksAt(int64_t x0, int64_t y0, int64_t z0, int64_t x1, int64_t y1, int64_t z1);
    bool hasChunk(int64_t x, int64_t z);
    LevelChunk* getChunkAt(int64_t x, int64_t z);
    LevelChunk* getChunk(int64_t x, int64_t z);
    bool setTileAndDataNoUpdate(int64_t x, int y, int64_t z, int tile, int data);
    bool setTileNoUpdate(int64_t x, int y, int64_t z, int tile);
    int getData(int64_t x, int y, int64_t z);
    void setData(int64_t x, int y, int64_t z, int data);
    bool setDataNoUpdate(int64_t x, int y, int64_t z, int data);
    bool setTileAndData(int64_t x, int y, int64_t z, int tile, int data);
    void sendTileUpdated(int64_t x, int y, int64_t z);
    void lightColumnChanged(int64_t x, int64_t z, int y0, int y1);
    void setTileDirty(int64_t x, int y, int64_t z);
    void setTilesDirty(int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1);
    const Material* getMaterial(int64_t x, int y, int64_t z);
    void loadPlayer(Player* player, bool doAddPlayer);
    void swap(int64_t x1, int y1, int64_t z1, int64_t x2, int y2, int64_t z2);
    void updateNeighborsAt(int64_t x, int y, int64_t z, int tile);
    int getHeightmap(int64_t x, int64_t z);
    BiomeSource* getBiomeSource();
    Biome* getBiome(int64_t x, int64_t z);
    int getRawBrightness(int64_t x, int y, int64_t z);
    int getRawBrightness(int64_t x, int y, int64_t z, bool propagate);
    float getBrightness(int64_t x, int y, int64_t z);
    int getBrightness(const LightLayer& layer, int64_t x, int y, int64_t z);
    void setBrightness(const LightLayer& layer, int64_t x, int y, int64_t z, int brightness);
    void updateLightIfOtherThan(const LightLayer& layer, int64_t x, int y, int64_t z, int expected);
    int getLightsToUpdate();
    bool updateLights();
    void setUpdateLights(bool doUpdate);
    void updateLight(const LightLayer& layer, int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1);
    void updateLight(const LightLayer& layer, int64_t x0, int y0, int64_t z0, int64_t x1, int y1, int64_t z1, bool join);
    HitResult clip(const Vec3& a, const Vec3& b, bool liquid = false, bool solidOnly = false);
    bool addEntity(Entity* e);
    void removeEntity(Entity* e);
    void removePlayer(Player* player);
    Entity* getEntity(int entityId);
    Mob* getMob(int entityId);
    Biome::MobSpawnerData getRandomMobSpawnAt(const MobCategory& mobCategory, int x, int y, int z);
    void addListener(LevelListener* listener);
    void removeListener(LevelListener* listener);
    void dispatchEntityData(Entity* e);
    std::vector<AABB> boxes;
    std::vector<AABB>& getCubes(const Entity* source, const AABB& box);
    bool isDay();
    float getTimeOfDay(float a);
    float getSunAngle(float a);
    int getSkyDarken(float a);
    Vec3 getFogColor(float a);
    Vec3 getCloudColor(float a);
    Vec3 getSkyColor(Entity* source, float a);
    bool canSeeSky(int64_t x, int y, int64_t z);
    int getLightDepth(int64_t x, int64_t z);
    float getStarBrightness(float a);
    bool updateSkyBrightness();
    bool isSkyLit(int64_t x, int y, int64_t z);
    void tickEntities();
    virtual void tick();
    void tick(Entity* e);
    void tick(Entity* e, bool actual);
    bool tickPendingTicks(bool force);
    void animateTick(int64_t xt, int yt, int64_t zt);
    void addToTickNextTick(int64_t x, int y, int64_t z, int tileId, int tickDelay);
    bool isUnobstructed(const AABB& aabb);
    float getSeenPercent(const Vec3& center, const AABB& bb);
    void explode(Entity* source, float x, float y, float z, float r);
    void explode(Entity* source, float x, float y, float z, float r, bool fire);
    bool containsAnyLiquid(const AABB& box);
    bool containsFireTile(const AABB& box);
    bool containsMaterial(const AABB& box, const Material* material);
    bool containsLiquid(const AABB& box, const Material* material);
    bool checkAndHandleWater(const AABB& box, const Material* material, Entity* e);
    void extinguishFire(int64_t x, int y, int64_t z, int face);
    EntityList& getEntities(Entity* except, const AABB& bb);
    const EntityList& getAllEntities();
    TileEntity* getTileEntity(int64_t x, int y, int64_t z);
    void setTileEntity(int64_t x, int y, int64_t z, TileEntity* tileEntity);
    void removeTileEntity(int64_t x, int y, int64_t z);
    void tileEntityChanged(int64_t x, int y, int64_t z, TileEntity* te);
    void prepare();
    bool isNew() { return _isNew; }
    int getSeaLevel();
    bool mayPlace(int tileId, int64_t x, int y, int64_t z, bool ignoreEntities, unsigned char face);
    bool mayInteract(Player* player, int64_t xt, int yt, int64_t zt);
    bool findPath(Path* path, Entity* from, Entity* to, float maxDist, bool canOpenDoors, bool avoidWater);
    bool findPath(Path* path, Entity* from, int xBest, int yBest, int zBest, float maxDist, bool canOpenDoors, bool avoidWater);
    bool getDirectSignal(int64_t x, int y, int64_t z, int dir);
    bool hasDirectSignal(int64_t x, int y, int64_t z);
    bool getSignal(int64_t x, int y, int64_t z, int dir);
    bool hasNeighborSignal(int64_t x, int y, int64_t z);
    Player* getNearestPlayer(Entity* source, float maxDist);
    Player* getNearestPlayer(float x, float y, float z, float maxDist);
    int getEntitiesOfType(int entityType, const AABB& bb, EntityList& list);
    int getEntitiesOfClass(int type, const AABB& bb, EntityList& list);
    int countInstanceOfType(int typeId);
    int countInstanceOfBaseType(int baseTypeId);
    virtual void updateSleepingPlayerList() {}
    long getTime();
    void setTime(long time);
    long getSeed();
    Pos getSharedSpawnPos();
    void setSpawnPos(Pos spawnPos);
    void setSpawnSettings(bool spawnEnemies, bool spawnFriendlies);
    ChunkSource* getChunkSource();
    LevelStorage* getLevelStorage();
    LevelData* getLevelData();
    void saveLevelData();
    void saveGame();
    void loadEntities();
    void addParticle(const std::string& id, float x, float y, float z, float xd, float yd, float zd, int data = 0);
    void addParticle(ParticleType::Id id, float x, float y, float z, float xd, float yd, float zd, int data = 0);
    void playSound(Entity* entity, const std::string& name, float volume, float pitch);
    void playSound(float x, float y, float z, const std::string& name, float volume, float pitch);
    void levelEvent(Player* source, int type, int64_t x, int y, int64_t z, int data);
    void tileEvent(int64_t x, int y, int64_t z, int b0, int b1);
    void broadcastEntityEvent(Entity* e, char eventId);
    void awakenAllPlayers();
    void takePicture(TripodCamera* cam, Entity* e);
    virtual void setInitialSpawn();
    bool inRange(int64_t x, int y, int64_t z);
    float getCelestialAngle(float a) {
        return getSunAngle(a);  // 这个函数已经存在
    }

protected:
    void setZombieAi(std::vector<Zombie*>& zombies);
    virtual void entityAdded(Entity* e);
    virtual void entityRemoved(Entity* e);
    virtual void tileUpdated(int64_t x, int y, int64_t z, int tile);
    void updateSkyDarken();
    virtual ChunkSource* createChunkSource();

private:
    void neighborChanged(int64_t x, int y, int64_t z, int type);
    void tickTiles();

public:
    bool instaTick;
    bool isClientSide;
    bool isGeneratingTerrain;
    int difficulty;
    AdventureSettings adventureSettings;
    int isNightMode();
    void setNightMode(bool isNightMode);
    EntityList entities;
    EntityMap entityIdLookup;
    TileEntityList tileEntities;
    TileEntityList pendingTileEntities;
    bool updatingTileEntities;
    PlayerList players;
    int skyDarken;
    bool noNeighborUpdate;
    Dimension* dimension;
    IRakNetInstance* raknetInstance;
    Random random;

protected:
    bool isFindingSpawn;
    ListenerList _listeners;
    ChunkSource* _chunkSource;
    LevelStorage* const levelStorage;
    LevelData levelData;
    bool allPlayersAreSleeping;
    bool _spawnFriendlies;
    bool _spawnEnemies;
    int _randValue;
    int _addend;

private:
    EntityList  _es;
    TickDataSet _tickNextTickSet;
    ChunkPosSet _chunksToPoll;
    LightUpdateList _lightUpdates;
    EntityMap _pendingEntityData;
    bool _updateLights;
    int  _maxRecurse;
    bool _isNew;
    bool _nightMode;
    PathFinder* _pathFinder;
    float _lastSavedPlayerTime;
    PendingList _pendingPlayerRemovals;
};

#endif
