#ifndef NET_MINECRAFT_CLIENT_RENDERER__TileRenderer_H__
#define NET_MINECRAFT_CLIENT_RENDERER__TileRenderer_H__

//package net.minecraft.client.renderer;
#include <cstdint>

class Tile;
class FenceTile;
class FenceGateTile;
class ThinFenceTile;
class StairTile;
class LevelSource;
class Material;

class TileRenderer
{
public:
    TileRenderer(LevelSource* level = 0);

    static bool stripeRepairEnabled;

    void tesselateInWorld(Tile* tile, int64_t x, int64_t y, int64_t z, int64_t fixedTexture);
    bool tesselateInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
	void tesselateInWorldNoCulling(Tile* tile, int64_t x, int64_t y, int64_t z);

	bool tesselateTorchInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
    bool tesselateLadderInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
    bool tesselateCactusInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
    bool tesselateCactusInWorld(Tile* tt, int64_t x, int64_t y, int64_t z, float r, float g, float b);
	bool tesselateCrossInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateStemInWorld(Tile* _tt, int64_t x, int64_t y, int64_t z);
	// 文件：src/client/renderer/TileRenderer.h
    // ...
    bool tesselateWaterInWorld( Tile* tt, int64_t x, int64_t y, int64_t z );   // ★ 改为 int64_t
    // ...
	bool tesselateStairsInWorld(StairTile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateDoorInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateFenceInWorld(FenceTile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateThinFenceInWorld(ThinFenceTile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateFenceGateInWorld(FenceGateTile* tt, int64_t x, int64_t y, int64_t z);
	bool tesselateBedInWorld(Tile *tt, int64_t x, int64_t y, int64_t z);
	bool tesselateRowInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);

    void tesselateTorch(Tile* tt, float x, float y, float z, float xxa, float zza);
	void tesselateCrossTexture(Tile* tt, int64_t data, float x, float y, float z);
	void tesselateStemTexture(Tile* tt, int64_t data, float h, float x, float y, float z);
	void tesselateStemDirTexture(Tile* tt, int64_t data, int64_t dir, float h, float x, float y, float z);
	void tesselateRowTexture(Tile* tt, int64_t data, float x, float y, float z);

    void renderBlock(Tile* tt, LevelSource* level, int64_t x, int64_t y, int64_t z);

    /*public*/
	bool tesselateBlockInWorld(Tile* tt, int64_t x, int64_t y, int64_t z);
    bool tesselateBlockInWorld(Tile* tt, int64_t x, int64_t y, int64_t z, float r, float g, float b);
	bool tesselateBlockInWorldWithAmbienceOcclusion(Tile* tt, int64_t pX, int64_t pY, int64_t pZ, float pBaseRed, float pBaseGreen, float pBaseBlue);

    void renderFaceDown(Tile* tt, float x, float y, float z, int64_t tex);
    void renderFaceUp(Tile* tt, float x, float y, float z, int64_t tex);
    void renderNorth(Tile* tt, float x, float y, float z, int64_t tex);
    void renderSouth(Tile* tt, float x, float y, float z, int64_t tex);
    void renderWest(Tile* tt, float x, float y, float z, int64_t tex);
    void renderEast(Tile* tt, float x, float y, float z, int64_t tex);

    void renderTile(Tile* tile, int64_t data);
	void renderGuiTile(Tile* tile, int64_t data);

    static bool canRender(int64_t renderShape);
private:
    float getWaterHeight( int64_t x, int64_t y, int64_t z, const Material* m );

    LevelSource* level;
	int64_t fixedTexture;
	bool xFlipTexture;
	bool noCulling;

	bool applyAmbienceOcclusion;
	float ll000, llx00, ll0y0, ll00z, llX00, ll0Y0, ll00Z;
	float llxyz, llxy0, llxyZ, ll0yz, ll0yZ, llXyz, llXy0;
	float llXyZ, llxYz, llxY0, llxYZ, ll0Yz, llXYz, llXY0;
	float ll0YZ, llXYZ, llx0z, llX0z, llx0Z, llX0Z;
	int64_t blsmooth;
	float c1r, c2r, c3r, c4r;
	float c1g, c2g, c3g, c4g;
	float c1b, c2b, c3b, c4b;
	bool llTrans0Yz, llTransXY0, llTransxY0, llTrans0YZ;
	bool llTransx0z, llTransX0Z, llTransx0Z, llTransX0z;
	bool llTrans0yz, llTransXy0, llTransxy0, llTrans0yZ;
};


#endif /*NET_MINECRAFT_CLIENT_RENDERER__TileRenderer_H__*/
