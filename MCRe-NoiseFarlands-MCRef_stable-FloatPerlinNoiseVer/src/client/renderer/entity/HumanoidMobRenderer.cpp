#include "HumanoidMobRenderer.h"
#include "EntityRenderDispatcher.h"
#include "../ItemInHandRenderer.h"
#include "../TileRenderer.h"
#include "../Tesselator.h"
#include "../../model/HumanoidModel.h"
#include "../../../world/level/tile/Tile.h"
#include "../../../world/entity/player/Player.h"
#include "../../../world/entity/player/Inventory.h"
#include "../../../world/item/ItemInstance.h"
#include "../../../world/item/Item.h"
#include "../../../world/item/BowItem.h"

HumanoidMobRenderer::HumanoidMobRenderer(HumanoidModel* humanoidModel, float shadow)
:	super(humanoidModel, shadow),
		humanoidModel(humanoidModel),
		lastCapeXRot(0),
		lastCapeZRot(0)
{
}

void HumanoidMobRenderer::renderHand() {
	humanoidModel->attackTime = 0;
	humanoidModel->setupAnim(0, 0, 0, 0, 0, 1 / 16.0f);

	//@attn @cuberender @enableClientState @vertexarray
	glEnableClientState2(GL_VERTEX_ARRAY);
	glEnableClientState2(GL_TEXTURE_COORD_ARRAY);
	//glEnableClientState2(GL_COLOR_ARRAY); 
	humanoidModel->arm0.render(1 / 16.0f);
	glDisableClientState2(GL_VERTEX_ARRAY);
	glDisableClientState2(GL_TEXTURE_COORD_ARRAY);
	//glDisableClientState2(GL_COLOR_ARRAY); 
}

void HumanoidMobRenderer::additionalRendering(Mob* mob, float a) {
	ItemInstance* item = mob->getCarriedItem();
	if (item != NULL && item->count > 0) {
		glPushMatrix2();
		humanoidModel->arm0.translateTo(1 / 16.0f);
		glTranslatef2(-1.0f / 16.0f, 7.0f / 16.0f, 1.0f / 16.0f);

		if (item->id < 256 && TileRenderer::canRender(Tile::tiles[item->id]->getRenderShape())) {
			float s = 8.0f / 16.0f;
			glTranslatef2(0.0f, 3.0f / 16.0f, -5 / 16.0f);
			s *= 0.75f;
			glRotatef2(20.0f, 1.0f, 0.0f, 0.0f);
			glRotatef2(45.0f, 0.0f, 1.0f, 0.0f);
			glScalef2(s, -s, s);
		} else if (item->id == Item::bow->id) {
			const float s = 10.0f / 16.0f;
			glTranslatef2(0 / 16.0f, 2 / 16.0f, 5 / 16.0f);
			glRotatef2(-20, 0, 1, 0);
			glScalef2(s, -s, s);
			glRotatef2(-100, 1, 0, 0);
			glRotatef2(45, 0, 1, 0);
		} else if (Item::items[item->id]->isHandEquipped()) {
			float s = 10.0f / 16.0f;
			glTranslatef2(0.0f, 3.0f / 16.0f, 0.0f);
			glScalef2(s, -s, s);
			glRotatef2(-100.0f, 1.0f, 0.0f, 0.0f);
			glRotatef2(45.0f, 0.0f, 1.0f, 0.0f);
		} else {
			float s = 6 / 16.0f;
			glTranslatef2(+4 / 16.0f, +3 / 16.0f, -3 / 16.0f);
			glScalef2(s, s, s);
			glRotatef2(60.0f, 0.0f, 0.0f, 1.0f);
			glRotatef2(-90.0f, 1.0f, 0.0f, 0.0f);
			glRotatef2(20.0f, 0.0f, 0.0f, 1.0f);
		}
		entityRenderDispatcher->itemInHandRenderer->renderItem(mob, item);
		glPopMatrix2();
	}

	// Render player cape if available
{
    Player* player = Player::asPlayer(mob);
    if (player) {
        const std::string capeTex = player->getCapeTexture();
        if (!capeTex.empty()) {

            bindTexture(capeTex);

            glPushMatrix2();

            // Attach to player body
            humanoidModel->body.translateTo(1 / 16.0f);

            // Convert model units (pixels) to world units
            glScalef2(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);

            // Position cape slightly down and behind the shoulders
            glTranslatef2(0.0f, 1.0f, 2.0f);

            // Java-like cape physics (interpolated inertia + body motion)
            float pt = a;

            double capeX = player->getCapePrevX() + (player->getCapeX() - player->getCapePrevX()) * pt;
            double capeY = player->getCapePrevY() + (player->getCapeY() - player->getCapePrevY()) * pt;
            double capeZ = player->getCapePrevZ() + (player->getCapeZ() - player->getCapePrevZ()) * pt;

            double px = player->xo + (player->x - player->xo) * pt;
            double py = player->yo + (player->y - player->yo) * pt;
            double pz = player->zo + (player->z - player->zo) * pt;

            double dx = capeX - px;
            double dy = capeY - py;
            double dz = capeZ - pz;

            float bodyYaw = player->yBodyRotO + (player->yBodyRot - player->yBodyRotO) * pt;

            float rad = bodyYaw * Mth::PI / 180.0f;
            double sinYaw = Mth::sin(rad);
            double cosYaw = -Mth::cos(rad);

            float forward = (float)(dx * sinYaw + dz * cosYaw) * 100.0f;
            float sideways = (float)(dx * cosYaw - dz * sinYaw) * 100.0f;
            if (forward < 0.0f) forward = 0.0f;

            float lift = (float)dy * 10.0f;
            if (lift < -6.0f) lift = -6.0f;
            if (lift > 32.0f) lift = 32.0f;

            float walk =
                Mth::sin((player->walkAnimPos + player->walkAnimSpeed) * 6.0f) *
                32.0f *
                player->walkAnimSpeed;

            float capeXRot = 6.0f + forward / 2.0f + lift + walk;
            float capeZRot = sideways / 2.0f;

            // Smooth out jitter by lerping from the previous frame
            const float smooth = 0.3f;
            capeXRot = lastCapeXRot + (capeXRot - lastCapeXRot) * smooth;
            capeZRot = lastCapeZRot + (capeZRot - lastCapeZRot) * smooth;

            lastCapeXRot = capeXRot;
            lastCapeZRot = capeZRot;

            glRotatef2(capeXRot, 1.0f, 0.0f, 0.0f);
            glRotatef2(capeZRot, 0.0f, 0.0f, 1.0f);

            Tesselator& t = Tesselator::instance;
            t.begin();

            // UV coordinates (64x32 skin layout)
            const float u0 = 1.0f / 64.0f;
            const float u1 = 11.0f / 64.0f;
            const float u2 = 12.0f / 64.0f;
            const float u3 = 22.0f / 64.0f;

            const float uL0 = 0.0f / 64.0f;
            const float uL1 = 1.0f / 64.0f;

            const float uR0 = 11.0f / 64.0f;
            const float uR1 = 12.0f / 64.0f;

            const float v0 = 0.0f / 32.0f;
            const float v1 = 1.0f / 32.0f;

            const float vTop = 1.0f / 32.0f;
            const float vBottom = 17.0f / 32.0f;

            // Cape size (10x16x1 pixels)
            const float halfW = 5.0f;
            const float height = 16.0f;
            const float depth = 1.0f;

            // Front
            t.tex(u2, vTop);    t.vertex(-halfW, 0.0f, 0.0f);
            t.tex(u3, vTop);    t.vertex(halfW, 0.0f, 0.0f);
            t.tex(u3, vBottom); t.vertex(halfW, height, 0.0f);
            t.tex(u2, vBottom); t.vertex(-halfW, height, 0.0f);

            // Back 
            t.tex(u0, vTop);    t.vertex(halfW, 0.0f, depth);
            t.tex(u1, vTop);    t.vertex(-halfW, 0.0f, depth);
            t.tex(u1, vBottom); t.vertex(-halfW, height, depth);
            t.tex(u0, vBottom); t.vertex(halfW, height, depth);

            // Left
            t.tex(uL0, vTop);    t.vertex(-halfW, 0.0f, depth);
            t.tex(uL1, vTop);    t.vertex(-halfW, 0.0f, 0.0f);
            t.tex(uL1, vBottom); t.vertex(-halfW, height, 0.0f);
            t.tex(uL0, vBottom); t.vertex(-halfW, height, depth);

            // Right
            t.tex(uR0, vTop);    t.vertex(halfW, 0.0f, 0.0f);
            t.tex(uR1, vTop);    t.vertex(halfW, 0.0f, depth);
            t.tex(uR1, vBottom); t.vertex(halfW, height, depth);
            t.tex(uR0, vBottom); t.vertex(halfW, height, 0.0f);

            // Top
            t.tex(u0, v0); t.vertex(-halfW, 0.0f, depth);
            t.tex(u1, v0); t.vertex(halfW, 0.0f, depth);
            t.tex(u1, v1); t.vertex(halfW, 0.0f, 0.0f);
            t.tex(u0, v1); t.vertex(-halfW, 0.0f, 0.0f);

            // Bottom
            t.tex(u2, v0); t.vertex(halfW, height, 0.0f);
            t.tex(u3, v0); t.vertex(-halfW, height, 0.0f);
            t.tex(u3, v1); t.vertex(-halfW, height, depth);
            t.tex(u2, v1); t.vertex(halfW, height, depth);

            t.draw();

            glPopMatrix2();
        }
    }
}
}

void HumanoidMobRenderer::render( Entity* mob_, float x, float y, float z, float rot, float a ) {
	Mob* mob = (Mob*)mob_;
	ItemInstance* carriedItem = mob->getCarriedItem();
	if(carriedItem != NULL)
		humanoidModel->holdingRightHand = true;

	humanoidModel->sneaking = mob->isSneaking();

	super::render(mob_, x, y, z, rot, a);
	humanoidModel->holdingRightHand = false;
}
