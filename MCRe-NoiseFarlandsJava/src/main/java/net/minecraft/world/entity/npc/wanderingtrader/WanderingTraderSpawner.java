package net.minecraft.world.entity.npc.wanderingtrader;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WanderingTraderData;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import org.jspecify.annotations.Nullable;

public class WanderingTraderSpawner implements CustomSpawner {
    private static final int DEFAULT_TICK_DELAY = 1200;
    public static final int DEFAULT_SPAWN_DELAY = 24000;
    public static final int MIN_SPAWN_CHANCE = 25;
    private static final int MAX_SPAWN_CHANCE = 75;
    private static final int SPAWN_CHANCE_INCREASE = 25;
    private static final int SPAWN_ONE_IN_X_CHANCE = 10;
    private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
    private final RandomSource random = RandomSource.create();
    private final SavedDataStorage savedDataStorage;
    private int tickDelay;
    private @Nullable WanderingTraderData traderData;

    public WanderingTraderSpawner(final SavedDataStorage savedDataStorage) {
        this.savedDataStorage = savedDataStorage;
        this.tickDelay = 1200;
        this.traderData = null;
    }
    
    private static boolean disabledEntitySpawnMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.disabledEntitySpawn;
    }

    @Override
    public void tick(final ServerLevel level, final boolean spawnEnemies) {
        if (level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS) && !disabledEntitySpawnMode()) {
            if (--this.tickDelay <= 0) {
                this.tickDelay = 1200;
                WanderingTraderData data = this.getTraderData();
                int spawnDelay = data.spawnDelay() - 1200;
                data.setSpawnDelay(spawnDelay);
                if (spawnDelay <= 0) {
                    data.setSpawnDelay(24000);
                    int chanceToSpawn = data.spawnChance();
                    int newSpawnChance = Mth.clamp(chanceToSpawn + 25, 25, 75);
                    data.setSpawnChance(newSpawnChance);
                    if (this.random.nextInt(100) <= chanceToSpawn) {
                        if (this.spawn(level)) {
                            data.setSpawnChance(25);
                        }
                    }
                }
            }
        }
    }

    private WanderingTraderData getTraderData() {
        if (this.traderData == null) {
            this.traderData = this.savedDataStorage.computeIfAbsent(WanderingTraderData.TYPE);
        }

        return this.traderData;
    }

    private boolean spawn(final ServerLevel level) {
        Player player = level.getRandomPlayer();
        if (player == null) {
            return true;
        }

        if (this.random.nextInt(10) != 0) {
            return false;
        }

        BlockPos playerPos = player.blockPosition();
        int radius = 48;
        PoiManager poiManager = level.getPoiManager();
        Optional<BlockPos> poiPos = poiManager.find(p -> p.is(PoiTypes.MEETING), p -> true, playerPos, 48, PoiManager.Occupancy.ANY);
        BlockPos referencePos = poiPos.orElse(playerPos);
        BlockPos spawnPosition = this.findSpawnPositionNear(level, referencePos, 48);
        if (spawnPosition != null && this.hasEnoughSpace(level, spawnPosition)) {
            if (level.getBiome(spawnPosition).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
                return false;
            }

            WanderingTrader trader = EntityTypes.WANDERING_TRADER.spawn(level, spawnPosition, EntitySpawnReason.EVENT);
            if (trader != null) {
                for (int i = 0; i < 2; i++) {
                    this.tryToSpawnLlamaFor(level, trader, 4);
                }

                trader.setDespawnDelay(48000);
                trader.setWanderTarget(referencePos);
                trader.setHomeTo(referencePos, 16);
                return true;
            }
        }

        return false;
    }

    private void tryToSpawnLlamaFor(final ServerLevel level, final WanderingTrader trader, final int radius) {
        BlockPos spawnPosition = this.findSpawnPositionNear(level, trader.blockPosition(), radius);
        if (spawnPosition != null) {
            TraderLlama llama = EntityTypes.TRADER_LLAMA.spawn(level, spawnPosition, EntitySpawnReason.EVENT);
            if (llama != null) {
                llama.setLeashedTo(trader, true);
            }
        }
    }

    private @Nullable BlockPos findSpawnPositionNear(final LevelReader level, final BlockPos referencePosition, final int radius) {
        BlockPos spawnPosition = null;
        SpawnPlacementType wanderingTraderSpawnType = SpawnPlacements.getPlacementType(EntityTypes.WANDERING_TRADER);

        for (int i = 0; i < 10; i++) {
            int xPosition = referencePosition.getX() + this.random.nextInt(radius * 2) - radius;
            int zPosition = referencePosition.getZ() + this.random.nextInt(radius * 2) - radius;
            int yPosition = level.getHeight(SpawnPlacements.getHeightmapType(EntityTypes.WANDERING_TRADER), xPosition, zPosition);
            BlockPos spawnPos = new BlockPos(xPosition, yPosition, zPosition);
            if (wanderingTraderSpawnType.isSpawnPositionOk(level, spawnPos, EntityTypes.WANDERING_TRADER)) {
                spawnPosition = spawnPos;
                break;
            }
        }

        return spawnPosition;
    }

    private boolean hasEnoughSpace(final BlockGetter level, final BlockPos spawnPos) {
        for (BlockPos pos : BlockPos.betweenClosed(spawnPos, spawnPos.offset(1, 2, 1))) {
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}