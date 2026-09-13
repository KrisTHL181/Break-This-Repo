#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__FurnaceTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__FurnaceTile_H__

//package net.minecraft.world.level->tile;

#include "EntityTile.h"
#include "../../../util/Random.h"

class Level;
class Mob;
class Player;
class LevelSource;

class FurnaceTile: public EntityTile
{
    typedef EntityTile super;
public:
    FurnaceTile(int id, bool lit);

	int getTexture(int face);
    int getTexture(LevelSource* level, int64_t x, int64_t y, int64_t z, int face);

    void animateTick(Level* level, int xt, int yt, int zt, Random* random);

    bool use(Level* level, int64_t x, int64_t y, int64_t z, Player* player);
	int getResource(int data, Random* random/*, int playerBonusLevel*/);

    static void setLit(bool lit, Level* level, int64_t x, int64_t y, int64_t z);

    TileEntity* newTileEntity();

    void setPlacedBy(Level* level, int64_t x, int64_t y, int64_t z, Mob* by);

	void onPlace(Level* level, int64_t x, int64_t y, int64_t z);
    void onRemove(Level* level, int64_t x, int64_t y, int64_t z);
private:
    void recalcLockDir(Level* level, int64_t x, int64_t y, int64_t z);

    Random random;
    const bool lit;
    static bool noDrop;
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__FurnaceTile_H__*/
