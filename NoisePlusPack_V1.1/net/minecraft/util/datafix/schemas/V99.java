package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import org.slf4j.Logger;

public class V99 extends Schema {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Map<String, String> ITEM_TO_BLOCKENTITY = (Map<String, String>)DataFixUtils.make(Maps.newHashMap(), p_145919_ -> {
      p_145919_.put("minecraft:furnace", "Furnace");
      p_145919_.put("minecraft:lit_furnace", "Furnace");
      p_145919_.put("minecraft:chest", "Chest");
      p_145919_.put("minecraft:trapped_chest", "Chest");
      p_145919_.put("minecraft:ender_chest", "EnderChest");
      p_145919_.put("minecraft:jukebox", "RecordPlayer");
      p_145919_.put("minecraft:dispenser", "Trap");
      p_145919_.put("minecraft:dropper", "Dropper");
      p_145919_.put("minecraft:sign", "Sign");
      p_145919_.put("minecraft:mob_spawner", "MobSpawner");
      p_145919_.put("minecraft:noteblock", "Music");
      p_145919_.put("minecraft:brewing_stand", "Cauldron");
      p_145919_.put("minecraft:enhanting_table", "EnchantTable");
      p_145919_.put("minecraft:command_block", "CommandBlock");
      p_145919_.put("minecraft:beacon", "Beacon");
      p_145919_.put("minecraft:skull", "Skull");
      p_145919_.put("minecraft:daylight_detector", "DLDetector");
      p_145919_.put("minecraft:hopper", "Hopper");
      p_145919_.put("minecraft:banner", "Banner");
      p_145919_.put("minecraft:flower_pot", "FlowerPot");
      p_145919_.put("minecraft:repeating_command_block", "CommandBlock");
      p_145919_.put("minecraft:chain_command_block", "CommandBlock");
      p_145919_.put("minecraft:standing_sign", "Sign");
      p_145919_.put("minecraft:wall_sign", "Sign");
      p_145919_.put("minecraft:piston_head", "Piston");
      p_145919_.put("minecraft:daylight_detector_inverted", "DLDetector");
      p_145919_.put("minecraft:unpowered_comparator", "Comparator");
      p_145919_.put("minecraft:powered_comparator", "Comparator");
      p_145919_.put("minecraft:wall_banner", "Banner");
      p_145919_.put("minecraft:standing_banner", "Banner");
      p_145919_.put("minecraft:structure_block", "Structure");
      p_145919_.put("minecraft:end_portal", "Airportal");
      p_145919_.put("minecraft:end_gateway", "EndGateway");
      p_145919_.put("minecraft:shield", "Banner");
   });
   public static final Map<String, String> ITEM_TO_ENTITY = Map.of("minecraft:armor_stand", "ArmorStand", "minecraft:painting", "Painting");
   protected static final HookFunction ADD_NAMES = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_18312_, T p_18313_) {
         return V99.addNames(new Dynamic(p_18312_, p_18313_), V99.ITEM_TO_BLOCKENTITY, V99.ITEM_TO_ENTITY);
      }
   };

   public V99(int p_18185_, Schema p_18186_) {
      super(p_18185_, p_18186_);
   }

   protected static void registerThrowableProjectile(Schema p_18225_, Map<String, Supplier<TypeTemplate>> p_18226_, String p_18227_) {
      p_18225_.register(p_18226_, p_18227_, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18225_)));
   }

   protected static void registerMinecart(Schema p_18237_, Map<String, Supplier<TypeTemplate>> p_18238_, String p_18239_) {
      p_18237_.register(p_18238_, p_18239_, () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18237_)));
   }

   protected static void registerInventory(Schema p_18247_, Map<String, Supplier<TypeTemplate>> p_18248_, String p_18249_) {
      p_18247_.register(p_18248_, p_18249_, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_18247_))));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_18305_) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      p_18305_.register(map, "Item", p_18301_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_18305_)));
      p_18305_.registerSimple(map, "XPOrb");
      registerThrowableProjectile(p_18305_, map, "ThrownEgg");
      p_18305_.registerSimple(map, "LeashKnot");
      p_18305_.registerSimple(map, "Painting");
      p_18305_.register(map, "Arrow", p_18295_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18305_)));
      p_18305_.register(map, "TippedArrow", p_18292_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18305_)));
      p_18305_.register(map, "SpectralArrow", p_397789_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18305_)));
      registerThrowableProjectile(p_18305_, map, "Snowball");
      registerThrowableProjectile(p_18305_, map, "Fireball");
      registerThrowableProjectile(p_18305_, map, "SmallFireball");
      registerThrowableProjectile(p_18305_, map, "ThrownEnderpearl");
      p_18305_.registerSimple(map, "EyeOfEnderSignal");
      p_18305_.register(
         map, "ThrownPotion", p_18289_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18305_), "Potion", References.ITEM_STACK.in(p_18305_))
      );
      registerThrowableProjectile(p_18305_, map, "ThrownExpBottle");
      p_18305_.register(map, "ItemFrame", p_18284_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_18305_)));
      registerThrowableProjectile(p_18305_, map, "WitherSkull");
      p_18305_.registerSimple(map, "PrimedTnt");
      p_18305_.register(
         map,
         "FallingSand",
         p_18279_ -> DSL.optionalFields("Block", References.BLOCK_NAME.in(p_18305_), "TileEntityData", References.BLOCK_ENTITY.in(p_18305_))
      );
      p_18305_.register(map, "FireworksRocketEntity", p_18274_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(p_18305_)));
      p_18305_.registerSimple(map, "Boat");
      p_18305_.register(
         map, "Minecart", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18305_), "Items", DSL.list(References.ITEM_STACK.in(p_18305_)))
      );
      registerMinecart(p_18305_, map, "MinecartRideable");
      p_18305_.register(
         map,
         "MinecartChest",
         p_18269_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18305_), "Items", DSL.list(References.ITEM_STACK.in(p_18305_)))
      );
      registerMinecart(p_18305_, map, "MinecartFurnace");
      registerMinecart(p_18305_, map, "MinecartTNT");
      p_18305_.register(
         map, "MinecartSpawner", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18305_), References.UNTAGGED_SPAWNER.in(p_18305_))
      );
      p_18305_.register(
         map,
         "MinecartHopper",
         p_18264_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18305_), "Items", DSL.list(References.ITEM_STACK.in(p_18305_)))
      );
      p_18305_.register(
         map,
         "MinecartCommandBlock",
         () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18305_), "LastOutput", References.TEXT_COMPONENT.in(p_18305_))
      );
      p_18305_.registerSimple(map, "ArmorStand");
      p_18305_.registerSimple(map, "Creeper");
      p_18305_.registerSimple(map, "Skeleton");
      p_18305_.registerSimple(map, "Spider");
      p_18305_.registerSimple(map, "Giant");
      p_18305_.registerSimple(map, "Zombie");
      p_18305_.registerSimple(map, "Slime");
      p_18305_.registerSimple(map, "Ghast");
      p_18305_.registerSimple(map, "PigZombie");
      p_18305_.register(map, "Enderman", p_18298_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(p_18305_)));
      p_18305_.registerSimple(map, "CaveSpider");
      p_18305_.registerSimple(map, "Silverfish");
      p_18305_.registerSimple(map, "Blaze");
      p_18305_.registerSimple(map, "LavaSlime");
      p_18305_.registerSimple(map, "EnderDragon");
      p_18305_.registerSimple(map, "WitherBoss");
      p_18305_.registerSimple(map, "Bat");
      p_18305_.registerSimple(map, "Witch");
      p_18305_.registerSimple(map, "Endermite");
      p_18305_.registerSimple(map, "Guardian");
      p_18305_.registerSimple(map, "Pig");
      p_18305_.registerSimple(map, "Sheep");
      p_18305_.registerSimple(map, "Cow");
      p_18305_.registerSimple(map, "Chicken");
      p_18305_.registerSimple(map, "Squid");
      p_18305_.registerSimple(map, "Wolf");
      p_18305_.registerSimple(map, "MushroomCow");
      p_18305_.registerSimple(map, "SnowMan");
      p_18305_.registerSimple(map, "Ozelot");
      p_18305_.registerSimple(map, "VillagerGolem");
      p_18305_.register(
         map,
         "EntityHorse",
         p_390453_ -> DSL.optionalFields(
            "Items",
            DSL.list(References.ITEM_STACK.in(p_18305_)),
            "ArmorItem",
            References.ITEM_STACK.in(p_18305_),
            "SaddleItem",
            References.ITEM_STACK.in(p_18305_)
         )
      );
      p_18305_.registerSimple(map, "Rabbit");
      p_18305_.register(
         map,
         "Villager",
         p_390451_ -> DSL.optionalFields(
            "Inventory",
            DSL.list(References.ITEM_STACK.in(p_18305_)),
            "Offers",
            DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_18305_)))
         )
      );
      p_18305_.registerSimple(map, "EnderCrystal");
      p_18305_.register(map, "AreaEffectCloud", p_326715_ -> DSL.optionalFields("Particle", References.PARTICLE.in(p_18305_)));
      p_18305_.registerSimple(map, "ShulkerBullet");
      p_18305_.registerSimple(map, "DragonFireball");
      p_18305_.registerSimple(map, "Shulker");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_18303_) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      registerInventory(p_18303_, map, "Furnace");
      registerInventory(p_18303_, map, "Chest");
      p_18303_.registerSimple(map, "EnderChest");
      p_18303_.register(map, "RecordPlayer", p_18235_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(p_18303_)));
      registerInventory(p_18303_, map, "Trap");
      registerInventory(p_18303_, map, "Dropper");
      p_18303_.register(map, "Sign", () -> sign(p_18303_));
      p_18303_.register(map, "MobSpawner", p_18223_ -> References.UNTAGGED_SPAWNER.in(p_18303_));
      p_18303_.registerSimple(map, "Music");
      p_18303_.registerSimple(map, "Piston");
      registerInventory(p_18303_, map, "Cauldron");
      p_18303_.registerSimple(map, "EnchantTable");
      p_18303_.registerSimple(map, "Airportal");
      p_18303_.register(map, "Control", () -> DSL.optionalFields("LastOutput", References.TEXT_COMPONENT.in(p_18303_)));
      p_18303_.registerSimple(map, "Beacon");
      p_18303_.register(map, "Skull", () -> DSL.optionalFields("custom_name", References.TEXT_COMPONENT.in(p_18303_)));
      p_18303_.registerSimple(map, "DLDetector");
      registerInventory(p_18303_, map, "Hopper");
      p_18303_.registerSimple(map, "Comparator");
      p_18303_.register(map, "FlowerPot", p_18192_ -> DSL.optionalFields("Item", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(p_18303_))));
      p_18303_.register(map, "Banner", () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_18303_)));
      p_18303_.registerSimple(map, "Structure");
      p_18303_.registerSimple(map, "EndGateway");
      return map;
   }

   public static TypeTemplate sign(Schema p_397989_) {
      return DSL.optionalFields(
         new Pair[]{
            Pair.of("Text1", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("Text2", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("Text3", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("Text4", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("FilteredText1", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("FilteredText2", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("FilteredText3", References.TEXT_COMPONENT.in(p_397989_)),
            Pair.of("FilteredText4", References.TEXT_COMPONENT.in(p_397989_))
         }
      );
   }

   public void registerTypes(Schema p_18307_, Map<String, Supplier<TypeTemplate>> p_18308_, Map<String, Supplier<TypeTemplate>> p_18309_) {
      p_18307_.registerType(
         false,
         References.LEVEL,
         () -> DSL.optionalFields(
            "CustomBossEvents", DSL.compoundList(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(p_18307_))), References.LIGHTWEIGHT_LEVEL.in(p_18307_)
         )
      );
      p_18307_.registerType(false, References.LIGHTWEIGHT_LEVEL, DSL::remainder);
      p_18307_.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(p_18307_)), "EnderItems", DSL.list(References.ITEM_STACK.in(p_18307_)))
      );
      p_18307_.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
            "Level",
            DSL.optionalFields(
               "Entities",
               DSL.list(References.ENTITY_TREE.in(p_18307_)),
               "TileEntities",
               DSL.list(DSL.or(References.BLOCK_ENTITY.in(p_18307_), DSL.remainder())),
               "TileTicks",
               DSL.list(DSL.fields("i", References.BLOCK_NAME.in(p_18307_)))
            )
         )
      );
      p_18307_.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(p_18307_), DSL.taggedChoiceLazy("id", DSL.string(), p_18309_))
      );
      p_18307_.registerType(
         true, References.ENTITY_TREE, () -> DSL.optionalFields("Riding", References.ENTITY_TREE.in(p_18307_), References.ENTITY.in(p_18307_))
      );
      p_18307_.registerType(false, References.ENTITY_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
      p_18307_.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_18307_),
            DSL.optionalFields("CustomName", DSL.constType(DSL.string()), DSL.taggedChoiceLazy("id", DSL.string(), p_18308_))
         )
      );
      p_18307_.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(
            DSL.optionalFields("id", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(p_18307_)), "tag", itemStackTag(p_18307_)),
            ADD_NAMES,
            HookFunction.IDENTITY
         )
      );
      p_18307_.registerType(false, References.OPTIONS, DSL::remainder);
      p_18307_.registerType(false, References.BLOCK_NAME, () -> DSL.or(DSL.constType(DSL.intType()), DSL.constType(NamespacedSchema.namespacedString())));
      p_18307_.registerType(false, References.ITEM_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
      p_18307_.registerType(false, References.STATS, DSL::remainder);
      p_18307_.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
      p_18307_.registerType(false, References.SAVED_DATA_TICKETS, DSL::remainder);
      p_18307_.registerType(
         false,
         References.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(p_18307_)))))
      );
      p_18307_.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
      p_18307_.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
      p_18307_.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
      p_18307_.registerType(
         false,
         References.SAVED_DATA_SCOREBOARD,
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields(
               "Objectives",
               DSL.list(References.OBJECTIVE.in(p_18307_)),
               "Teams",
               DSL.list(References.TEAM.in(p_18307_)),
               "PlayerScores",
               DSL.list(DSL.optionalFields("display", References.TEXT_COMPONENT.in(p_18307_)))
            )
         )
      );
      p_18307_.registerType(false, References.SAVED_DATA_STOPWATCHES, DSL::remainder);
      p_18307_.registerType(
         false,
         References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
         () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(p_18307_))))
      );
      p_18307_.registerType(false, References.SAVED_DATA_WORLD_BORDER, DSL::remainder);
      p_18307_.registerType(false, References.DEBUG_PROFILE, DSL::remainder);
      p_18307_.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
      p_18307_.registerType(false, References.OBJECTIVE, DSL::remainder);
      p_18307_.registerType(
         false,
         References.TEAM,
         () -> DSL.optionalFields(
            "MemberNamePrefix",
            References.TEXT_COMPONENT.in(p_18307_),
            "MemberNameSuffix",
            References.TEXT_COMPONENT.in(p_18307_),
            "DisplayName",
            References.TEXT_COMPONENT.in(p_18307_)
         )
      );
      p_18307_.registerType(true, References.UNTAGGED_SPAWNER, DSL::remainder);
      p_18307_.registerType(false, References.POI_CHUNK, DSL::remainder);
      p_18307_.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
      p_18307_.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(p_18307_))));
      p_18307_.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
      p_18307_.registerType(
         true,
         References.VILLAGER_TRADE,
         () -> DSL.optionalFields(
            "buy", References.ITEM_STACK.in(p_18307_), "buyB", References.ITEM_STACK.in(p_18307_), "sell", References.ITEM_STACK.in(p_18307_)
         )
      );
      p_18307_.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
      p_18307_.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType(DSL.string()));
      p_18307_.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(p_18307_))),
            "blocks",
            DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(p_18307_))),
            "palette",
            DSL.list(References.BLOCK_STATE.in(p_18307_))
         )
      );
      p_18307_.registerType(false, References.BLOCK_STATE, DSL::remainder);
      p_18307_.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
      p_18307_.registerType(true, References.ENTITY_EQUIPMENT, () -> DSL.optional(DSL.field("Equipment", DSL.list(References.ITEM_STACK.in(p_18307_)))));
   }

   public static TypeTemplate itemStackTag(Schema p_392240_) {
      return DSL.optionalFields(
         new Pair[]{
            Pair.of("EntityTag", References.ENTITY_TREE.in(p_392240_)),
            Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(p_392240_)),
            Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(p_392240_))),
            Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(p_392240_))),
            Pair.of("Items", DSL.list(References.ITEM_STACK.in(p_392240_))),
            Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(p_392240_))),
            Pair.of("pages", DSL.list(References.TEXT_COMPONENT.in(p_392240_))),
            Pair.of("filtered_pages", DSL.compoundList(References.TEXT_COMPONENT.in(p_392240_))),
            Pair.of("display", DSL.optionalFields("Name", References.TEXT_COMPONENT.in(p_392240_), "Lore", DSL.list(References.TEXT_COMPONENT.in(p_392240_))))
         }
      );
   }

   protected static <T> T addNames(Dynamic<T> p_18206_, Map<String, String> p_18207_, Map<String, String> p_334570_) {
      return (T)p_18206_.update("tag", p_145917_ -> p_145917_.update("BlockEntityTag", p_145912_ -> {
         String s = p_18206_.get("id").asString().result().map(NamespacedSchema::ensureNamespaced).orElse("minecraft:air");
         if (!"minecraft:air".equals(s)) {
            String s1 = p_18207_.get(s);
            if (s1 != null) {
               return p_145912_.set("id", p_18206_.createString(s1));
            }

            LOGGER.warn("Unable to resolve BlockEntity for ItemStack: {}", s);
         }

         return p_145912_;
      }).update("EntityTag", p_341252_ -> {
         if (p_341252_.get("id").result().isPresent()) {
            return p_341252_;
         }

         String s = NamespacedSchema.ensureNamespaced(p_18206_.get("id").asString(""));
         String s1 = p_334570_.get(s);
         return s1 != null ? p_341252_.set("id", p_18206_.createString(s1)) : p_341252_;
      })).getValue();
   }
}
