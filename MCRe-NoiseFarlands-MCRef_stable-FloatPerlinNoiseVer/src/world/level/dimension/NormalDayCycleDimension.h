#ifndef NET_MINECRAFT_WORLD_LEVEL_DIMENSION__NormalDayCycleDimension_H__
#define NET_MINECRAFT_WORLD_LEVEL_DIMENSION__NormalDayCycleDimension_H__

//package net.minecraft.world.level.dimension;

#include "Dimension.h"
#include "../Level.h"
#include "../../../util/Mth.h"

class NormalDayCycleDimension: public Dimension {
public:
	float getTimeOfDay(long time, float a) {
		int dayStep = (int) (time % Level::TICKS_PER_DAY);
		float td = (dayStep + a) / Level::TICKS_PER_DAY - 0.25f;
		if (td < 0) td += 1;
		if (td > 1) td -= 1;
		float tdo = td;
		td = 1 - (cos(td * Mth::PI) + 1) * 0.5f;
		return tdo + (td - tdo) / 3.0f;
	}

	Vec3 getFogColor( float td, float a ) {
    // Beta 1.7.3 雾色：偏淡蓝
    float r = 0.75f, g = 0.82f, b = 0.95f;
    float br = Mth::cos(td * Mth::PI * 2) * 0.2f + 0.8f;  // 白天稍亮，夜晚稍暗
    r *= br; g *= br; b *= br;
    return Vec3(r, g, b);
	}
	
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_DIMENSION__NormalDayCycleDimension_H__*/
