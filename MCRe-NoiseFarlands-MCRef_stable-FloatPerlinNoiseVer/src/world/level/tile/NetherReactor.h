#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactor_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactor_H__

#include "EntityTile.h"

class Material;
class NetherReactor : public EntityTile {
	typedef EntityTile super;
public:
	int getTexture(int face, int data);
	NetherReactor(int id, int tex, const Material* material);
	bool use(Level* level, int64_t x, int64_t y, int64_t z, Player* player);
	static void setPhase(Level* level, int64_t x, int64_t y, int64_t z, int phase);
	TileEntity* newTileEntity();
	bool canSpawnStartNetherReactor( Level* level, int64_t x, int64_t y, int64_t z, Player* player );
	bool allPlayersCloseToReactor( Level* level, int64_t x, int64_t y, int64_t z);
};

#endif /* NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactor_H__ */
