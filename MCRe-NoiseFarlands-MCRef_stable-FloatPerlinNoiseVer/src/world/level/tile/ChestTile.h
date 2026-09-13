#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__ChestTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__ChestTile_H__

#include "EntityTile.h"
class Level;
class LevelSource;
class Mob;

#include "../../../util/Random.h"

//package net.minecraft.world.level->tile;

class ChestTile: public EntityTile
{
	typedef EntityTile super;
public:
    static const int EVENT_SET_OPEN_COUNT = 1;

    ChestTile(int id);

    bool isSolidRender();

    /*@Override*/
    bool isCubeShaped();

    int getRenderShape();

	bool mayPlace(Level* level, int64_t x, int64_t y, int64_t z, unsigned char face);
	void setPlacedBy(Level* level, int64_t x, int64_t y, int64_t z, Mob* by);

	void onPlace(Level* level, int64_t x, int64_t y, int64_t z);
	void onRemove(Level* level, int64_t x, int64_t y, int64_t z);

    void recalcLockDir(Level* level, int64_t x, int64_t y, int64_t z);

    int getTexture(LevelSource* level, int64_t x, int64_t y, int64_t z, int face);
    int getTexture(int face);

	void neighborChanged(Level* level, int64_t x, int64_t y, int64_t z, int type);

    bool use(Level* level, int64_t x, int64_t y, int64_t z, Player* player);

    TileEntity* newTileEntity();

private:
	bool isFullChest(Level* level, int64_t x, int64_t y, int64_t z);

	Random random;
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__ChestTile_H__*/
