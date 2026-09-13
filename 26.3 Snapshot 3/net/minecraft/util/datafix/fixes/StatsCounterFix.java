package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.schemas.V1451_6;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

public class StatsCounterFix extends DataFix {
   private static final Set<String> SPECIAL_OBJECTIVE_CRITERIA = Set.of(
      "dummy",
      "trigger",
      "deathCount",
      "playerKillCount",
      "totalKillCount",
      "health",
      "food",
      "air",
      "armor",
      "xp",
      "level",
      "killedByTeam.aqua",
      "killedByTeam.black",
      "killedByTeam.blue",
      "killedByTeam.dark_aqua",
      "killedByTeam.dark_blue",
      "killedByTeam.dark_gray",
      "killedByTeam.dark_green",
      "killedByTeam.dark_purple",
      "killedByTeam.dark_red",
      "killedByTeam.gold",
      "killedByTeam.gray",
      "killedByTeam.green",
      "killedByTeam.light_purple",
      "killedByTeam.red",
      "killedByTeam.white",
      "killedByTeam.yellow",
      "teamkill.aqua",
      "teamkill.black",
      "teamkill.blue",
      "teamkill.dark_aqua",
      "teamkill.dark_blue",
      "teamkill.dark_gray",
      "teamkill.dark_green",
      "teamkill.dark_purple",
      "teamkill.dark_red",
      "teamkill.gold",
      "teamkill.gray",
      "teamkill.green",
      "teamkill.light_purple",
      "teamkill.red",
      "teamkill.white",
      "teamkill.yellow"
   );
   private static final Set<String> SKIP = ImmutableSet.builder()
      .add("stat.craftItem.minecraft.spawn_egg")
      .add("stat.useItem.minecraft.spawn_egg")
      .add("stat.breakItem.minecraft.spawn_egg")
      .add("stat.pickup.minecraft.spawn_egg")
      .add("stat.drop.minecraft.spawn_egg")
      .build();
   private static final Map<String, String> CUSTOM_MAP = ImmutableMap.builder()
      .put("stat.leaveGame", "minecraft:leave_game")
      .put("stat.playOneMinute", "minecraft:play_one_minute")
      .put("stat.timeSinceDeath", "minecraft:time_since_death")
      .put("stat.sneakTime", "minecraft:sneak_time")
      .put("stat.walkOneCm", "minecraft:walk_one_cm")
      .put("stat.crouchOneCm", "minecraft:crouch_one_cm")
      .put("stat.sprintOneCm", "minecraft:sprint_one_cm")
      .put("stat.swimOneCm", "minecraft:swim_one_cm")
      .put("stat.fallOneCm", "minecraft:fall_one_cm")
      .put("stat.climbOneCm", "minecraft:climb_one_cm")
      .put("stat.flyOneCm", "minecraft:fly_one_cm")
      .put("stat.diveOneCm", "minecraft:dive_one_cm")
      .put("stat.minecartOneCm", "minecraft:minecart_one_cm")
      .put("stat.boatOneCm", "minecraft:boat_one_cm")
      .put("stat.pigOneCm", "minecraft:pig_one_cm")
      .put("stat.horseOneCm", "minecraft:horse_one_cm")
      .put("stat.aviateOneCm", "minecraft:aviate_one_cm")
      .put("stat.jump", "minecraft:jump")
      .put("stat.drop", "minecraft:drop")
      .put("stat.damageDealt", "minecraft:damage_dealt")
      .put("stat.damageTaken", "minecraft:damage_taken")
      .put("stat.deaths", "minecraft:deaths")
      .put("stat.mobKills", "minecraft:mob_kills")
      .put("stat.animalsBred", "minecraft:animals_bred")
      .put("stat.playerKills", "minecraft:player_kills")
      .put("stat.fishCaught", "minecraft:fish_caught")
      .put("stat.talkedToVillager", "minecraft:talked_to_villager")
      .put("stat.tradedWithVillager", "minecraft:traded_with_villager")
      .put("stat.cakeSlicesEaten", "minecraft:eat_cake_slice")
      .put("stat.cauldronFilled", "minecraft:fill_cauldron")
      .put("stat.cauldronUsed", "minecraft:use_cauldron")
      .put("stat.armorCleaned", "minecraft:clean_armor")
      .put("stat.bannerCleaned", "minecraft:clean_banner")
      .put("stat.brewingstandInteraction", "minecraft:interact_with_brewingstand")
      .put("stat.beaconInteraction", "minecraft:interact_with_beacon")
      .put("stat.dropperInspected", "minecraft:inspect_dropper")
      .put("stat.hopperInspected", "minecraft:inspect_hopper")
      .put("stat.dispenserInspected", "minecraft:inspect_dispenser")
      .put("stat.noteblockPlayed", "minecraft:play_noteblock")
      .put("stat.noteblockTuned", "minecraft:tune_noteblock")
      .put("stat.flowerPotted", "minecraft:pot_flower")
      .put("stat.trappedChestTriggered", "minecraft:trigger_trapped_chest")
      .put("stat.enderchestOpened", "minecraft:open_enderchest")
      .put("stat.itemEnchanted", "minecraft:enchant_item")
      .put("stat.recordPlayed", "minecraft:play_record")
      .put("stat.furnaceInteraction", "minecraft:interact_with_furnace")
      .put("stat.craftingTableInteraction", "minecraft:interact_with_crafting_table")
      .put("stat.chestOpened", "minecraft:open_chest")
      .put("stat.sleepInBed", "minecraft:sleep_in_bed")
      .put("stat.shulkerBoxOpened", "minecraft:open_shulker_box")
      .build();
   private static final String BLOCK_KEY = "stat.mineBlock";
   private static final String NEW_BLOCK_KEY = "minecraft:mined";
   private static final Map<String, String> ITEM_KEYS = ImmutableMap.builder()
      .put("stat.craftItem", "minecraft:crafted")
      .put("stat.useItem", "minecraft:used")
      .put("stat.breakItem", "minecraft:broken")
      .put("stat.pickup", "minecraft:picked_up")
      .put("stat.drop", "minecraft:dropped")
      .build();
   private static final Map<String, String> ENTITY_KEYS = ImmutableMap.builder()
      .put("stat.entityKilledBy", "minecraft:killed_by")
      .put("stat.killEntity", "minecraft:killed")
      .build();
   private static final Map<String, String> ENTITIES = ImmutableMap.builder()
      .put("Bat", "minecraft:bat")
      .put("Blaze", "minecraft:blaze")
      .put("CaveSpider", "minecraft:cave_spider")
      .put("Chicken", "minecraft:chicken")
      .put("Cow", "minecraft:cow")
      .put("Creeper", "minecraft:creeper")
      .put("Donkey", "minecraft:donkey")
      .put("ElderGuardian", "minecraft:elder_guardian")
      .put("Enderman", "minecraft:enderman")
      .put("Endermite", "minecraft:endermite")
      .put("EvocationIllager", "minecraft:evocation_illager")
      .put("Ghast", "minecraft:ghast")
      .put("Guardian", "minecraft:guardian")
      .put("Horse", "minecraft:horse")
      .put("Husk", "minecraft:husk")
      .put("Llama", "minecraft:llama")
      .put("LavaSlime", "minecraft:magma_cube")
      .put("MushroomCow", "minecraft:mooshroom")
      .put("Mule", "minecraft:mule")
      .put("Ozelot", "minecraft:ocelot")
      .put("Parrot", "minecraft:parrot")
      .put("Pig", "minecraft:pig")
      .put("PolarBear", "minecraft:polar_bear")
      .put("Rabbit", "minecraft:rabbit")
      .put("Sheep", "minecraft:sheep")
      .put("Shulker", "minecraft:shulker")
      .put("Silverfish", "minecraft:silverfish")
      .put("SkeletonHorse", "minecraft:skeleton_horse")
      .put("Skeleton", "minecraft:skeleton")
      .put("Slime", "minecraft:slime")
      .put("Spider", "minecraft:spider")
      .put("Squid", "minecraft:squid")
      .put("Stray", "minecraft:stray")
      .put("Vex", "minecraft:vex")
      .put("Villager", "minecraft:villager")
      .put("VindicationIllager", "minecraft:vindication_illager")
      .put("Witch", "minecraft:witch")
      .put("WitherSkeleton", "minecraft:wither_skeleton")
      .put("Wolf", "minecraft:wolf")
      .put("ZombieHorse", "minecraft:zombie_horse")
      .put("PigZombie", "minecraft:zombie_pigman")
      .put("ZombieVillager", "minecraft:zombie_villager")
      .put("Zombie", "minecraft:zombie")
      .build();
   private static final String NEW_CUSTOM_KEY = "minecraft:custom";

   public StatsCounterFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   private static StatsCounterFix.@Nullable StatType unpackLegacyKey(final String key) {
      if (SKIP.contains(key)) {
         return null;
      } else {
         String customKey = CUSTOM_MAP.get(key);
         if (customKey != null) {
            return new StatsCounterFix.StatType("minecraft:custom", customKey);
         } else {
            int splitIndex = StringUtils.ordinalIndexOf(key, ".", 2);
            if (splitIndex < 0) {
               return null;
            } else {
               String prefix = key.substring(0, splitIndex);
               if ("stat.mineBlock".equals(prefix)) {
                  String newKey = upgradeBlock(key.substring(splitIndex + 1).replace('.', ':'));
                  return new StatsCounterFix.StatType("minecraft:mined", newKey);
               } else {
                  String itemKey = ITEM_KEYS.get(prefix);
                  if (itemKey != null) {
                     String oldItem = key.substring(splitIndex + 1).replace('.', ':');
                     String newItem = upgradeItem(oldItem);
                     String newKey = newItem == null ? oldItem : newItem;
                     return new StatsCounterFix.StatType(itemKey, newKey);
                  } else {
                     String entityKey = ENTITY_KEYS.get(prefix);
                     if (entityKey != null) {
                        String oldEntity = key.substring(splitIndex + 1).replace('.', ':');
                        String newKey = ENTITIES.getOrDefault(oldEntity, oldEntity);
                        return new StatsCounterFix.StatType(entityKey, newKey);
                     } else {
                        return null;
                     }
                  }
               }
            }
         }
      }
   }

   public TypeRewriteRule makeRule() {
      return TypeRewriteRule.seq(this.makeStatFixer(), this.makeObjectiveFixer());
   }

   private TypeRewriteRule makeStatFixer() {
      Type<?> inputType = this.getInputSchema().getType(References.STATS);
      Type<?> outputType = this.getOutputSchema().getType(References.STATS);
      return this.fixTypeEverywhereTyped("StatsCounterFix", inputType, outputType, input -> {
         Dynamic<?> tag = (Dynamic<?>)input.get(DSL.remainderFinder());
         Map<Dynamic<?>, Dynamic<?>> stats = Maps.newHashMap();
         Optional<? extends Map<? extends Dynamic<?>, ? extends Dynamic<?>>> map = tag.getMapValues().result();
         if (map.isPresent()) {
            for (Entry<? extends Dynamic<?>, ? extends Dynamic<?>> entry : map.get().entrySet()) {
               if (entry.getValue().asNumber().result().isPresent()) {
                  String key = entry.getKey().asString("");
                  StatsCounterFix.StatType statType = unpackLegacyKey(key);
                  if (statType != null) {
                     Dynamic<?> newTypeKey = tag.createString(statType.type());
                     Dynamic<?> element = stats.computeIfAbsent(newTypeKey, k -> tag.emptyMap());
                     stats.put(newTypeKey, element.set(statType.typeKey(), entry.getValue()));
                  }
               }
            }
         }

         return Util.readTypedOrThrow(outputType, tag.emptyMap().set("stats", tag.createMap(stats)));
      });
   }

   private TypeRewriteRule makeObjectiveFixer() {
      Type<?> inputType = this.getInputSchema().getType(References.OBJECTIVE);
      Type<?> outputType = this.getOutputSchema().getType(References.OBJECTIVE);
      return this.fixTypeEverywhereTyped("ObjectiveStatFix", inputType, outputType, input -> {
         Dynamic<?> tag = (Dynamic<?>)input.get(DSL.remainderFinder());
         Dynamic<?> updatedTag = tag.update("CriteriaName", name -> (Dynamic)DataFixUtils.orElse(name.asString().result().map(key -> {
            if (SPECIAL_OBJECTIVE_CRITERIA.contains(key)) {
               return (String)key;
            }

            StatsCounterFix.StatType statType = unpackLegacyKey(key);
            return statType == null ? "dummy" : V1451_6.packNamespacedWithDot(statType.type) + ":" + V1451_6.packNamespacedWithDot(statType.typeKey);
         }).map(name::createString), name));
         return Util.readTypedOrThrow(outputType, updatedTag);
      });
   }

   private static @Nullable String upgradeItem(final String name) {
      return ItemStackTheFlatteningFix.updateItem(name, 0);
   }

   private static String upgradeBlock(final String name) {
      return BlockStateData.upgradeBlock(name);
   }

   private record StatType(String type, String typeKey) {
   }
}
