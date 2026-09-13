#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__WebTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__WebTile_H__

//package net.minecraft.world.level.tile;

#include "../../entity/Entity.h"
#include "../../item/Item.h"
#include "../Level.h"
#include "../material/Material.h"
#include "../../phys/AABB.h"

class WebTile: public Tile
{
    typedef Tile super;
public:
    WebTile(int id, int tex)
	:	super(id, tex, Material::web)
	{
    }

	int getRenderLayer(){
		return RENDERLAYER_ALPHATEST;
	}

    /*@Override*/
    void entityInside(Level* level, int64_t x, int64_t y, int64_t z, Entity* entity) {
        entity->makeStuckInWeb();
    }

    /*@Override*/
    bool isSolidRender() {
        return false;
    }

    /*@Override*/
    AABB* getAABB(Level* level, int64_t x, int64_t y, int64_t z) {
        return NULL;
    }

    /*@Override*/
    int getRenderShape() {
        return Tile::SHAPE_CROSS_TEXTURE;
    }

    bool blocksLight() {
        return false;
    }

    bool isCubeShaped() {
        return false;
    }

    /*@Override*/
    int getResource(int data, Random* random) {
        return Item::string->id;
    }
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__WebTile_H__*/
