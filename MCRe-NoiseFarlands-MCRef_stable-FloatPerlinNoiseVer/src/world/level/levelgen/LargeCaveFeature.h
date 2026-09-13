#ifndef NET_MINECRAFT_WORLD_LEVEL_LEVELGEN__LargeCaveFeature_H__
#define NET_MINECRAFT_WORLD_LEVEL_LEVELGEN__LargeCaveFeature_H__

//package net.minecraft.world.level.levelgen;

#include "../../../util/Random.h"
#include "../../../util/Mth.h"

#include "LargeFeature.h"

#include "../Level.h"
#include "../tile/Tile.h"
#include "../tile/GrassTile.h"

class LargeCaveFeature: public LargeFeature {
protected:
    void addRoom(int xOffs, int zOffs, unsigned char* blocks, float xRoom, float yRoom, float zRoom);
    void addTunnel(int xOffs, int zOffs, unsigned char* blocks, float xCave, float yCave, float zCave,
                   float thickness, float yRot, float xRot, int step, int dist, float yScale);
    void addFeature(Level* level, int x, int z, int xOffs, int zOffs,
                    unsigned char* blocks, int blocksSize) override;
};
#endif
