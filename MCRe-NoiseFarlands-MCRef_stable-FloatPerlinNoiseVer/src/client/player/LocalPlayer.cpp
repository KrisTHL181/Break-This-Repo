#include "LocalPlayer.h"
#include "../Minecraft.h"
#include "../../ErrorCodes.h"
#include "../../world/entity/EntityEvent.h"
#include "../../world/entity/player/Player.h"
#include "../../world/inventory/BaseContainerMenu.h"
#include "../../world/item/BowItem.h"
#include "../../world/level/Level.h"
#include "../../world/level/tile/Tile.h"
#include "../../world/level/tile/entity/TileEntity.h"
#include "../../world/level/material/Material.h"
#include "../../network/packet/ContainerClosePacket.h"
#include "../../network/packet/MovePlayerPacket.h"
#include "../../network/packet/PlayerEquipmentPacket.h"
#include "../../network/RakNetInstance.h"
#include "../../network/packet/DropItemPacket.h"
#include "../../network/packet/SetHealthPacket.h"
#include "../../network/packet/SendInventoryPacket.h"
#include "../../network/packet/EntityEventPacket.h"
#include "../../network/packet/PlayerActionPacket.h"
#include <vector>
#include <cctype>
#include "../../platform/log.h"
#include "../../platform/HttpClient.h"
#include "../../platform/CThread.h"
#include "../../util/StringUtils.h"
#include "client/Options.h"

#if defined(_WIN32)
#include <direct.h>
#include <sys/stat.h>
#else
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#endif

#ifndef STANDALONE_SERVER
#include "../gui/Screen.h"
#include "../gui/screens/FurnaceScreen.h"
#include "../gui/screens/ChestScreen.h"
#include "../gui/screens/crafting/WorkbenchScreen.h"
#include "../gui/screens/crafting/StonecutterScreen.h"
#include "../gui/screens/InBedScreen.h"
#include "../gui/screens/TextEditScreen.h"
#include "../particle/TakeAnimationParticle.h"
#endif
#include "../../network/packet/AnimatePacket.h"
#include "../../world/item/ArmorItem.h"
#include "../../network/packet/PlayerArmorEquipmentPacket.h"

namespace {
#ifndef STANDALONE_SERVER

static bool isBase64(unsigned char c) {
    return (std::isalnum(c) || (c == '+') || (c == '/'));
}

static std::string base64Decode(const std::string& encoded) {
    static const std::string base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string out;
    int in_len = (int)encoded.size();
    int i = 0;
    int in_ = 0;
    unsigned char char_array_4[4], char_array_3[3];

    while (in_len-- && (encoded[in_] != '=') && isBase64(encoded[in_])) {
        char_array_4[i++] = encoded[in_]; in_++;
        if (i == 4) {
            for (i = 0; i < 4; i++)
                char_array_4[i] = (unsigned char)base64Chars.find(char_array_4[i]);

            char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
            char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
            char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

            for (i = 0; i < 3; i++)
                out += char_array_3[i];
            i = 0;
        }
    }

    if (i) {
        for (int j = i; j < 4; j++)
            char_array_4[j] = 0;
        for (int j = 0; j < 4; j++)
            char_array_4[j] = (unsigned char)base64Chars.find(char_array_4[j]);

        char_array_3[0] = (char_array_4[0] << 2) + ((char_array_4[1] & 0x30) >> 4);
        char_array_3[1] = ((char_array_4[1] & 0xf) << 4) + ((char_array_4[2] & 0x3c) >> 2);
        char_array_3[2] = ((char_array_4[2] & 0x3) << 6) + char_array_4[3];

        for (int j = 0; (j < i - 1); j++)
            out += char_array_3[j];
    }
    return out;
}

static std::string extractJsonString(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\"";
    size_t pos = json.find(search);
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos + search.size());
    if (pos == std::string::npos) return "";
    pos++;
    while (pos < json.size() && std::isspace((unsigned char)json[pos])) pos++;
    if (pos >= json.size() || json[pos] != '"') return "";
    pos++;
    size_t end = json.find('"', pos);
    if (end == std::string::npos) return "";
    return json.substr(pos, end - pos);
}

static std::string getTextureUrlForUsername(const std::string& username, const std::string& textureKey) {
    if (username.empty()) {
        LOGI("[%s] username empty\n", textureKey.c_str());
        return "";
    }

    LOGI("[%s] resolving UUID for user '%s'...\n", textureKey.c_str(), username.c_str());
    std::vector<unsigned char> body;
    std::string apiUrl = "http://api.mojang.com/users/profiles/minecraft/" + username;
    if (!HttpClient::download(apiUrl, body)) {
        LOGW("[%s] failed to download UUID for %s\n", textureKey.c_str(), username.c_str());
        return "";
    }

    std::string response(body.begin(), body.end());
    std::string uuid = extractJsonString(response, "id");
    if (uuid.empty()) {
        LOGW("[%s] no UUID found in Mojang response for %s\n", textureKey.c_str(), username.c_str());
        return "";
    }

    LOGI("[%s] UUID=%s for user %s\n", textureKey.c_str(), uuid.c_str(), username.c_str());

    std::string profileUrl = "http://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
    if (!HttpClient::download(profileUrl, body)) {
        LOGW("[%s] failed to download profile for UUID %s\n", textureKey.c_str(), uuid.c_str());
        return "";
    }

    response.assign(body.begin(), body.end());
    std::string encoded = extractJsonString(response, "value");
    if (encoded.empty()) {
        LOGW("[%s] no value field in profile response for UUID %s\n", textureKey.c_str(), uuid.c_str());
        return "";
    }

    std::string decoded = base64Decode(encoded);

    std::string searchKey = "\"" + textureKey + "\"";
    size_t texturePos = decoded.find(searchKey);
    if (texturePos == std::string::npos) {
        LOGW("[%s] no %s entry in decoded profile for UUID %s\n", textureKey.c_str(), textureKey.c_str(), uuid.c_str());
        return "";
    }
    size_t urlPos = decoded.find("\"url\"", texturePos);
    if (urlPos == std::string::npos) {
        LOGW("[%s] no url field under %s for UUID %s\n", textureKey.c_str(), textureKey.c_str(), uuid.c_str());
        return "";
    }

    // extract the URL value from the substring starting at urlPos
    std::string urlFragment = decoded.substr(urlPos);
    std::string textureUrl = extractJsonString(urlFragment, "url");
    if (textureUrl.empty()) {
        LOGW("[%s] failed to parse %s URL for UUID %s\n", textureKey.c_str(), textureKey.c_str(), uuid.c_str());
        return "";
    }

    LOGI("[%s] %s URL for %s: %s\n", textureKey.c_str(), textureKey.c_str(), username.c_str(), textureUrl.c_str());
    return textureUrl;
}

static std::string getSkinUrlForUsername(const std::string& username) {
    return getTextureUrlForUsername(username, "SKIN");
}

static std::string getCapeUrlForUsername(const std::string& username) {
    return getTextureUrlForUsername(username, "CAPE");
}

#endif

static bool ensureDirectoryExists(const std::string& path) {
#if defined(_WIN32)
    return _mkdir(path.c_str()) == 0 || errno == EEXIST;
#else
    struct stat st;
    if (stat(path.c_str(), &st) == 0 && S_ISDIR(st.st_mode))
        return true;

    std::string subPath;
    size_t i = 0;
    while (i < path.length()) {
        i = path.find_first_of("/\\", i);
        if (i == std::string::npos) {
            subPath = path;
        } else {
            subPath = path.substr(0, i);
        }

        if (!subPath.empty()) {
            if (stat(subPath.c_str(), &st) != 0) {
                if (mkdir(subPath.c_str(), 0755) != 0 && errno != EEXIST)
                    return false;
            } else if (!S_ISDIR(st.st_mode)) {
                return false;
            }
        }

        if (i == std::string::npos)
            break;
        i += 1;
    }

    if (stat(path.c_str(), &st) == 0 && S_ISDIR(st.st_mode))
        return true;
    return mkdir(path.c_str(), 0755) == 0 || errno == EEXIST;
#endif
}

static bool fileExists(const std::string& path) {
    struct stat st;
    if (stat(path.c_str(), &st) != 0)
        return false;

#if defined(_WIN32)
    return (st.st_mode & _S_IFREG) != 0;
#else
    return S_ISREG(st.st_mode);
#endif
}

#ifndef STANDALONE_SERVER

static void* fetchSkinForPlayer(void* param) {
    LocalPlayer* player = (LocalPlayer*)param;
    if (!player) return NULL;

    LOGI("[Skin] starting skin download for %s\n", player->name.c_str());

    const std::string cacheDir = "data/images/skins";
    if (!ensureDirectoryExists(cacheDir)) {
        LOGW("[Skin] failed to create cache directory %s\n", cacheDir.c_str());
    }

    std::string cacheFile = cacheDir + "/" + player->name + ".png";
    if (fileExists(cacheFile)) {
        LOGI("[Skin] using cached skin for %s\n", player->name.c_str());
        player->setTextureName("skins/" + player->name + ".png");
        return NULL;
    }

    std::string skinUrl = getSkinUrlForUsername(player->name);
    if (skinUrl.empty()) {
        LOGW("[Skin] skin URL lookup failed for %s\n", player->name.c_str());
        player->setTextureName("mob/char.png");
        return NULL;
    }

    LOGI("[Skin] downloading skin from %s\n", skinUrl.c_str());
    std::vector<unsigned char> skinData;
    if (!HttpClient::download(skinUrl, skinData) || skinData.empty()) {
        LOGW("[Skin] download failed for %s\n", skinUrl.c_str());
        player->setTextureName("mob/char.png");
        return NULL;
    }

    // Save to cache
    FILE* fp = fopen(cacheFile.c_str(), "wb");
    if (fp) {
        fwrite(skinData.data(), 1, skinData.size(), fp);
        fclose(fp);
        LOGI("[Skin] cached skin to %s\n", cacheFile.c_str());

		player->setTextureName("skins/" + player->name + ".png");
	} else {
        LOGW("[Skin] failed to write skin cache %s\n", cacheFile.c_str());
        player->setTextureName("mob/char.png");
    }

    return NULL;
}

static void* fetchCapeForPlayer(void* param) {
    LocalPlayer* player = (LocalPlayer*)param;
    if (!player) return NULL;

    LOGI("[Cape] starting cape download for %s\n", player->name.c_str());

    const std::string cacheDir = "data/images/capes";
    if (!ensureDirectoryExists(cacheDir)) {
        LOGW("[Cape] failed to create cache directory %s\n", cacheDir.c_str());
    }

    std::string cacheFile = cacheDir + "/" + player->name + ".png";
    if (fileExists(cacheFile)) {
        LOGI("[Cape] using cached cape for %s\n", player->name.c_str());
        player->setCapeTextureName("capes/" + player->name + ".png");
        return NULL;
    }

    std::string capeUrl = getCapeUrlForUsername(player->name);
    if (capeUrl.empty()) {
        LOGW("[Cape] cape URL lookup failed for %s\n", player->name.c_str());
        return NULL;
    }

    LOGI("[Cape] downloading cape from %s\n", capeUrl.c_str());
    std::vector<unsigned char> capeData;
    if (!HttpClient::download(capeUrl, capeData) || capeData.empty()) {
        LOGW("[Cape] download failed for %s\n", capeUrl.c_str());
        return NULL;
    }

    // Save to cache
    FILE* fp = fopen(cacheFile.c_str(), "wb");
    if (fp) {
        fwrite(capeData.data(), 1, capeData.size(), fp);
        fclose(fp);
        LOGI("[Cape] cached cape to %s\n", cacheFile.c_str());

		player->setCapeTextureName("capes/" + player->name + ".png");
    } else {
        LOGW("[Cape] failed to write cape cache %s\n", cacheFile.c_str());
    }

    return NULL;
}

#endif

//@note: doesn't work completely, since it doesn't care about stairs rotation
static bool isJumpable(int tileId) {
	return tileId != Tile::fence->id
		&& tileId != Tile::fenceGate->id
		&& tileId != Tile::stoneSlabHalf->id
		&& tileId != Tile::trapdoor->id
        && tileId != Tile::sign->id
        && tileId != Tile::wallSign->id
		&& (Tile::tiles[tileId] != NULL && Tile::tiles[tileId]->getRenderShape() != Tile::SHAPE_STAIRS);
}

} // anonymous namespace

LocalPlayer::LocalPlayer(Minecraft* minecraft, Level* level, const std::string& username, int dimension, bool isCreative)
:	Player(level, isCreative),
	minecraft(minecraft),
	input(NULL),
	sentInventoryItemId(-1),
	sentInventoryItemData(-1),
	autoJumpEnabled(true),
	armorTypeHash(0),
	sprinting(false),
	sprintDoubleTapTimer(0),
	prevForwardHeld(false),
    m_skinThread(nullptr),
    m_capeThread(nullptr)
{
	this->dimension = dimension;
	_init();
#ifndef STANDALONE_SERVER

	if (minecraft->options.getStringValue(OPTIONS_USERNAME).size() != 0) {
		textureName = "mob/char.png";

		this->name = minecraft->options.getStringValue(OPTIONS_USERNAME);
		printf("test \n");
		// Fetch user skin and cape from Mojang servers in the background (avoids blocking the main thread)
		// TODO: Fix this memory leak
		// 原代码：
// new CThread(fetchSkinForPlayer, this);
// new CThread(fetchCapeForPlayer, this);

// 新代码：
m_skinThread = new CThread(fetchSkinForPlayer, this);
m_capeThread = new CThread(fetchCapeForPlayer, this);
	}
#endif
}

LocalPlayer::~LocalPlayer() {
    delete m_skinThread;   // CThread 析构应安全处理正在运行的线程
    delete m_capeThread;
    // 原有 delete input 等...
	//delete input;
	//input = NULL;
}

/*private*/
void LocalPlayer::calculateFlight(float xa, float ya, float za) {
	float flySpeed = 0.1;
	float sensivity = minecraft->options.getProgressValue(OPTIONS_SENSITIVITY);

    xa = xa * flySpeed;
    ya = 0;
    za = za * flySpeed;

#ifdef ANDROID
    if (Keyboard::isKeyDown(103)) ya = .2f * flySpeed;
    if (Keyboard::isKeyDown(102)) ya = -.2f * flySpeed;
#else
    if (Keyboard::isKeyDown(Keyboard::KEY_E)) ya = .2f * flySpeed;
    if (Keyboard::isKeyDown(Keyboard::KEY_Q)) ya = -.2f * flySpeed;
#endif

    flyX = 10 * smoothFlyX.getNewDeltaValue(xa, .35f * sensivity);
    flyY = 10 * smoothFlyY.getNewDeltaValue(ya, .35f * sensivity);
    flyZ = 10 * smoothFlyZ.getNewDeltaValue(za, .35f * sensivity);
}

bool LocalPlayer::isSolidTile(int x, int y, int z) {
	int tileId = level->getTile(x, y, z);
	return tileId > 0 && Tile::tiles[tileId]->material->isSolid();
}

void LocalPlayer::tick() {

	super::tick();
	 // 每帧用 Big 精度重算原点，自动切换
    // 用 Big 绝对坐标推进 WorldOrigin（精度无限）
m_origin.tickBig(m_bigAbsX, m_bigAbsY, m_bigAbsZ);
	if(!useItem.isNull()) {
		ItemInstance* item = inventory->getSelected();
		if(item != NULL && *item == useItem) {
			if (useItemDuration <= 25 && useItemDuration % 4 == 0) {
				spawnEatParticles(item, 5);
			}
			if(--useItemDuration == 0) {
				if(!level->isClientSide) {
					completeUsingItem();
				} else {
					EntityEventPacket p(entityId, EntityEvent::USE_ITEM_COMPLETE);
					level->raknetInstance->send(p);
				}
			}
		}
		else {
			stopUsingItem();
		}
	}
	if (minecraft->isOnline())
	{
		if (std::abs(x - sentX) > .1f || std::abs(y - sentY) > .01f || std::abs(z - sentZ) > .1f || std::abs(sentRotX - xRot) > 1 || std::abs(sentRotY - yRot) > 1)
		{
			MovePlayerPacket packet(entityId, x, y - heightOffset, z, xRot, yRot);
			minecraft->raknetInstance->send(packet);
			sentX = x;
			sentY = y;
			sentZ = z;
			sentRotX = xRot;
			sentRotY = yRot;
		}

		ItemInstance* item = inventory->getSelected();
		int newItemId   = (item && item->count > 0) ? item->id : 0;
		int newItemData = (item && item->count > 0) ? item->getAuxValue() : 0;

		if (sentInventoryItemId != newItemId
		||  sentInventoryItemData != newItemData)
		{
			sentInventoryItemId   = newItemId;
			sentInventoryItemData = newItemData;
			PlayerEquipmentPacket packet(entityId, newItemId, newItemData);
			minecraft->raknetInstance->send(packet);
		}
	}
/*
    for (int i = 0; i < 4; ++i) {
        ItemInstance* a = getArmor(i);
        if (!a) continue;

        ArmorItem* item = (ArmorItem*) a->getItem();

        printf("armor %d: %d\n", i, a->getAuxValue());
    }
*/

	updateArmorTypeHash();
#ifndef STANDALONE_SERVER
	if (!minecraft->screen && containerMenu) {
		static bool hasPostedError = false;
		if (!hasPostedError) {
			minecraft->gui.postError( ErrorCodes::ContainerRefStillExistsAfterDestruction );
			hasPostedError = true;
		}
	}
#endif
	//LOGI("biome: %s\n", level->getBiomeSource()->getBiome((int)x >> 4, (int)z >> 4)->name.c_str());
}

/*public*/
void LocalPlayer::aiStep() {
	jumpTriggerTime--;
	ascendTriggerTime--;
	descendTriggerTime--;

	bool wasJumping = input->jumping;
#ifndef STANDALONE_SERVER
    bool screenCovering = minecraft->screen && !minecraft->screen->passEvents;
	if (!screenCovering)
		input->tick(this);

	// Sprint: detect W double-tap
	{
		bool forwardHeld = (input->ya > 0);
		if (forwardHeld && !prevForwardHeld && minecraft->options.getBooleanValue(OPTIONS_ALLOW_SPRINT)) {
			// leading edge of W press
			if (sprintDoubleTapTimer > 0)
				sprinting = true;
			else
				sprintDoubleTapTimer = 7;
		}
		if (!forwardHeld) {
			sprinting = false;
		}
		if (sprintDoubleTapTimer > 0) sprintDoubleTapTimer--;
		prevForwardHeld = forwardHeld;
	}
	if (input->sneaking || abilities.flying)
		sprinting = false;

    if (input->sneaking) {
        if (ySlideOffset < 0.2f) ySlideOffset = 0.2f;
    }
#endif
	if (abilities.mayfly) {
		// Check for flight toggle
		if (!wasJumping && input->jumping) {
			if (jumpTriggerTime <= 0) jumpTriggerTime = 7;
			else {
				abilities.flying = !abilities.flying;
				jumpTriggerTime = 0;
			}
		}
		if (abilities.flying) {
			if (input->wantUp) {
				yd += 0.15f;
				//xd = zd = 0;
			}
			if (input->wantDown) {
				yd -= 0.15f;
			}
		}
	}

	if(isUsingItem()) {
		const float k = 0.35f;
		input->xa *= k;
		input->ya *= k;
	}

	Mob::aiStep();
    super::aiStep();

	//if (onGround && abilities.flying)
	//	abilities.flying = false;

	if (interpolateOnly())
		updateAi();
}

/*public*/
void LocalPlayer::closeContainer() {
	if (level->isClientSide) {
		ContainerClosePacket packet(containerMenu->containerId);
		minecraft->raknetInstance->send(packet);
	}
	super::closeContainer();
    minecraft->setScreen(NULL);
}

//@Override
void LocalPlayer::move(double xa, double ya, double za) {
    if (this == minecraft->player && minecraft->options.getBooleanValue(OPTIONS_IS_FLYING)) {
        noPhysics = true;
        double tmp = walkDist;
        // 飞行速度使用 flyingSpeed（默认 0.02f）
        calculateFlight(xa, ya, za);
        fallDistance = 0;
        yd = 0;
        super::move(flyX, flyY, flyZ);
        onGround = true;
        walkDist = tmp;
    } else {
        if (autoJumpTime > 0) {
            autoJumpTime--;
            input->jumping = true;
        }
        double prevX = x, prevZ = z;
        super::move(xa, ya, za);
        double newX = x, newZ = z;
        if (autoJumpTime <= 0 && minecraft->options.getBooleanValue(OPTIONS_AUTOJUMP)) {
            bool jump = false;
            if (Mth::floor(prevX * 2.0f) != Mth::floor(newX * 2.0f) || Mth::floor(prevZ * 2.0f) != Mth::floor(newZ * 2.0f)) {
                float dist = Mth::sqrt(xa * xa + za * za);
                const int xx = Mth::floor(x + xa / dist);
                const int zz = Mth::floor(z + za / dist);
                const int tileId = level->getTile(xx, (int)(y-1), zz);
                jump = (isSolidTile(xx, (int)(y-1), zz)
                    && !isSolidTile(xx, (int)y, zz) && !isSolidTile(xx, (int)(y+1), zz))
                    && isJumpable(tileId);
            }
            if (jump) autoJumpTime = 1;
        }
    }
}

void LocalPlayer::updateAi() {
    super::updateAi();
    this->xxa = input->xa;
    this->yya = input->ya;
    this->jumping = input->jumping || autoJumpTime > 0;
}

void LocalPlayer::take( Entity* e, int orgCount )
{
#ifndef STANDALONE_SERVER
	if (e->isItemEntity())
		minecraft->particleEngine->add(new TakeAnimationParticle(minecraft->level, (ItemEntity*)e, this, -0.5f));
#endif
}

void LocalPlayer::setKey( int eventKey, bool eventKeyState )
{
	input->setKey(eventKey, eventKeyState);
}

void LocalPlayer::releaseAllKeys()
{
	if (input) input->releaseAllKeys();
}

float LocalPlayer::getWalkingSpeedModifier() {
	return sprinting ? 1.3f : 1.0f;
}

float LocalPlayer::getFieldOfViewModifier() {
	float targetFov = 1.0f;
	if(abilities.flying) targetFov *= 1.1f;
	targetFov *= ((walkingSpeed * getWalkingSpeedModifier()) / DEFAULT_WALK_SPEED +1) / 2;

	if(isUsingItem() && getUseItem()->id == Item::bow->id) {
		float ticksHeld = (float)getTicksUsingItem();
		float scale = ticksHeld / BowItem::MAX_DRAW_DURATION;
		if(scale > 1) {
			scale = 1;
		}
		else {
			scale *= scale;
		}
		targetFov *= 1.0f - scale * 0.15f;
	}
	return targetFov;
}
void LocalPlayer::addAdditonalSaveData( CompoundTag* entityTag )
{
	super::addAdditonalSaveData(entityTag);
	entityTag->putInt("Score", score);
}

void LocalPlayer::readAdditionalSaveData( CompoundTag* entityTag )
{
	super::readAdditionalSaveData(entityTag);
	score = entityTag->getInt("Score");
}

bool LocalPlayer::isSneaking()
{
	return input->sneaking;
}

void LocalPlayer::hurtTo( int newHealth )
{
	int dmg = health - newHealth;
	if (dmg <= 0) {
		this->health = newHealth;
	} else {
		lastHurt = dmg;
		lastHealth = health;
		invulnerableTime = invulnerableDuration;

		minecraft->player->bypassArmor = true;
		actuallyHurt(dmg);
		minecraft->player->bypassArmor = false;

		hurtTime = hurtDuration = 10;
	}
}

void LocalPlayer::actuallyHurt( int dmg )
{
#ifndef STANDALONE_SERVER
    if (minecraft->screen && minecraft->screen->closeOnPlayerHurt()) {
		if (containerMenu) closeContainer();
        else minecraft->setScreen(NULL);
	}
#endif
    super::actuallyHurt(dmg);
}

void LocalPlayer::respawn()
{
	minecraft->respawnPlayer();
}

void LocalPlayer::die(Entity* source)
{
	// If we're an online client, send the inventory to be dropped
	// If we're the server, drop the inventory immediately
	if (level->isClientSide) {
		SendInventoryPacket packet(this, true);
		minecraft->raknetInstance->send(packet);
	}
	inventory->dropAll(level->isClientSide);
	for (int i = 0; i < NUM_ARMOR; ++i) {
		ItemInstance* item = getArmor(i);
		if (!ItemInstance::isArmorItem(item)) return;

		drop(new ItemInstance(*item), true);
		setArmor(i, NULL);
	}

	super::die(source);
}

void LocalPlayer::swing() {
    super::swing();

    if (swingTime == -1) {
        AnimatePacket packet(AnimatePacket::Swing, this);
        packet.reliability = UNRELIABLE;
        packet.priority = MEDIUM_PRIORITY;
        minecraft->raknetInstance->send(packet);
    }
}

void LocalPlayer::reset() {
	super::reset();
	this->_init();
}

void LocalPlayer::_init() {
	autoJumpTime		= 0;
	jumpTriggerTime		= 0;
	ascendTriggerTime	= 0;
	descendTriggerTime	= 0;
	ascending	= false;
	descending	= false;
	sentX = sentY = sentZ = 0.0;
    sentRotX = sentRotY = 0.0f;
	autoSendPosRot = false;  // 🔧 本地玩家不发送网络位置包

	ItemInstance* item = inventory->getSelected();
	sentInventoryItemId = item? item->id : 0;
	sentInventoryItemData = item? item->getAuxValue() : 0;
}

void LocalPlayer::startCrafting(int x, int y, int z, int tableSize) {
#ifndef STANDALONE_SERVER
	if (!minecraft->isCreativeMode())
		minecraft->setScreen( new WorkbenchScreen(tableSize) );
#endif
}

void LocalPlayer::startStonecutting(int x, int y, int z) {
#ifndef STANDALONE_SERVER
	if (!minecraft->isCreativeMode())
		minecraft->setScreen( new StonecutterScreen() );
#endif
}

void LocalPlayer::openFurnace( FurnaceTileEntity* e ) {
#ifndef STANDALONE_SERVER
	if (!minecraft->isCreativeMode())
		minecraft->setScreen( new FurnaceScreen(this, e) );
#endif
}

void LocalPlayer::openContainer( ChestTileEntity* container ) {
#ifndef STANDALONE_SERVER
	if (!minecraft->isCreativeMode())
		minecraft->setScreen( new ChestScreen(this, container) );
#endif
}

void LocalPlayer::drop( ItemInstance* item, bool randomly )
{
	if (!item)
		return;

	if (level->isClientSide) {
		DropItemPacket packet(entityId, *item);
		minecraft->raknetInstance->send(packet);
		// delete the ItemEntity here, since we don't add it to level
		delete item;
	} else {
		super::drop(item, randomly);
	}
}

void LocalPlayer::causeFallDamage( float distance )
{
	int dmg = (int) ceil((distance - 3));
	if (dmg > 0) {
		if (level->isClientSide) {
			SetHealthPacket packet(SetHealthPacket::HEALTH_MODIFY_OFFSET + dmg);
			minecraft->raknetInstance->send(packet);
		}
	}
	super::causeFallDamage(distance);

}

void LocalPlayer::displayClientMessage( const std::string& messageId ) {
#ifndef STANDALONE_SERVER
	minecraft->gui.displayClientMessage(messageId);
#endif
}

int LocalPlayer::startSleepInBed( int x, int y, int z ) {
	int startSleepInBedReturnValue = super::startSleepInBed(x, y, z);
#ifndef STANDALONE_SERVER
	if(startSleepInBedReturnValue == BedSleepingResult::OK)
		minecraft->setScreen(new InBedScreen());
#endif
	return startSleepInBedReturnValue;
}

void LocalPlayer::stopSleepInBed( bool forcefulWakeUp, bool updateLevelList, bool saveRespawnPoint ) {
	if(level->isClientSide) {
		PlayerActionPacket packet(PlayerActionPacket::STOP_SLEEPING, 0, 0, 0, 0, entityId);
		minecraft->raknetInstance->send(packet);
	}
#ifndef STANDALONE_SERVER
	minecraft->setScreen(NULL);
#endif
	super::stopSleepInBed(forcefulWakeUp, updateLevelList, saveRespawnPoint);
}

void LocalPlayer::openTextEdit( TileEntity* tileEntity ) {
#if !defined(STANDALONE_SERVER) && !defined(RPI)
	if(tileEntity->type == TileEntityType::Sign)
		minecraft->setScreen(new TextEditScreen((SignTileEntity*) tileEntity));
#endif
}

void LocalPlayer::updateArmorTypeHash() {
    int hash = getArmorTypeHash();
    if (hash != armorTypeHash) {
        PlayerArmorEquipmentPacket p(this);
        minecraft->raknetInstance->send(p);
        armorTypeHash = hash;
    }
}
