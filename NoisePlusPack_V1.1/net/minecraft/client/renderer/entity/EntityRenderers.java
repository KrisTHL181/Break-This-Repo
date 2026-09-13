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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EntityRenderers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<EntityType<?>, EntityRendererProvider<?>> PROVIDERS = new Object2ObjectOpenHashMap();

   private static <T extends Entity> void register(EntityType<? extends T> p_174037_, EntityRendererProvider<T> p_174038_) {
      PROVIDERS.put(p_174037_, p_174038_);
   }

   public static Map<EntityType<?>, EntityRenderer<?, ?>> createEntityRenderers(EntityRendererProvider.Context p_174050_) {
      Builder<EntityType<?>, EntityRenderer<?, ?>> builder = ImmutableMap.builder();
      PROVIDERS.forEach((p_448319_, p_448320_) -> {
         try {
            builder.put(p_448319_, p_448320_.create(p_174050_));
         } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to create model for " + BuiltInRegistries.ENTITY_TYPE.getKey((EntityType<?>)p_448319_), exception);
         }
      });
      return builder.build();
   }

   public static <T extends Avatar & ClientAvatarEntity> Map<PlayerModelType, AvatarRenderer<T>> createAvatarRenderers(EntityRendererProvider.Context p_429582_) {
      try {
         return Map.of(PlayerModelType.WIDE, new AvatarRenderer<>(p_429582_, false), PlayerModelType.SLIM, new AvatarRenderer<>(p_429582_, true));
      } catch (Exception exception) {
         throw new IllegalArgumentException("Failed to create avatar models", exception);
      }
   }

   public static boolean validateRegistrations() {
      boolean flag = true;

      for (EntityType<?> entitytype : BuiltInRegistries.ENTITY_TYPE) {
         if (entitytype != EntityType.PLAYER && entitytype != EntityType.MANNEQUIN && !PROVIDERS.containsKey(entitytype)) {
            LOGGER.warn("No renderer registered for {}", BuiltInRegistries.ENTITY_TYPE.getKey(entitytype));
            flag = false;
         }
      }

      return !flag;
   }

   static {
      register(EntityType.ACACIA_BOAT, p_357975_ -> new BoatRenderer(p_357975_, ModelLayers.ACACIA_BOAT));
      register(EntityType.ACACIA_CHEST_BOAT, p_357971_ -> new BoatRenderer(p_357971_, ModelLayers.ACACIA_CHEST_BOAT));
      register(EntityType.ALLAY, AllayRenderer::new);
      register(EntityType.AREA_EFFECT_CLOUD, NoopRenderer::new);
      register(EntityType.ARMADILLO, ArmadilloRenderer::new);
      register(EntityType.ARMOR_STAND, ArmorStandRenderer::new);
      register(EntityType.ARROW, TippableArrowRenderer::new);
      register(EntityType.AXOLOTL, AxolotlRenderer::new);
      register(EntityType.BAMBOO_CHEST_RAFT, p_357998_ -> new RaftRenderer(p_357998_, ModelLayers.BAMBOO_CHEST_RAFT));
      register(EntityType.BAMBOO_RAFT, p_357997_ -> new RaftRenderer(p_357997_, ModelLayers.BAMBOO_RAFT));
      register(EntityType.BAT, BatRenderer::new);
      register(EntityType.BEE, BeeRenderer::new);
      register(EntityType.BIRCH_BOAT, p_357989_ -> new BoatRenderer(p_357989_, ModelLayers.BIRCH_BOAT));
      register(EntityType.BIRCH_CHEST_BOAT, p_357987_ -> new BoatRenderer(p_357987_, ModelLayers.BIRCH_CHEST_BOAT));
      register(EntityType.BLAZE, BlazeRenderer::new);
      register(EntityType.BLOCK_DISPLAY, DisplayRenderer.BlockDisplayRenderer::new);
      register(EntityType.BOGGED, BoggedRenderer::new);
      register(EntityType.BREEZE, BreezeRenderer::new);
      register(EntityType.BREEZE_WIND_CHARGE, WindChargeRenderer::new);
      register(EntityType.CAMEL, CamelRenderer::new);
      register(EntityType.CAMEL_HUSK, CamelHuskRenderer::new);
      register(EntityType.CAT, CatRenderer::new);
      register(EntityType.CAVE_SPIDER, CaveSpiderRenderer::new);
      register(EntityType.CHERRY_BOAT, p_357995_ -> new BoatRenderer(p_357995_, ModelLayers.CHERRY_BOAT));
      register(EntityType.CHERRY_CHEST_BOAT, p_358000_ -> new BoatRenderer(p_358000_, ModelLayers.CHERRY_CHEST_BOAT));
      register(EntityType.CHEST_MINECART, p_174090_ -> new MinecartRenderer(p_174090_, ModelLayers.CHEST_MINECART));
      register(EntityType.CHICKEN, ChickenRenderer::new);
      register(EntityType.COD, CodRenderer::new);
      register(EntityType.COMMAND_BLOCK_MINECART, p_174088_ -> new MinecartRenderer(p_174088_, ModelLayers.COMMAND_BLOCK_MINECART));
      register(EntityType.COPPER_GOLEM, CopperGolemRenderer::new);
      register(EntityType.COW, CowRenderer::new);
      register(EntityType.CREAKING, CreakingRenderer::new);
      register(EntityType.CREEPER, CreeperRenderer::new);
      register(EntityType.DARK_OAK_BOAT, p_357977_ -> new BoatRenderer(p_357977_, ModelLayers.DARK_OAK_BOAT));
      register(EntityType.DARK_OAK_CHEST_BOAT, p_357982_ -> new BoatRenderer(p_357982_, ModelLayers.DARK_OAK_CHEST_BOAT));
      register(EntityType.DOLPHIN, DolphinRenderer::new);
      register(EntityType.DONKEY, p_389512_ -> new DonkeyRenderer<>(p_389512_, DonkeyRenderer.Type.DONKEY));
      register(EntityType.DRAGON_FIREBALL, DragonFireballRenderer::new);
      register(EntityType.DROWNED, DrownedRenderer::new);
      register(EntityType.EGG, ThrownItemRenderer::new);
      register(EntityType.ELDER_GUARDIAN, ElderGuardianRenderer::new);
      register(EntityType.ENDERMAN, EndermanRenderer::new);
      register(EntityType.ENDERMITE, EndermiteRenderer::new);
      register(EntityType.ENDER_DRAGON, EnderDragonRenderer::new);
      register(EntityType.ENDER_PEARL, ThrownItemRenderer::new);
      register(EntityType.END_CRYSTAL, EndCrystalRenderer::new);
      register(EntityType.EVOKER, EvokerRenderer::new);
      register(EntityType.EVOKER_FANGS, EvokerFangsRenderer::new);
      register(EntityType.EXPERIENCE_BOTTLE, ThrownItemRenderer::new);
      register(EntityType.EXPERIENCE_ORB, ExperienceOrbRenderer::new);
      register(EntityType.EYE_OF_ENDER, p_174084_ -> new ThrownItemRenderer<>(p_174084_, 1.0F, true));
      register(EntityType.FALLING_BLOCK, FallingBlockRenderer::new);
      register(EntityType.FIREBALL, p_174060_ -> new ThrownItemRenderer<>(p_174060_, 3.0F, true));
      register(EntityType.FIREWORK_ROCKET, FireworkEntityRenderer::new);
      register(EntityType.FISHING_BOBBER, FishingHookRenderer::new);
      register(EntityType.FOX, FoxRenderer::new);
      register(EntityType.FROG, FrogRenderer::new);
      register(EntityType.FURNACE_MINECART, p_174080_ -> new MinecartRenderer(p_174080_, ModelLayers.FURNACE_MINECART));
      register(EntityType.GHAST, GhastRenderer::new);
      register(EntityType.HAPPY_GHAST, HappyGhastRenderer::new);
      register(EntityType.GIANT, p_174078_ -> new GiantMobRenderer(p_174078_, 6.0F));
      register(EntityType.GLOW_ITEM_FRAME, ItemFrameRenderer::new);
      register(
         EntityType.GLOW_SQUID,
         p_448316_ -> new GlowSquidRenderer(
            p_448316_, new SquidModel(p_448316_.bakeLayer(ModelLayers.GLOW_SQUID)), new SquidModel(p_448316_.bakeLayer(ModelLayers.GLOW_SQUID_BABY))
         )
      );
      register(EntityType.GOAT, GoatRenderer::new);
      register(EntityType.GUARDIAN, GuardianRenderer::new);
      register(EntityType.HOGLIN, HoglinRenderer::new);
      register(EntityType.HOPPER_MINECART, p_174074_ -> new MinecartRenderer(p_174074_, ModelLayers.HOPPER_MINECART));
      register(EntityType.HORSE, HorseRenderer::new);
      register(EntityType.HUSK, HuskRenderer::new);
      register(EntityType.ILLUSIONER, IllusionerRenderer::new);
      register(EntityType.INTERACTION, NoopRenderer::new);
      register(EntityType.IRON_GOLEM, IronGolemRenderer::new);
      register(EntityType.ITEM, ItemEntityRenderer::new);
      register(EntityType.ITEM_DISPLAY, DisplayRenderer.ItemDisplayRenderer::new);
      register(EntityType.ITEM_FRAME, ItemFrameRenderer::new);
      register(EntityType.JUNGLE_BOAT, p_357978_ -> new BoatRenderer(p_357978_, ModelLayers.JUNGLE_BOAT));
      register(EntityType.JUNGLE_CHEST_BOAT, p_357985_ -> new BoatRenderer(p_357985_, ModelLayers.JUNGLE_CHEST_BOAT));
      register(EntityType.LEASH_KNOT, LeashKnotRenderer::new);
      register(EntityType.LIGHTNING_BOLT, LightningBoltRenderer::new);
      register(EntityType.LINGERING_POTION, ThrownItemRenderer::new);
      register(EntityType.LLAMA, p_357974_ -> new LlamaRenderer(p_357974_, ModelLayers.LLAMA, ModelLayers.LLAMA_BABY));
      register(EntityType.LLAMA_SPIT, LlamaSpitRenderer::new);
      register(EntityType.MAGMA_CUBE, MagmaCubeRenderer::new);
      register(EntityType.MANGROVE_BOAT, p_357972_ -> new BoatRenderer(p_357972_, ModelLayers.MANGROVE_BOAT));
      register(EntityType.MANGROVE_CHEST_BOAT, p_357992_ -> new BoatRenderer(p_357992_, ModelLayers.MANGROVE_CHEST_BOAT));
      register(EntityType.MARKER, NoopRenderer::new);
      register(EntityType.MINECART, p_174070_ -> new MinecartRenderer(p_174070_, ModelLayers.MINECART));
      register(EntityType.MOOSHROOM, MushroomCowRenderer::new);
      register(EntityType.MULE, p_389513_ -> new DonkeyRenderer<>(p_389513_, DonkeyRenderer.Type.MULE));
      register(EntityType.NAUTILUS, NautilusRenderer::new);
      register(EntityType.OAK_BOAT, p_357980_ -> new BoatRenderer(p_357980_, ModelLayers.OAK_BOAT));
      register(EntityType.OAK_CHEST_BOAT, p_357990_ -> new BoatRenderer(p_357990_, ModelLayers.OAK_CHEST_BOAT));
      register(EntityType.OCELOT, OcelotRenderer::new);
      register(EntityType.OMINOUS_ITEM_SPAWNER, OminousItemSpawnerRenderer::new);
      register(EntityType.PAINTING, PaintingRenderer::new);
      register(EntityType.PALE_OAK_BOAT, p_357979_ -> new BoatRenderer(p_357979_, ModelLayers.PALE_OAK_BOAT));
      register(EntityType.PALE_OAK_CHEST_BOAT, p_357993_ -> new BoatRenderer(p_357993_, ModelLayers.PALE_OAK_CHEST_BOAT));
      register(EntityType.PANDA, PandaRenderer::new);
      register(EntityType.PARCHED, ParchedRenderer::new);
      register(EntityType.PARROT, ParrotRenderer::new);
      register(EntityType.PHANTOM, PhantomRenderer::new);
      register(EntityType.PIG, PigRenderer::new);
      register(
         EntityType.PIGLIN,
         p_421018_ -> new PiglinRenderer(p_421018_, ModelLayers.PIGLIN, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR)
      );
      register(
         EntityType.PIGLIN_BRUTE,
         p_421020_ -> new PiglinRenderer(
            p_421020_, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BRUTE_ARMOR
         )
      );
      register(EntityType.PILLAGER, PillagerRenderer::new);
      register(EntityType.POLAR_BEAR, PolarBearRenderer::new);
      register(EntityType.PUFFERFISH, PufferfishRenderer::new);
      register(EntityType.RABBIT, RabbitRenderer::new);
      register(EntityType.RAVAGER, RavagerRenderer::new);
      register(EntityType.SALMON, SalmonRenderer::new);
      register(EntityType.SHEEP, SheepRenderer::new);
      register(EntityType.SHULKER, ShulkerRenderer::new);
      register(EntityType.SHULKER_BULLET, ShulkerBulletRenderer::new);
      register(EntityType.SILVERFISH, SilverfishRenderer::new);
      register(EntityType.SKELETON, SkeletonRenderer::new);
      register(EntityType.SKELETON_HORSE, p_389514_ -> new UndeadHorseRenderer(p_389514_, UndeadHorseRenderer.Type.SKELETON));
      register(EntityType.SLIME, SlimeRenderer::new);
      register(EntityType.SMALL_FIREBALL, p_174082_ -> new ThrownItemRenderer<>(p_174082_, 0.75F, true));
      register(EntityType.SNIFFER, SnifferRenderer::new);
      register(EntityType.SNOWBALL, ThrownItemRenderer::new);
      register(EntityType.SNOW_GOLEM, SnowGolemRenderer::new);
      register(EntityType.SPAWNER_MINECART, p_174058_ -> new MinecartRenderer(p_174058_, ModelLayers.SPAWNER_MINECART));
      register(EntityType.SPECTRAL_ARROW, SpectralArrowRenderer::new);
      register(EntityType.SPIDER, SpiderRenderer::new);
      register(EntityType.SPLASH_POTION, ThrownItemRenderer::new);
      register(EntityType.SPRUCE_BOAT, p_357999_ -> new BoatRenderer(p_357999_, ModelLayers.SPRUCE_BOAT));
      register(EntityType.SPRUCE_CHEST_BOAT, p_357973_ -> new BoatRenderer(p_357973_, ModelLayers.SPRUCE_CHEST_BOAT));
      register(
         EntityType.SQUID,
         p_448321_ -> new SquidRenderer<>(
            p_448321_, new SquidModel(p_448321_.bakeLayer(ModelLayers.SQUID)), new SquidModel(p_448321_.bakeLayer(ModelLayers.SQUID_BABY))
         )
      );
      register(EntityType.STRAY, StrayRenderer::new);
      register(EntityType.STRIDER, StriderRenderer::new);
      register(EntityType.TADPOLE, TadpoleRenderer::new);
      register(EntityType.TEXT_DISPLAY, DisplayRenderer.TextDisplayRenderer::new);
      register(EntityType.TNT, TntRenderer::new);
      register(EntityType.TNT_MINECART, TntMinecartRenderer::new);
      register(EntityType.TRADER_LLAMA, p_357988_ -> new LlamaRenderer(p_357988_, ModelLayers.TRADER_LLAMA, ModelLayers.TRADER_LLAMA_BABY));
      register(EntityType.TRIDENT, ThrownTridentRenderer::new);
      register(EntityType.TROPICAL_FISH, TropicalFishRenderer::new);
      register(EntityType.TURTLE, TurtleRenderer::new);
      register(EntityType.VEX, VexRenderer::new);
      register(EntityType.VILLAGER, VillagerRenderer::new);
      register(EntityType.VINDICATOR, VindicatorRenderer::new);
      register(EntityType.WANDERING_TRADER, WanderingTraderRenderer::new);
      register(EntityType.WARDEN, WardenRenderer::new);
      register(EntityType.WIND_CHARGE, WindChargeRenderer::new);
      register(EntityType.WITCH, WitchRenderer::new);
      register(EntityType.WITHER, WitherBossRenderer::new);
      register(EntityType.WITHER_SKELETON, WitherSkeletonRenderer::new);
      register(EntityType.WITHER_SKULL, WitherSkullRenderer::new);
      register(EntityType.WOLF, WolfRenderer::new);
      register(EntityType.ZOGLIN, ZoglinRenderer::new);
      register(EntityType.ZOMBIE, ZombieRenderer::new);
      register(EntityType.ZOMBIE_HORSE, p_389511_ -> new UndeadHorseRenderer(p_389511_, UndeadHorseRenderer.Type.ZOMBIE));
      register(EntityType.ZOMBIE_NAUTILUS, ZombieNautilusRenderer::new);
      register(EntityType.ZOMBIE_VILLAGER, ZombieVillagerRenderer::new);
      register(
         EntityType.ZOMBIFIED_PIGLIN,
         p_421019_ -> new ZombifiedPiglinRenderer(
            p_421019_,
            ModelLayers.ZOMBIFIED_PIGLIN,
            ModelLayers.ZOMBIFIED_PIGLIN_BABY,
            ModelLayers.ZOMBIFIED_PIGLIN_ARMOR,
            ModelLayers.ZOMBIFIED_PIGLIN_BABY_ARMOR
         )
      );
   }
}
