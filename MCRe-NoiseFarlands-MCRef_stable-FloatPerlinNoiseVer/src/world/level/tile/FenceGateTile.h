#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__FenceGateTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__FenceGateTile_H__

#include "Tile.h"
#include "LevelEvent.h"
#include "../Level.h"
#include "../material/Material.h"
#include "../../Direction.h"
#include "../../entity/Mob.h"
#include "../../entity/player/Player.h"
#include "../../phys/AABB.h"
#include "../../../util/Mth.h"

class FenceGateTile: public Tile
{
    typedef Tile super;

    static const int DIRECTION_MASK = 3;
    static const int OPEN_BIT = 4;
public:
    FenceGateTile(int id, int tex)
    :   super(id, tex, Material::wood)
    {
    }

    bool mayPlace(Level* level, int64_t x, int64_t y, int64_t z) {
        if (!level->getMaterial(x, y - 1, z)->isSolid()) return false;
        return super::mayPlace(level, x, y, z);
    }

    /*@Override*/
    AABB* getAABB(Level* level, int64_t x, int64_t y, int64_t z) {
        int data = level->getData(x, y, z);
        if (isOpen(data)) {
            return NULL;
        }

        const float xx = (float)x;
        const float yy = (float)y;
        const float zz = (float)z;
        if (data == Direction::NORTH || data == Direction::SOUTH) {
            tmpBB.set(xx, yy, zz + 6.0f / 16.0f, xx + 1, yy + 1.5f, zz + 10.0f / 16.0f);
        } else {
            tmpBB.set(xx + 6.0f / 16.0f, yy, zz, xx + 10.0f / 16.0f, yy + 1.5f, zz + 1);
        }
		return &tmpBB;
    }

    bool blocksLight() {
        return false;
    }

    bool isSolidRender() {
        return false;
    }

    bool isCubeShaped() {
        return false;
    }

    int getRenderShape() {
        return Tile::SHAPE_FENCE_GATE;
    }

    /*@Override*/
    void setPlacedBy(Level* level, int64_t x, int64_t y, int64_t z, Mob* by) {
        int dir = (((Mth::floor64(by->yRot * 4 / (360) + 0.5f)) & 3)) % 4;
        level->setData(x, y, z, dir);
    }

    /*@Override*/
    bool use(Level* level, int64_t x, int64_t y, int64_t z, Player* player) {
        int data = level->getData(x, y, z);
        if (isOpen(data)) {
            level->setData(x, y, z, data & ~OPEN_BIT);
        } else {
            // open the door from the player
            int dir = (((Mth::floor64(player->yRot * 4 / (360) + 0.5f)) & 3)) % 4;
            int current = getDirection(data);
            if (current == ((dir + 2) % 4)) {
                data = dir;
            }
            level->setData(x, y, z, data | OPEN_BIT);
        }
        level->levelEvent(player, LevelEvent::SOUND_OPEN_DOOR, x, y, z, 0);
        return true;
    }

    static bool isOpen(int data) {
        return (data & OPEN_BIT) != 0;
    }

    static int getDirection(int data) {
        return (data & DIRECTION_MASK);
    }
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_TILE__FenceGateTile_H__*/
