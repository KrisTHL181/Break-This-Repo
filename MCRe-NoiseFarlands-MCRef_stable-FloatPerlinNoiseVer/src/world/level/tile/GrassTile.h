#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__GrassTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__GrassTile_H__

//package net.minecraft.world.level.tile;

#include "../../../util/Random.h"
#include "../material/Material.h"
#include "../Level.h"
#include "../LevelSource.h"
#include "../FoliageColor.h"

#include "Tile.h"

class GrassTile: public Tile
{
	typedef Tile super;
public:
	static const int MIN_BRIGHTNESS = 4;

	GrassTile(int id);

	int getTexture(LevelSource* level, int64_t x, int64_t y, int64_t z, int face);
	int getTexture(int face, int data);
    int getColor(LevelSource* level, int64_t x, int64_t y, int64_t z) {
    return FoliageColor::getDefaultColor(); // 与树叶、高草颜色一致
	}

    void tick(Level* level, int64_t x, int64_t y, int64_t z, Random* random);
    int getResource(int data, Random* random);
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__GrassTile_H__*/
