#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__Mushroom_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__Mushroom_H__

#include "Bush.h"

class Mushroom : public Bush
{
	typedef Bush super;
public:
	Mushroom(int id, int tex);

    void tick(Level* level, int64_t x, int64_t y, int64_t z, Random* random);

    bool mayPlace(Level* level, int64_t x, int64_t y, int64_t z, unsigned char face);
	bool mayPlaceOn(int tile);

    bool canSurvive(Level* level, int64_t x, int64_t y, int64_t z);
};

#endif /* NET_MINECRAFT_WORLD_LEVEL_TILE__Mushroom_H__ */