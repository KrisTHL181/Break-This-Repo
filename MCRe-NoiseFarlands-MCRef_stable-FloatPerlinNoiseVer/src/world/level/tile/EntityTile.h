#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__EntityTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__EntityTile_H__

//package net.minecraft.world.level->tile;

#include "Tile.h"

class Level;
class TileEntity;
class Material;

/*abstract*/
class EntityTile: public Tile
{
    typedef Tile super;
public:
    virtual TileEntity* newTileEntity() = 0;

    void onPlace(Level* level, int64_t x, int64_t y, int64_t z);
    void onRemove(Level* level, int64_t x, int64_t y, int64_t z);

    void triggerEvent(Level* level, int64_t x, int64_t y, int64_t z, int b0, int b1);
protected:
    EntityTile(int id, const Material* material);
    EntityTile(int id, int tex, const Material* material);
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__EntityTile_H__*/
