#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__TrapDoorTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__TrapDoorTile_H__

#include "Tile.h"
class TrapDoorTile : public Tile {
	typedef Tile super;

public:
	TrapDoorTile(int id, const Material* material);
	bool blocksLight();
	bool isSolidRender();
	bool isCubeShaped();
	int getRenderLayer();
	//@Override
	//	public boolean isPathfindable(LevelSource level, int64_t x, int64_t y, int64_t z) {
	//		return !isOpen(level.getData(x, y, z));
	//}
	int getRenderShape();
	AABB getTileAABB(Level* level, int64_t x, int64_t y, int64_t z);
	AABB* getAABB(Level* level, int64_t x, int64_t y, int64_t z);
	void updateShape(LevelSource* level, int64_t x, int64_t y, int64_t z);
	void updateDefaultShape();
	void setShape(int data);
	void attack(Level* level, int64_t x, int64_t y, int64_t z, Player* player);
	bool use(Level* level, int64_t x, int64_t y, int64_t z, Player* player);
	void setOpen(Level* level, int64_t x, int64_t y, int64_t z, bool shouldOpen);
	void neighborChanged(Level* level, int64_t x, int64_t y, int64_t z, int type);
	HitResult clip(Level* level, int xt, int yt, int zt, const Vec3& a, const Vec3& b);
	int getDir(int dir);
	int getPlacedOnFaceDataValue(Level* level, int64_t x, int64_t y, int64_t z, int face, float clickX, float clickY, float clickZ, int itemValue);
	bool mayPlace(Level* level, int64_t x, int64_t y, int64_t z, unsigned char face);
	static bool isOpen(int data);
	static bool attachesTo(int id);
};

#endif /* NET_MINECRAFT_WORLD_LEVEL_TILE__TrapDoorTile_H__ */