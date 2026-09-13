#include "Minecraft.h"
#include "Options.h"
#include "client/Options.h"
#include "client/player/input/IBuildInput.h"
#include "platform/input/Keyboard.h"
#include "world/item/Item.h"
#include "world/item/ItemInstance.h"
#include <string>
#include <cstdlib>

#if defined(APPLE_DEMO_PROMOTION)
    #define NO_NETWORK
#endif

#if defined(RPI)
	#define CREATORMODE
#endif
#include "../network/RakNetInstance.h"
#include "../network/ClientSideNetworkHandler.h"
#include "../network/ServerSideNetworkHandler.h"
//#include "../network/Packet.h"
#include "../world/entity/player/Inventory.h"
#include "../world/level/tile/Tile.h"
#include "../world/level/storage/LevelStorageSource.h"
#include "../world/level/storage/LevelStorage.h"
#include "player/input/KeyboardInput.h"
#include "player/input/ControllerTurnInput.h"
#include "player/input/XperiaPlayInput.h"
#include "world/level/chunk/ChunkSource.h"

#ifndef STANDALONE_SERVER
#include "player/input/touchscreen/TouchInputHolder.h"
#include "particle/ParticleEngine.h"
#include "gui/Screen.h"
#include "gui/Font.h"
#include "gui/screens/RenameMPLevelScreen.h"
#include "gui/screens/ConsoleScreen.h"
#include "gui/screens/ChatScreen.h"
#include "sound/SoundEngine.h"
#include "player/input/touchscreen/TouchscreenInput.h"
#include "renderer/Chunk.h"
#include "gui/screens/PrerenderTilesScreen.h"
#include "renderer/Textures.h"
#include "gui/screens/DeathScreen.h"
#include "gui/screens/FurnaceScreen.h"
#include "gui/screens/ArmorScreen.h"
#include "renderer/tileentity/TileEntityRenderDispatcher.h"
#include "renderer/ptexture/DynamicTexture.h"
#include "renderer/GameRenderer.h"
#include "renderer/ItemInHandRenderer.h"
#include "renderer/LevelRenderer.h"
#include "renderer/entity/EntityRenderDispatcher.h"
#include "gui/Screen.h"
#include "gui/Font.h"
#include "gui/screens/RenameMPLevelScreen.h"
#include "sound/SoundEngine.h"
#endif // STANDALONE_SERVER

#include "player/LocalPlayer.h"
#include "gamemode/CreativeMode.h"
#include "gamemode/SurvivalMode.h"
#include "player/LocalPlayer.h"
#include "../platform/CThread.h"
#include "../platform/input/Mouse.h"
#include "../AppPlatform.h"
#include "../LicenseCodes.h"
#include "../util/PerfTimer.h"
#include "../util/PerfRenderer.h"
#include "player/input/MouseBuildInput.h"

#include "player/input/IInputHolder.h"

#include "player/input/MouseTurnInput.h"
#include "../world/entity/MobFactory.h"
#include "../world/level/MobSpawner.h"
#include "../util/Mth.h"
#include "../network/packet/InteractPacket.h"
#include "../network/packet/RespawnPacket.h"
#include "../network/packet/AdventureSettingsPacket.h"
#include "../network/packet/SetSpawnPositionPacket.h"
#include "IConfigListener.h"
#include "../world/entity/MobCategory.h"
#include "../world/Difficulty.h"
#include "../server/ServerLevel.h"

#ifdef CREATORMODE
#include "../server/CreatorLevel.h"
#endif

#include "../network/command/CommandServer.h"
#include "gamemode/CreatorMode.h"

#include <algorithm>
//Exper

Minecraft* Minecraft::instance = nullptr;

static void checkGlError(const char* tag) {
#ifdef GLDEBUG
	while (1) {
        const int errCode = glGetError();
        if (errCode == GL_NO_ERROR) break;

		LOGE("################\nOpenGL-error @ %s : #%d\n", tag, errCode);
	}
#endif /*GLDEBUG*/
}

/*static*/
const char* Minecraft::progressMessages[] = {
	"Locating server",
	"Building terrain",
	"Preparing",
	"Saving chunks"
};

int Minecraft::customDebugId = Minecraft::CDI_NONE;

#if defined(_MSC_VER)
	#pragma warning( disable : 4355 ) // 'this' pointer in initialization list which is perfectly legal
#endif

bool Minecraft::useAmbientOcclusion = false;

Minecraft::Minecraft() :	
	level(NULL),
	player(NULL),
	cameraTargetPlayer(NULL),
	levelRenderer(NULL),
	gameRenderer(NULL),
#ifndef STANDALONE_SERVER
	particleEngine(NULL),
	_perfRenderer(NULL),
#endif
	_commandServer(NULL),
#ifndef STANDALONE_SERVER
	textures(NULL),
#endif
	lastTickTime(-1),
	lastTime(0),
	ticksSinceLastUpdate(0),
	gameMode(NULL),
	mouseGrabbed(true),
	missTime(0),
	pause(false),
	_running(false),
	timer(20),
#ifndef STANDALONE_SERVER
	gui(this),
#endif
	netCallback(NULL),
#ifndef STANDALONE_SERVER
	screen(NULL),
	font(NULL),
#endif
	screenMutex(false),
#ifndef STANDALONE_SERVER
	scheduledScreen(NULL),
	hasScheduledScreen(false),
	soundEngine(NULL),
#endif
	ticks(0),
	isGeneratingLevel(false),
	_hasSignaledGeneratingLevelFinished(true),
	generateLevelThread(NULL),
	progressStagePercentage(0),
	progressStageStatusId(0),
	isLookingForMultiplayer(false),
	_licenseId(LicenseCodes::WAIT_PLATFORM_NOT_READY),
	inputHolder(0),
	_supportsNonTouchscreen(false),
#ifndef STANDALONE_SERVER
	screenChooser(this),
#endif
	width(1), height(1),
#ifdef __APPLE__
    _isSuperFast(false),
#endif
	_powerVr(false),
	commandPort(4711),
	reserved_d1(0),reserved_d2(0),
	reserved_f1(0),reserved_f2(0), options(this)
{
    instance = this;   // <-- 添加这一行
//#ifdef ANDROID
    #if defined(NO_NETWORK)
    raknetInstance = new IRakNetInstance();
#else
	raknetInstance = new RakNetInstance();
#endif
#ifndef STANDALONE_SERVER
	soundEngine = new SoundEngine(20.0f);
	soundEngine->init(this, &options);
#endif
	//setupPieces();
}

Minecraft::~Minecraft()
{
	delete netCallback;
	delete raknetInstance;
#ifndef STANDALONE_SERVER
	delete levelRenderer;
	delete gameRenderer;
	delete particleEngine;

	delete soundEngine;
#endif
	delete gameMode;
#ifndef STANDALONE_SERVER
	delete font;
	delete textures;

	if (screen != NULL) {
		delete screen;
		screen = NULL;
	}
#endif
	if (level != NULL) {
		level->saveGame();
		if (level->getChunkSource())
			level->getChunkSource()->saveAll(true);
		delete level->getLevelStorage();
		delete level;
		level = NULL;
	}

	//delete player;
	delete inputHolder;

	delete storageSource;
	delete _perfRenderer;
	delete _commandServer;

	MobFactory::clearStaticTestMobs();
#ifndef STANDALONE_SERVER
	EntityRenderDispatcher::destroy();
#endif
#ifndef STANDALONE_SERVER
    ItemRenderer::teardown_static();
#endif
}

// Only called by server
void Minecraft::selectLevel( const std::string& levelId, const std::string& levelName, const LevelSettings& settings )
{
#if defined(CREATORMODE)
	level = new CreatorLevel(
#else
	level = new ServerLevel(
#endif
		storageSource->selectLevel(levelId, false),
		levelName,
		settings,
		SharedConstants::GeneratorVersion);

	// note: settings is useless beyond this point, since it's
	//       either copied to LevelData (or LevelData read from file)
	setLevel(level, "Generating level");
	setIsCreativeMode(level->getLevelData()->getGameType() == GameType::Creative);
	_running = true;
}

void Minecraft::setLevel(Level* level, const std::string& message /* ="" */, LocalPlayer* forceInsertPlayer /* = NULL */) {
	cameraTargetPlayer = NULL;
	LOGI("Seed is %ld\n", level->getSeed());

	if (level != NULL) {
		level->raknetInstance = raknetInstance;
        gameMode->initLevel(level);

		if (!player && forceInsertPlayer)
		{
			player = forceInsertPlayer;
			player->resetPos(false);
			//level->addEntity(forceInsertPlayer);
		}
		else if (player != NULL) {
			player->resetPos(false);
			if (level != NULL) {
				level->addEntity(player);
			}
		}
		this->level = level;
		_hasSignaledGeneratingLevelFinished = false;
#ifdef STANDALONE_SERVER
		const bool threadedLevelCreation = false;
#else
		const bool threadedLevelCreation = true;
#endif

		if (threadedLevelCreation) {
			// Threaded
			// "Lock"
			isGeneratingLevel = true;
			generateLevelThread = new CThread(Minecraft::prepareLevel_tspawn, this);
		} else {
			// Non-threaded
			generateLevel("Currently not used", level);
		}
    } else {
        player = NULL;
    }

    this->lastTickTime = 0;
	this->_running = true;
}

void Minecraft::leaveGame(bool renameLevel /*=false*/)
{
    if (isGeneratingLevel || !_hasSignaledGeneratingLevelFinished)
        return;
    
	isGeneratingLevel = false;
	bool saveLevel = level && (!level->isClientSide || renameLevel);

	raknetInstance->disconnect();
	if (saveLevel) {
		// If server or wanting to save level as client, save all unsaved chunks!
		level->getChunkSource()->saveAll(true);
	}

	LOGI("Clearing levels\n");

	cameraTargetPlayer = NULL;
#ifndef STANDALONE_SERVER
	levelRenderer->setLevel(NULL);
	particleEngine->setLevel(NULL);
#endif
	LOGI("Erasing callback\n");
	delete netCallback;
	netCallback = NULL;

	LOGI("Erasing level\n");
	if (level != NULL) {
		delete level->getLevelStorage();
		delete level;
		level = NULL;
	}
	//delete player;
	player = NULL;
	cameraTargetPlayer = NULL;

	_running = false;
#ifndef STANDALONE_SERVER
	gui.clearMessages();
	if (renameLevel) {
		setScreen(new RenameMPLevelScreen(LevelStorageSource::TempLevelId));
	}
	else
		screenChooser.setScreen(SCREEN_STARTMENU);
#endif
}

void Minecraft::prepareLevel(const std::string& title) {
	LOGI("status: 1\n");
	progressStageStatusId = 1;

	Stopwatch A, B, C, D;
	A.start();

	Stopwatch L;

	// Dont update lights if we load the level (ok, actually just with leveldata version=1.+(?))
	if (!level->isNew())
		level->setUpdateLights(false);

	int Max = CHUNK_CACHE_WIDTH * CHUNK_CACHE_WIDTH;
	int pp = 0;
	for (int x = 8; x < (CHUNK_CACHE_WIDTH * CHUNK_WIDTH); x += CHUNK_WIDTH) {
        for (int z = 8; z < (CHUNK_CACHE_WIDTH * CHUNK_WIDTH); z += CHUNK_WIDTH) {
            progressStagePercentage = 100 * pp++ / Max;
            //printf("level generation progress %d\n", progressStagePercentage);
			B.start();
            level->getTile(x, 64, z);
			B.stop();
			L.start();
			if (level->isNew())
				while (level->updateLights())
					;
			L.stop();
        }
    }
	A.stop();
	level->setUpdateLights(true);

	C.start();
	for (int x = 0; x < CHUNK_CACHE_WIDTH; x++)
	{
		for (int z = 0; z < CHUNK_CACHE_WIDTH; z++)
		{
			LevelChunk* chunk = level->getChunk(x, z);
			if (chunk && !chunk->createdFromSave)
			{
				chunk->unsaved = false;
				chunk->clearUpdateMap();
			}
		}
	}
	C.stop();

	LOGI("status: 3\n");
	progressStageStatusId = 3;
	if (level->isNew()) {
		level->setInitialSpawn(); // @note: should obviously be called from Level itself
		level->saveLevelData();
		level->getChunkSource()->saveAll(false);
		level->saveGame();
	} else {
		level->saveLevelData();
		level->loadEntities();
	}

	progressStagePercentage = -1;
	progressStageStatusId = 2;
	LOGI("status: 2\n");

	D.start();
	level->prepare();
	D.stop();

	A.print("Generate level: ");
	L.print(" - light: ");
	B.print(" - getTl: ");
	C.print(" - clear: ");
	D.print(" - prepr: ");
	progressStageStatusId = 0;
}

void Minecraft::update() {
#ifndef STANDALONE_SERVER
    TileRenderer::stripeRepairEnabled = this->options.getBooleanValue(OPTIONS_STRIPE_REPAIR);
    bool newStripeRepair = options.getBooleanValue(OPTIONS_STRIPE_REPAIR);
    if (TileRenderer::stripeRepairEnabled != newStripeRepair) {
        TileRenderer::stripeRepairEnabled = newStripeRepair;
        if (levelRenderer) {
            levelRenderer->allChanged();
        }
        onGraphicsReset();
    }
#endif
	//LOGI("Enter Update\n");

	if (Options::debugGl)
		LOGI(">>>>>>>>>>\n");

	TIMER_PUSH("root");

	//if (level) {
	//	LOGI("numplayers: %d\n", level->players.size());
	//	for (int i = 0; i < level->players.size(); ++i) {
	//		Player* p = level->players[i];
	//		bool inEnt = std::find(level->entities.begin(), level->entities.end(), p) != level->entities.end();
	//		LOGI("  %p, %d, %d - in? %d\n", p, p->entityId, p->owner.ToUint32(p->owner), inEnt);
	//	}
	//}

	// If we're paused (local world / invisible server), freeze gameplay and
	// networking and only keep UI responsive.
	bool freezeGame = pause;

	if (!freezeGame) {
		timer.advanceTime();
	}

	if (raknetInstance && !freezeGame) {
		raknetInstance->runEvents(netCallback);
	}

	TIMER_PUSH("tick");
	int toTick = freezeGame ? 1 : timer.ticks;
	if (!freezeGame) timer.ticks = 0;
	for (int i = 0; i < toTick; ++i, ++ticks)
		tick(i, toTick-1);

	TIMER_POP_PUSH("updatelights");
	if (level && !isGeneratingLevel) {
		level->updateLights();
	}
	TIMER_POP();

	#ifndef STANDALONE_SERVER
        TIMER_PUSH("gameModeRender");        // ★ 给 gameMode->render 单独标签
        if (gameMode != NULL) gameMode->render(timer.a);
        TIMER_POP();                         // ★ 关闭 gameModeRender

        TIMER_PUSH("sound");
        soundEngine->update(player, timer.a);
        TIMER_POP();                         // 额外确保 sound 单独闭合（避免嵌套）
        TIMER_PUSH("render");
        gameRenderer->render(timer.a);
        TIMER_POP();
#else
	CThread::sleep(1);
	#endif
#ifndef STANDALONE_SERVER
	Multitouch::resetThisUpdate();
#endif
	TIMER_POP();
#ifndef STANDALONE_SERVER
	checkGlError("Update finished");

	if (options.getBooleanValue(OPTIONS_RENDER_DEBUG)) {
#ifndef PLATFORM_DESKTOP
		if (!PerfTimer::enabled) {
			PerfTimer::reset();
			PerfTimer::enabled = true;
		}
		_perfRenderer->renderFpsMeter(1);
		checkGlError("render debug");
#endif
	} else {
		PerfTimer::enabled = false;
	}
#endif
	//LOGI("Exit Update\n");
}

void Minecraft::tick(int nTick, int maxTick) {
    TIMER_PUSH("tick-work");          // ★ 包裹整个 tick 函数体

    if (missTime > 0) missTime--;
#ifndef STANDALONE_SERVER
    if (!screen && player) {
        if (player->health <= 0) {
            setScreen(new DeathScreen());
        }
    }
#endif
    TIMER_PUSH("gameMode");
    if (level && !pause) {
        gameMode->tick();
    }

    TIMER_POP_PUSH("commandServer");
    if (level && _commandServer) {
        _commandServer->tick();
    }

    TIMER_POP_PUSH("input");
    tickInput();
#ifndef STANDALONE_SERVER
    TIMER_POP_PUSH("gui");
    gui.tick();
#endif
    //
    // Ongoing level generation ...
    //
    if (isGeneratingLevel) {
        TIMER_POP();   // ★ 弹出 tick-work，然后返回
        return;
    } else if (!_hasSignaledGeneratingLevelFinished) {
        if (generateLevelThread) {
            delete generateLevelThread;
            generateLevelThread = NULL;
        }
        _levelGenerated();
    }

    //
    // Normal game loop ...
    //
    if (level != NULL) {
        if (!pause) {
#ifndef STANDALONE_SERVER
            TIMER_POP_PUSH("gameRenderer");
            gameRenderer->tick(nTick, maxTick);

            TIMER_POP_PUSH("levelRenderer");
            levelRenderer->tick();
#endif
            level->difficulty = options.getIntValue(OPTIONS_DIFFICULTY);
            if (level->isClientSide) level->difficulty = Difficulty::EASY;

            TIMER_POP_PUSH("level");
            level->tickEntities();
            level->tick();
#ifndef STANDALONE_SERVER
            TIMER_POP_PUSH("animateTick");
            if (player) {
                level->animateTick(Mth::floor(player->x), Mth::floor(player->y), Mth::floor(player->z));
            }
#endif
        }
    }
#ifndef STANDALONE_SERVER
    textures->loadAndBindTexture("terrain.png");
    if (!pause && !(screen && !screen->renderGameBehind())) {
        #if !defined(RPI)
            #ifdef __APPLE__
            if (isSuperFast())
            #endif
            {
            if (nTick == maxTick) {
                TIMER_POP_PUSH("textures");
                textures->tick(true);
            }
            }
        #endif
    }
    TIMER_POP_PUSH("particles");
    if (!pause) {
        particleEngine->tick();
    }
    if (screen) {
        screenMutex = true;
        screen->tick();
        screenMutex = false;
    }

    Multitouch::reset();
#endif
    TIMER_POP();   // ★ 正常结束前弹出 tick-work
    // 原有的最后一个 TIMER_POP() 已删除，因为 tick-work 的闭合已足够
	TIMER_POP();
}

class InputRAII {
public:
	~InputRAII() {
#ifndef STANDALONE_SERVER
		Mouse::reset();
		Keyboard::reset();
#endif
	}
};

void Minecraft::tickInput() {
#ifndef STANDALONE_SERVER
	InputRAII raiiInput;

	if (screen && !screen->passEvents) {
		screenMutex = true;
		screen->updateEvents();
		//screen->updateSetScreen();
		screenMutex = false;
		if (hasScheduledScreen) {
			setScreen(scheduledScreen);
			scheduledScreen = NULL;
			hasScheduledScreen = false;
		}
		return;
	}

	if (!player) {
		return;
	}

#ifdef RPI
	bool mouseDiggable = true;
	bool allowGuiClicks = !mouseGrabbed;
#else
	bool mouseDiggable = !gui.isInside(Mouse::getX(), Mouse::getY());
	bool allowGuiClicks = true;
#endif

	TIMER_PUSH("mouse");
	while (Mouse::next()) {
		//if (Mouse::getButtonState(MouseAction::ACTION_LEFT))
		//	LOGI("mouse-down-at: %d, %d\n", Mouse::getX(), Mouse::getY());
        	int passedTime = getTimeMs() - lastTickTime;
        	if (passedTime > 200) continue; // @note: As long Mouse::clear CLEARS the whole buffer, it's safe to break here
		// But since it might be rewritten anyway (and hopefully there aren't a lot of messages, we just continue.

		const MouseAction& e = Mouse::getEvent();

		if (!useTouchscreen() && !mouseGrabbed) {
			if (!screen && e.data == MouseAction::DATA_DOWN) {
				grabMouse();
			}
		}

		if (allowGuiClicks && e.action == MouseAction::ACTION_LEFT && e.data == MouseAction::DATA_DOWN) {
			gui.handleClick(MouseAction::ACTION_LEFT, Mouse::getX(), Mouse::getY());
		}

		if (e.action == MouseAction::ACTION_WHEEL) {
			// If chat/console is open, use the wheel to scroll through chat history.
			if (screen && (dynamic_cast<ChatScreen*>(screen) || dynamic_cast<ConsoleScreen*>(screen))) {
				gui.scrollChat(e.dy);
			} else {
				Inventory* v = player->inventory;

				int numSlots = gui.getNumSlots();
				if (!useTouchscreen()) numSlots--;

				int slot = (v->selected - e.dy + numSlots) % numSlots;
				v->selectSlot(slot);
			}
		}
		/*
		if (mouseDiggable && options.useMouseForDigging) {
			if (Mouse::getEventButton() == MouseAction::ACTION_LEFT && Mouse::getEventButtonState()) {
				handleMouseClick(MouseAction::ACTION_LEFT);
				lastClickTick = ticks;
			}
			if (Mouse::getEventButton() == MouseAction::ACTION_RIGHT && Mouse::getEventButtonState()) {
				handleMouseClick(MouseAction::ACTION_RIGHT);
				lastClickTick = ticks;
			}
		}
		*/
	}

	TIMER_POP_PUSH("keyboard");
	while (Keyboard::next()) {
		int key = Keyboard::getEventKey();
		bool isPressed = (Keyboard::getEventKeyState() == KeyboardAction::KEYDOWN);
		player->setKey(key, isPressed);

		if (isPressed) {
			gui.handleKeyPressed(key);

			if (key >= '0' && key <= '9') {
				int digit = key - '0';
				int slot = digit - 1;

				if (slot >= 0 && slot < gui.getNumSlots())
					player->inventory->selectSlot(slot);

				#if defined(WIN32)
					if (digit >= 1 && GetAsyncKeyState(VK_CONTROL) < 0) {
						// Set adventure settings here!
						AdventureSettingsPacket p(level->adventureSettings);
						p.toggle((AdventureSettingsPacket::Flags)(1 << slot));
						p.fillIn(level->adventureSettings);
						raknetInstance->send(p);
					}
					if (digit == 0) {
						Pos pos((int)player->x, (int)player->y-1, (int)player->z);
						SetSpawnPositionPacket p(pos);
						raknetInstance->send(p);
					}
				#endif
			}

			if (key == Keyboard::KEY_LEFT_CTRL) {
				player->setSprinting(true);
			}

			if (key == Keyboard::KEY_E) {
				screenChooser.setScreen(SCREEN_BLOCKSELECTION);
			}

			if (!screen && key == Keyboard::KEY_T && level) {
				setScreen(new ConsoleScreen());
			}

			if (key == Keyboard::KEY_F3) {
				options.toggle(OPTIONS_RENDER_DEBUG);
			}

			if (key == Keyboard::KEY_F5) {
				options.toggle(OPTIONS_THIRD_PERSON_VIEW);
				/*
				ImprovedNoise noise;
				for (int i = 0; i < 16; ++i)
					printf("%d\t%f\n", i, noise.grad2(i, 3, 8));
				*/
			}

			if (!screen && key == Keyboard::KEY_O || key == 250) {
				releaseMouse();
			}

			if (key == Keyboard::KEY_F) {
				int dst = options.getIntValue(OPTIONS_VIEW_DISTANCE);
				options.set(OPTIONS_VIEW_DISTANCE, (dst + 1) % 4);
			}
			#ifdef CHEATS
				if (key == Keyboard::KEY_U) {
					onGraphicsReset();
					player->heal(100);
				}

				if (key == Keyboard::KEY_B || key == 108) // Toggle the game mode
					setIsCreativeMode(!isCreativeMode());

				if (key == Keyboard::KEY_P) // Step forward in time
					level->setTime( level->getTime() + 1000);

				if (key == Keyboard::KEY_G) {
					setScreen(new ArmorScreen());
					/*
					std::vector<AABB>& boxs = level->getCubes(NULL, AABB(128.1f, 73, 128.1f, 128.9f, 74.9f, 128.9f));
					LOGI("boxes: %d\n", (int)boxs.size());
					*/
				}

				if (key == Keyboard::KEY_Y) {
					textures->reloadAll();
					player->hurtTo(2);
				}
				if (key == Keyboard::KEY_Z || key == 108) {
					for (int i = 0; i < 1; ++i) {
						Mob* mob = NULL;
						int forceId = 0;//MobTypes::Sheep;

						int types[] = {
							MobTypes::Sheep,
							MobTypes::Pig,
							MobTypes::Chicken,
							MobTypes::Cow,
						};

						int mobType = (forceId > 0)? forceId : types[Mth::random(sizeof(types) / sizeof(int))];
						mob = MobFactory::CreateMob(mobType, level);

						//((Animal*)mob)->setAge(-1000);
						float dx = 4 - 8 * Mth::random() + 4 * Mth::sin(Mth::DEGRAD * player->yRot);
						float dz = 4 - 8 * Mth::random() + 4 * Mth::cos(Mth::DEGRAD * player->yRot);
						if (mob && !MobSpawner::addMob(level, mob, player->x + dx, player->y, player->z + dz, Mth::random()*360, 0, true))
							delete mob;
					}
				}

				if (key == Keyboard::KEY_X) {
					const EntityList& entities = level->getAllEntities();
					for (int i = entities.size()-1; i >= 0; --i) {
						Entity* e = entities[i];
						if (!e->isPlayer())
							level->removeEntity(e);
					}
				}

				if (key == Keyboard::KEY_C /*|| key == 4*/) {
					player->inventory->clearInventoryWithDefault();
					// @todo: Add saving here for benchmarking
				}
				if (key == Keyboard::KEY_H) {
					setScreen( new PrerenderTilesScreen() );
				}

				if (key == Keyboard::KEY_O) {
					for (int i = Inventory::MAX_SELECTION_SIZE; i < player->inventory->getContainerSize(); ++i)
						if (player->inventory->getItem(i))
							player->inventory->dropSlot(i, false);
				}
				if (key == Keyboard::KEY_M) {
					Difficulty difficulty = (Difficulty)options.getIntValue(OPTIONS_DIFFICULTY);
					options.set(OPTIONS_DIFFICULTY, (difficulty == Difficulty::PEACEFUL)?
						Difficulty::NORMAL : Difficulty::PEACEFUL);
					//setIsCreativeMode( !isCreativeMode() );
				}

				if (options.getBooleanValue(OPTIONS_RENDER_DEBUG)) {
					if (key >= '0' && key <= '9') {
						_perfRenderer->debugFpsMeterKeyPress(key - '0');
					}
				}
			#endif

			if (key == Keyboard::KEY_ESCAPE)
				pauseGame(false);

			#ifndef OPENGL_ES
				if (key == Keyboard::KEY_P) {
					static bool isWireFrame = false;
					isWireFrame = !isWireFrame;
					glPolygonMode(GL_FRONT, isWireFrame? GL_LINE : GL_FILL);
					//glPolygonMode(GL_BACK, isWireFrame? GL_LINE : GL_FILL);
				}
			#endif
		}
		#ifdef WIN32
			if (key == Keyboard::KEY_M) {
				for (int i = 0; i < 5 * SharedConstants::TicksPerSecond; ++i)
					level->tick();
			}
		#endif
	}

	TIMER_POP_PUSH("handlemouse");
	
	static bool prevMouseDownLeft = false;

	if (Mouse::getButtonState(MouseAction::ACTION_LEFT) == 0) {
		gameMode->stopDestroyBlock();
	}

	if (!Mouse::isButtonDown(MouseAction::ACTION_RIGHT)) {
		gameMode->releaseUsingItem(player);
	}

	if (useTouchscreen()) {
		// Touch: gesture recognizer classifies the action type (turn/destroy/build)
		BuildActionIntention bai;

		if (inputHolder && inputHolder->getBuildInput()->tickBuild(player, &bai)) {
			handleBuildAction(&bai);
		} else {
			gameMode->stopDestroyBlock();
		}
	} else {
		// Desktop: left mouse = destroy/attack
		if (Mouse::isButtonDown(MouseAction::ACTION_LEFT)) {
			auto baiFlags = BuildActionIntention::BAI_REMOVE | BuildActionIntention::BAI_ATTACK;

			if (!prevMouseDownLeft) baiFlags |= BuildActionIntention::BAI_FIRSTREMOVE;

			BuildActionIntention bai(baiFlags);
			handleBuildAction(&bai);
		}

		prevMouseDownLeft = Mouse::isButtonDown(MouseAction::ACTION_LEFT);

		// Build and use/interact is on same button
		// USPESHNO spizheno
		static int buildHoldTicks = 0;
		if (Mouse::isButtonDown(MouseAction::ACTION_RIGHT)) {
			if (buildHoldTicks >= 5) buildHoldTicks = 0;

			if (++buildHoldTicks == 1) {
				BuildActionIntention bai(BuildActionIntention::BAI_BUILD | BuildActionIntention::BAI_INTERACT);
				handleBuildAction(&bai);
			}
		} else {
			buildHoldTicks = 0;
			gameMode->releaseUsingItem(player);
		}
	}

	lastTickTime = getTimeMs();

	// we have (hopefully) handled the keyboard & mouse queue and it
	// can now be emptied. If wanted, the reset could be changed to:
	// index -= numRead; // then this code doesn't have to be placed here
	// + it prepares for tick not handling all or any events.
	// update: RAII'ing instead, see above
	//Keyboard::reset();
	//Mouse::reset();

	TIMER_POP();
#endif
}

void Minecraft::handleBuildAction(BuildActionIntention* action) {
#ifndef STANDALONE_SERVER
	if (action->isRemove()) {
    // 触屏长按时，即使上一帧未命中，也允许开始挖掘
    if (useTouchscreen() && action->isFirstRemove()) {
        missTime = 0;
    }
    if (missTime > 0) return;
    player->swing();
	}
	if(player->isUsingItem())
		return;
    bool mayUse = true;

	if (!hitResult.isHit()) {
		if (action->isRemove() && !gameMode->isCreativeType()) {
			missTime = 10;
		}
    } else if (hitResult.type == ENTITY) {
        if (action->isAttack()) {
			player->swing();
			//LOGI("attacking!\n");
			InteractPacket packet(InteractPacket::Attack, player->entityId, hitResult.entity->entityId);
			raknetInstance->send(packet);
            gameMode->attack(player, hitResult.entity);
		} else if (action->isInteract()) {
			if (hitResult.entity->interactPreventDefault())
				mayUse = false;
			//LOGI("interacting!\n");
			InteractPacket packet(InteractPacket::Interact, player->entityId, hitResult.entity->entityId);
			raknetInstance->send(packet);
            gameMode->interact(player, hitResult.entity);
        }
    } else if (hitResult.type == TILE) {
		int x = hitResult.x;
        int y = hitResult.y;
        int z = hitResult.z;
        int face = hitResult.f;

		int oldTileId = level->getTile(x, y, z);
        Tile* oldTile = Tile::tiles[oldTileId];

		//bool tryDestroyBlock = false;

		if (action->isRemove()) {
			if (!oldTile)
				return;

			//LOGI("tile: %s - %d, %d, %d. b: %f - %f\n", oldTile->getDescriptionId().c_str(), x, y, z, oldTile->getBrightness(level, x, y, z), oldTile->getBrightness(level, x, y+1, z));
            level->extinguishFire(x, y, z, hitResult.f);
			
			if (action->isFirstRemove()) {
				gameMode->startDestroyBlock(x, y, z, hitResult.f);
			} else {
				gameMode->continueDestroyBlock(x, y, z, hitResult.f);
			}

			particleEngine->crack(x, y, z, hitResult.f);
			player->swing();
        }
		else {
			ItemInstance* item = player->inventory->getSelected();
 			if (gameMode->useItemOn(player, level, item, x, y, z, face, hitResult.pos)) {
			    mayUse = false;
			    player->swing();
			#ifdef RPI
				} else if (item && item->id == ((Item*)Item::sword_iron)->id) {
					player->swing();
			#endif
			}
			if (item && item->count <= 0) {
				player->inventory->clearSlot(player->inventory->selected);
			}
			//} else if (item && item->count != oldCount) {
			//	  gameRenderer->itemInHandRenderer->itemPlaced();
			//}
		}
	}
	if (mayUse && action->isInteract()) {
		ItemInstance* item = player->inventory->getSelected();
		if (item && !player->isUsingItem()) {
			if (gameMode->useItem(player, level, item)) {
				gameRenderer->itemInHandRenderer->itemUsed();
			}
			if (item && item->count <= 0) {
				player->inventory->clearSlot(player->inventory->selected);
			}
		}
	}
#endif
}

bool Minecraft::isOnlineClient()
{
    return (level != NULL && level->isClientSide);
}

bool Minecraft::isOnline()
{
	return netCallback != NULL;
}

void Minecraft::pauseGame(bool isBackPaused) {
	// Only freeze gameplay when running a local server and it is not accepting
	// incoming connections (invisible server), which includes typical single-
	// player/lobby mode. If the server is visible, the game should keep ticking.
	bool canFreeze = false;
	if (raknetInstance && raknetInstance->isServer() && netCallback) {
		ServerSideNetworkHandler* ss = (ServerSideNetworkHandler*) netCallback;
		if (!ss->allowsIncomingConnections())
			canFreeze = true;
	}
	pause = canFreeze;

#ifndef STANDALONE_SERVER
	if (screen != NULL) return;
	screenChooser.setScreen(isBackPaused? SCREEN_PAUSEPREV : SCREEN_PAUSE);
#endif
}
void Minecraft::gameLostFocus() {
#ifndef STANDALONE_SERVER
	if(screen != NULL) {
		screen->lostFocus();
	}
#endif
}


void Minecraft::setScreen( Screen* screen )
{
#ifndef	STANDALONE_SERVER
	Mouse::reset();
	Multitouch::reset();
	Multitouch::resetThisUpdate();

	if (screenMutex) {
		hasScheduledScreen = true;
		scheduledScreen = screen;
		return;
	}

	if (screen != NULL && screen->isErrorScreen())
		return;
	if (screen == NULL && level == NULL)
		screen = screenChooser.createScreen(SCREEN_STARTMENU);

	if (this->screen != NULL) {
		this->screen->removed();
		delete this->screen;
	}

	this->screen = screen;
	if (screen != NULL) {
		releaseMouse();
		//ScreenSizeCalculator ssc = new ScreenSizeCalculator(options, width, height);
		int screenWidth = (int)(width * Gui::InvGuiScale); //ssc.getWidth();
		int screenHeight = (int)(height * Gui::InvGuiScale); //ssc.getHeight();
		screen->init(this, screenWidth, screenHeight);

		if (screen->isInGameScreen() && level) {
			level->saveLevelData();
            level->saveGame();
        }

		//noRender = false;
	} else {
		// Closing a screen and returning to the game should unpause.
		pause = false;
		grabMouse();
	}
#endif
}

void Minecraft::grabMouse()
{
#ifndef STANDALONE_SERVER
	if (mouseGrabbed) return;
	mouseGrabbed = true;
	mouseHandler.grab();
	//setScreen(NULL);
#endif
}

void Minecraft::releaseMouse()
{
#ifndef STANDALONE_SERVER
	if (!mouseGrabbed) {
		return;
	}
	if (player) {
		player->releaseAllKeys();
	}
	mouseGrabbed = false;
	mouseHandler.release();
#endif
}

bool Minecraft::useTouchscreen() {
#ifdef RPI
	return false;
#endif
	return options.getBooleanValue(OPTIONS_USE_TOUCHSCREEN) && !_supportsNonTouchscreen;
}
bool Minecraft::supportNonTouchScreen() {
	return _supportsNonTouchscreen;
}
void Minecraft::init()
{
#ifndef STANDALONE_SERVER
	checkGlError("Init enter");

	_supportsNonTouchscreen = !platform()->supportsTouchscreen();

	LOGI("IS TOUCHSCREEN? %d\n", options.getBooleanValue(OPTIONS_USE_TOUCHSCREEN));

	textures = new Textures(&options, platform());
	textures->addDynamicTexture(new WaterTexture());
	textures->addDynamicTexture(new WaterSideTexture());
	gui.texturesLoaded(textures);

	levelRenderer = new LevelRenderer(this);
	gameRenderer = new GameRenderer(this);
	particleEngine = new ParticleEngine(level, textures);

	// Platform specific initialization here
	font = new Font(&options, "font/default8.png", textures);

	_perfRenderer = new PerfRenderer(this, font);

	checkGlError("Init complete");
#endif

	options.load();

	setIsCreativeMode(false); // false means it's Survival Mode
	reloadOptions();
}

void Minecraft::setSize(int w, int h) {
#ifndef STANDALONE_SERVER
    transformResolution(&w, &h);

	width  = w;
	height = h;

	int guiScale = options.getIntValue(OPTIONS_GUI_SCALE);

	// determine gui scale, optionally overriding auto
	if (guiScale != 0) {
		// manual selection: 1->small, 2->medium, 3->large, 4->larger, 5->largest
		switch (guiScale) {
		case 1: Gui::GuiScale = 2.0f; break;
		case 2: Gui::GuiScale = 3.0f; break;
		case 3: Gui::GuiScale = 4.0f; break;
		case 4: Gui::GuiScale = 5.0f; break;
		case 5: Gui::GuiScale = 6.0f; break;
		default: Gui::GuiScale = 1.0f; break; // auto
		}
	} else {
		// auto compute from resolution
		if (width >= 1000) {
        #ifdef __APPLE__
            Gui::GuiScale = (width > 2000)? 8.0f : 4.0f;
        #else
            Gui::GuiScale = 4.0f;
        #endif
    }
		else if (width >= 800) {
#ifdef __APPLE__
        Gui::GuiScale = 4.0f;
#else
		Gui::GuiScale = 3.0f;
#endif
    }
		else if (width >= 400)
			Gui::GuiScale = 2.0f;
		else
			Gui::GuiScale = 1.0f;
	}

	Gui::InvGuiScale = 1.0f / Gui::GuiScale;
	int screenWidth  = (int)(width  * Gui::InvGuiScale);
	int screenHeight = (int)(height * Gui::InvGuiScale);

	// if (platform()) {
	// 	float pixelsPerMillimeter = options.getProgressValue(&Options::Option::PIXELS_PER_MILLIMETER);
	// 	pixelCalc.setPixelsPerMillimeter(pixelsPerMillimeter);
	// 	pixelCalcUi.setPixelsPerMillimeter(pixelsPerMillimeter * Gui::InvGuiScale);
	// }

	Config config = createConfig(this);
	gui.onConfigChanged(config);
    
	if (screen)
		screen->setSize(screenWidth, screenHeight);

	if (inputHolder)
		inputHolder->onConfigChanged(config);
	//LOGI("Setting size: %d, %d: %f\n", width, height, Gui::InvGuiScale);

#ifdef WIN32
	char resbuf[128];
	sprintf(resbuf, "            %d x %d @ scale %.2f", width, height, Gui::GuiScale);
	//gui.addMessage(resbuf);
#endif
#endif /* STANDALONE_SERVER */
}

void Minecraft::reloadOptions() {
	options.save();
	bool wasTouchscreen = options.getBooleanValue(OPTIONS_USE_TOUCHSCREEN);
	options.set(OPTIONS_USE_TOUCHSCREEN, useTouchscreen());
	options.save();

	if ((wasTouchscreen != useTouchscreen()) || (inputHolder == 0))
		_reloadInput();

	// user->name = options.username;

	LOGI("Reloading-options\n");

    // @todo @fix Android and iOS behaves a bit differently when leaving
    //            an options screen (Android recreates OpenGL surface)
    setSize(width, height);
}

void Minecraft::_reloadInput() {
#ifndef STANDALONE_SERVER
	delete inputHolder;

	const bool useTouchHolder = useTouchscreen();
	if (useTouchHolder) {
		inputHolder = new TouchInputHolder(this, &options);
	} else {
		#if defined(ANDROID) || defined(__APPLE__) 
			inputHolder = new CustomInputHolder(
				new XperiaPlayInput(&options),
				new ControllerTurnInput(2, ControllerTurnInput::MODE_DELTA),
				new IBuildInput());
		#else
			inputHolder = new CustomInputHolder(
				new KeyboardInput(&options),
				new MouseTurnInput(MouseTurnInput::MODE_DELTA, width/2, height/2),
				new MouseBuildInput());
		#endif
	}

	mouseHandler.setTurnInput(inputHolder->getTurnInput());
	if (level && player) {
		player->input = inputHolder->getMoveInput();
	}
#endif
}


//
// Multiplayer
//
void Minecraft::locateMultiplayer() {
	isLookingForMultiplayer = true;

	raknetInstance->pingForHosts(19132);
	netCallback = new ClientSideNetworkHandler(this, raknetInstance);
}

void Minecraft::cancelLocateMultiplayer() {
	isLookingForMultiplayer = false;

	raknetInstance->stopPingForHosts();

	delete netCallback;
	netCallback = NULL;
}

bool Minecraft::joinMultiplayer( const PingedCompatibleServer& server )
{
	if (isLookingForMultiplayer && netCallback) {
		isLookingForMultiplayer = false;
		return raknetInstance->connect(server.address.ToString(false), server.address.GetPort());
	}
	return false;
}

bool Minecraft::joinMultiplayerFromString( const std::string& server )
{	
	std::string ip = "";
	std::string port = "19132";
	
	size_t pos = server.find(":");

	if (pos != std::string::npos) {
		ip = server.substr(0, pos);
		port = server.substr(pos + 1);
	} else {
		ip = server;
	}

	printf("%s \n", port.c_str());
	
	if (isLookingForMultiplayer && netCallback) {
		isLookingForMultiplayer = false;
		printf("test");
		int portNum = atoi(port.c_str());
		return raknetInstance->connect(ip.c_str(), portNum);
	}
	return false;
}

void Minecraft::hostMultiplayer(int port) {
    // Tear down last instance
    raknetInstance->disconnect();
    delete netCallback;
    netCallback = NULL;

#if !defined(NO_NETWORK)
	netCallback = new ServerSideNetworkHandler(this, raknetInstance);
    #ifdef STANDALONE_SERVER
        raknetInstance->host("Server", port, 16);
    #else
        raknetInstance->host(options.getStringValue(OPTIONS_USERNAME), port);
    #endif
#endif
}

//
// Level generation
//
/*static*/

void* Minecraft::prepareLevel_tspawn(void *p_param)
{
	Minecraft* mc = (Minecraft*) p_param;
	mc->generateLevel("Currently not used", mc->level);
	return 0;
}

void Minecraft::generateLevel( const std::string& message, Level* level )
{
	Stopwatch s;
	s.start();
	prepareLevel(message);
	s.stop();
	s.print("Level generated: ");

	// "Unlock"
	isGeneratingLevel = false;
}

void Minecraft::_levelGenerated()
{
#ifndef STANDALONE_SERVER
	if (player == NULL) {
		player = (LocalPlayer*) gameMode->createPlayer(level);
		gameMode->initPlayer(player);
	}

	if (player) {
		player->input = inputHolder->getMoveInput();
	}

	if (levelRenderer != NULL) levelRenderer->setLevel(level);
	if (particleEngine != NULL) particleEngine->setLevel(level);

	gameMode->adjustPlayer(player);
	gui.onLevelGenerated();
#endif

	level->validateSpawn();
	level->loadPlayer(player, true);
	// if we are client side, we trust the server to have given us a correct position
	if (player && !level->isClientSide) {
		player->resetPos(false);
	}

	this->cameraTargetPlayer = player;

	std::string serverName = options.getStringValue(OPTIONS_USERNAME) + " - " + level->getLevelData()->levelName;

	if (raknetInstance->isServer())
		raknetInstance->announceServer(serverName);

	if (netCallback) {
		netCallback->levelGenerated(level);
	}

#if defined(WIN32) || defined(RPI)
	if (_commandServer) {
		delete _commandServer;
	}
	_commandServer = new CommandServer(this);
	_commandServer->init(commandPort);
#endif

	// Hack to (hopefully) get the players to show (note: in LevelListener
	// instead, since adding yourself always generates a entityAdded)
#ifndef STANDALONE_SERVER
    EntityRenderDispatcher::getInstance()->onGraphicsReset();
#endif
	_hasSignaledGeneratingLevelFinished = true;
}

Player* Minecraft::respawnPlayer(int playerId) {
	for (unsigned int i = 0; i < level->players.size(); ++i) {
		if (level->players[i]->entityId == playerId) {
			resetPlayer(level->players[i]);
			return level->players[i];
		}
	}
	return NULL;
}

void Minecraft::resetPlayer(Player* player) {
	level->validateSpawn();
	player->reset();

	Pos p;
	if(player->hasRespawnPosition()) {
		p = player->getRespawnPosition();
	}
	else {
		p = level->getSharedSpawnPos();
	}
	player->setPos((float)p.x + 0.5f, (float)p.y + 1.0f, (float)p.z + 0.5f);
	player->resetPos(true);

	if (isCreativeMode())
		player->inventory->clearInventoryWithDefault();
}

void Minecraft::respawnPlayer() {
	// RESET THE FRACKING PLAYER HERE
	//bool slowCheck = false;
	//for (int i = 0; i < level->entities.size(); ++i)
	//	if (level->entities[i] == player) slowCheck = true;
	//
	//LOGI("Has entity? %d, %d\n", level->getEntity(player->entityId), slowCheck);

	resetPlayer(player);

	// tell server (or other client) that we re-spawned
	RespawnPacket packet(player);
	raknetInstance->send(packet);
}

void Minecraft::onGraphicsReset()
{
#ifndef STANDALONE_SERVER
	textures->clear();
	
	font->onGraphicsReset();
	gui.onGraphicsReset();

	if (levelRenderer) levelRenderer->onGraphicsReset();
	if (gameRenderer) gameRenderer->onGraphicsReset();

	EntityRenderDispatcher::getInstance()->onGraphicsReset();
	TileEntityRenderDispatcher::getInstance()->onGraphicsReset();
#endif
}

int Minecraft::getProgressStatusId() {
	return progressStageStatusId;
}

const char* Minecraft::getProgressMessage()
{
	return progressMessages[progressStageStatusId];
}

bool Minecraft::isLevelGenerated()
{
	return level != NULL && !isGeneratingLevel;
}

LevelStorageSource* Minecraft::getLevelSource()
{
	return storageSource;
}

// int Minecraft::getLicenseId() {
// 	if (!LicenseCodes::isReady(_licenseId))
// 		_licenseId = platform()->checkLicense();
// 	return _licenseId;
// }

void Minecraft::audioEngineOn() {
#ifndef STANDALONE_SERVER
    soundEngine->enable(true);
#endif
}
void Minecraft::audioEngineOff() {
#ifndef STANDALONE_SERVER
    soundEngine->enable(false);
#endif
}

void Minecraft::setIsCreativeMode(bool isCreative)
{
#ifdef CREATORMODE
	delete gameMode;
	gameMode = new CreatorMode(this);
	_isCreativeMode = true;
#else
	if (!gameMode || isCreative != _isCreativeMode)
	{
		delete gameMode;
		if (isCreative) gameMode = new CreativeMode(this);
		else			gameMode = new SurvivalMode(this);
		_isCreativeMode = isCreative;
	}
#endif
	if (player)
		gameMode->initAbilities(player->abilities);
}

bool Minecraft::isCreativeMode() {
	return _isCreativeMode;
}

bool Minecraft::isKindleFire(int kindleVersion) {
	if (kindleVersion != 1)
		return false;

	std::string model = platform()->getPlatformStringVar(PlatformStringVars::DEVICE_BUILD_MODEL);
	std::string modelLower(model);
	std::transform(modelLower.begin(), modelLower.end(), modelLower.begin(), tolower);

	return (modelLower.find("kindle") != std::string::npos) && (modelLower.find("fire") != std::string::npos);
}

bool Minecraft::transformResolution(int* w, int* h)
{
	bool changed = false;

	// Kindle Fire 1: reporting wrong height in
	// certain cases (e.g. after screen lock)
	if (isKindleFire(1) && *h >= 560 && *h <= 620) {
		*h = 580;
		changed = true;
	}

	return changed;
}

ICreator* Minecraft::getCreator()
{
#ifdef CREATORMODE
	return ((CreatorMode*)gameMode)->getCreator();
#else
	return NULL;
#endif
}

void Minecraft::optionUpdated(OptionId option, bool value ) {
	if(netCallback != NULL && option == OPTIONS_SERVER_VISIBLE) {
		ServerSideNetworkHandler* ss = (ServerSideNetworkHandler*) netCallback;
		ss->allowIncomingConnections(value);
	} else if (option == OPTIONS_USE_TOUCHSCREEN) {
		_reloadInput();
	}
}

void Minecraft::optionUpdated(OptionId option, float value ) {
// #ifndef STANDALONE_SERVER
// 	if(option == OPTIONS_PIXELS_PER_MILLIMETER) {
// 		pixelCalcUi.setPixelsPerMillimeter(value * Gui::InvGuiScale);
// 		pixelCalc.setPixelsPerMillimeter(value);
// 	}
// #endif
}

void Minecraft::optionUpdated(OptionId option, int value ) {
    if(option == OPTIONS_GUI_SCALE) {
        // reapply screen scaling using current window size
        setSize(width, height);
    }
}
