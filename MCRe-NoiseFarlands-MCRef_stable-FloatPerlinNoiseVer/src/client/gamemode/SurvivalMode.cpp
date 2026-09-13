#include "SurvivalMode.h"
#include "../Minecraft.h"
#include "../player/LocalPlayer.h"
#ifndef STANDALONE_SERVER
#include "../particle/ParticleEngine.h"
#include "../sound/SoundEngine.h"
#endif
#include "../../world/level/Level.h"
#include "../../world/entity/player/Abilities.h"
#include "world/item/NegativePickaxeItem.h"   // 文件头部

SurvivalMode::SurvivalMode( Minecraft* minecraft )
:	super(minecraft),
	xDestroyBlock(-1),
	yDestroyBlock(-1),
	zDestroyBlock(-1)
{
}

void SurvivalMode::continueDestroyBlock( int x, int y, int z, int face ) {
	if (destroyDelay > 0) {
		destroyDelay--;
		return;
	}

	if (x == xDestroyBlock && y == yDestroyBlock && z == zDestroyBlock) {
		int t = minecraft->level->getTile(x, y, z);
		if (t == 0) return;
		Tile* tile = Tile::tiles[t];

		destroyProgress += tile->getDestroyProgress(minecraft->player);

		if ((++destroyTicks & 3) == 1) {
#ifndef STANDALONE_SERVER
			if (tile != NULL) {
				minecraft->soundEngine->play(tile->soundType->getStepSound(), x + 0.5f, y + 0.5f, z + 0.5f, (tile->soundType->getVolume() + 1) / 8, tile->soundType->getPitch() * 0.5f);
			}
#endif
		}

		if (destroyProgress >= 1) {
			destroyBlock(x, y, z, face);
			destroyProgress = 0;
			oDestroyProgress = 0;
			destroyTicks = 0;
			destroyDelay = 5;
		}
	} else {
		destroyProgress = 0;
		oDestroyProgress = 0;
		destroyTicks = 0;
		xDestroyBlock = x;
		yDestroyBlock = y;
		zDestroyBlock = z;
	}
}

bool SurvivalMode::destroyBlock( int x, int y, int z, int face ) {
    int t = minecraft->level->getTile(x, y, z);
    int data = minecraft->level->getData(x, y, z);

    ItemInstance* heldItem = minecraft->player->inventory->getSelected();
    if (heldItem && heldItem->getItem() == Item::negativePickaxe) {
        // 随机找一个实心方块
        int newBlock = 1;
        for (int tries = 0; tries < 50; ++tries) {
            int candidate = minecraft->level->random.nextInt(255) + 1;
            if (Tile::tiles[candidate] && Tile::tiles[candidate]->getRenderShape() != Tile::SHAPE_INVISIBLE) {
                newBlock = candidate;
                break;
            }
        }
        // 替换原来的方块
        minecraft->level->setTileAndData(x, y, z, newBlock, 0);
        // 消耗镐子耐久
        // 每挖一个方块，损伤值 +1（负耐久越来越深）
heldItem->setAuxValue(heldItem->getAuxValue() + 1);
if (heldItem->count == 0) heldItem->count = 1;   // 防意外消失
        if (heldItem->count == 0) {
            minecraft->player->inventory->clearSlot(minecraft->player->inventory->selected);
        }
        return true;   // 已完成“破坏”
    }

    // ===== 以下保持原版破坏流程 =====
    if (destroyDelay > 0) {
        destroyDelay--;
        return true;
    }
    // ... 之后的代码完全不动 ...
	bool changed = GameMode::destroyBlock(x, y, z, face);
	bool couldDestroy = minecraft->player->canDestroy(Tile::tiles[t]);

	ItemInstance* item = minecraft->player->inventory->getSelected();
	if (item != NULL) {
		item->mineBlock(t, x, y, z);
		if (item->count == 0) {
			//item->snap(minecraft->player);
			minecraft->player->inventory->clearSlot(minecraft->player->inventory->selected);
		}
	}
	if (changed && couldDestroy) {
		ItemInstance instance(t, 1, data);
		Tile::tiles[t]->playerDestroy(minecraft->level, minecraft->player, x, y, z, data);
	}
	return changed;
}

void SurvivalMode::stopDestroyBlock() {
	destroyProgress = 0;
	destroyDelay = 0;
}

void SurvivalMode::initAbilities( Abilities& abilities ) {
	abilities.flying = false;
	abilities.mayfly = false;
	abilities.instabuild = false;
	abilities.invulnerable = false;
}

void SurvivalMode::startDestroyBlock( int x, int y, int z, int face ) {
	if(minecraft->player->getCarriedItem() != NULL && minecraft->player->getCarriedItem()->id == Item::bow->id)
		return;

	int t = minecraft->level->getTile(x, y, z);
	if (t > 0 && destroyProgress == 0) Tile::tiles[t]->attack(minecraft->level, x, y, z, minecraft->player);
	if (t > 0 && Tile::tiles[t]->getDestroyProgress(minecraft->player) >= 1)
		destroyBlock(x, y, z, face);
}
