package net.minecraft.client.particle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParticleResources implements PreparableReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private final Map<Identifier, ParticleResources.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final Int2ObjectMap<ParticleProvider<?>> providers = new Int2ObjectOpenHashMap();
   private @Nullable Runnable onReload;

   public ParticleResources() {
      this.registerProviders();
   }

   public void onReload(final Runnable onReload) {
      this.onReload = onReload;
   }

   private void registerProviders() {
      this.register(ParticleTypes.ANGRY_VILLAGER, HeartParticle.AngryVillagerProvider::new);
      this.register(ParticleTypes.BLOCK_MARKER, new BlockMarker.Provider());
      this.register(ParticleTypes.BLOCK, new TerrainParticle.Provider());
      this.register(ParticleTypes.BUBBLE, BubbleParticle.Provider::new);
      this.register(ParticleTypes.BUBBLE_COLUMN_UP, BubbleColumnUpParticle.Provider::new);
      this.register(ParticleTypes.BUBBLE_POP, BubblePopParticle.Provider::new);
      this.register(ParticleTypes.SULFUR_BUBBLES, SulfurBubbleParticle.Provider::new);
      this.register(ParticleTypes.NOXIOUS_GAS, NoxiousGasParticle.Provider::new);
      this.register(ParticleTypes.NOXIOUS_GAS_CLOUD, new NoxiousGasCloudParticle.Provider());
      this.register(ParticleTypes.GEYSER, new GeyserEruptionParticle.Provider());
      this.register(ParticleTypes.GEYSER_BASE, GeyserBaseParticle.Provider::new);
      this.register(ParticleTypes.GEYSER_POOF, GeyserBaseParticle.Provider::new);
      this.register(ParticleTypes.GEYSER_PLUME, GeyserPlumeParticle.Provider::new);
      this.register(ParticleTypes.CAMPFIRE_COSY_SMOKE, CampfireSmokeParticle.CosyProvider::new);
      this.register(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, CampfireSmokeParticle.SignalProvider::new);
      this.register(ParticleTypes.CLOUD, PlayerCloudParticle.Provider::new);
      this.register(ParticleTypes.COMPOSTER, SuspendedTownParticle.ComposterFillProvider::new);
      this.register(ParticleTypes.COPPER_FIRE_FLAME, FlameParticle.Provider::new);
      this.register(ParticleTypes.CRIT, CritParticle.Provider::new);
      this.register(ParticleTypes.CURRENT_DOWN, WaterCurrentDownParticle.Provider::new);
      this.register(ParticleTypes.DAMAGE_INDICATOR, CritParticle.DamageIndicatorProvider::new);
      this.register(ParticleTypes.DRAGON_BREATH, DragonBreathParticle.Provider::new);
      this.register(ParticleTypes.DOLPHIN, SuspendedTownParticle.DolphinSpeedProvider::new);
      this.register(ParticleTypes.DRIPPING_LAVA, DripParticle.LavaHangProvider::new);
      this.register(ParticleTypes.FALLING_LAVA, DripParticle.LavaFallProvider::new);
      this.register(ParticleTypes.LANDING_LAVA, DripParticle.LavaLandProvider::new);
      this.register(ParticleTypes.DRIPPING_WATER, DripParticle.WaterHangProvider::new);
      this.register(ParticleTypes.FALLING_WATER, DripParticle.WaterFallProvider::new);
      this.register(ParticleTypes.DUST, DustParticle.Provider::new);
      this.register(ParticleTypes.DUST_COLOR_TRANSITION, DustColorTransitionParticle.Provider::new);
      this.register(ParticleTypes.EFFECT, SpellParticle.InstantProvider::new);
      this.register(ParticleTypes.ELDER_GUARDIAN, new ElderGuardianParticle.Provider());
      this.register(ParticleTypes.ENCHANTED_HIT, CritParticle.MagicProvider::new);
      this.register(ParticleTypes.ENCHANT, FlyTowardsPositionParticle.EnchantProvider::new);
      this.register(ParticleTypes.END_ROD, EndRodParticle.Provider::new);
      this.register(ParticleTypes.ENTITY_EFFECT, SpellParticle.MobEffectProvider::new);
      this.register(ParticleTypes.EXPLOSION_EMITTER, new HugeExplosionSeedParticle.Provider());
      this.register(ParticleTypes.EXPLOSION, HugeExplosionParticle.Provider::new);
      this.register(ParticleTypes.SONIC_BOOM, SonicBoomParticle.Provider::new);
      this.register(ParticleTypes.FALLING_DUST, FallingDustParticle.Provider::new);
      this.register(ParticleTypes.GUST, GustParticle.Provider::new);
      this.register(ParticleTypes.SMALL_GUST, GustParticle.SmallProvider::new);
      this.register(ParticleTypes.GUST_EMITTER_LARGE, new GustSeedParticle.Provider(3.0, 7, 0));
      this.register(ParticleTypes.GUST_EMITTER_SMALL, new GustSeedParticle.Provider(1.0, 3, 2));
      this.register(ParticleTypes.FIREWORK, FireworkParticles.SparkProvider::new);
      this.register(ParticleTypes.FISHING, WakeParticle.Provider::new);
      this.register(ParticleTypes.FLAME, FlameParticle.Provider::new);
      this.register(ParticleTypes.INFESTED, SpellParticle.Provider::new);
      this.register(ParticleTypes.SCULK_SOUL, SoulParticle.EmissiveProvider::new);
      this.register(ParticleTypes.SCULK_CHARGE, SculkChargeParticle.Provider::new);
      this.register(ParticleTypes.SCULK_CHARGE_POP, SculkChargePopParticle.Provider::new);
      this.register(ParticleTypes.SOUL, SoulParticle.Provider::new);
      this.register(ParticleTypes.SOUL_FIRE_FLAME, FlameParticle.Provider::new);
      this.register(ParticleTypes.FLASH, FireworkParticles.FlashProvider::new);
      this.register(ParticleTypes.HAPPY_VILLAGER, SuspendedTownParticle.HappyVillagerProvider::new);
      this.register(ParticleTypes.HEART, HeartParticle.Provider::new);
      this.register(ParticleTypes.INSTANT_EFFECT, SpellParticle.InstantProvider::new);
      this.register(ParticleTypes.ITEM, new BreakingItemParticle.Provider());
      this.register(ParticleTypes.ITEM_SLIME, new BreakingItemParticle.SlimeProvider());
      this.register(ParticleTypes.ITEM_COBWEB, new BreakingItemParticle.CobwebProvider());
      this.register(ParticleTypes.ITEM_SNOWBALL, new BreakingItemParticle.SnowballProvider());
      this.register(ParticleTypes.LARGE_SMOKE, LargeSmokeParticle.Provider::new);
      this.register(ParticleTypes.LAVA, LavaParticle.Provider::new);
      this.register(ParticleTypes.MYCELIUM, SuspendedTownParticle.Provider::new);
      this.register(ParticleTypes.NAUTILUS, FlyTowardsPositionParticle.NautilusProvider::new);
      this.register(ParticleTypes.NOTE, NoteParticle.Provider::new);
      this.register(ParticleTypes.POOF, ExplodeParticle.Provider::new);
      this.register(ParticleTypes.PORTAL, PortalParticle.Provider::new);
      this.register(ParticleTypes.RAIN, WaterDropParticle.Provider::new);
      this.register(ParticleTypes.SMOKE, SmokeParticle.Provider::new);
      this.register(ParticleTypes.WHITE_SMOKE, WhiteSmokeParticle.Provider::new);
      this.register(ParticleTypes.SNEEZE, PlayerCloudParticle.SneezeProvider::new);
      this.register(ParticleTypes.SNOWFLAKE, SnowflakeParticle.Provider::new);
      this.register(ParticleTypes.SPIT, SpitParticle.Provider::new);
      this.register(ParticleTypes.SWEEP_ATTACK, AttackSweepParticle.Provider::new);
      this.register(ParticleTypes.TOTEM_OF_UNDYING, TotemParticle.Provider::new);
      this.register(ParticleTypes.SQUID_INK, SquidInkParticle.Provider::new);
      this.register(ParticleTypes.UNDERWATER, SuspendedParticle.UnderwaterProvider::new);
      this.register(ParticleTypes.SPLASH, SplashParticle.Provider::new);
      this.register(ParticleTypes.WITCH, SpellParticle.WitchProvider::new);
      this.register(ParticleTypes.DRIPPING_HONEY, DripParticle.HoneyHangProvider::new);
      this.register(ParticleTypes.FALLING_HONEY, DripParticle.HoneyFallProvider::new);
      this.register(ParticleTypes.LANDING_HONEY, DripParticle.HoneyLandProvider::new);
      this.register(ParticleTypes.FALLING_NECTAR, DripParticle.NectarFallProvider::new);
      this.register(ParticleTypes.FALLING_SPORE_BLOSSOM, DripParticle.SporeBlossomFallProvider::new);
      this.register(ParticleTypes.SPORE_BLOSSOM_AIR, SuspendedParticle.SporeBlossomAirProvider::new);
      this.register(ParticleTypes.ASH, AshParticle.Provider::new);
      this.register(ParticleTypes.CRIMSON_SPORE, SuspendedParticle.CrimsonSporeProvider::new);
      this.register(ParticleTypes.WARPED_SPORE, SuspendedParticle.WarpedSporeProvider::new);
      this.register(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, DripParticle.ObsidianTearHangProvider::new);
      this.register(ParticleTypes.FALLING_OBSIDIAN_TEAR, DripParticle.ObsidianTearFallProvider::new);
      this.register(ParticleTypes.LANDING_OBSIDIAN_TEAR, DripParticle.ObsidianTearLandProvider::new);
      this.register(ParticleTypes.REVERSE_PORTAL, ReversePortalParticle.ReversePortalProvider::new);
      this.register(ParticleTypes.WHITE_ASH, WhiteAshParticle.Provider::new);
      this.register(ParticleTypes.SMALL_FLAME, FlameParticle.SmallFlameProvider::new);
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_WATER, DripParticle.DripstoneWaterHangProvider::new);
      this.register(ParticleTypes.FALLING_DRIPSTONE_WATER, DripParticle.DripstoneWaterFallProvider::new);
      this.register(ParticleTypes.CHERRY_LEAVES, FallingLeavesParticle.CherryProvider::new);
      this.register(ParticleTypes.PALE_OAK_LEAVES, FallingLeavesParticle.PaleOakProvider::new);
      this.register(ParticleTypes.RED_POPLAR_LEAVES, FallingLeavesParticle.PoplarProvider::new);
      this.register(ParticleTypes.ORANGE_POPLAR_LEAVES, FallingLeavesParticle.PoplarProvider::new);
      this.register(ParticleTypes.YELLOW_POPLAR_LEAVES, FallingLeavesParticle.PoplarProvider::new);
      this.register(ParticleTypes.TINTED_LEAVES, FallingLeavesParticle.TintedLeavesProvider::new);
      this.register(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, DripParticle.DripstoneLavaHangProvider::new);
      this.register(ParticleTypes.FALLING_DRIPSTONE_LAVA, DripParticle.DripstoneLavaFallProvider::new);
      this.register(ParticleTypes.VIBRATION, VibrationSignalParticle.Provider::new);
      this.register(ParticleTypes.TRAIL, TrailParticle.Provider::new);
      this.register(ParticleTypes.PAUSE_MOB_GROWTH, SimpleVerticalParticle.PauseMobGrowthProvider::new);
      this.register(ParticleTypes.RESET_MOB_GROWTH, SimpleVerticalParticle.ResetMobGrowthProvider::new);
      this.register(ParticleTypes.GLOW_SQUID_INK, SquidInkParticle.GlowInkProvider::new);
      this.register(ParticleTypes.GLOW, GlowParticle.GlowSquidProvider::new);
      this.register(ParticleTypes.WAX_ON, GlowParticle.WaxOnProvider::new);
      this.register(ParticleTypes.WAX_OFF, GlowParticle.WaxOffProvider::new);
      this.register(ParticleTypes.ELECTRIC_SPARK, GlowParticle.ElectricSparkProvider::new);
      this.register(ParticleTypes.SCRAPE, GlowParticle.ScrapeProvider::new);
      this.register(ParticleTypes.SHRIEK, ShriekParticle.Provider::new);
      this.register(ParticleTypes.EGG_CRACK, SuspendedTownParticle.EggCrackProvider::new);
      this.register(ParticleTypes.DUST_PLUME, DustPlumeParticle.Provider::new);
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER, TrialSpawnerDetectionParticle.Provider::new);
      this.register(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS, TrialSpawnerDetectionParticle.Provider::new);
      this.register(ParticleTypes.VAULT_CONNECTION, FlyTowardsPositionParticle.VaultConnectionProvider::new);
      this.register(ParticleTypes.DUST_PILLAR, new TerrainParticle.DustPillarProvider());
      this.register(ParticleTypes.RAID_OMEN, SpellParticle.Provider::new);
      this.register(ParticleTypes.TRIAL_OMEN, SpellParticle.Provider::new);
      this.register(ParticleTypes.OMINOUS_SPAWNING, FlyStraightTowardsParticle.OminousSpawnProvider::new);
      this.register(ParticleTypes.BLOCK_CRUMBLE, new TerrainParticle.CrumblingProvider());
      this.register(ParticleTypes.FIREFLY, FireflyParticle.FireflyProvider::new);
      this.register(ParticleTypes.SULFUR_CUBE_GOO, BreakingItemParticle.SulfurCubeProvider::new);
   }

   private <T extends ParticleOptions> void register(final ParticleType<T> type, final ParticleProvider<T> provider) {
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(type), provider);
   }

   private <T extends ParticleOptions> void register(final ParticleType<T> type, final ParticleResources.SpriteParticleRegistration<T> provider) {
      ParticleResources.MutableSpriteSet spriteSet = new ParticleResources.MutableSpriteSet();
      this.spriteSets.put(BuiltInRegistries.PARTICLE_TYPE.getKey(type), spriteSet);
      this.providers.put(BuiltInRegistries.PARTICLE_TYPE.getId(type), provider.create(spriteSet));
   }

   @Override
   public CompletableFuture<Void> reload(
      final PreparableReloadListener.SharedState currentReload,
      final Executor taskExecutor,
      final PreparableReloadListener.PreparationBarrier preparationBarrier,
      final Executor reloadExecutor
   ) {
      ResourceManager manager = currentReload.resourceManager();

      record ParticleDefinition(Identifier id, Optional<List<Identifier>> sprites) {
      }

      CompletableFuture<List<ParticleDefinition>> spriteSetsToLoad = CompletableFuture.<Map<Identifier, Resource>>supplyAsync(
            () -> PARTICLE_LISTER.listMatchingResources(manager), taskExecutor
         )
         .thenCompose(
            definitionsToScan -> {
               List<CompletableFuture<ParticleDefinition>> loadTasks = new ArrayList<>(definitionsToScan.size());
               definitionsToScan.forEach(
                  (resourceId, resource) -> {
                     Identifier particleId = PARTICLE_LISTER.fileToId(resourceId);
                     loadTasks.add(
                        CompletableFuture.supplyAsync(
                           () -> new ParticleDefinition(particleId, this.loadParticleDescription(particleId, resource)), taskExecutor
                        )
                     );
                  }
               );
               return Util.sequence(loadTasks);
            }
         );
      CompletableFuture<SpriteLoader.Preparations> pendingSprites = currentReload.get(AtlasManager.PENDING_STITCH).get(AtlasIds.PARTICLES);
      return CompletableFuture.allOf(spriteSetsToLoad, pendingSprites).thenCompose(preparationBarrier::wait).thenAcceptAsync(unused -> {
         if (this.onReload != null) {
            this.onReload.run();
         }

         ProfilerFiller reloadProfiler = Profiler.get();
         reloadProfiler.push("upload");
         SpriteLoader.Preparations sprites = pendingSprites.join();
         reloadProfiler.popPush("bindSpriteSets");
         Set<Identifier> missingSprites = new HashSet<>();
         TextureAtlasSprite missingSprite = sprites.missing();
         spriteSetsToLoad.join().forEach(p -> {
            Optional<List<Identifier>> spriteIds = p.sprites();
            if (!spriteIds.isEmpty()) {
               List<TextureAtlasSprite> contents = new ArrayList<>();

               for (Identifier spriteId : spriteIds.get()) {
                  TextureAtlasSprite sprite = sprites.getSprite(spriteId);
                  if (sprite == null) {
                     missingSprites.add(spriteId);
                     contents.add(missingSprite);
                  } else {
                     contents.add(sprite);
                  }
               }

               if (contents.isEmpty()) {
                  contents.add(missingSprite);
               }

               this.spriteSets.get(p.id()).rebind(contents);
            }
         });
         if (!missingSprites.isEmpty()) {
            LOGGER.warn("Missing particle sprites: {}", missingSprites.stream().sorted().map(Identifier::toString).collect(Collectors.joining(",")));
         }

         reloadProfiler.pop();
      }, reloadExecutor);
   }

   private Optional<List<Identifier>> loadParticleDescription(final Identifier id, final Resource resource) {
      if (!this.spriteSets.containsKey(id)) {
         LOGGER.debug("Redundant texture list for particle: {}", id);
         return Optional.empty();
      }

      try (Reader reader = resource.openAsReader()) {
         ParticleDescription description = ParticleDescription.fromJson(GsonHelper.parse(reader));
         return Optional.of(description.getTextures());
      } catch (IOException e) {
         throw new IllegalStateException("Failed to load description for particle " + id, e);
      }
   }

   public Int2ObjectMap<ParticleProvider<?>> getProviders() {
      return this.providers;
   }

   private static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      @Override
      public TextureAtlasSprite get(final int index, final int max) {
         return this.sprites.get(index * (this.sprites.size() - 1) / max);
      }

      @Override
      public TextureAtlasSprite get(final RandomSource random) {
         return this.sprites.get(random.nextInt(this.sprites.size()));
      }

      @Override
      public TextureAtlasSprite first() {
         return this.sprites.getFirst();
      }

      public void rebind(final List<TextureAtlasSprite> ids) {
         this.sprites = ImmutableList.copyOf(ids);
      }
   }

   @FunctionalInterface
   private interface SpriteParticleRegistration<T extends ParticleOptions> {
      ParticleProvider<T> create(SpriteSet spriteSet);
   }
}
