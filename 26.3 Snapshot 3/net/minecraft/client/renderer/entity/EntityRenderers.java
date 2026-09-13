package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.animal.squid.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.PlayerModelType;
import org.slf4j.Logger;

public class EntityRenderers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = new Object2ObjectOpenHashMap();

   private static <T extends Entity> void register(final EntityType<? extends T> type, final EntityRendererProvider<T> renderer) {
      PROVIDERS.put(type, renderer);
   }

   public static Map<EntityType<?>, EntityRenderer<?, ?>> createEntityRenderers(final EntityRendererProvider.Context context) {
      Builder<EntityType<?>, EntityRenderer<?, ?>> result = ImmutableMap.builder();
      PROVIDERS.forEach((type, provider) -> {
         try {
            result.put(type, provider.create(context));
         } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create model for " + BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)type), e);
         }
      });
      return result.build();
   }

   public static <T extends Avatar & ClientAvatarEntity> Map<PlayerModelType, AvatarRenderer<T>> createAvatarRenderers(
      final EntityRendererProvider.Context context
   ) {
      try {
         return Map.of(PlayerModelType.WIDE, new AvatarRenderer<>(context, false), PlayerModelType.SLIM, new AvatarRenderer<>(context, true));
      } catch (Exception e) {
         throw new IllegalArgumentException("Failed to create avatar models", e);
      }
   }

   public static boolean validateRegistrations() {
      boolean hasAllModels = true;

      for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
         if (type != EntityTypes.PLAYER && type != EntityTypes.MANNEQUIN && !PROVIDERS.containsKey(type)) {
            LOGGER.warn("No renderer registered for {}", BuiltInRegistries.ENTITY_TYPE.getKey(type));
            hasAllModels = false;
         }
      }

      return !hasAllModels;
   }

   static {
      register(EntityTypes.ACACIA_BOAT, context -> new BoatRenderer(context, ModelLayers.ACACIA_BOAT));
      register(EntityTypes.ACACIA_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.ACACIA_CHEST_BOAT));
      register(EntityTypes.ALLAY, AllayRenderer::new);
      register(EntityTypes.AREA_EFFECT_CLOUD, NoopRenderer::new);
      register(EntityTypes.ARMADILLO, ArmadilloRenderer::new);
      register(EntityTypes.ARMOR_STAND, ArmorStandRenderer::new);
      register(EntityTypes.ARROW, TippableArrowRenderer::new);
      register(EntityTypes.AXOLOTL, AxolotlRenderer::new);
      register(EntityTypes.BAMBOO_CHEST_RAFT, context -> new RaftRenderer(context, ModelLayers.BAMBOO_CHEST_RAFT));
      register(EntityTypes.BAMBOO_RAFT, context -> new RaftRenderer(context, ModelLayers.BAMBOO_RAFT));
      register(EntityTypes.BAT, BatRenderer::new);
      register(EntityTypes.BEE, BeeRenderer::new);
      register(EntityTypes.BIRCH_BOAT, context -> new BoatRenderer(context, ModelLayers.BIRCH_BOAT));
      register(EntityTypes.BIRCH_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.BIRCH_CHEST_BOAT));
      register(EntityTypes.BLAZE, BlazeRenderer::new);
      register(EntityTypes.BLOCK_DISPLAY, DisplayRenderer.BlockDisplayRenderer::new);
      register(EntityTypes.BOGGED, BoggedRenderer::new);
      register(EntityTypes.BREEZE, BreezeRenderer::new);
      register(EntityTypes.BREEZE_WIND_CHARGE, WindChargeRenderer::new);
      register(EntityTypes.CAMEL, CamelRenderer::new);
      register(EntityTypes.CAMEL_HUSK, CamelHuskRenderer::new);
      register(EntityTypes.CAT, CatRenderer::new);
      register(EntityTypes.CAVE_SPIDER, CaveSpiderRenderer::new);
      register(EntityTypes.CHERRY_BOAT, context -> new BoatRenderer(context, ModelLayers.CHERRY_BOAT));
      register(EntityTypes.CHERRY_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.CHERRY_CHEST_BOAT));
      register(EntityTypes.CHEST_MINECART, context -> new MinecartRenderer(context, ModelLayers.CHEST_MINECART));
      register(EntityTypes.CHICKEN, ChickenRenderer::new);
      register(EntityTypes.COD, CodRenderer::new);
      register(EntityTypes.COMMAND_BLOCK_MINECART, context -> new MinecartRenderer(context, ModelLayers.COMMAND_BLOCK_MINECART));
      register(EntityTypes.COPPER_GOLEM, CopperGolemRenderer::new);
      register(EntityTypes.COW, CowRenderer::new);
      register(EntityTypes.CREAKING, CreakingRenderer::new);
      register(EntityTypes.CREEPER, CreeperRenderer::new);
      register(EntityTypes.CUSHION, CushionRenderer::new);
      register(EntityTypes.DARK_OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.DARK_OAK_BOAT));
      register(EntityTypes.DARK_OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.DARK_OAK_CHEST_BOAT));
      register(EntityTypes.DOLPHIN, DolphinRenderer::new);
      register(
         EntityTypes.DONKEY,
         context -> new DonkeyRenderer<>(
            context, EquipmentClientInfo.LayerType.DONKEY_SADDLE, ModelLayers.DONKEY_SADDLE, DonkeyRenderer.Type.DONKEY, DonkeyRenderer.Type.DONKEY_BABY
         )
      );
      register(EntityTypes.DRAGON_FIREBALL, DragonFireballRenderer::new);
      register(EntityTypes.DROWNED, DrownedRenderer::new);
      register(EntityTypes.EGG, ThrownItemRenderer::new);
      register(EntityTypes.ELDER_GUARDIAN, ElderGuardianRenderer::new);
      register(EntityTypes.ENDERMAN, EndermanRenderer::new);
      register(EntityTypes.ENDERMITE, EndermiteRenderer::new);
      register(EntityTypes.ENDER_DRAGON, EnderDragonRenderer::new);
      register(EntityTypes.ENDER_PEARL, ThrownItemRenderer::new);
      register(EntityTypes.END_CRYSTAL, EndCrystalRenderer::new);
      register(EntityTypes.EVOKER, EvokerRenderer::new);
      register(EntityTypes.EVOKER_FANGS, EvokerFangsRenderer::new);
      register(EntityTypes.EXPERIENCE_BOTTLE, ThrownItemRenderer::new);
      register(EntityTypes.EXPERIENCE_ORB, ExperienceOrbRenderer::new);
      register(EntityTypes.EYE_OF_ENDER, context -> new ThrownItemRenderer<>(context, 1.0F, true));
      register(EntityTypes.FALLING_BLOCK, FallingBlockRenderer::new);
      register(EntityTypes.FIREBALL, context -> new ThrownItemRenderer<>(context, 3.0F, true));
      register(EntityTypes.FIREWORK_ROCKET, FireworkEntityRenderer::new);
      register(EntityTypes.FISHING_BOBBER, FishingHookRenderer::new);
      register(EntityTypes.FOX, FoxRenderer::new);
      register(EntityTypes.FROG, FrogRenderer::new);
      register(EntityTypes.FURNACE_MINECART, context -> new MinecartRenderer(context, ModelLayers.FURNACE_MINECART));
      register(EntityTypes.GHAST, GhastRenderer::new);
      register(EntityTypes.HAPPY_GHAST, HappyGhastRenderer::new);
      register(EntityTypes.GIANT, context -> new GiantMobRenderer(context, 6.0F));
      register(EntityTypes.GLOW_ITEM_FRAME, ItemFrameRenderer::new);
      register(
         EntityTypes.GLOW_SQUID,
         context -> new GlowSquidRenderer(
            context, new SquidModel(context.bakeLayer(ModelLayers.GLOW_SQUID)), new SquidModel(context.bakeLayer(ModelLayers.GLOW_SQUID_BABY))
         )
      );
      register(EntityTypes.GOAT, GoatRenderer::new);
      register(EntityTypes.GUARDIAN, GuardianRenderer::new);
      register(EntityTypes.HOGLIN, HoglinRenderer::new);
      register(EntityTypes.HOPPER_MINECART, context -> new MinecartRenderer(context, ModelLayers.HOPPER_MINECART));
      register(EntityTypes.HORSE, HorseRenderer::new);
      register(EntityTypes.HUSK, HuskRenderer::new);
      register(EntityTypes.ILLUSIONER, IllusionerRenderer::new);
      register(EntityTypes.INTERACTION, NoopRenderer::new);
      register(EntityTypes.IRON_GOLEM, IronGolemRenderer::new);
      register(EntityTypes.ITEM, ItemEntityRenderer::new);
      register(EntityTypes.ITEM_DISPLAY, DisplayRenderer.ItemDisplayRenderer::new);
      register(EntityTypes.ITEM_FRAME, ItemFrameRenderer::new);
      register(EntityTypes.JUNGLE_BOAT, context -> new BoatRenderer(context, ModelLayers.JUNGLE_BOAT));
      register(EntityTypes.JUNGLE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.JUNGLE_CHEST_BOAT));
      register(EntityTypes.LEASH_KNOT, LeashKnotRenderer::new);
      register(EntityTypes.LIGHTNING_BOLT, LightningBoltRenderer::new);
      register(EntityTypes.LINGERING_POTION, ThrownItemRenderer::new);
      register(EntityTypes.LLAMA, context -> new LlamaRenderer(context, ModelLayers.LLAMA, ModelLayers.LLAMA_BABY));
      register(EntityTypes.LLAMA_SPIT, LlamaSpitRenderer::new);
      register(EntityTypes.MAGMA_CUBE, MagmaCubeRenderer::new);
      register(EntityTypes.MANGROVE_BOAT, context -> new BoatRenderer(context, ModelLayers.MANGROVE_BOAT));
      register(EntityTypes.MANGROVE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.MANGROVE_CHEST_BOAT));
      register(EntityTypes.MARKER, NoopRenderer::new);
      register(EntityTypes.MINECART, context -> new MinecartRenderer(context, ModelLayers.MINECART));
      register(EntityTypes.MOOSHROOM, MushroomCowRenderer::new);
      register(
         EntityTypes.MULE,
         context -> new DonkeyRenderer<>(
            context, EquipmentClientInfo.LayerType.MULE_SADDLE, ModelLayers.MULE_SADDLE, DonkeyRenderer.Type.MULE, DonkeyRenderer.Type.MULE_BABY
         )
      );
      register(EntityTypes.NAUTILUS, NautilusRenderer::new);
      register(EntityTypes.OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.OAK_BOAT));
      register(EntityTypes.OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.OAK_CHEST_BOAT));
      register(EntityTypes.OCELOT, OcelotRenderer::new);
      register(EntityTypes.OMINOUS_ITEM_SPAWNER, OminousItemSpawnerRenderer::new);
      register(EntityTypes.PAINTING, PaintingRenderer::new);
      register(EntityTypes.PALE_OAK_BOAT, context -> new BoatRenderer(context, ModelLayers.PALE_OAK_BOAT));
      register(EntityTypes.PALE_OAK_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.PALE_OAK_CHEST_BOAT));
      register(EntityTypes.PANDA, PandaRenderer::new);
      register(EntityTypes.PARCHED, ParchedRenderer::new);
      register(EntityTypes.PARROT, ParrotRenderer::new);
      register(EntityTypes.PHANTOM, PhantomRenderer::new);
      register(EntityTypes.PIG, PigRenderer::new);
      register(
         EntityTypes.PIGLIN,
         context -> new PiglinRenderer(context, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR)
      );
      register(
         EntityTypes.PIGLIN_BRUTE,
         context -> new PiglinRenderer(
            context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BRUTE_ARMOR
         )
      );
      register(EntityTypes.PILLAGER, PillagerRenderer::new);
      register(EntityTypes.POLAR_BEAR, PolarBearRenderer::new);
      register(EntityTypes.POPLAR_BOAT, context -> new BoatRenderer(context, ModelLayers.POPLAR_BOAT));
      register(EntityTypes.POPLAR_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.POPLAR_CHEST_BOAT));
      register(EntityTypes.PUFFERFISH, PufferfishRenderer::new);
      register(EntityTypes.RABBIT, RabbitRenderer::new);
      register(EntityTypes.RAVAGER, RavagerRenderer::new);
      register(EntityTypes.SALMON, SalmonRenderer::new);
      register(EntityTypes.SHEEP, SheepRenderer::new);
      register(EntityTypes.SHULKER, ShulkerRenderer::new);
      register(EntityTypes.SHULKER_BULLET, ShulkerBulletRenderer::new);
      register(EntityTypes.SILVERFISH, SilverfishRenderer::new);
      register(EntityTypes.SKELETON, SkeletonRenderer::new);
      register(
         EntityTypes.SKELETON_HORSE,
         context -> new UndeadHorseRenderer(
            context,
            EquipmentClientInfo.LayerType.SKELETON_HORSE_SADDLE,
            ModelLayers.SKELETON_HORSE_SADDLE,
            UndeadHorseRenderer.Type.SKELETON,
            UndeadHorseRenderer.Type.SKELETON_BABY
         )
      );
      register(EntityTypes.SLIME, SlimeRenderer::new);
      register(EntityTypes.SMALL_FIREBALL, context -> new ThrownItemRenderer<>(context, 0.75F, true));
      register(EntityTypes.SNIFFER, SnifferRenderer::new);
      register(EntityTypes.SNOWBALL, ThrownItemRenderer::new);
      register(EntityTypes.SNOW_GOLEM, SnowGolemRenderer::new);
      register(EntityTypes.SPAWNER_MINECART, context -> new MinecartRenderer(context, ModelLayers.SPAWNER_MINECART));
      register(EntityTypes.SPECTRAL_ARROW, SpectralArrowRenderer::new);
      register(EntityTypes.SPIDER, SpiderRenderer::new);
      register(EntityTypes.SPLASH_POTION, ThrownItemRenderer::new);
      register(EntityTypes.SPRUCE_BOAT, context -> new BoatRenderer(context, ModelLayers.SPRUCE_BOAT));
      register(EntityTypes.SPRUCE_CHEST_BOAT, context -> new BoatRenderer(context, ModelLayers.SPRUCE_CHEST_BOAT));
      register(
         EntityTypes.SQUID,
         context -> new SquidRenderer<>(
            context, new SquidModel(context.bakeLayer(ModelLayers.SQUID)), new SquidModel(context.bakeLayer(ModelLayers.SQUID_BABY))
         )
      );
      register(EntityTypes.STRAY, StrayRenderer::new);
      register(EntityTypes.STRIDER, StriderRenderer::new);
      register(EntityTypes.SULFUR_CUBE, SulfurCubeRenderer::new);
      register(EntityTypes.TADPOLE, TadpoleRenderer::new);
      register(EntityTypes.TEXT_DISPLAY, DisplayRenderer.TextDisplayRenderer::new);
      register(EntityTypes.TNT, TntRenderer::new);
      register(EntityTypes.TNT_MINECART, TntMinecartRenderer::new);
      register(EntityTypes.TRADER_LLAMA, context -> new LlamaRenderer(context, ModelLayers.TRADER_LLAMA, ModelLayers.TRADER_LLAMA_BABY));
      register(EntityTypes.TRIDENT, ThrownTridentRenderer::new);
      register(EntityTypes.TROPICAL_FISH, TropicalFishRenderer::new);
      register(EntityTypes.TURTLE, TurtleRenderer::new);
      register(EntityTypes.VEX, VexRenderer::new);
      register(EntityTypes.VILLAGER, VillagerRenderer::new);
      register(EntityTypes.VINDICATOR, VindicatorRenderer::new);
      register(EntityTypes.WANDERING_TRADER, WanderingTraderRenderer::new);
      register(EntityTypes.WARDEN, WardenRenderer::new);
      register(EntityTypes.WIND_CHARGE, WindChargeRenderer::new);
      register(EntityTypes.WITCH, WitchRenderer::new);
      register(EntityTypes.WITHER, WitherBossRenderer::new);
      register(EntityTypes.WITHER_SKELETON, WitherSkeletonRenderer::new);
      register(EntityTypes.WITHER_SKULL, WitherSkullRenderer::new);
      register(EntityTypes.WOLF, WolfRenderer::new);
      register(EntityTypes.ZOGLIN, ZoglinRenderer::new);
      register(EntityTypes.ZOMBIE, ZombieRenderer::new);
      register(
         EntityTypes.ZOMBIE_HORSE,
         context -> new UndeadHorseRenderer(
            context,
            EquipmentClientInfo.LayerType.ZOMBIE_HORSE_SADDLE,
            ModelLayers.ZOMBIE_HORSE_SADDLE,
            UndeadHorseRenderer.Type.ZOMBIE,
            UndeadHorseRenderer.Type.ZOMBIE_BABY
         )
      );
      register(EntityTypes.ZOMBIE_NAUTILUS, ZombieNautilusRenderer::new);
      register(EntityTypes.ZOMBIE_VILLAGER, ZombieVillagerRenderer::new);
      register(
         EntityTypes.ZOMBIFIED_PIGLIN,
         context -> new ZombifiedPiglinRenderer(
            context,
            ModelLayers.ZOMBIFIED_PIGLIN,
            ModelLayers.ZOMBIFIED_PIGLIN_BABY,
            ModelLayers.ZOMBIFIED_PIGLIN_ARMOR,
            ModelLayers.ZOMBIFIED_PIGLIN_BABY_ARMOR
         )
      );
   }
}
