package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V1460 extends NamespacedSchema {
   public V1460(int p_17553_, Schema p_17554_) {
      super(p_17553_, p_17554_);
   }

   protected static void registerMob(Schema p_17561_, Map<String, Supplier<TypeTemplate>> p_17562_, String p_17563_) {
      p_17561_.registerSimple(p_17562_, p_17563_);
   }

   protected static void registerInventory(Schema p_17576_, Map<String, Supplier<TypeTemplate>> p_17577_, String p_17578_) {
      p_17576_.register(p_17577_, p_17578_, () -> V1458.nameableInventory(p_17576_));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17658_) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      p_17658_.register(map, "minecraft:area_effect_cloud", p_326692_ -> DSL.optionalFields("Particle", References.PARTICLE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:armor_stand");
      p_17658_.register(map, "minecraft:arrow", p_17683_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:bat");
      registerMob(p_17658_, map, "minecraft:blaze");
      p_17658_.registerSimple(map, "minecraft:boat");
      registerMob(p_17658_, map, "minecraft:cave_spider");
      p_17658_.register(
         map,
         "minecraft:chest_minecart",
         p_17680_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), "Items", DSL.list(References.ITEM_STACK.in(p_17658_)))
      );
      registerMob(p_17658_, map, "minecraft:chicken");
      p_17658_.register(
         map,
         "minecraft:commandblock_minecart",
         p_390346_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), "LastOutput", References.TEXT_COMPONENT.in(p_17658_))
      );
      registerMob(p_17658_, map, "minecraft:cow");
      registerMob(p_17658_, map, "minecraft:creeper");
      p_17658_.register(
         map,
         "minecraft:donkey",
         p_390366_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_17658_)), "SaddleItem", References.ITEM_STACK.in(p_17658_))
      );
      p_17658_.registerSimple(map, "minecraft:dragon_fireball");
      p_17658_.registerSimple(map, "minecraft:egg");
      registerMob(p_17658_, map, "minecraft:elder_guardian");
      p_17658_.registerSimple(map, "minecraft:ender_crystal");
      registerMob(p_17658_, map, "minecraft:ender_dragon");
      p_17658_.register(map, "minecraft:enderman", p_17677_ -> DSL.optionalFields("carriedBlockState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:endermite");
      p_17658_.registerSimple(map, "minecraft:ender_pearl");
      p_17658_.registerSimple(map, "minecraft:evocation_fangs");
      registerMob(p_17658_, map, "minecraft:evocation_illager");
      p_17658_.registerSimple(map, "minecraft:eye_of_ender_signal");
      p_17658_.register(
         map,
         "minecraft:falling_block",
         p_17668_ -> DSL.optionalFields("BlockState", References.BLOCK_STATE.in(p_17658_), "TileEntityData", References.BLOCK_ENTITY.in(p_17658_))
      );
      p_17658_.registerSimple(map, "minecraft:fireball");
      p_17658_.register(map, "minecraft:fireworks_rocket", p_390348_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(p_17658_)));
      p_17658_.register(map, "minecraft:furnace_minecart", p_17654_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:ghast");
      registerMob(p_17658_, map, "minecraft:giant");
      registerMob(p_17658_, map, "minecraft:guardian");
      p_17658_.register(
         map,
         "minecraft:hopper_minecart",
         p_17651_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), "Items", DSL.list(References.ITEM_STACK.in(p_17658_)))
      );
      p_17658_.register(
         map,
         "minecraft:horse",
         p_390377_ -> DSL.optionalFields("ArmorItem", References.ITEM_STACK.in(p_17658_), "SaddleItem", References.ITEM_STACK.in(p_17658_))
      );
      registerMob(p_17658_, map, "minecraft:husk");
      registerMob(p_17658_, map, "minecraft:illusion_illager");
      p_17658_.register(map, "minecraft:item", p_17645_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_17658_)));
      p_17658_.register(map, "minecraft:item_frame", p_17642_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_17658_)));
      p_17658_.registerSimple(map, "minecraft:leash_knot");
      p_17658_.register(
         map,
         "minecraft:llama",
         p_390355_ -> DSL.optionalFields(
            "Items",
            DSL.list(References.ITEM_STACK.in(p_17658_)),
            "SaddleItem",
            References.ITEM_STACK.in(p_17658_),
            "DecorItem",
            References.ITEM_STACK.in(p_17658_)
         )
      );
      p_17658_.registerSimple(map, "minecraft:llama_spit");
      registerMob(p_17658_, map, "minecraft:magma_cube");
      p_17658_.register(map, "minecraft:minecart", p_17634_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:mooshroom");
      p_17658_.register(
         map,
         "minecraft:mule",
         p_390362_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_17658_)), "SaddleItem", References.ITEM_STACK.in(p_17658_))
      );
      registerMob(p_17658_, map, "minecraft:ocelot");
      p_17658_.registerSimple(map, "minecraft:painting");
      registerMob(p_17658_, map, "minecraft:parrot");
      registerMob(p_17658_, map, "minecraft:pig");
      registerMob(p_17658_, map, "minecraft:polar_bear");
      p_17658_.register(map, "minecraft:potion", p_17624_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:rabbit");
      registerMob(p_17658_, map, "minecraft:sheep");
      registerMob(p_17658_, map, "minecraft:shulker");
      p_17658_.registerSimple(map, "minecraft:shulker_bullet");
      registerMob(p_17658_, map, "minecraft:silverfish");
      registerMob(p_17658_, map, "minecraft:skeleton");
      p_17658_.register(map, "minecraft:skeleton_horse", p_17665_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:slime");
      p_17658_.registerSimple(map, "minecraft:small_fireball");
      p_17658_.registerSimple(map, "minecraft:snowball");
      registerMob(p_17658_, map, "minecraft:snowman");
      p_17658_.register(
         map,
         "minecraft:spawner_minecart",
         p_17614_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_), References.UNTAGGED_SPAWNER.in(p_17658_))
      );
      p_17658_.register(map, "minecraft:spectral_arrow", p_17609_ -> DSL.optionalFields("inBlockState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:spider");
      registerMob(p_17658_, map, "minecraft:squid");
      registerMob(p_17658_, map, "minecraft:stray");
      p_17658_.registerSimple(map, "minecraft:tnt");
      p_17658_.register(map, "minecraft:tnt_minecart", p_17604_ -> DSL.optionalFields("DisplayState", References.BLOCK_STATE.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:vex");
      p_17658_.register(
         map,
         "minecraft:villager",
         p_390353_ -> DSL.optionalFields(
            "Inventory",
            DSL.list(References.ITEM_STACK.in(p_17658_)),
            "Offers",
            DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17658_)))
         )
      );
      registerMob(p_17658_, map, "minecraft:villager_golem");
      registerMob(p_17658_, map, "minecraft:vindication_illager");
      registerMob(p_17658_, map, "minecraft:witch");
      registerMob(p_17658_, map, "minecraft:wither");
      registerMob(p_17658_, map, "minecraft:wither_skeleton");
      p_17658_.registerSimple(map, "minecraft:wither_skull");
      registerMob(p_17658_, map, "minecraft:wolf");
      p_17658_.registerSimple(map, "minecraft:xp_bottle");
      p_17658_.registerSimple(map, "minecraft:xp_orb");
      registerMob(p_17658_, map, "minecraft:zombie");
      p_17658_.register(map, "minecraft:zombie_horse", p_390379_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_17658_)));
      registerMob(p_17658_, map, "minecraft:zombie_pigman");
      p_17658_.register(
         map,
         "minecraft:zombie_villager",
         p_390357_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_17658_))))
      );
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_17656_) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      registerInventory(p_17656_, map, "minecraft:furnace");
      registerInventory(p_17656_, map, "minecraft:chest");
      registerInventory(p_17656_, map, "minecraft:trapped_chest");
      p_17656_.registerSimple(map, "minecraft:ender_chest");
      p_17656_.register(map, "minecraft:jukebox", p_17586_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(p_17656_)));
      registerInventory(p_17656_, map, "minecraft:dispenser");
      registerInventory(p_17656_, map, "minecraft:dropper");
      p_17656_.register(map, "minecraft:sign", () -> V99.sign(p_17656_));
      p_17656_.register(map, "minecraft:mob_spawner", p_17574_ -> References.UNTAGGED_SPAWNER.in(p_17656_));
      p_17656_.register(map, "minecraft:piston", p_17559_ -> DSL.optionalFields("blockState", References.BLOCK_STATE.in(p_17656_)));
      registerInventory(p_17656_, map, "minecraft:brewing_stand");
      p_17656_.register(map, "minecraft:enchanting_table", () -> V1458.nameable(p_17656_));
      p_17656_.registerSimple(map, "minecraft:end_portal");
      p_17656_.register(map, "minecraft:beacon", () -> V1458.nameable(p_17656_));
      p_17656_.register(map, "minecraft:skull", () -> DSL.optionalFields("custom_name", References.TEXT_COMPONENT.in(p_17656_)));
      p_17656_.registerSimple(map, "minecraft:daylight_detector");
      registerInventory(p_17656_, map, "minecraft:hopper");
      p_17656_.registerSimple(map, "minecraft:comparator");
      p_17656_.register(map, "minecraft:banner", () -> V1458.nameable(p_17656_));
      p_17656_.registerSimple(map, "minecraft:structure_block");
      p_17656_.registerSimple(map, "minecraft:end_gateway");
      p_17656_.register(map, "minecraft:command_block", () -> DSL.optionalFields("LastOutput", References.TEXT_COMPONENT.in(p_17656_)));
      registerInventory(p_17656_, map, "minecraft:shulker_box");
      p_17656_.registerSimple(map, "minecraft:bed");
      return map;
   }

   public void registerTypes(Schema p_17660_, Map<String, Supplier<TypeTemplate>> p_17661_, Map<String, Supplier<TypeTemplate>> p_17662_) {
      p_17660_.registerType(
         false,
         References.LEVEL,
         () -> DSL.optionalFields(
            "CustomBossEvents", DSL.compoundList(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(p_17660_))), References.LIGHTWEIGHT_LEVEL.in(p_17660_)
         )
      );
      p_17660_.registerType(false, References.LIGHTWEIGHT_LEVEL, DSL::remainder);
      p_17660_.registerType(false, References.RECIPE, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(
         false,
         References.PLAYER,
         () -> DSL.optionalFields(
            new Pair[]{
               Pair.of("RootVehicle", DSL.optionalFields("Entity", References.ENTITY_TREE.in(p_17660_))),
               Pair.of("ender_pearls", DSL.list(References.ENTITY_TREE.in(p_17660_))),
               Pair.of("Inventory", DSL.list(References.ITEM_STACK.in(p_17660_))),
               Pair.of("EnderItems", DSL.list(References.ITEM_STACK.in(p_17660_))),
               Pair.of("ShoulderEntityLeft", References.ENTITY_TREE.in(p_17660_)),
               Pair.of("ShoulderEntityRight", References.ENTITY_TREE.in(p_17660_)),
               Pair.of(
                  "recipeBook",
                  DSL.optionalFields("recipes", DSL.list(References.RECIPE.in(p_17660_)), "toBeDisplayed", DSL.list(References.RECIPE.in(p_17660_)))
               )
            }
         )
      );
      p_17660_.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
            "Level",
            DSL.optionalFields(
               "Entities",
               DSL.list(References.ENTITY_TREE.in(p_17660_)),
               "TileEntities",
               DSL.list(DSL.or(References.BLOCK_ENTITY.in(p_17660_), DSL.remainder())),
               "TileTicks",
               DSL.list(DSL.fields("i", References.BLOCK_NAME.in(p_17660_))),
               "Sections",
               DSL.list(DSL.optionalFields("Palette", DSL.list(References.BLOCK_STATE.in(p_17660_))))
            )
         )
      );
      p_17660_.registerType(
         true,
         References.BLOCK_ENTITY,
         () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(p_17660_), DSL.taggedChoiceLazy("id", namespacedString(), p_17662_))
      );
      p_17660_.registerType(
         true, References.ENTITY_TREE, () -> DSL.optionalFields("Passengers", DSL.list(References.ENTITY_TREE.in(p_17660_)), References.ENTITY.in(p_17660_))
      );
      p_17660_.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_17660_),
            DSL.optionalFields("CustomName", References.TEXT_COMPONENT.in(p_17660_), DSL.taggedChoiceLazy("id", namespacedString(), p_17661_))
         )
      );
      p_17660_.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(DSL.optionalFields("id", References.ITEM_NAME.in(p_17660_), "tag", V99.itemStackTag(p_17660_)), V705.ADD_NAMES, HookFunction.IDENTITY)
      );
      p_17660_.registerType(false, References.HOTBAR, () -> DSL.compoundList(DSL.list(References.ITEM_STACK.in(p_17660_))));
      p_17660_.registerType(false, References.OPTIONS, DSL::remainder);
      p_17660_.registerType(
         false,
         References.STRUCTURE,
         () -> DSL.optionalFields(
            "entities",
            DSL.list(DSL.optionalFields("nbt", References.ENTITY_TREE.in(p_17660_))),
            "blocks",
            DSL.list(DSL.optionalFields("nbt", References.BLOCK_ENTITY.in(p_17660_))),
            "palette",
            DSL.list(References.BLOCK_STATE.in(p_17660_))
         )
      );
      p_17660_.registerType(false, References.BLOCK_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.ITEM_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.BLOCK_STATE, DSL::remainder);
      p_17660_.registerType(false, References.FLAT_BLOCK_STATE, DSL::remainder);
      Supplier<TypeTemplate> supplier = () -> DSL.compoundList(References.ITEM_NAME.in(p_17660_), DSL.constType(DSL.intType()));
      p_17660_.registerType(
         false,
         References.STATS,
         () -> DSL.optionalFields(
            "stats",
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:crafted", supplier.get()),
                  Pair.of("minecraft:used", supplier.get()),
                  Pair.of("minecraft:broken", supplier.get()),
                  Pair.of("minecraft:picked_up", supplier.get()),
                  Pair.of("minecraft:dropped", supplier.get()),
                  Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
               }
            )
         )
      );
      p_17660_.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_TICKETS, DSL::remainder);
      p_17660_.registerType(
         false,
         References.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("Name", References.TEXT_COMPONENT.in(p_17660_)))))
      );
      p_17660_.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
      p_17660_.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
      p_17660_.registerType(
         false,
         References.SAVED_DATA_SCOREBOARD,
         () -> DSL.optionalFields(
            "data",
            DSL.optionalFields(
               "Objectives",
               DSL.list(References.OBJECTIVE.in(p_17660_)),
               "Teams",
               DSL.list(References.TEAM.in(p_17660_)),
               "PlayerScores",
               DSL.list(DSL.optionalFields("display", References.TEXT_COMPONENT.in(p_17660_)))
            )
         )
      );
      p_17660_.registerType(false, References.SAVED_DATA_STOPWATCHES, DSL::remainder);
      p_17660_.registerType(
         false,
         References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
         () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(p_17660_))))
      );
      p_17660_.registerType(false, References.SAVED_DATA_WORLD_BORDER, DSL::remainder);
      p_17660_.registerType(false, References.DEBUG_PROFILE, DSL::remainder);
      p_17660_.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
      Map<String, Supplier<TypeTemplate>> map = V1451_6.createCriterionTypes(p_17660_);
      p_17660_.registerType(
         false,
         References.OBJECTIVE,
         () -> DSL.hook(
            DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), map), "DisplayName", References.TEXT_COMPONENT.in(p_17660_)),
            V1451_6.UNPACK_OBJECTIVE_ID,
            V1451_6.REPACK_OBJECTIVE_ID
         )
      );
      p_17660_.registerType(
         false,
         References.TEAM,
         () -> DSL.optionalFields(
            "MemberNamePrefix",
            References.TEXT_COMPONENT.in(p_17660_),
            "MemberNameSuffix",
            References.TEXT_COMPONENT.in(p_17660_),
            "DisplayName",
            References.TEXT_COMPONENT.in(p_17660_)
         )
      );
      p_17660_.registerType(
         true,
         References.UNTAGGED_SPAWNER,
         () -> DSL.optionalFields(
            "SpawnPotentials", DSL.list(DSL.fields("Entity", References.ENTITY_TREE.in(p_17660_))), "SpawnData", References.ENTITY_TREE.in(p_17660_)
         )
      );
      p_17660_.registerType(
         false,
         References.ADVANCEMENTS,
         () -> DSL.optionalFields(
            "minecraft:adventure/adventuring_time",
            DSL.optionalFields("criteria", DSL.compoundList(References.BIOME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:adventure/kill_a_mob",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:adventure/kill_all_mobs",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string()))),
            "minecraft:husbandry/bred_all_animals",
            DSL.optionalFields("criteria", DSL.compoundList(References.ENTITY_NAME.in(p_17660_), DSL.constType(DSL.string())))
         )
      );
      p_17660_.registerType(false, References.BIOME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.ENTITY_NAME, () -> DSL.constType(namespacedString()));
      p_17660_.registerType(false, References.POI_CHUNK, DSL::remainder);
      p_17660_.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
      p_17660_.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(p_17660_))));
      p_17660_.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
      p_17660_.registerType(
         true,
         References.VILLAGER_TRADE,
         () -> DSL.optionalFields(
            "buy", References.ITEM_STACK.in(p_17660_), "buyB", References.ITEM_STACK.in(p_17660_), "sell", References.ITEM_STACK.in(p_17660_)
         )
      );
      p_17660_.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
      p_17660_.registerType(true, References.TEXT_COMPONENT, () -> DSL.constType(DSL.string()));
      p_17660_.registerType(
         true,
         References.ENTITY_EQUIPMENT,
         () -> DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(References.ITEM_STACK.in(p_17660_)))),
            new TypeTemplate[]{
               DSL.optional(DSL.field("HandItems", DSL.list(References.ITEM_STACK.in(p_17660_)))),
               DSL.optional(DSL.field("body_armor_item", References.ITEM_STACK.in(p_17660_))),
               DSL.optional(DSL.field("saddle", References.ITEM_STACK.in(p_17660_)))
            }
         )
      );
   }
}
