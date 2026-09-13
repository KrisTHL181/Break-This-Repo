#ifndef NET_MINECRAFT_WORLD_LEVEL_LEVELGEN_SYNTH__PerlinNoise_H__
#define NET_MINECRAFT_WORLD_LEVEL_LEVELGEN_SYNTH__PerlinNoise_H__

//package net.minecraft.world.level.levelgen.synth;

#include "../../../../util/Random.h"
#include "Synth.h"

class ImprovedNoise;

class PerlinNoise: public Synth
{
public:
    PerlinNoise(int levels);
    PerlinNoise(Random* random, int levels);
	~PerlinNoise();

    float getValue(float x, float y) const;
    float getValue(float x, float y, float z) const;
    float getValue(double x, double y, double z) const;

    // 新增：非 const 版本，满足基类 Synth 纯虚函数
    inline float getValue(float x, float y) override {
        return static_cast<const PerlinNoise*>(this)->getValue(x, y);
    }
    inline float getValue(float x, float y, float z) {
        return static_cast<const PerlinNoise*>(this)->getValue(x, y, z);
    }
    inline float getValue(double x, double y, double z) {
        return static_cast<const PerlinNoise*>(this)->getValue(x, y, z);
	}

	//float[] getRegion(float[] buffer, float x, float y, float z, int xSize, int ySize, int zSize, float xScale, float yScale, float zScale) {
    float* getRegion(float* buffer, float x, float y, float z, int xSize, int ySize, int zSize, float xScale, float yScale, float zScale);
    float* getRegion(float* sr, int x, int z, int xSize, int zSize, float xScale, float zScale, float pow);

    int hashCode();

private:
    ImprovedNoise** noiseLevels;
    int levels;

    Random _random;
    Random* _rndPtr;

    void init(int levels);
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_LEVELGEN_SYNTH__PerlinNoise_H__*/
