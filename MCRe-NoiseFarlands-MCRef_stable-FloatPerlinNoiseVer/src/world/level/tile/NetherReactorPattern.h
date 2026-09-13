#ifndef NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactorPattern_H__
#include <cstdint>
#define NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactorPattern_H__

class NetherReactorPattern {
public:
	NetherReactorPattern();
	unsigned int  getTileAt(int level, int64_t x, int64_t z);
private:
	void setTileAt(int level, int64_t x, int64_t z, int tile);
private:
	unsigned int pattern[3][3][3];
};

#endif /* NET_MINECRAFT_WORLD_LEVEL_TILE__NetherReactorPattern_H__ */
