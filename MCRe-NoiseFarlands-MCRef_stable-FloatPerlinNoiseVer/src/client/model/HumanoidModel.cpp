#include "HumanoidModel.h"
#include "../Minecraft.h"
#include "../../util/Mth.h"
#include "../../world/entity/player/Player.h"
#include "../../world/entity/player/Inventory.h"

HumanoidModel::HumanoidModel( float g /*= 0*/, float yOffset /*= 0*/, int texW /*= 64*/, int texH /*= 32*/ )
:    holdingLeftHand(false),
	holdingRightHand(false),
	sneaking(false),
	bowAndArrow(false),
	head(0, 0),
	hair(32, 0),
	//ear (24, 0),
	body(16, 16),
	arm0(24 + 16, 16),
	arm1(24 + 16, 16),
	leg0(0, 16),
	leg1(0, 16)
{
	texWidth = texW;
	texHeight = texH;

	head.setModel(this);
	hair.setModel(this);
	body.setModel(this);
	arm0.setModel(this);
	arm1.setModel(this);
	leg0.setModel(this);
	leg1.setModel(this);

	// If the texture is 64x64, use the modern skin layout for arms/legs and add overlay layers.
	bool modernSkin = (texWidth == 64 && texHeight == 64);
	if (modernSkin) {
		// Left arm and left leg are located in the bottom half of a 64x64 skin.
		arm1.texOffs(32, 48);
		leg1.texOffs(16, 48);
	}

	head.addBox(-4, -8, -4, 8, 8, 8, g); // Head
	head.setPos(0, 0 + yOffset, 0);

	if (modernSkin) {
		hair.addBox(-4, -8, -4, 8, 8, 8, g + 0.5f); // Outer head layer (hat)
		hair.setPos(0, 0 + yOffset, 0);
	}

	body.addBox(-4, 0, -2, 8, 12, 4, g); // Body
	body.setPos(0, 0 + yOffset, 0);

	arm0.addBox(-3, -2, -2, 4, 12, 4, g); // Arm0
	arm0.setPos(-5, 2 + yOffset, 0);

	arm1.mirror = true;
	arm1.addBox(-1, -2, -2, 4, 12, 4, g); // Arm1
	arm1.setPos(5, 2 + yOffset, 0);

	leg0.addBox(-2, 0, -2, 4, 12, 4, g); // Leg0
	leg0.setPos(-2, 12 + yOffset, 0);

	leg1.mirror = true;
	leg1.addBox(-2, 0, -2, 4, 12, 4, g); // Leg1
	leg1.setPos(2, 12 + yOffset, 0);

	if (modernSkin) {
		// Overlay layers for 64x64 skins (same geometry, different texture regions)
		body.texOffs(16, 32);
		body.addBox(-4, 0, -2, 8, 12, 4, g + 0.5f);

		arm0.texOffs(40, 32);
		arm0.addBox(-3, -2, -2, 4, 12, 4, g + 0.5f);

		arm1.texOffs(48, 48);
		arm1.addBox(-1, -2, -2, 4, 12, 4, g + 0.5f);

		leg0.texOffs(0, 32);
		leg0.addBox(-2, 0, -2, 4, 12, 4, g + 0.5f);

		leg1.texOffs(0, 48);
		leg1.addBox(-2, 0, -2, 4, 12, 4, g + 0.5f);
	}
}

void HumanoidModel::render(Entity* e, float time, float r, float bob, float yRot, float xRot, float scale )
{
	if(e != NULL && e->isMob()) {		
		Mob* mob = (Mob*)(e);
		ItemInstance* item = mob->getCarriedItem();
		if(item != NULL) {
			if(mob->getUseItemDuration() >  0) {
				UseAnim::UseAnimation anim = item->getUseAnimation();
				if(anim == UseAnim::bow) {
					bowAndArrow = true;
				}
			}
		}
	}
	
	setupAnim(time, r, bob, yRot, xRot, scale);
	
	// Sync overlay with head rotation/position
	if (texWidth == 64 && texHeight == 64) {
		hair.xRot = head.xRot;
		hair.yRot = head.yRot;
		hair.zRot = head.zRot;
		hair.y = head.y;
	}

	head.render(scale);
	if (texWidth == 64 && texHeight == 64) {
		hair.render(scale);
	}
	body.render(scale);
	arm0.render(scale);
	arm1.render(scale);
	leg0.render(scale);
	leg1.render(scale);
	bowAndArrow = false;
}

void HumanoidModel::render( HumanoidModel* model, float scale )
{
	head.yRot = model->head.yRot;
	head.y = model->head.y;
	head.xRot = model->head.xRot;
	if (texWidth == 64 && texHeight == 64) {
		hair.yRot = head.yRot;
		hair.xRot = head.xRot;
		hair.zRot = head.zRot;
		hair.y = head.y;
	}

	arm0.xRot = model->arm0.xRot;
	arm0.zRot = model->arm0.zRot;

	arm1.xRot = model->arm1.xRot;
	arm1.zRot = model->arm1.zRot;

	leg0.xRot = model->leg0.xRot;
	leg1.xRot = model->leg1.xRot;

	head.render(scale);
	if (texWidth == 64 && texHeight == 64) {
		hair.render(scale);
	}
	body.render(scale);
	arm0.render(scale);
	arm1.render(scale);
	leg0.render(scale);
	leg1.render(scale);
}

void HumanoidModel::renderHorrible( float time, float r, float bob, float yRot, float xRot, float scale )
{
	setupAnim(time, r, bob, yRot, xRot, scale);
	head.renderHorrible(scale);
	body.renderHorrible(scale);
	arm0.renderHorrible(scale);
	arm1.renderHorrible(scale);
	leg0.renderHorrible(scale);
	leg1.renderHorrible(scale);
	//hair.renderHorrible(scale);
}
// Updated to match Minecraft Java, all except hair.
void HumanoidModel::setupAnim( float time, float r, float bob, float yRot, float xRot, float scale )
{
	head.yRot = yRot / (180 / Mth::PI);
	head.xRot = xRot / (180 / Mth::PI);
	const float tcos0 = Mth::cos(time * 0.6662f) * r;
	const float tcos1 = Mth::cos(time * 0.6662f + Mth::PI) * r;

	arm0.xRot = tcos1;
	arm1.xRot = tcos0;
	arm0.zRot = 0;
	arm1.zRot = 0;

	leg0.xRot = tcos0 * 1.4f;
	leg1.xRot = tcos1 * 1.4f;
	leg0.yRot = 0;
	leg1.yRot = 0;

	if (riding) {
		arm0.xRot += -Mth::PI / 2 * 0.4f;
		arm1.xRot += -Mth::PI / 2 * 0.4f;
		leg0.xRot = -Mth::PI / 2 * 0.8f;
		leg1.xRot = -Mth::PI / 2 * 0.8f;
		leg0.yRot = Mth::PI / 2 * 0.2f;
		leg1.yRot = -Mth::PI / 2 * 0.2f;
	}

	if (holdingLeftHand != 0) {
		arm1.xRot = arm1.xRot * 0.5f - Mth::PI / 2.0f * 0.2f * holdingLeftHand;
	}
	if (holdingRightHand != 0) {
		arm0.xRot = arm0.xRot * 0.5f - Mth::PI / 2.0f * 0.2f * holdingRightHand;
	}
	arm0.yRot = 0;
	arm1.yRot = 0;

	if (attackTime > -9990) {
		float swing = attackTime;
		body.yRot = Mth::sin(Mth::sqrt(swing) * Mth::PI * 2) * 0.2f;
		arm0.z = Mth::sin(body.yRot) * 5;
		arm0.x = -Mth::cos(body.yRot) * 5;
		arm1.z = -Mth::sin(body.yRot) * 5;
		arm1.x = Mth::cos(body.yRot) * 5;
		arm0.yRot += body.yRot;
		arm1.yRot += body.yRot;
		arm1.xRot += body.yRot;

		swing = 1 - attackTime;
		swing *= swing;
		swing *= swing;
		swing = 1 - swing;
		float aa = Mth::sin(swing * Mth::PI);
		float bb = Mth::sin(attackTime * Mth::PI) * -(head.xRot - 0.7f) * 0.75f;
		arm0.xRot -= aa * 1.2f + bb;
		arm0.yRot += body.yRot * 2;
		arm0.zRot = Mth::sin(attackTime * Mth::PI) * -0.4f;
	}

	if (sneaking) {
		body.xRot = 0.5f;
		arm0.xRot += 0.4f;
		arm1.xRot += 0.4f;
		leg0.z = +4.0f;
		leg1.z = +4.0f;
		leg0.y = +9.0f;
		leg1.y = +9.0f;
		head.y = +1;
	} else {
		body.xRot = 0.0f;
		leg0.z = +0.0f;
		leg1.z = +0.0f;
		leg0.y = +12.0f;
		leg1.y = +12.0f;
		head.y = 0;
	}

	const float bcos = Mth::cos(bob * 0.09f) * 0.05f + 0.05f;
	const float bsin = Mth::sin(bob * 0.067f) * 0.05f;

	arm0.zRot += bcos;
	arm1.zRot -= bcos;
	arm0.xRot += bsin;
	arm1.xRot -= bsin;

	if (bowAndArrow) {
		float attack2 = 0;
		float attack = 0;

		arm0.zRot = 0;
		arm1.zRot = 0;
		arm0.yRot = -(0.1f - attack2 * 0.6f) + head.yRot;
		arm1.yRot = +(0.1f - attack2 * 0.6f) + head.yRot + 0.4f;
		arm0.xRot = -Mth::PI / 2.0f + head.xRot;
		arm1.xRot = -Mth::PI / 2.0f + head.xRot;
		arm0.xRot -= attack2 * 1.2f - attack * 0.4f;
		arm1.xRot -= attack2 * 1.2f - attack * 0.4f;

		arm0.zRot += bcos;
		arm1.zRot -= bcos;
		arm0.xRot += bsin;
		arm1.xRot -= bsin;
	}
}

void HumanoidModel::onGraphicsReset()
{
	head.onGraphicsReset();
	body.onGraphicsReset();
	arm0.onGraphicsReset();
	arm1.onGraphicsReset();
	leg0.onGraphicsReset();
	leg1.onGraphicsReset();
	//hair.onGraphicsReset();
}

//void renderHair(float scale) {
//    hair.yRot = head.yRot;
//    hair.xRot = head.xRot;
//    hair.render(scale);
//}
//
//void renderEars(float scale) {
//    ear.yRot = head.yRot;
//    ear.xRot = head.xRot;
//    ear.x=0;
//    ear.y=0;
//    ear.render(scale);
//}
//
