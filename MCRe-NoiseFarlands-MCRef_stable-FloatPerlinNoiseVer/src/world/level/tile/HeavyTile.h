#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__HeavyTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__HeavyTile_H__

//package net.minecraft.world.level.tile;

#include "Tile.h"

class FallingTile;

class HeavyTile: public Tile {
    typedef Tile super;
public:
    static bool instaFall;

    HeavyTile(int id, int tex);
    HeavyTile(int id, int tex, const Material* material);

    void onPlace(Level* level, int64_t x, int64_t y, int64_t z);
    void neighborChanged(Level* level, int64_t x, int64_t y, int64_t z, int type);

    void tick(Level* level, int64_t x, int64_t y, int64_t z, Random* random);
    int getTickDelay(Level* level);

    static bool isFree(Level* level, int64_t x, int64_t y, int64_t z);

    virtual void falling(FallingTile* entity);
    virtual void onLand(Level* level, int xt, int yt, int zt, int data);
private:
    void checkSlide(Level* level, int64_t x, int64_t y, int64_t z);
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__HeavyTile_H__*/
