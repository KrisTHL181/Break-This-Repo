package net.minecraft.util.datafix.fixes;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class TrialSpawnerConfigInRegistryFix extends NamedEntityFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public TrialSpawnerConfigInRegistryFix(final Schema outputSchema) {
      super(outputSchema, false, "TrialSpawnerConfigInRegistryFix", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   public Dynamic<?> fixTag(final Dynamic<Tag> input) {
      Optional<Dynamic<Tag>> normalConfig = input.get("normal_config").result();
      if (normalConfig.isEmpty()) {
         return input;
      }

      Optional<Dynamic<Tag>> ominousConfig = input.get("ominous_config").result();
      if (ominousConfig.isEmpty()) {
         return input;
      }

      Identifier registryLocation = TrialSpawnerConfigInRegistryFix.VanillaTrialChambers.CONFIGS_TO_KEY.get(Pair.of(normalConfig.get(), ominousConfig.get()));
      return registryLocation == null
         ? input
         : input.set("normal_config", input.createString(registryLocation.withSuffix("/normal").toString()))
            .set("ominous_config", input.createString(registryLocation.withSuffix("/ominous").toString()));
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), input -> {
         DynamicOps<?> inputType = input.getOps();
         Dynamic<?> result = this.fixTag(input.convert(NbtOps.INSTANCE));
         return result.convert(inputType);
      });
   }

   private static final class VanillaTrialChambers {
      public static final Map<Pair<Dynamic<Tag>, Dynamic<Tag>>, Identifier> CONFIGS_TO_KEY = new HashMap<>();

      private static void register(final Identifier location, final String normalNbt, final String ominousNbt) {
         try {
            CompoundTag normalTag = parse(normalNbt);
            CompoundTag ominousTag = parse(ominousNbt);
            CompoundTag ominousMergedTag = normalTag.copy().merge(ominousTag);
            CompoundTag ominousMergedTagDefaultsOmitted = removeDefaults(ominousMergedTag.copy());
            Dynamic<Tag> dynamicNormal = asDynamic(normalTag);
            CONFIGS_TO_KEY.put(Pair.of(dynamicNormal, asDynamic(ominousTag)), location);
            CONFIGS_TO_KEY.put(Pair.of(dynamicNormal, asDynamic(ominousMergedTag)), location);
            CONFIGS_TO_KEY.put(Pair.of(dynamicNormal, asDynamic(ominousMergedTagDefaultsOmitted)), location);
         } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to parse NBT for " + location, e);
         }
      }

      private static Dynamic<Tag> asDynamic(final CompoundTag normalTag) {
         return new Dynamic(NbtOps.INSTANCE, normalTag);
      }

      private static CompoundTag parse(final String nbt) {
         try {
            return TagParser.parseCompoundFully(nbt);
         } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException("Failed to parse Trial Spawner NBT config: " + nbt, e);
         }
      }

      private static CompoundTag removeDefaults(final CompoundTag tag) {
         if (tag.getIntOr("spawn_range", 0) == 4) {
            tag.remove("spawn_range");
         }

         if (tag.getFloatOr("total_mobs", 0.0F) == 6.0F) {
            tag.remove("total_mobs");
         }

         if (tag.getFloatOr("simultaneous_mobs", 0.0F) == 2.0F) {
            tag.remove("simultaneous_mobs");
         }

         if (tag.getFloatOr("total_mobs_added_per_player", 0.0F) == 2.0F) {
            tag.remove("total_mobs_added_per_player");
         }

         if (tag.getFloatOr("simultaneous_mobs_added_per_player", 0.0F) == 1.0F) {
            tag.remove("simultaneous_mobs_added_per_player");
         }

         if (tag.getIntOr("ticks_between_spawn", 0) == 40) {
            tag.remove("ticks_between_spawn");
         }

         return tag;
      }

      static {
         register(
            Identifier.withDefaultNamespace("trial_chamber/breeze"),
            "{simultaneous_mobs: 1.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:breeze\"}}, weight: 1}], ticks_between_spawn: 20, total_mobs: 2.0f, total_mobs_added_per_player: 1.0f}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 2.0f, total_mobs: 4.0f}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/melee/husk"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:husk\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/melee/spider"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:spider\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/melee/zombie"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/ranged/poison_skeleton"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/ranged/skeleton"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/ranged/stray"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/slow_ranged/poison_skeleton"),
            "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}}, weight: 1}], ticks_between_spawn: 160}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:bogged\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/slow_ranged/skeleton"),
            "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}}, weight: 1}], ticks_between_spawn: 160}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {id: \"minecraft:skeleton\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/slow_ranged/stray"),
            "{simultaneous_mobs: 4.0f, simultaneous_mobs_added_per_player: 2.0f, spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}}, weight: 1}], ticks_between_spawn: 160}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}],spawn_potentials: [{data: {entity: {id: \"minecraft:stray\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_ranged\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/small_melee/baby_zombie"),
            "{simultaneous_mobs: 2.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], spawn_potentials: [{data: {entity: {IsBaby: 1b, id: \"minecraft:zombie\"}, equipment: {loot_table: \"minecraft:equipment/trial_chamber_melee\", slot_drop_chances: 0.0f}}, weight: 1}]}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/small_melee/cave_spider"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:cave_spider\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/small_melee/silverfish"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {id: \"minecraft:silverfish\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
         );
         register(
            Identifier.withDefaultNamespace("trial_chamber/small_melee/slime"),
            "{simultaneous_mobs: 3.0f, simultaneous_mobs_added_per_player: 0.5f, spawn_potentials: [{data: {entity: {Size: 1, id: \"minecraft:slime\"}}, weight: 3}, {data: {entity: {Size: 2, id: \"minecraft:slime\"}}, weight: 1}], ticks_between_spawn: 20}",
            "{loot_tables_to_eject: [{data: \"minecraft:spawners/ominous/trial_chamber/key\", weight: 3}, {data: \"minecraft:spawners/ominous/trial_chamber/consumables\", weight: 7}], simultaneous_mobs: 4.0f, total_mobs: 12.0f}"
         );
      }
   }
}
