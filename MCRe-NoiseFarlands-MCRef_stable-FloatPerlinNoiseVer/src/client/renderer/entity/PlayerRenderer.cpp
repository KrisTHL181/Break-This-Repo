#include "PlayerRenderer.h"
#include "EntityRenderDispatcher.h"
#include "../Textures.h"
#include "../../../world/entity/player/Player.h"
#include "../../../world/level/Level.h"
#include "../../../world/item/ArmorItem.h"

static const std::string armorFilenames[10] = {
	"armor/cloth_1.png",	"armor/cloth_2.png",
	"armor/chain_1.png",	"armor/chain_2.png",
	"armor/iron_1.png",		"armor/iron_2.png",
	"armor/diamond_1.png",	"armor/diamond_2.png",
	"armor/gold_1.png",		"armor/gold_2.png",
};

PlayerRenderer::PlayerRenderer( HumanoidModel* humanoidModel, float shadow )
:	super(humanoidModel, shadow),
	playerModel64(humanoidModel),
	playerModel32(new HumanoidModel(0, 0, 64, 32)),
	armorParts1(new HumanoidModel(1.0f, 0, 64, 32)),
	armorParts2(new HumanoidModel(0.5f, 0, 64, 32))
{
	// default to legacy skin path until we know the exact texture size
	model = playerModel32;
	humanoidModel = playerModel32;
}

PlayerRenderer::~PlayerRenderer() {
	// prevent MobRenderer destructor from deleting model pointers we manage manually
	model = nullptr;

	delete playerModel32;
	delete playerModel64;
	delete armorParts1;
	delete armorParts2;
}

void PlayerRenderer::setupPosition( Entity* mob, float x, float y, float z ) {
	Player* player = (Player*) mob;
	if(player->isAlive() && player->isSleeping()) {
		return super::setupPosition(mob, x + player->bedOffsetX, y + player->bedOffsetY, z + player->bedOffsetZ);
	}
	return super::setupPosition(mob, x, y, z);
}

void PlayerRenderer::setupRotations( Entity* mob, float bob, float bodyRot, float a ) {
	Player* player = (Player*) mob;
	if(player->isAlive() && player->isSleeping()) {
		glRotatef(player->getSleepRotation(), 0, 1, 0);
		glRotatef(getFlipDegrees(player), 0, 0, 1);
		glRotatef(270, 0, 1, 0);
		return;
	}
	super::setupRotations(mob, bob, bodyRot, a);
}

bool PlayerRenderer::isModernPlayerSkin(Mob* mob) {
	const std::string texName = mob->getTexture();
	TextureId texId = entityRenderDispatcher->textures->loadTexture(texName);
	if (!Textures::isTextureIdValid(texId))
		return false;
	const TextureData* texData = entityRenderDispatcher->textures->getTemporaryTextureData(texId);
	return texData && texData->w == 64 && texData->h == 64;
}

void PlayerRenderer::renderName( Mob* mob, float x, float y, float z ){
	//@todo: figure out how to handle HideGUI
	if (mob != entityRenderDispatcher->cameraEntity && mob->level->adventureSettings.showNameTags) {
		renderNameTag(mob, ((Player*)mob)->name, x, y, z, 32);
	}
}

void PlayerRenderer::render(Entity* mob_, float x, float y, float z, float rot, float a) {
	Mob* mob = (Mob*) mob_;
	HumanoidModel* desired = isModernPlayerSkin(mob) ? playerModel64 : playerModel32;
	if (model != desired || humanoidModel != desired) {
		model = desired;
		humanoidModel = desired;
	}
	LOGI("[PlayerRenderer] %s: skin=%s, modelTex=%dx%d, desired=%s\n", 
		((Player*)mob)->name.c_str(), mob->getTexture().c_str(), 
		humanoidModel->texWidth, humanoidModel->texHeight,
		(desired == playerModel64 ? "64" : "32"));
	HumanoidMobRenderer::render(mob_, x, y, z, rot, a);
}

int PlayerRenderer::prepareArmor(Mob* mob, int layer, float a) {
	Player* player = (Player*) mob;

	ItemInstance* itemInstance = player->getArmor(layer);
	if (!ItemInstance::isArmorItem(itemInstance))
		return -1;

	ArmorItem* armorItem = (ArmorItem*) itemInstance->getItem();
	int fnIndex = (armorItem->modelIndex + armorItem->modelIndex) + (layer == 2 ? 1 : 0);
	bindTexture(armorFilenames[fnIndex]);

	HumanoidModel* armor = layer == 2 ? armorParts2 : armorParts1;

	armor->head.visible = layer == 0;
	//armor.hair.visible = layer == 0;
	armor->body.visible = layer == 1 || layer == 2;
	armor->arm0.visible = layer == 1;
	armor->arm1.visible = layer == 1;
	armor->leg0.visible = layer == 2 || layer == 3;
	armor->leg1.visible = layer == 2 || layer == 3;

	setArmor(armor);

	/*if (itemInstance.isEnchanted())
		return 15; */

	return 1;
}

void PlayerRenderer::onGraphicsReset() {
	if (playerModel32) playerModel32->onGraphicsReset();
	if (playerModel64) playerModel64->onGraphicsReset();

	if (armorParts1) armorParts1->onGraphicsReset();
	if (armorParts2) armorParts2->onGraphicsReset();

	super::onGraphicsReset();
}
