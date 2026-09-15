#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__EndStoneTile_H__
#define NET_MINECRAFT_WORLD_LEVEL_TILE__EndStoneTile_H__

#include "Tile.h"
#include "../material/Material.h"

class EndStoneTile : public Tile {
    typedef Tile super;
public:
    EndStoneTile(int id, int tex)
        : super(id, tex, Material::stone)
    {
        setDestroyTime(3.0f);
        setExplodeable(45.0f);
        setSoundType(Tile::SOUND_STONE);
    }
};

#endif
