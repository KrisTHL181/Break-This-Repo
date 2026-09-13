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

   public TrialSpawnerConfigInRegistryFix(Schema p_369765_) {
      super(p_369765_, false, "TrialSpawnerConfigInRegistryFix", References.BLOCK_ENTITY, "minecraft:trial_spawner");
   }

   public Dynamic<?> fixTag(Dynamic<Tag> p_362102_) {
      Optional<Dynamic<Tag>> optional = p_362102_.get("normal_config").result();
      if (optional.isEmpty()) {
         return p_362102_;
      }

      Optional<Dynamic<Tag>> optional1 = p_362102_.get("ominous_config").result();
      if (optional1.isEmpty()) {
         return p_362102_;
      }

      Identifier identifier = TrialSpawnerConfigInRegistryFix.VanillaTrialChambers.CONFIGS_TO_KEY.get(Pair.of(optional.get(), optional1.get()));
      return identifier == null
         ? p_362102_
         : p_362102_.set("normal_config", p_362102_.createString(identifier.withSuffix("/normal").toString()))
            .set("ominous_config", p_362102_.createString(identifier.withSuffix("/ominous").toString()));
   }

   @Override
   protected Typed<?> fix(Typed<?> p_362424_) {
      return p_362424_.update(DSL.remainderFinder(), p_361190_ -> {
         DynamicOps<?> dynamicops = p_361190_.getOps();
         Dynamic<?> dynamic = this.fixTag(p_361190_.convert(NbtOps.INSTANCE));
         return dynamic.convert(dynamicops);
      });
   }

   static final class VanillaTrialChambers {
      public static final Map<Pair<Dynamic<Tag>, Dynamic<Tag>>, Identifier> CONFIGS_TO_KEY = new HashMap<>();

      private VanillaTrialChambers() {
      }

      private static void register(Identifier p_453908_, String p_367097_, String p_370028_) {
         try {
            CompoundTag compoundtag = parse(p_367097_);
            CompoundTag compoundtag1 = parse(p_370028_);
            CompoundTag compoundtag2 = compoundtag.copy().merge(compoundtag1);
            CompoundTag compoundtag3 = removeDefaults(compoundtag2.copy());
            Dynamic<Tag> dynamic = asDynamic(compoundtag);
            CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag1)), p_453908_);
            CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag2)), p_453908_);
            CONFIGS_TO_KEY.put(Pair.of(dynamic, asDynamic(compoundtag3)), p_453908_);
         } catch (RuntimeException runtimeexception) {
            throw new IllegalStateException("Failed to parse NBT for " + p_453908_, runtimeexception);
         }
      }

      private static Dynamic<Tag> asDynamic(CompoundTag p_364176_) {
         return new Dynamic(NbtOps.INSTANCE, p_364176_);
      }

      private static CompoundTag parse(String p_367124_) {
         try {
            return TagParser.parseCompoundFully(p_367124_);
         } catch (CommandSyntaxException commandsyntaxexception) {
            throw new IllegalArgumentException("Failed to parse Trial Spawner NBT config: " + p_367124_, commandsyntaxexception);
         }
      }

      private static CompoundTag removeDefaults(CompoundTag p_368568_) {
         if (p_368568_.getIntOr("spawn_range", 0) == 4) {
            p_368568_.remove("spawn_range");
         }

         if (p_368568_.getFloatOr("total_mobs", 0.0F) == 6.0F) {
            p_368568_.remove("total_mobs");
         }

         if (p_368568_.getFloatOr("simultaneous_mobs", 0.0F) == 2.0F) {
            p_368568_.remove("simultaneous_mobs");
         }

         if (p_368568_.getFloatOr("total_mobs_added_per_player", 0.0F) == 2.0F) {
            p_368568_.remove("total_mobs_added_per_player");
         }

         if (p_368568_.getFloatOr("simultaneous_mobs_added_per_player", 0.0F) == 1.0F) {
            p_368568_.remove("simultaneous_mobs_added_per_player");
         }

         if (p_368568_.getIntOr("ticks_between_spawn", 0) == 40) {
            p_368568_.remove("ticks_between_spawn");
         }

         return p_368568_;
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
