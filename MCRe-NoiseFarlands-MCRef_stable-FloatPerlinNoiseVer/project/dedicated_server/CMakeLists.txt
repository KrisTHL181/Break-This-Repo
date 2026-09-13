cmake_minimum_required(VERSION 3.10)

project(mcpe_server CXX)

# ====== ★ 关键：先设 64 位 + 禁用 sized deallocation ======
# 必须在 add_subdirectory(RakNet) 之前设，RakNet 会继承
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m64 -fno-sized-deallocation")
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -m64")

find_package(Boost REQUIRED)
find_package(Threads REQUIRED)

# RakNet 会继承上面的 CMAKE_CXX_FLAGS → 64 位 + 无 sized deallocation
add_subdirectory("${PROJECT_SOURCE_DIR}/../lib_projects/raknet/jni"
                 "${CMAKE_CURRENT_BINARY_DIR}/raknet")

# ---- 只需 preprocessor 宏，不需要编译器 flag ----
add_definitions(-DSTANDALONE_SERVER)
add_compile_options(-w)

# ---- 源文件 (原版118 + MCRe新增) ----
set(CompileFiles
    # ── 入口 ──
    ../../src/main.cpp
    ../../src/main_dedicated.cpp

    # ── 平台抽象层 ──
    ../../src/platform/input/Controller.cpp
    ../../src/platform/input/Keyboard.cpp
    ../../src/platform/input/Mouse.cpp
    ../../src/platform/input/Multitouch.cpp
    ../../src/platform/time.cpp
    ../../src/platform/CThread.cpp

    # ── 应用层 ──
    ../../src/NinecraftApp.cpp
    ../../src/Performance.cpp
    ../../src/SharedConstants.cpp
    ../../src/client/IConfigListener.cpp
    ../../src/client/Minecraft.cpp
    ../../src/client/Options.cpp
    ../../src/client/Option.cpp
    ../../src/client/OptionsFile.cpp
    ../../src/client/OptionStrings.cpp

    # ── 游戏模式(GameMode) ──
    ../../src/client/gamemode/GameMode.cpp
    ../../src/client/gamemode/CreativeMode.cpp
    ../../src/client/gamemode/SurvivalMode.cpp

    # ── 玩家 ──
    ../../src/client/player/LocalPlayer.cpp
    ../../src/client/player/RemotePlayer.cpp
    ../../src/client/player/input/KeyboardInput.cpp

    # ── i18n ──
    ../../src/locale/I18n.cpp

    # ── NBT ──
    ../../src/nbt/Tag.cpp

    # ── 网络层 ──
    ../../src/network/command/CommandServer.cpp
    ../../src/network/ClientSideNetworkHandler.cpp
    ../../src/network/NetEventCallback.cpp
    ../../src/network/Packet.cpp
    ../../src/network/RakNetInstance.cpp
    ../../src/network/ServerSideNetworkHandler.cpp

    # ── 服务端 ──
    ../../src/server/ArgumentsSettings.cpp
    ../../src/server/ServerLevel.cpp
    ../../src/server/ServerPlayer.cpp

    # ── 工具类 ──
    ../../src/util/DataIO.cpp
    ../../src/util/Mth.cpp
    ../../src/util/StringUtils.cpp
    ../../src/util/PerfTimer.cpp
    ../../src/util/DebugLog.cpp              # ★ MCRe 新增

    # ── 方向/Facing ──
    ../../src/world/Direction.cpp

    # ── Entity 体系 ──
    ../../src/world/entity/AgableMob.cpp
    ../../src/world/entity/Entity.cpp
    ../../src/world/entity/EntityFactory.cpp
    ../../src/world/entity/FlyingMob.cpp
    ../../src/world/entity/HangingEntity.cpp
    ../../src/world/entity/Mob.cpp
    ../../src/world/entity/MobCategory.cpp
    ../../src/world/entity/Motive.cpp
    ../../src/world/entity/Painting.cpp
    ../../src/world/entity/PathfinderMob.cpp
    ../../src/world/entity/SynchedEntityData.cpp
    ../../src/world/entity/ai/control/MoveControl.cpp
    ../../src/world/entity/animal/Animal.cpp
    ../../src/world/entity/animal/Chicken.cpp
    ../../src/world/entity/animal/Cow.cpp
    ../../src/world/entity/animal/Pig.cpp
    ../../src/world/entity/animal/Sheep.cpp
    ../../src/world/entity/animal/WaterAnimal.cpp
    ../../src/world/entity/item/FallingTile.cpp
    ../../src/world/entity/item/ItemEntity.cpp
    ../../src/world/entity/item/PrimedTnt.cpp
    ../../src/world/entity/item/TripodCamera.cpp
    ../../src/world/entity/monster/Creeper.cpp
    ../../src/world/entity/monster/Monster.cpp
    ../../src/world/entity/monster/PigZombie.cpp
    ../../src/world/entity/monster/Skeleton.cpp
    ../../src/world/entity/monster/Spider.cpp
    ../../src/world/entity/monster/Zombie.cpp
    ../../src/world/entity/projectile/Arrow.cpp
    ../../src/world/entity/projectile/Throwable.cpp
    ../../src/world/entity/player/Inventory.cpp
    ../../src/world/entity/player/Player.cpp
    ../../src/world/food/SimpleFoodData.cpp

    # ── 容器/物品系统 ──
    ../../src/world/inventory/BaseContainerMenu.cpp
    ../../src/world/inventory/ContainerMenu.cpp
    ../../src/world/inventory/FillingContainer.cpp
    ../../src/world/inventory/FurnaceMenu.cpp
    ../../src/world/item/ArmorItem.cpp
    ../../src/world/item/BedItem.cpp
    ../../src/world/item/DyePowderItem.cpp
    ../../src/world/item/HangingEntityItem.cpp
    ../../src/world/item/HatchetItem.cpp
    ../../src/world/item/HoeItem.cpp
    ../../src/world/item/Item.cpp
    ../../src/world/item/ItemInstance.cpp
    ../../src/world/item/NegativePickaxeItem.cpp  # ★ MCRe 新增
    ../../src/world/item/PickaxeItem.cpp
    ../../src/world/item/ShovelItem.cpp
    ../../src/world/item/crafting/ArmorRecipes.cpp
    ../../src/world/item/crafting/FurnaceRecipes.cpp
    ../../src/world/item/crafting/OreRecipes.cpp
    ../../src/world/item/crafting/Recipe.cpp
    ../../src/world/item/crafting/Recipes.cpp
    ../../src/world/item/crafting/StructureRecipes.cpp
    ../../src/world/item/crafting/ToolRecipes.cpp
    ../../src/world/item/crafting/WeaponRecipes.cpp

    # ── Level/世界 ──
    ../../src/world/level/Explosion.cpp
    ../../src/world/level/Level.cpp
    ../../src/world/level/LightLayer.cpp
    ../../src/world/level/LightUpdate.cpp
    ../../src/world/level/MobSpawner.cpp
    ../../src/world/level/Region.cpp
    ../../src/world/level/TickNextTickData.cpp
    ../../src/world/level/biome/Biome.cpp
    ../../src/world/level/biome/BiomeSource.cpp
    ../../src/world/level/chunk/LevelChunk.cpp
    ../../src/world/level/dimension/Dimension.cpp
    ../../src/world/level/levelgen/CanyonFeature.cpp
    ../../src/world/level/levelgen/DungeonFeature.cpp
    ../../src/world/level/levelgen/LargeCaveFeature.cpp
    ../../src/world/level/levelgen/LargeFeature.cpp
    ../../src/world/level/levelgen/RandomLevelSource.cpp
    ../../src/world/level/levelgen/TheEndLevelSource.cpp  # ★ MCRe 新增
    ../../src/world/level/levelgen/feature/Feature.cpp
    ../../src/world/level/levelgen/synth/ImprovedNoise.cpp
    ../../src/world/level/levelgen/synth/PerlinNoise.cpp
    ../../src/world/level/levelgen/synth/Synth.cpp
    ../../src/world/level/material/Material.cpp
    ../../src/world/level/pathfinder/Path.cpp
    ../../src/world/level/storage/ExternalFileLevelStorage.cpp
    ../../src/world/level/storage/ExternalFileLevelStorageSource.cpp
    ../../src/world/level/storage/FolderMethods.cpp
    ../../src/world/level/storage/LevelData.cpp
    ../../src/world/level/storage/LevelStorageSource.cpp
    ../../src/world/level/storage/RegionFile.cpp

    # ── 方块体系 ──
    ../../src/world/level/tile/BedTile.cpp
    ../../src/world/level/tile/ChestTile.cpp
    ../../src/world/level/tile/CropTile.cpp
    ../../src/world/level/tile/DoorTile.cpp
    ../../src/world/level/tile/EntityTile.cpp
    ../../src/world/level/tile/FurnaceTile.cpp
    ../../src/world/level/tile/GrassTile.cpp
    ../../src/world/level/tile/HeavyTile.cpp
    ../../src/world/level/tile/LightGemTile.cpp
    ../../src/world/level/tile/MelonTile.cpp
    ../../src/world/level/tile/Mushroom.cpp
    ../../src/world/level/tile/NetherReactor.cpp
    ../../src/world/level/tile/NetherReactorPattern.cpp
    ../../src/world/level/tile/StairTile.cpp
    ../../src/world/level/tile/StemTile.cpp
    ../../src/world/level/tile/StoneSlabTile.cpp
    ../../src/world/level/tile/TallGrass.cpp
    ../../src/world/level/tile/Tile.cpp
    ../../src/world/level/tile/TrapDoorTile.cpp
    ../../src/world/level/tile/entity/ChestTileEntity.cpp
    ../../src/world/level/tile/entity/FurnaceTileEntity.cpp
    ../../src/world/level/tile/entity/NetherReactorTileEntity.cpp
    ../../src/world/level/tile/entity/SignTileEntity.cpp
    ../../src/world/level/tile/entity/TileEntity.cpp

    # ── 物理 ──
    ../../src/world/phys/HitResult.cpp
)

# ---- 可执行文件 ----
add_executable(mcpe_server ${CompileFiles})

# ---- 链接 ----
target_link_libraries(mcpe_server
    raknet
    ${CMAKE_THREAD_LIBS_INIT}
    ${Boost_LIBRARIES}
)

if(WIN32 OR MINGW OR CMAKE_SYSTEM_NAME STREQUAL "Windows")
    target_link_libraries(mcpe_server ws2_32)
endif()

# ---- 头文件搜索路径 ----
target_include_directories(mcpe_server PUBLIC
    "../../src/"
    ${Boost_INCLUDE_DIRS}
)
