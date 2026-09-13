package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V705 extends NamespacedSchema {
   static final Map<String, String> ITEM_TO_ENTITY = ImmutableMap.builder()
      .put("minecraft:armor_stand", "minecraft:armor_stand")
      .put("minecraft:painting", "minecraft:painting")
      .put("minecraft:armadillo_spawn_egg", "minecraft:armadillo")
      .put("minecraft:allay_spawn_egg", "minecraft:allay")
      .put("minecraft:axolotl_spawn_egg", "minecraft:axolotl")
      .put("minecraft:bat_spawn_egg", "minecraft:bat")
      .put("minecraft:bee_spawn_egg", "minecraft:bee")
      .put("minecraft:blaze_spawn_egg", "minecraft:blaze")
      .put("minecraft:bogged_spawn_egg", "minecraft:bogged")
      .put("minecraft:breeze_spawn_egg", "minecraft:breeze")
      .put("minecraft:cat_spawn_egg", "minecraft:cat")
      .put("minecraft:camel_spawn_egg", "minecraft:camel")
      .put("minecraft:cave_spider_spawn_egg", "minecraft:cave_spider")
      .put("minecraft:chicken_spawn_egg", "minecraft:chicken")
      .put("minecraft:cod_spawn_egg", "minecraft:cod")
      .put("minecraft:cow_spawn_egg", "minecraft:cow")
      .put("minecraft:creeper_spawn_egg", "minecraft:creeper")
      .put("minecraft:dolphin_spawn_egg", "minecraft:dolphin")
      .put("minecraft:donkey_spawn_egg", "minecraft:donkey")
      .put("minecraft:drowned_spawn_egg", "minecraft:drowned")
      .put("minecraft:elder_guardian_spawn_egg", "minecraft:elder_guardian")
      .put("minecraft:ender_dragon_spawn_egg", "minecraft:ender_dragon")
      .put("minecraft:enderman_spawn_egg", "minecraft:enderman")
      .put("minecraft:endermite_spawn_egg", "minecraft:endermite")
      .put("minecraft:evoker_spawn_egg", "minecraft:evoker")
      .put("minecraft:fox_spawn_egg", "minecraft:fox")
      .put("minecraft:frog_spawn_egg", "minecraft:frog")
      .put("minecraft:ghast_spawn_egg", "minecraft:ghast")
      .put("minecraft:glow_squid_spawn_egg", "minecraft:glow_squid")
      .put("minecraft:goat_spawn_egg", "minecraft:goat")
      .put("minecraft:guardian_spawn_egg", "minecraft:guardian")
      .put("minecraft:hoglin_spawn_egg", "minecraft:hoglin")
      .put("minecraft:horse_spawn_egg", "minecraft:horse")
      .put("minecraft:husk_spawn_egg", "minecraft:husk")
      .put("minecraft:iron_golem_spawn_egg", "minecraft:iron_golem")
      .put("minecraft:llama_spawn_egg", "minecraft:llama")
      .put("minecraft:magma_cube_spawn_egg", "minecraft:magma_cube")
      .put("minecraft:mooshroom_spawn_egg", "minecraft:mooshroom")
      .put("minecraft:mule_spawn_egg", "minecraft:mule")
      .put("minecraft:ocelot_spawn_egg", "minecraft:ocelot")
      .put("minecraft:panda_spawn_egg", "minecraft:panda")
      .put("minecraft:parrot_spawn_egg", "minecraft:parrot")
      .put("minecraft:phantom_spawn_egg", "minecraft:phantom")
      .put("minecraft:pig_spawn_egg", "minecraft:pig")
      .put("minecraft:piglin_spawn_egg", "minecraft:piglin")
      .put("minecraft:piglin_brute_spawn_egg", "minecraft:piglin_brute")
      .put("minecraft:pillager_spawn_egg", "minecraft:pillager")
      .put("minecraft:polar_bear_spawn_egg", "minecraft:polar_bear")
      .put("minecraft:pufferfish_spawn_egg", "minecraft:pufferfish")
      .put("minecraft:rabbit_spawn_egg", "minecraft:rabbit")
      .put("minecraft:ravager_spawn_egg", "minecraft:ravager")
      .put("minecraft:salmon_spawn_egg", "minecraft:salmon")
      .put("minecraft:sheep_spawn_egg", "minecraft:sheep")
      .put("minecraft:shulker_spawn_egg", "minecraft:shulker")
      .put("minecraft:silverfish_spawn_egg", "minecraft:silverfish")
      .put("minecraft:skeleton_spawn_egg", "minecraft:skeleton")
      .put("minecraft:skeleton_horse_spawn_egg", "minecraft:skeleton_horse")
      .put("minecraft:slime_spawn_egg", "minecraft:slime")
      .put("minecraft:sniffer_spawn_egg", "minecraft:sniffer")
      .put("minecraft:snow_golem_spawn_egg", "minecraft:snow_golem")
      .put("minecraft:spider_spawn_egg", "minecraft:spider")
      .put("minecraft:squid_spawn_egg", "minecraft:squid")
      .put("minecraft:stray_spawn_egg", "minecraft:stray")
      .put("minecraft:strider_spawn_egg", "minecraft:strider")
      .put("minecraft:tadpole_spawn_egg", "minecraft:tadpole")
      .put("minecraft:trader_llama_spawn_egg", "minecraft:trader_llama")
      .put("minecraft:tropical_fish_spawn_egg", "minecraft:tropical_fish")
      .put("minecraft:turtle_spawn_egg", "minecraft:turtle")
      .put("minecraft:vex_spawn_egg", "minecraft:vex")
      .put("minecraft:villager_spawn_egg", "minecraft:villager")
      .put("minecraft:vindicator_spawn_egg", "minecraft:vindicator")
      .put("minecraft:wandering_trader_spawn_egg", "minecraft:wandering_trader")
      .put("minecraft:warden_spawn_egg", "minecraft:warden")
      .put("minecraft:witch_spawn_egg", "minecraft:witch")
      .put("minecraft:wither_spawn_egg", "minecraft:wither")
      .put("minecraft:wither_skeleton_spawn_egg", "minecraft:wither_skeleton")
      .put("minecraft:wolf_spawn_egg", "minecraft:wolf")
      .put("minecraft:zoglin_spawn_egg", "minecraft:zoglin")
      .put("minecraft:zombie_spawn_egg", "minecraft:zombie")
      .put("minecraft:zombie_horse_spawn_egg", "minecraft:zombie_horse")
      .put("minecraft:zombie_villager_spawn_egg", "minecraft:zombie_villager")
      .put("minecraft:zombified_piglin_spawn_egg", "minecraft:zombified_piglin")
      .put("minecraft:item_frame", "minecraft:item_frame")
      .put("minecraft:boat", "minecraft:oak_boat")
      .put("minecraft:oak_boat", "minecraft:oak_boat")
      .put("minecraft:oak_chest_boat", "minecraft:oak_chest_boat")
      .put("minecraft:spruce_boat", "minecraft:spruce_boat")
      .put("minecraft:spruce_chest_boat", "minecraft:spruce_chest_boat")
      .put("minecraft:birch_boat", "minecraft:birch_boat")
      .put("minecraft:birch_chest_boat", "minecraft:birch_chest_boat")
      .put("minecraft:jungle_boat", "minecraft:jungle_boat")
      .put("minecraft:jungle_chest_boat", "minecraft:jungle_chest_boat")
      .put("minecraft:acacia_boat", "minecraft:acacia_boat")
      .put("minecraft:acacia_chest_boat", "minecraft:acacia_chest_boat")
      .put("minecraft:cherry_boat", "minecraft:cherry_boat")
      .put("minecraft:cherry_chest_boat", "minecraft:cherry_chest_boat")
      .put("minecraft:dark_oak_boat", "minecraft:dark_oak_boat")
      .put("minecraft:dark_oak_chest_boat", "minecraft:dark_oak_chest_boat")
      .put("minecraft:mangrove_boat", "minecraft:mangrove_boat")
      .put("minecraft:mangrove_chest_boat", "minecraft:mangrove_chest_boat")
      .put("minecraft:bamboo_raft", "minecraft:bamboo_raft")
      .put("minecraft:bamboo_chest_raft", "minecraft:bamboo_chest_raft")
      .put("minecraft:minecart", "minecraft:minecart")
      .put("minecraft:chest_minecart", "minecraft:chest_minecart")
      .put("minecraft:furnace_minecart", "minecraft:furnace_minecart")
      .put("minecraft:tnt_minecart", "minecraft:tnt_minecart")
      .put("minecraft:hopper_minecart", "minecraft:hopper_minecart")
      .build();
   protected static final HookFunction ADD_NAMES = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_18167_, T p_18168_) {
         return V99.addNames(new Dynamic(p_18167_, p_18168_), V704.ITEM_TO_BLOCKENTITY, V705.ITEM_TO_ENTITY);
      }
   };

   public V705(int p_18075_, Schema p_18076_) {
      super(p_18075_, p_18076_);
   }

   protected static void registerMob(Schema p_18083_, Map<String, Supplier<TypeTemplate>> p_18084_, String p_18085_) {
      p_18083_.registerSimple(p_18084_, p_18085_);
   }

   protected static void registerThrowableProjectile(Schema p_18094_, Map<String, Supplier<TypeTemplate>> p_18095_, String p_18096_) {
      p_18094_.register(p_18095_, p_18096_, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18094_)));
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_18148_) {
      Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
      p_18148_.register(map, "minecraft:area_effect_cloud", p_326713_ -> DSL.optionalFields("Particle", References.PARTICLE.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:armor_stand");
      p_18148_.register(map, "minecraft:arrow", p_18164_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:bat");
      registerMob(p_18148_, map, "minecraft:blaze");
      p_18148_.registerSimple(map, "minecraft:boat");
      registerMob(p_18148_, map, "minecraft:cave_spider");
      p_18148_.register(
         map,
         "minecraft:chest_minecart",
         p_18161_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_), "Items", DSL.list(References.ITEM_STACK.in(p_18148_)))
      );
      registerMob(p_18148_, map, "minecraft:chicken");
      p_18148_.register(
         map,
         "minecraft:commandblock_minecart",
         p_390423_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_), "LastOutput", References.TEXT_COMPONENT.in(p_18148_))
      );
      registerMob(p_18148_, map, "minecraft:cow");
      registerMob(p_18148_, map, "minecraft:creeper");
      p_18148_.register(
         map,
         "minecraft:donkey",
         p_390427_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_18148_)), "SaddleItem", References.ITEM_STACK.in(p_18148_))
      );
      p_18148_.registerSimple(map, "minecraft:dragon_fireball");
      registerThrowableProjectile(p_18148_, map, "minecraft:egg");
      registerMob(p_18148_, map, "minecraft:elder_guardian");
      p_18148_.registerSimple(map, "minecraft:ender_crystal");
      registerMob(p_18148_, map, "minecraft:ender_dragon");
      p_18148_.register(map, "minecraft:enderman", p_18158_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:endermite");
      registerThrowableProjectile(p_18148_, map, "minecraft:ender_pearl");
      p_18148_.registerSimple(map, "minecraft:eye_of_ender_signal");
      p_18148_.register(
         map,
         "minecraft:falling_block",
         p_18143_ -> DSL.optionalFields("Block", References.BLOCK_NAME.in(p_18148_), "TileEntityData", References.BLOCK_ENTITY.in(p_18148_))
      );
      registerThrowableProjectile(p_18148_, map, "minecraft:fireball");
      p_18148_.register(map, "minecraft:fireworks_rocket", p_390431_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(p_18148_)));
      p_18148_.register(map, "minecraft:furnace_minecart", p_18137_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:ghast");
      registerMob(p_18148_, map, "minecraft:giant");
      registerMob(p_18148_, map, "minecraft:guardian");
      p_18148_.register(
         map,
         "minecraft:hopper_minecart",
         p_18134_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_), "Items", DSL.list(References.ITEM_STACK.in(p_18148_)))
      );
      p_18148_.register(
         map,
         "minecraft:horse",
         p_390421_ -> DSL.optionalFields("ArmorItem", References.ITEM_STACK.in(p_18148_), "SaddleItem", References.ITEM_STACK.in(p_18148_))
      );
      registerMob(p_18148_, map, "minecraft:husk");
      p_18148_.register(map, "minecraft:item", p_18128_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_18148_)));
      p_18148_.register(map, "minecraft:item_frame", p_18125_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(p_18148_)));
      p_18148_.registerSimple(map, "minecraft:leash_knot");
      registerMob(p_18148_, map, "minecraft:magma_cube");
      p_18148_.register(map, "minecraft:minecart", p_18122_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:mooshroom");
      p_18148_.register(
         map,
         "minecraft:mule",
         p_390425_ -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(p_18148_)), "SaddleItem", References.ITEM_STACK.in(p_18148_))
      );
      registerMob(p_18148_, map, "minecraft:ocelot");
      p_18148_.registerSimple(map, "minecraft:painting");
      registerMob(p_18148_, map, "minecraft:parrot");
      registerMob(p_18148_, map, "minecraft:pig");
      registerMob(p_18148_, map, "minecraft:polar_bear");
      p_18148_.register(
         map, "minecraft:potion", p_18116_ -> DSL.optionalFields("Potion", References.ITEM_STACK.in(p_18148_), "inTile", References.BLOCK_NAME.in(p_18148_))
      );
      registerMob(p_18148_, map, "minecraft:rabbit");
      registerMob(p_18148_, map, "minecraft:sheep");
      registerMob(p_18148_, map, "minecraft:shulker");
      p_18148_.registerSimple(map, "minecraft:shulker_bullet");
      registerMob(p_18148_, map, "minecraft:silverfish");
      registerMob(p_18148_, map, "minecraft:skeleton");
      p_18148_.register(map, "minecraft:skeleton_horse", p_18140_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:slime");
      registerThrowableProjectile(p_18148_, map, "minecraft:small_fireball");
      registerThrowableProjectile(p_18148_, map, "minecraft:snowball");
      registerMob(p_18148_, map, "minecraft:snowman");
      p_18148_.register(
         map,
         "minecraft:spawner_minecart",
         p_18110_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_), References.UNTAGGED_SPAWNER.in(p_18148_))
      );
      p_18148_.register(map, "minecraft:spectral_arrow", p_18107_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:spider");
      registerMob(p_18148_, map, "minecraft:squid");
      registerMob(p_18148_, map, "minecraft:stray");
      p_18148_.registerSimple(map, "minecraft:tnt");
      p_18148_.register(map, "minecraft:tnt_minecart", p_18104_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(p_18148_)));
      p_18148_.register(
         map,
         "minecraft:villager",
         p_390429_ -> DSL.optionalFields(
            "Inventory",
            DSL.list(References.ITEM_STACK.in(p_18148_)),
            "Offers",
            DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_18148_)))
         )
      );
      registerMob(p_18148_, map, "minecraft:villager_golem");
      registerMob(p_18148_, map, "minecraft:witch");
      registerMob(p_18148_, map, "minecraft:wither");
      registerMob(p_18148_, map, "minecraft:wither_skeleton");
      registerThrowableProjectile(p_18148_, map, "minecraft:wither_skull");
      registerMob(p_18148_, map, "minecraft:wolf");
      registerThrowableProjectile(p_18148_, map, "minecraft:xp_bottle");
      p_18148_.registerSimple(map, "minecraft:xp_orb");
      registerMob(p_18148_, map, "minecraft:zombie");
      p_18148_.register(map, "minecraft:zombie_horse", p_390435_ -> DSL.optionalFields("SaddleItem", References.ITEM_STACK.in(p_18148_)));
      registerMob(p_18148_, map, "minecraft:zombie_pigman");
      p_18148_.register(
         map,
         "minecraft:zombie_villager",
         p_390418_ -> DSL.optionalFields("Offers", DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(p_18148_))))
      );
      p_18148_.registerSimple(map, "minecraft:evocation_fangs");
      registerMob(p_18148_, map, "minecraft:evocation_illager");
      registerMob(p_18148_, map, "minecraft:illusion_illager");
      p_18148_.register(
         map,
         "minecraft:llama",
         p_390433_ -> DSL.optionalFields(
            "Items",
            DSL.list(References.ITEM_STACK.in(p_18148_)),
            "SaddleItem",
            References.ITEM_STACK.in(p_18148_),
            "DecorItem",
            References.ITEM_STACK.in(p_18148_)
         )
      );
      p_18148_.registerSimple(map, "minecraft:llama_spit");
      registerMob(p_18148_, map, "minecraft:vex");
      registerMob(p_18148_, map, "minecraft:vindication_illager");
      return map;
   }

   public void registerTypes(Schema p_18150_, Map<String, Supplier<TypeTemplate>> p_18151_, Map<String, Supplier<TypeTemplate>> p_18152_) {
      super.registerTypes(p_18150_, p_18151_, p_18152_);
      p_18150_.registerType(
         true,
         References.ENTITY,
         () -> DSL.and(
            References.ENTITY_EQUIPMENT.in(p_18150_),
            DSL.optionalFields("CustomName", DSL.constType(DSL.string()), DSL.taggedChoiceLazy("id", namespacedString(), p_18151_))
         )
      );
      p_18150_.registerType(
         true,
         References.ITEM_STACK,
         () -> DSL.hook(DSL.optionalFields("id", References.ITEM_NAME.in(p_18150_), "tag", V99.itemStackTag(p_18150_)), ADD_NAMES, HookFunction.IDENTITY)
      );
   }
}
