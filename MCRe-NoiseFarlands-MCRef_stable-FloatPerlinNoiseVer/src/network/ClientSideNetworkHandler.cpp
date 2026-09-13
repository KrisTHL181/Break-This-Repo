#include "ClientSideNetworkHandler.h"
#include "client/Options.h"
#include "packet/PacketInclude.h"
#include "RakNetInstance.h"
#include "../world/level/chunk/ChunkSource.h"
#include "../world/level/Level.h"
#include "../world/level/storage/LevelStorageSource.h"
#include "../world/entity/player/Player.h"
#include "../world/entity/player/Inventory.h"
#include "../client/Minecraft.h"
#include "../client/gamemode/GameMode.h"
#ifndef STANDALONE_SERVER
#include "../client/gui/screens/DisconnectionScreen.h"
#endif
#include "../client/player/LocalPlayer.h"
#include "../client/multiplayer/MultiPlayerLevel.h"
#include "../client/player/input/KeyboardInput.h"
#include "../client/sound/SoundEngine.h"
#include "../world/entity/MobFactory.h"
#include "../raknet/RakPeerInterface.h"
#include "../world/level/Explosion.h"
#include "../world/level/tile/entity/FurnaceTileEntity.h"
#include "../world/inventory/BaseContainerMenu.h"
#ifndef STANDALONE_SERVER
#include "../client/particle/TakeAnimationParticle.h"
#endif
#include "../world/entity/EntityFactory.h"
#include "../world/entity/item/PrimedTnt.h"
#include "../world/entity/projectile/Arrow.h"
#include "../world/level/tile/entity/ChestTileEntity.h"
#include "../client/player/RemotePlayer.h"
#include "../world/level/tile/LevelEvent.h"
#include "../world/entity/item/FallingTile.h"

static MultiPlayerLevel* mpcast(Level* l) { return (MultiPlayerLevel*) l; }

ClientSideNetworkHandler::ClientSideNetworkHandler(Minecraft* minecraft, IRakNetInstance* raknetInstance)
:	minecraft(minecraft),
	raknetInstance(raknetInstance),
	level(NULL),
	initialChunksLoaded(false),
	totalInitialChunks(0),
	loadedInitialChunks(0)
{
	rakPeer = raknetInstance->getPeer();
}

ClientSideNetworkHandler::~ClientSideNetworkHandler()
{
}

void ClientSideNetworkHandler::requestNextChunk()
{
	if (pendingChunks.empty()) return;

	auto chunk = pendingChunks.front();
	pendingChunks.pop();

	RequestChunkPacket packet(chunk.first, chunk.second);
	raknetInstance->send(packet);
}

bool ClientSideNetworkHandler::isChunkLoaded(int x, int z)
{
	auto it = loadedChunks.find(std::make_pair(x, z));
	return it != loadedChunks.end() && it->second;
}

void ClientSideNetworkHandler::onConnect(const RakNet::RakNetGUID& hostGuid)
{
	LOGI("onConnect, server guid: %s, local guid: %s\n", hostGuid.ToString(), rakPeer->GetMyGUID().ToString());
	serverGuid = hostGuid;

	clearChunksLoaded();
	LoginPacket packet(minecraft->options.getStringValue(OPTIONS_USERNAME).c_str(), SharedConstants::NetworkProtocolVersion);
	raknetInstance->send(packet);
}

void ClientSideNetworkHandler::onUnableToConnect()
{
	LOGI("onUnableToConnect\n");
}

void ClientSideNetworkHandler::onDisconnect(const RakNet::RakNetGUID& guid)
{
	LOGI("onDisconnect\n");
	if (level)
	{
		level->isClientSide = false;
		for (int i = (int)level->players.size()-1; i >= 0; --i ) {
			Player* p = level->players[i];
			if (p != minecraft->player) {
				p->reallyRemoveIfPlayer = true;
				level->removeEntity(p);
			}
		}
	}
#ifndef STANDALONE_SERVER
	minecraft->gui.addMessage("Disconnected from server");
#endif
}

// ClientSideNetworkHandler.cpp —— 也去掉（可选）
void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, LoginStatusPacket* packet){
    // 🧊 无视服务器返回的版本状态，直接标记已登录
    raknetInstance->setIsLoggedIn(true);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, StartGamePacket* packet)
{
	LOGI("StartGamePacket\n");

#ifdef RPI
	if (packet->gameType != GameType::Creative) {
		minecraft->setScreen(new DisconnectionScreen("Could not connect: Incompatible server!"));
		return;
	}
#endif

	const std::string& levelId = LevelStorageSource::TempLevelId;
	LevelStorageSource* storageSource = minecraft->getLevelSource();
	storageSource->deleteLevel(levelId);
	//level = new Level(storageSource->selectLevel(levelId, true), "temp", packet->levelSeed, SharedConstants::StorageVersion);
	MultiPlayerLevel* level = new MultiPlayerLevel(
		storageSource->selectLevel(levelId, true),
		"temp",
		LevelSettings(packet->levelSeed, LevelSettings::validateGameType(packet->gameType)),
		SharedConstants::StorageVersion);
	level->isClientSide = true;

	bool isCreative = (packet->gameType == GameType::Creative);
	LocalPlayer* player = new LocalPlayer(minecraft, level, minecraft->options.getStringValue(OPTIONS_USERNAME), level->dimension->id, isCreative);
	player->owner = rakPeer->GetMyGUID();
	player->entityId = packet->entityId;
	player->moveTo(packet->x, packet->y, packet->z, player->yRot, player->xRot);

	LOGI("new pos: %f, %f [%f - %f]\n", player->x, player->z, player->bb.y0, player->bb.y1);

	minecraft->setLevel(level, "ClientSideNetworkHandler -> setLevel", player);
	minecraft->setIsCreativeMode(isCreative);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, MessagePacket* packet)
{
	LOGI("MessagePacket\n");
#ifndef STANDALONE_SERVER
	minecraft->gui.addMessage(packet->message.C_String());
#endif
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, SetTimePacket* packet)
{
	if (!level)
		return;

	LOGI("SetTimePacket\n");
	level->setTime(packet->time);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AddEntityPacket* packet)
{
	if (!level)
		return;

	Entity* e = EntityFactory::CreateEntity(packet->type, level);
	if (!e)
		return;

	e->entityId = packet->entityId;
	e->setPos(packet->x, packet->y, packet->z);

	switch (packet->type)
	{
		case EntityTypes::IdFallingTile: {
			int data = -packet->data();
            FallingTile* ft = (FallingTile*) e;
            ft->tile = data & 0xff;
            ft->data = data >> 16;
		}
		case EntityTypes::IdArrow: {
			Entity* owner = level->getEntity(packet->data());
			if (owner && owner->isMob())
				((Arrow*)e)->ownerId = owner->entityId;
			break;
		}
		default:
			break;
	}

    if (packet->hasMovementData()) {
		e->lerpMotion(packet->xd, packet->yd, packet->zd);
	}

	mpcast(level)->putEntity(packet->entityId, e);
}

void ClientSideNetworkHandler::handle( const RakNet::RakNetGUID& source, AddPaintingPacket* packet ) {
	if (!level)
		return;

	Painting* painting = new Painting(level, packet->xTile, packet->yTile, packet->zTile, packet->dir, packet->motive);
	mpcast(level)->putEntity(packet->entityId, painting);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AddMobPacket* packet)
{
	LOGI("AddMobPacket (%p)\n", level);

	if (!level) {
		LOGW("Trying to add a mob with no level!\n");
		return;
	}
	if (!packet->type) {
		LOGE("Trying to add a mob without a type id\n");
		return;
	}

	Mob* mob = MobFactory::CreateMob(packet->type, level);
	if (!mob) {
		LOGE("Server tried to add an unknown mob type! :%d\n", packet->type);
		return;
	}

	mob->entityId = packet->entityId;
	mob->moveTo(packet->x, packet->y, packet->z, packet->yRot, packet->xRot);
	mob->getEntityData()->assignValues(&packet->unpack);
	level->addEntity(mob);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AddPlayerPacket* packet)
{
	if (!level) {
		return;
	}
	LOGI("AddPlayerPacket\n");

	Player* player = new RemotePlayer(level, minecraft->isCreativeMode());
	minecraft->gameMode->initAbilities(player->abilities);
	player->entityId = packet->entityId;
	level->addEntity(player);

	player->moveTo(packet->x, packet->y, packet->z, packet->yRot, packet->xRot);
	player->name = packet->name.C_String();
	player->owner = packet->owner;
	player->getEntityData()->assignValues(&packet->unpack);
    int slot = Inventory::MAX_SELECTION_SIZE;
    if (packet->carriedItemId == 0) {
        player->inventory->clearSlot(slot);
    } else {
        ItemInstance newItem(packet->carriedItemId, 1, packet->carriedItemAuxValue);
        player->inventory->replaceSlot(slot, &newItem);
    }
    player->inventory->moveToSelectedSlot(slot, true);

	std::string message = packet->name.C_String();
	message += " joined the game";
#ifndef STANDALONE_SERVER
	minecraft->gui.addMessage(message);
#endif
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, RemovePlayerPacket* packet) {
	if (!level || source == minecraft->player->owner) return;

	if (Player* player = findPlayer(level, packet->entityId, &packet->owner)) {
		player->reallyRemoveIfPlayer = true;
		level->removeEntity(player);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, RemoveEntityPacket* packet)
{
	if (!level) return;

	Entity* entity = level->getEntity(packet->entityId);
	LOGI("RemoveEntityPacket %p %p, %d\n", entity, minecraft->player, entity?(int)(entity->isPlayer()): -1);
	if (!entity) return;

	level->removeEntity(entity);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AddItemEntityPacket* packet)
{
	if (!level) return;

	ItemEntity* entity = new ItemEntity(level, packet->x, packet->y, packet->z, ItemInstance(packet->itemId, packet->itemCount, packet->auxValue));
	entity->xd = packet->xa();
	entity->yd = packet->ya();
	entity->zd = packet->za();

	mpcast(level)->putEntity(packet->id, entity);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, TakeItemEntityPacket* packet) {
	if (!level) return;

	Entity* e = level->getEntity(packet->itemId);
	if (!e)
		return;
	
	ItemInstance item;
	if (e->isItemEntity()) {
		item = ((ItemEntity*) e)->item;
#ifndef STANDALONE_SERVER
		if (Entity* to = level->getEntity(packet->playerId))
			minecraft->particleEngine->add(new TakeAnimationParticle(level, (ItemEntity*)e, to, -0.5f));
#endif
	}
	else if (e->getEntityTypeId() == EntityTypes::IdArrow)
		item = ItemInstance(Item::arrow);

	if (item.isNull())
		return;

	if (minecraft->player->entityId == packet->playerId
	&& !minecraft->player->inventory->add(&item)) {
		DropItemPacket dropPacket(packet->playerId, item);
		minecraft->raknetInstance->send(dropPacket);
	}
	level->playSound(e, "random.pop", 0.2f, 1.0f * 2.f);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, MovePlayerPacket* packet)
{
	if (!level)
		return;

	Entity* entity = level->getEntity(packet->entityId);
	if (entity)
	{
		entity->lerpTo(packet->x, packet->y, packet->z, packet->yRot, packet->xRot, 3);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, MoveEntityPacket* packet)
{
	if (!level)
		return;

	Entity* entity = level->getEntity(packet->entityId);
	if (entity)
	{
		float xRot = packet->hasRot? packet->xRot : entity->xRot;
		float yRot = packet->hasRot? packet->yRot : entity->yRot;
		entity->lerpTo(packet->x, packet->y, packet->z, yRot, xRot, 3);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, UpdateBlockPacket* packet)
{
	if (!level) return;

	int x = packet->x, z = packet->z;

	if (isChunkLoaded(x >> 4, z >> 4))
	{
		int y = packet->y;
		int tileId = Tile::transformToValidBlockId(packet->blockId, x, y, z);
		level->setTileAndData(x, y, z, tileId, packet->blockData);
	} else {
		SBufferedBlockUpdate update;
		update.blockId = packet->blockId;
		update.blockData = packet->blockData;
		update.setData = true;
		update.x = packet->x;
		update.y = packet->y;
		update.z = packet->z;
		bufferedBlockUpdates.push_back(update);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ExplodePacket* packet) {
	if (!level) return;
	Explosion explosion(level, NULL, packet->x, packet->y, packet->z, packet->r);
	explosion.toBlow.insert(packet->toBlow.begin(), packet->toBlow.end());
	explosion.finalizeExplosion();
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, LevelEventPacket* packet) {
	if (!level) return;
	if(packet->eventId == LevelEvent::ALL_PLAYERS_SLEEPING) {
		minecraft->player->setAllPlayersSleeping();
	}
	else {
		minecraft->level->levelEvent(NULL, packet->eventId, packet->x, packet->y, packet->z, packet->data);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, TileEventPacket* packet) {
	if (!level) return;
	minecraft->level->tileEvent(packet->x, packet->y, packet->z, packet->b0, packet->b1);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, EntityEventPacket* packet) {
	if (!level) return;

	Entity* e = level->getEntity(packet->entityId);
	if (e) e->handleEntityEvent(packet->eventId);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ChunkDataPacket* packet)
{
	if (!level) {
		LOGI("level @ handle ChunkDataPacket is 0\n");
		return;
	}

	LevelChunk* chunk = level->getChunkSource()->create(packet->x, packet->z);
	if (!chunk || chunk->isEmpty())
	{
		LOGI("Failed to find write-able chunk\n");
		return;
	}

	DataLayer& blockData = chunk->data;
	const int setSize = LEVEL_HEIGHT / 8;
	const int setShift = 4; // power of LEVEL_HEIGHT / 8

	bool recalcHeight = false;
	int x0 = 16, x1 = 0, z0 = 16, z1 = 0, y0 = LEVEL_HEIGHT, y1 = 0;
	int rx = packet->x << 4;
	int rz = packet->z << 4;

	unsigned char readBlockBuffer[setSize];
	unsigned char readDataBuffer[setSize / 2];

	for (int i = 0; i < CHUNK_COLUMNS; i++)
	{
		unsigned char updateBits = 0;
		packet->chunkData.Read(updateBits);

		if (updateBits > 0)
		{
			recalcHeight = true;

			int colX = (i % CHUNK_WIDTH);
			int colZ = (i / CHUNK_WIDTH);
			int colDataPosition = colX << 11 | colZ << 7;

			for (int set = 0; set < 8; set++)
			{
				if ((updateBits & (1 << set)) != 0) 
				{
					packet->chunkData.Read((char*)readBlockBuffer, setSize);
					packet->chunkData.Read((char*)readDataBuffer, setSize / 2);

					for (int part = 0; part < setSize; part++)
					{
						int x = rx + colX;
						int y = (set << setShift) + part;
						int z = rz + colZ;

						int tileId = Tile::transformToValidBlockId(readBlockBuffer[part], x, y, z);
						level->setTileNoUpdate(x, y, z, tileId);
					}

					memcpy(&blockData.data[(colDataPosition + (set << setShift)) >> 1], readDataBuffer, setSize >> 1);
				}

				if (((1 << set) << setShift) < y0)
					y0 = ((1 << set) << setShift);
				if (((1 << set) << setShift) + (LEVEL_HEIGHT / 8) - 1 > y1)
					y1 = ((1 << set) << setShift) + (LEVEL_HEIGHT / 8) - 1;
			}
			if ((i % CHUNK_WIDTH) < x0) x0 = (i % CHUNK_WIDTH);
			if ((i % CHUNK_WIDTH) > x1) x1 = (i % CHUNK_WIDTH);
			if ((i / CHUNK_WIDTH) < z0) z0 = (i / CHUNK_WIDTH);
			if ((i / CHUNK_WIDTH) > z1) z1 = (i / CHUNK_WIDTH);
		}
	}

 	if (recalcHeight)
 	{
 		level->setTilesDirty((packet->x << 4) + x0, y0, (packet->z << 4) + z0, (packet->x << 4) + x1, y1, (packet->z << 4) + z1);
 	}
	chunk->unsaved = false;

	loadedChunks[std::make_pair(packet->x, packet->z)] = true;

	if (!initialChunksLoaded) {
		loadedInitialChunks++;
		if (loadedInitialChunks == totalInitialChunks) {
			ReadyPacket readyPkt(ReadyPacket::READY_REQUESTEDCHUNKS);
			raknetInstance->send(readyPkt);
			initialChunksLoaded = true;

			for (unsigned int i = 0; i < bufferedBlockUpdates.size(); i++) {
				const SBufferedBlockUpdate& update = bufferedBlockUpdates[i];
				int tileId = Tile::transformToValidBlockId(update.blockId, update.x, update.y, update.z);
				if (update.setData)
					level->setTileAndData(update.x, update.y, update.z, tileId, update.blockData);
				else
					level->setTile(update.x, update.y, update.z, tileId);
			}
			bufferedBlockUpdates.clear();
		}
	} else {
		for (auto it = bufferedBlockUpdates.begin(); it != bufferedBlockUpdates.end(); ) {
			const SBufferedBlockUpdate& update = *it;
			if ((update.x >> 4) == packet->x && (update.z >> 4) == packet->z) {
				int tileId = Tile::transformToValidBlockId(update.blockId, update.x, update.y, update.z);
				if (update.setData)
					level->setTileAndData(update.x, update.y, update.z, tileId, update.blockData);
				else
					level->setTile(update.x, update.y, update.z, tileId);
				it = bufferedBlockUpdates.erase(it);
			} else {
				++it;
			}
		}
	}

	if (initialChunksLoaded) {
		int cx = Mth::floor(minecraft->player->x / 16.0f);
		int cz = Mth::floor(minecraft->player->z / 16.0f);
		const int radius = 5;
		for (int dx = -radius; dx <= radius; ++dx) {
			for (int dz = -radius; dz <= radius; ++dz) {
				int nx = cx + dx, nz = cz + dz;
				if (loadedChunks.find(std::make_pair(nx, nz)) == loadedChunks.end()) {
					pendingChunks.push(std::make_pair(nx, nz));
				}
			}
		}
	}

	requestNextChunk();
}

void ClientSideNetworkHandler::arrangeRequestChunkOrder()
{
	clearChunksLoaded();

	int cx = 0, cz = 0;
	Player* p = minecraft ? minecraft->player : NULL;
	if (p) {
		cx = Mth::floor(p->x / 16.0f);
		cz = Mth::floor(p->z / 16.0f);
	}

	const int radius = 5;
	int requested = 0;
	for (int dx = -radius; dx <= radius; ++dx) {
		for (int dz = -radius; dz <= radius; ++dz) {
			int nx = cx + dx;
			int nz = cz + dz;
			pendingChunks.push(std::make_pair(nx, nz));
			requested++;
		}
	}

	totalInitialChunks = requested;
	loadedInitialChunks = 0;
	initialChunksLoaded = false;
}

void ClientSideNetworkHandler::levelGenerated(Level* level)
{
	this->level = level;
	arrangeRequestChunkOrder();
	requestNextChunk();
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, PlayerEquipmentPacket* packet)
{
	if (!level)
		return;

	Entity* entity = level->getEntity(packet->entityId);
	if (!entity || !entity->isPlayer())
		return;

	Player* player = (Player*)entity;
	if (player->owner == rakPeer->GetMyGUID())
	{
		printf("Attempted to modify local player's inventory\n");
		return;
	}

	int slot = Inventory::MAX_SELECTION_SIZE;
	if (slot >= 0) {
		if (packet->itemId == 0) {
			player->inventory->clearSlot(slot);
		} else {
			ItemInstance newItem(packet->itemId, 63, packet->itemAuxValue);
			player->inventory->replaceSlot(slot, &newItem);
		}
		player->inventory->moveToSelectedSlot(slot, true);
	} else {
		LOGW("Warning: Remote player doesn't have his thing, Odd!\n");
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, PlayerArmorEquipmentPacket* packet) {
	if (!level)
		return;

	Entity* entity = level->getEntity(packet->entityId);
	if (!entity || !entity->isPlayer())
		return;

	Player* player = (Player*)entity;
	if (player->owner == rakPeer->GetMyGUID()) {
		printf("Attempted to modify local player's armor visually\n");
		return;
	}

	packet->fillIn(player);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, InteractPacket* packet)
{
	if (!level)
		return;

	Entity* src = level->getEntity(packet->sourceId);
	Entity* entity = level->getEntity(packet->targetId);
	if (src && entity && src->isPlayer())
	{
		Player* player = (Player*) src;
		if (InteractPacket::Attack == packet->action)
			minecraft->gameMode->attack(player, entity);
		if (InteractPacket::Interact == packet->action)
			minecraft->gameMode->interact(player, entity);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, SetEntityDataPacket* packet)
{
	if (!level)
		return;

	LOGI("SetEntityDataPacket\n");

	Entity* e = level->getEntity(packet->id);
	if (e) {
		SynchedEntityData* data = e->getEntityData();
		if (data)
			data->assignValues(&packet->getUnpackedData());
	}
}

void ClientSideNetworkHandler::handle( const RakNet::RakNetGUID& source, SetEntityMotionPacket* packet )
{
	if (!level)
		return;

	if (Entity* e = level->getEntity(packet->id)) {
		e->lerpMotion(packet->xd, packet->yd, packet->zd);
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AnimatePacket* packet)
{
	if (!level)
		return;

    if (minecraft->player->entityId == packet->entityId) {
        if (packet->action == AnimatePacket::Swing) return;
    }

	Entity* entity = level->getEntity(packet->entityId);
	if (!entity || !entity->isPlayer())
        return;
    
	Player* player = (Player*) entity;

    switch (packet->action) {
	case AnimatePacket::Swing:
		player->swing();
		break;
	case AnimatePacket::WAKE_UP:
		player->stopSleepInBed(false, false, false);
		break;
	default:
		LOGW("Unknown Animate action: %d\n", packet->action);
		break;
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, UseItemPacket* packet)
{
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, SetHealthPacket* packet)
{
	if (!level || !minecraft->player)
		return;

	minecraft->player->hurtTo(packet->health);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, SetSpawnPositionPacket* packet) {
	if (!level || !minecraft || !minecraft->player) return;
	if (!level->inRange(packet->x, packet->y, packet->z)) return;

	minecraft->player->setRespawnPosition(Pos(packet->x, packet->y, packet->z));
	level->getLevelData()->setSpawn(packet->x, packet->y, packet->z);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, HurtArmorPacket* packet) {
    if (!level || !minecraft->player) {
        return;
    }

    minecraft->player->hurtArmor(packet->dmg);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, RespawnPacket* packet)
{
	if (level) {
		NetEventCallback::handle(level, source, packet );
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ContainerOpenPacket* packet)
{
	if (!level || !minecraft || !minecraft->player)
		return;

	if (packet->type == ContainerType::FURNACE) {
		FurnaceTileEntity* te = new FurnaceTileEntity();
		te->clientSideOnly = true;
		minecraft->player->openFurnace(te);
		if (minecraft->player->containerMenu)
			minecraft->player->containerMenu->containerId = packet->containerId;
	}
	if (packet->type == ContainerType::CONTAINER) {
		ChestTileEntity* te = new ChestTileEntity();
		te->clientSideOnly = true;
		minecraft->player->openContainer(te);
		if (minecraft->player->containerMenu)
			minecraft->player->containerMenu->containerId = packet->containerId;
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ContainerClosePacket* packet)
{
	if (minecraft && minecraft->player && minecraft->player->containerMenu)
		minecraft->player->closeContainer();
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ContainerSetContentPacket* packet)
{
	if (!minecraft || !minecraft->player)
		return;

    if (packet->containerId == 0) {
        for (unsigned int i = 0; i < packet->items.size(); ++i) {
            minecraft->player->inventory->setItem(Inventory::MAX_SELECTION_SIZE + i, &packet->items[i]);
        }
    } else if (minecraft->player->containerMenu && minecraft->player->containerMenu->containerId == packet->containerId) {
        for (unsigned int i = 0; i < packet->items.size(); ++i) {
            minecraft->player->containerMenu->setSlot(i, &packet->items[i]);
        }
    }
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ContainerSetSlotPacket* packet)
{
	if (!minecraft->player
	  || !minecraft->player->containerMenu
	  || minecraft->player->containerMenu->containerId != packet->containerId)
	  return;

	minecraft->player->containerMenu->setSlot(packet->slot, &packet->item);
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, ContainerSetDataPacket* packet)
{
	if (minecraft->player && minecraft->player->containerMenu && minecraft->player->containerMenu->containerId == packet->containerId) {
		minecraft->player->containerMenu->setData(packet->id, packet->value);
	}
}

void ClientSideNetworkHandler::handle( const RakNet::RakNetGUID& source, ChatPacket* packet )
{
#ifndef STANDALONE_SERVER
	minecraft->gui.displayClientMessage(packet->message);
#endif
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, SignUpdatePacket* packet)
{
	if (!level)
		return;

	TileEntity* te = level->getTileEntity(packet->x, packet->y, packet->z);
	if (TileEntity::isType(te, TileEntityType::Sign)) {
		SignTileEntity* ste = (SignTileEntity*) te;
		if (ste->isEditable()) {
			for (int i = 0; i < SignTileEntity::NUM_LINES; i++) {
				ste->messages[i] = packet->lines[i];
			}
		}
	}
}

void ClientSideNetworkHandler::handle(const RakNet::RakNetGUID& source, AdventureSettingsPacket* packet) {
    if (!level)
		return;

    packet->fillIn(level->adventureSettings);
}

void ClientSideNetworkHandler::clearChunksLoaded()
{
	loadedChunks.clear();
	while (!pendingChunks.empty()) pendingChunks.pop();
	initialChunksLoaded = false;
	totalInitialChunks = 0;
	loadedInitialChunks = 0;
}
