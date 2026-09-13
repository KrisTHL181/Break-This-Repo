#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__StemTile_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__StemTile_H__
#include "Bush.h"
class StemTile : public Bush {
	typedef Bush super;
public:
	StemTile(int id, Tile* fruit);
	bool mayPlaceOn(int tile);
	void tick(Level* level, int64_t x, int64_t y, int64_t z, Random* random);
	void growCropsToMax(Level* level, int64_t x, int64_t y, int64_t z);
	int getColor(int data);
	int getColor(LevelSource* level, int64_t x, int64_t y, int64_t z);
	int getTexture(int face, int data);
	void updateDefaultShape();
	void updateShape(LevelSource* level, int64_t x, int64_t y, int64_t z);
	int getRenderShape();
	int getConnectDir(LevelSource* level, int64_t x, int64_t y, int64_t z);
	void spawnResources(Level* level, int64_t x, int64_t y, int64_t z, int data, float odds);
	int getResource(int data, Random* random);
	int getResourceCount(Random* random);
private:
	float getGrowthSpeed(Level* level, int64_t x, int64_t y, int64_t z);

private:
	Tile* fruit;
};

#endif /* NET_MINECRAFT_WORLD_LEVEL_TILE__StemTile_H__ */