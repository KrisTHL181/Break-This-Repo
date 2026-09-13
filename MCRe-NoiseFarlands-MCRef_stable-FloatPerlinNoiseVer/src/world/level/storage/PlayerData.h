#ifndef NET_MINECRAFT_WORLD_LEVEL_STORAGE__PlayerData_H__
#define NET_MINECRAFT_WORLD_LEVEL_STORAGE__PlayerData_H__

#include "../../entity/player/Player.h"
#include "../../entity/player/Inventory.h"
#include "../../phys/Vec3.h"
#include "../../../util/Mth.h"

static float clampRot(float r) {
    return Mth::clamp(r, -100000.0f, 100000.0f);
}
// 无限世界：移除坐标边界限制，直接返回原值
static double clampXZ(double r) { return r; }
static double clampY(double r)  { return r; }

class PlayerData {
public:
	void loadPlayer(Player* p) const {
    p->setPos(0, 0, 0);
    p->x = p->xo = p->xOld = clampXZ(pos.x);
    p->y = p->yo = p->yOld = clampY(pos.y);
    p->z = p->zo = p->zOld = clampXZ(pos.z);
    double motionX = motion.x, motionY = motion.y, motionZ = motion.z;
    if (Mth::abs(motionX) > 10.0) motionX = 0;
    if (Mth::abs(motionY) > 10.0) motionY = 0;
    if (Mth::abs(motionZ) > 10.0) motionZ = 0;
    p->xd = motionX;
    p->yd = motionY;
    p->zd = motionZ;
    p->xRot = p->xRotO = clampRot(xRot);
    p->yRot = p->yRotO = clampRot(yRot);
    p->fallDistance = fallDistance;
    p->onFire = onFire;
    p->airSupply = airSupply;
    p->onGround = onGround;
    p->setPos(pos.x, pos.y, pos.z);
	}

	Vec3 pos;
	Vec3 motion;
	float xRot, yRot;

	float fallDistance;
	short onFire;
	short airSupply;
	bool onGround;

	int inventorySlots[Inventory::MAX_SELECTION_SIZE];
};

#endif /*NET_MINECRAFT_WORLD_LEVEL_STORAGE__PlayerData_H__*/
