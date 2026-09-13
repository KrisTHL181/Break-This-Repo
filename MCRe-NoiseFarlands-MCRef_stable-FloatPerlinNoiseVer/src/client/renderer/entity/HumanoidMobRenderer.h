#ifndef NET_MINECRAFT_CLIENT_RENDERER_ENTITY__HumanoidMobRenderer_H__
#define NET_MINECRAFT_CLIENT_RENDERER_ENTITY__HumanoidMobRenderer_H__

//package net.minecraft.client.renderer.entity;

#include "MobRenderer.h"

class HumanoidModel;
class Mob;

class HumanoidMobRenderer: public MobRenderer
{
	typedef MobRenderer super;
public:
    HumanoidMobRenderer(HumanoidModel* humanoidModel, float shadow);

	void renderHand();
	void render(Entity* mob_, float x, float y, float z, float rot, float a);
protected:
    void additionalRendering(Mob* mob, float a);

	HumanoidModel* humanoidModel;

	// Last rotation values for cape smoothing (reduces jitter)
	float lastCapeXRot;
	float lastCapeZRot;
private:
	// i guess ill keep this just in case seomthing breaks
};

#endif /*NET_MINECRAFT_CLIENT_RENDERER_ENTITY__HumanoidMobRenderer_H__*/
