#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__StoneSlabTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__StoneSlabTile_H__

//package net.minecraft.world.level.tile;

#include "Tile.h"

class Level;
class LevelSource;

class StoneSlabTile: public Tile
{
	typedef Tile super;
public:
    static const int STONE_SLAB = 0;
    static const int SAND_SLAB  = 1;
    static const int WOOD_SLAB  = 2;
    static const int COBBLESTONE_SLAB = 3;
	static const int BRICK_SLAB = 4;
	static const int SMOOTHBRICK_SLAB = 5;

	static const int TYPE_MASK = 7;
	static const int TOP_SLOT_BIT = 8;


    static const std::string SLAB_NAMES[];
	static const int SLAB_NAMES_COUNT;

    StoneSlabTile(int id, bool fullSize);

	int getTexture(int face, int data);
    int getTexture(int face);

    bool isSolidRender();
    bool isCubeShaped();

	void updateShape(LevelSource* level, int64_t x, int64_t y, int64_t z);
	void updateDefaultShape();

	void addAABBs( Level* level, int64_t x, int64_t y, int64_t z, const AABB* box, std::vector<AABB>& boxes );

	//void onPlace(Level* level, int64_t x, int64_t y, int64_t z);
	int getPlacedOnFaceDataValue(Level* level, int64_t x, int64_t y, int64_t z, int face, float clickX, float clickY, float clickZ, int itemValue);

	int getResource(int data, Random* random);
    int getResourceCount(Random* random);

    bool shouldRenderFace(LevelSource* level, int64_t x, int64_t y, int64_t z, int face);

protected:
    int getSpawnResourcesAuxValue(int data);
private:
	bool fullSize;
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__StoneSlabTile_H__*/
