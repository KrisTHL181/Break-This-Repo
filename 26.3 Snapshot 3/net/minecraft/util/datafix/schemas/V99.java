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
   private static final Map<String, String> ITEM_TO_BLOCKENTITY = (Map<String, String>)DataFixUtils.make(Maps.newHashMap(), map -> {
      map.put("minecraft:furnace", "Furnace");
      map.put("minecraft:lit_furnace", "Furnace");
      map.put("minecraft:chest", "Chest");
      map.put("minecraft:trapped_chest", "Chest");
      map.put("minecraft:ender_chest", "EnderChest");
      map.put("minecraft:jukebox", "RecordPlayer");
      map.put("minecraft:dispenser", "Trap");
      map.put("minecraft:dropper", "Dropper");
      map.put("minecraft:sign", "Sign");
      map.put("minecraft:mob_spawner", "MobSpawner");
      map.put("minecraft:noteblock", "Music");
      map.put("minecraft:brewing_stand", "Cauldron");
      map.put("minecraft:enhanting_table", "EnchantTable");
      map.put("minecraft:command_block", "CommandBlock");
      map.put("minecraft:beacon", "Beacon");
      map.put("minecraft:skull", "Skull");
      map.put("minecraft:daylight_detector", "DLDetector");
      map.put("minecraft:hopper", "Hopper");
      map.put("minecraft:banner", "Banner");
      map.put("minecraft:flower_pot", "FlowerPot");
      map.put("minecraft:repeating_command_block", "CommandBlock");
      map.put("minecraft:chain_command_block", "CommandBlock");
      map.put("minecraft:standing_sign", "Sign");
      map.put("minecraft:wall_sign", "Sign");
      map.put("minecraft:piston_head", "Piston");
      map.put("minecraft:daylight_detector_inverted", "DLDetector");
      map.put("minecraft:unpowered_comparator", "Comparator");
      map.put("minecraft:powered_comparator", "Comparator");
      map.put("minecraft:wall_banner", "Banner");
      map.put("minecraft:standing_banner", "Banner");
      map.put("minecraft:structure_block", "Structure");
      map.put("minecraft:end_portal", "Airportal");
      map.put("minecraft:end_gateway", "EndGateway");
      map.put("minecraft:shield", "Banner");
   });
   public static final Map<String, String> ITEM_TO_ENTITY = Map.of("minecraft:armor_stand", "ArmorStand", "minecraft:painting", "Painting");
   protected static final HookFunction ADD_NAMES = new HookFunction() {
      public <T> T apply(final DynamicOps<T> ops, final T value) {
         return V99.addNames(new Dynamic(ops, value), V99.ITEM_TO_BLOCKENTITY, V99.ITEM_TO_ENTITY);
      }
   };

   public V99(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   protected static void registerThrowableProjectile(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.register(map, name, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(schema)));
   }

   protected static void registerMinecart(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.register(map, name, () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema)));
   }

   protected static void registerInventory(final Schema schema, final Map<String, Supplier<TypeTemplate>> map, final String name) {
      schema.register(map, name, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema))));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      schema.register(map, "Item", name -> DSL.optionalFields("Item", References.ITEM_STACK.in(schema)));
      schema.registerSimple(map, "XPOrb");
      registerThrowableProjectile(schema, map, "ThrownEgg");
      schema.registerSimple(map, "LeashKnot");
      schema.registerSimple(map, "Painting");
      schema.register(map, "Arrow", name -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(schema)));
      schema.register(map, "TippedArrow", name -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(schema)));
      schema.register(map, "SpectralArrow", name -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(schema)));
      registerThrowableProjectile(schema, map, "Snowball");
      registerThrowableProjectile(schema, map, "Fireball");
      registerThrowableProjectile(schema, map, "SmallFireball");
      registerThrowableProjectile(schema, map, "ThrownEnderpearl");
      schema.registerSimple(map, "EyeOfEnderSignal");
      schema.register(map, "ThrownPotion", name -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(schema), "Potion", References.ITEM_STACK.in(schema)));
      registerThrowableProjectile(schema, map, "ThrownExpBottle");
      schema.register(map, "ItemFrame", name -> DSL.optionalFields("Item", References.ITEM_STACK.in(schema)));
      registerThrowableProjectile(schema, map, "WitherSkull");
      schema.registerSimple(map, "PrimedTnt");
      schema.register(
         map, "FallingSand", name -> DSL.optionalFields("Block", References.BLOCK_NAME.in(schema), "TileEntityData", References.BLOCK_ENTITY.in(schema))
      );
      schema.register(map, "FireworksRocketEntity", name -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(schema)));
      schema.registerSimple(map, "Boat");
      schema.register(
         map, "Minecart", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema), "Items", DSL.list(References.ITEM_STACK.in(schema)))
      );
      registerMinecart(schema, map, "MinecartRideable");
      schema.register(
         map, "MinecartChest", name -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema), "Items", DSL.list(References.ITEM_STACK.in(schema)))
      );
      registerMinecart(schema, map, "MinecartFurnace");
      registerMinecart(schema, map, "MinecartTNT");
      schema.register(map, "MinecartSpawner", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema), References.UNTAGGED_SPAWNER.in(schema)));
      schema.register(
         map,
         "MinecartHopper",
         name -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema), "Items", DSL.list(References.ITEM_STACK.in(schema)))
      );
      schema.register(
         map,
         "MinecartCommandBlock",
         () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(schema), "LastOutput", References.TEXT_COMPONENT.in(schema))
      );
      schema.registerSimple(map, "ArmorStand");
      schema.registerSimple(map, "Creeper");
      schema.registerSimple(map, "Skeleton");
      schema.registerSimple(map, "Spider");
      schema.registerSimple(map, "Giant");
      schema.registerSimple(map, "Zombie");
      schema.registerSimple(map, "Slime");
      schema.registerSimple(map, "Ghast");
      schema.registerSimple(map, "PigZombie");
      schema.register(map, "Enderman", name -> DSL.optionalFields("carried", References.BLOCK_NAME.in(schema)));
      schema.registerSimple(map, "CaveSpider");
      schema.registerSimple(map, "Silverfish");
      schema.registerSimple(map, "Blaze");
      schema.registerSimple(map, "LavaSlime");
      schema.registerSimple(map, "EnderDragon");
      schema.registerSimple(map, "WitherBoss");
      schema.registerSimple(map, "Bat");
      schema.registerSimple(map, "Witch");
      schema.registerSimple(map, "Endermite");
      schema.registerSimple(map, "Guardian");
      schema.registerSimple(map, "Pig");
      schema.registerSimple(map, "Sheep");
      schema.registerSimple(map, "Cow");
      schema.registerSimple(map, "Chicken");
      schema.registerSimple(map, "Squid");
      schema.registerSimple(map, "Wolf");
      schema.registerSimple(map, "MushroomCow");
      schema.registerSimple(map, "SnowMan");
      schema.registerSimple(map, "Ozelot");
      schema.registerSimple(map, "VillagerGolem");
      schema.register(
         map,
         "EntityHorse",
         name -> DSL.optionalFields(
            "Items", DSL.list(References.ITEM_STACK.in(schema)), "ArmorItem", References.ITEM_STACK.in(schema), "SaddleItem", References.ITEM_STACK.in(schema)
         )
      );
      schema.registerSimple(map, "Rabbit");
      schema.register(
         map,
         "Villager",
         name -> DSL.optionalFields(
            "Inventory", DSL.list(References.ITEM_STACK.in(schema)), "Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(schema)))
         )
      );
      schema.registerSimple(map, "EnderCrystal");
      schema.register(map, "AreaEffectCloud", name -> DSL.optionalFields("Particle", References.PARTICLE.in(schema)));
      schema.registerSimple(map, "ShulkerBullet");
      schema.registerSimple(map, "DragonFireball");
      schema.registerSimple(map, "Shulker");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(final Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      registerInventory(schema, map, "Furnace");
      registerInventory(schema, map, "Chest");
      schema.registerSimple(map, "EnderChest");
      schema.register(map, "RecordPlayer", name -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(schema)));
      registerInventory(schema, map, "Trap");
      registerInventory(schema, map, "Dropper");
      schema.register(map, "Sign", () -> sign(schema));
      schema.register(map, "MobSpawner", name -> References.UNTAGGED_SPAWNER.in(schema));
      schema.registerSimple(map, "Music");
      schema.registerSimple(map, "Piston");
      registerInventory(schema, map, "Cauldron");
      schema.registerSimple(map, "EnchantTable");
      schema.registerSimple(map, "Airportal");
      schema.register(map, "Control", () -> DSL.optionalFields("LastOutput", References.TEXT_COMPONENT.in(schema)));
      schema.registerSimple(map, "Beacon");
      schema.register(map, "Skull", () -> DSL.optionalFields("custom_name", References.TEXT_COMPONENT.in(schema)));
      schema.registerSimple(map, "DLDetector");
      registerInventory(schema, map, "Hopper");
      schema.registerSimple(map, "Comparator");
      schema.register(map, "FlowerPot", name -> DSL.optionalFields("Item", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(schema))));
      schema.register(map, "Banner", () -> DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(schema)));
      schema.registerSimple(map, "Structure");
      schema.registerSimple(map, "EndGateway");
      return map;
   }

   public static TypeTemplate sign(final Schema schema) {
      return DSL.optionalFields(
         new Pair[]{
            Pair.of("Text1", References.TEXT_COMPONENT.in(schema)),
            Pair.of("Text2", References.TEXT_COMPONENT.in(schema)),
            Pair.of("Text3", References.TEXT_COMPONENT.in(schema)),
            Pair.of("Text4", References.TEXT_COMPONENT.in(schema)),
            Pair.of("FilteredText1", References.TEXT_COMPONENT.in(schema)),
            Pair.of("FilteredText2", References.TEXT_COMPONENT.in(schema)),
            Pair.of("FilteredText3", References.TEXT_COMPONENT.in(schema)),
            Pair.of("FilteredText4", References.TEXT_COMPONENT.in(schema))
         }
      );
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      schema.registerType(
         false,
         References.LEVEL,
         () -> DSL.optionalFields(
            "CustomBossEvents", DSL.compoundList(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(schema))), References.LIGHTWEIGHT_LEVEL.in(schema)
         )
      );
      schema.registerType(false, References.LIGHTWEIGHT_LEVEL, DSL::remainder);
      schema.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(schema)), "EnderItems", DSL.list(References.ITEM_STACK.in(schema)))
      );
      schema.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
            "Level",
            DSL.optionalFields(
               "Entities",
               DSL.list(References.ENTITY_TREE.in(schema)),
               "TileEntities",
               DSL.list(DSL.or(References.BLOCK_ENTITY.in(schema), DSL.remainder())),
               "TileTicks",
               DSL.list(DSL.fields("i", References.BLOCK_NAME.in(schema)))
            )
         )
      );
      schema.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(schema), DSL.taggedChoiceLazy("id", DSL.string(), blockEntityTypes))
      );
      schema.registerType(true, References.ENTITY_TREE, () -> DSL.optionalFields("Riding", References.ENTITY_TREE.in(schema), References.ENTITY.in(schema)));
      schema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
      schema.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(schema),
            DSL.optionalFields("CustomName", DSL.constType(DSL.string()), DSL.taggedChoiceLazy("id", DSL.string(), entityTypes))
         )
      );
      schema.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(
            DSL.optionalFields("id", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(schema)), "tag", itemStackTag(schema)),
            ADD_NAMES,
            HookFunction.IDENTITY
         )
      );
      schema.registerType(false, References.OPTIONS, DSL::remainder);
      schema.registerType(false, References.BLOCK_NAME, () -> DSL.or(DSL.constType(DSL.intType()), DSL.constType(NamespacedSchema.namespacedString())));
      schema.registerType(false, References.ITEM_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
      schema.registerType(false, References.STATS, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
      schema.registerType(
         false,
         References.SAVED_DATA_CUSTOM_BOSS_EVENTS,
         () -> DSL.optionalFields("data", DSL.compoundList(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(schema))))
      );
      schema.registerType(false, References.SAVED_DATA_ENDER_DRAGON_FIGHT, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_GAME_RULES, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_TICKETS, DSL::remainder);
      schema.registerType(
         false,
         References.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(schema)))))
      );
      schema.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_SCHEDULED_EVENTS, DSL::remainder);
      schema.registerType(
         false,
         References.SAVED_DATA_SCOREBOARD,
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields(
               "Objectives",
               DSL.list(References.OBJECTIVE.in(schema)),
               "Teams",
               DSL.list(References.TEAM.in(schema)),
               "PlayerScores",
               DSL.list(DSL.optionalFields("display", References.TEXT_COMPONENT.in(schema)))
            )
         )
      );
      schema.registerType(false, References.SAVED_DATA_STOPWATCHES, DSL::remainder);
      schema.registerType(
         false,
         References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
         () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(schema))))
      );
      schema.registerType(false, References.SAVED_DATA_WANDERING_TRADER, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_WEATHER, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_WORLD_BORDER, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_WORLD_CLOCKS, DSL::remainder);
      schema.registerType(false, References.SAVED_DATA_WORLD_GEN_SETTINGS, () -> DSL.fields("data", References.WORLD_GEN_SETTINGS.in(schema)));
      schema.registerType(false, References.DEBUG_PROFILE, DSL::remainder);
      schema.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
      schema.registerType(false, References.OBJECTIVE, DSL::remainder);
      schema.registerType(
         false,
         References.TEAM,
         () -> DSL.optionalFields(
            "MemberNamePrefix",
            References.TEXT_COMPONENT.in(schema),
            "MemberNameSuffix",
            References.TEXT_COMPONENT.in(schema),
            "DisplayName",
            References.TEXT_COMPONENT.in(schema)
         )
      );
      schema.registerType(true, References.UNTAGGED_SPAWNER, DSL::remainder);
      schema.registerType(false, References.POI_CHUNK, DSL::remainder);
      schema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
      schema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(schema))));
      schema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
      schema.registerType(
         true,
         References.VILLAGER_TRADE,
         () -> DSL.optionalFields("buy", References.ITEM_STACK.in(schema), "buyB", References.ITEM_STACK.in(schema), "sell", References.ITEM_STACK.in(schema))
      );
      schema.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
      schema.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType(DSL.string()));
      schema.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(schema))),
            "blocks",
            DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(schema))),
            "palette",
            DSL.list(References.BLOCK_STATE.in(schema))
         )
      );
      schema.registerType(false, References.BLOCK_STATE, DSL::remainder);
      schema.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
      schema.registerType(true, References.ENTITY_EQUIPMENT, () -> DSL.optional(DSL.field("Equipment", DSL.list(References.ITEM_STACK.in(schema)))));
   }

   public static TypeTemplate itemStackTag(final Schema schema) {
      return DSL.optionalFields(
         new Pair[]{
            Pair.of("EntityTag", References.ENTITY_TREE.in(schema)),
            Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(schema)),
            Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(schema))),
            Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(schema))),
            Pair.of("Items", DSL.list(References.ITEM_STACK.in(schema))),
            Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(schema))),
            Pair.of("pages", DSL.list(References.TEXT_COMPONENT.in(schema))),
            Pair.of("filtered_pages", DSL.compoundList(References.TEXT_COMPONENT.in(schema))),
            Pair.of("display", DSL.optionalFields("Name", References.TEXT_COMPONENT.in(schema), "Lore", DSL.list(References.TEXT_COMPONENT.in(schema))))
         }
      );
   }

   protected static <T> T addNames(final Dynamic<T> input, final Map<String, String> itemToBlockEntityMap, final Map<String, String> itemToEntityMap) {
      return (T)input.update("tag", itemStackTag -> itemStackTag.update("BlockEntityTag", blockEntity -> {
         String itemId = input.get("id").asString().result().map(NamespacedSchema::ensureNamespaced).orElse("minecraft:air");
         if (!"minecraft:air".equals(itemId)) {
            String expectedId = itemToBlockEntityMap.get(itemId);
            if (expectedId != null) {
               return blockEntity.set("id", input.createString(expectedId));
            }

            LOGGER.warn("Unable to resolve BlockEntity for ItemStack: {}", itemId);
         }

         return blockEntity;
      }).update("EntityTag", entity -> {
         if (entity.get("id").result().isPresent()) {
            return entity;
         }

         String itemId = NamespacedSchema.ensureNamespaced(input.get("id").asString(""));
         String expectedId = itemToEntityMap.get(itemId);
         return expectedId != null ? entity.set("id", input.createString(expectedId)) : entity;
      })).getValue();
   }
}
