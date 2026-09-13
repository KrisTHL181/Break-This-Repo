package net.minecraft.world.level.block.entity.trialspawner;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class TrialSpawnerConfigs {
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_BREEZE = TrialSpawnerConfigs.Keys.of("trial_chamber/breeze");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_HUSK = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/husk");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_SPIDER = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/spider");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_MELEE_ZOMBIE = TrialSpawnerConfigs.Keys.of("trial_chamber/melee/zombie");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_POISON_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/poison_skeleton");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/skeleton");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_RANGED_STRAY = TrialSpawnerConfigs.Keys.of("trial_chamber/ranged/stray");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON = TrialSpawnerConfigs.Keys.of(
      "trial_chamber/slow_ranged/poison_skeleton"
   );
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_SKELETON = TrialSpawnerConfigs.Keys.of("trial_chamber/slow_ranged/skeleton");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SLOW_RANGED_STRAY = TrialSpawnerConfigs.Keys.of("trial_chamber/slow_ranged/stray");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/baby_zombie");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/cave_spider");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/silverfish");
   private static final TrialSpawnerConfigs.Keys TRIAL_CHAMBER_SMALL_MELEE_SLIME = TrialSpawnerConfigs.Keys.of("trial_chamber/small_melee/slime");

   public static void bootstrap(final BootstrapContext<TrialSpawnerConfig> context) {
      register(
         context,
         TRIAL_CHAMBER_BREEZE,
         TrialSpawnerConfig.builder()
            .simultaneousMobs(1.0F)
            .simultaneousMobsAddedPerPlayer(0.5F)
            .ticksBetweenSpawn(20)
            .totalMobs(2.0F)
            .totalMobsAddedPerPlayer(1.0F)
            .spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.BREEZE)))
            .build(),
         TrialSpawnerConfig.builder()
            .simultaneousMobsAddedPerPlayer(0.5F)
            .ticksBetweenSpawn(20)
            .totalMobs(4.0F)
            .totalMobsAddedPerPlayer(1.0F)
            .spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.BREEZE)))
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_MELEE_HUSK,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.HUSK))).build(),
         trialChamberBase()
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.HUSK, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)))
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_MELEE_SPIDER,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SPIDER))).build(),
         trialChamberMeleeOminous()
            .spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SPIDER)))
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_MELEE_ZOMBIE,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.ZOMBIE))).build(),
         trialChamberBase()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.ZOMBIE, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_RANGED_POISON_SKELETON,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.BOGGED))).build(),
         trialChamberBase()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_RANGED_SKELETON,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SKELETON))).build(),
         trialChamberBase()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_RANGED_STRAY,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.STRAY))).build(),
         trialChamberBase()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SLOW_RANGED_POISON_SKELETON,
         trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.BOGGED))).build(),
         trialChamberSlowRanged()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.BOGGED, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SLOW_RANGED_SKELETON,
         trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SKELETON))).build(),
         trialChamberSlowRanged()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.SKELETON, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SLOW_RANGED_STRAY,
         trialChamberSlowRanged().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.STRAY))).build(),
         trialChamberSlowRanged()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnDataWithEquipment(EntityTypes.STRAY, BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_RANGED)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SMALL_MELEE_BABY_ZOMBIE,
         TrialSpawnerConfig.builder()
            .simultaneousMobsAddedPerPlayer(0.5F)
            .ticksBetweenSpawn(20)
            .spawnPotentialsDefinition(WeightedList.of(customSpawnDataWithEquipment(EntityTypes.ZOMBIE, tag -> tag.putBoolean("IsBaby", true), null)))
            .build(),
         TrialSpawnerConfig.builder()
            .simultaneousMobsAddedPerPlayer(0.5F)
            .ticksBetweenSpawn(20)
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(
               WeightedList.of(
                  customSpawnDataWithEquipment(EntityTypes.ZOMBIE, tag -> tag.putBoolean("IsBaby", true), BuiltInLootTables.EQUIPMENT_TRIAL_CHAMBER_MELEE)
               )
            )
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SMALL_MELEE_CAVE_SPIDER,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.CAVE_SPIDER))).build(),
         trialChamberMeleeOminous()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.CAVE_SPIDER)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SMALL_MELEE_SILVERFISH,
         trialChamberBase().spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SILVERFISH))).build(),
         trialChamberMeleeOminous()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(WeightedList.of(spawnData(EntityTypes.SILVERFISH)))
            .build()
      );
      register(
         context,
         TRIAL_CHAMBER_SMALL_MELEE_SLIME,
         trialChamberBase()
            .spawnPotentialsDefinition(
               WeightedList.<SpawnData>builder()
                  .add(customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)1)), 3)
                  .add(customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)2)), 1)
                  .build()
            )
            .build(),
         trialChamberMeleeOminous()
            .lootTablesToEject(
               WeightedList.<ResourceKey<LootTable>>builder()
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY, 3)
                  .add(BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES, 7)
                  .build()
            )
            .spawnPotentialsDefinition(
               WeightedList.<SpawnData>builder()
                  .add(customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)1)), 3)
                  .add(customSpawnData(EntityTypes.SLIME, tag -> tag.putByte("Size", (byte)2)), 1)
                  .build()
            )
            .build()
      );
   }

   private static <T extends Entity> SpawnData spawnData(final EntityType<T> type) {
      return customSpawnDataWithEquipment(type, tag -> {}, null);
   }

   private static <T extends Entity> SpawnData customSpawnData(final EntityType<T> type, final Consumer<CompoundTag> tagModifier) {
      return customSpawnDataWithEquipment(type, tagModifier, null);
   }

   private static <T extends Entity> SpawnData spawnDataWithEquipment(final EntityType<T> type, final ResourceKey<LootTable> equipmentLootTable) {
      return customSpawnDataWithEquipment(type, tag -> {}, equipmentLootTable);
   }

   private static <T extends Entity> SpawnData customSpawnDataWithEquipment(
      final EntityType<T> type, final Consumer<CompoundTag> tagModifier, final @Nullable ResourceKey<LootTable> equipmentLootTable
   ) {
      CompoundTag tag = new CompoundTag();
      tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
      tagModifier.accept(tag);
      Optional<EquipmentTable> table = Optional.ofNullable(equipmentLootTable).map(lootTable -> new EquipmentTable((ResourceKey<LootTable>)lootTable, 0.0F));
      return new SpawnData(tag, Optional.empty(), table);
   }

   private static void register(
      final BootstrapContext<TrialSpawnerConfig> context,
      final TrialSpawnerConfigs.Keys keys,
      final TrialSpawnerConfig normalConfig,
      final TrialSpawnerConfig ominousConfig
   ) {
      context.register(keys.normal, normalConfig);
      context.register(keys.ominous, ominousConfig);
   }

   private static ResourceKey<TrialSpawnerConfig> registryKey(final String id) {
      return ResourceKey.create(Registries.TRIAL_SPAWNER_CONFIG, Identifier.withDefaultNamespace(id));
   }

   private static TrialSpawnerConfig.Builder trialChamberMeleeOminous() {
      return TrialSpawnerConfig.builder().simultaneousMobs(4.0F).simultaneousMobsAddedPerPlayer(0.5F).ticksBetweenSpawn(20).totalMobs(12.0F);
   }

   private static TrialSpawnerConfig.Builder trialChamberSlowRanged() {
      return TrialSpawnerConfig.builder().simultaneousMobs(4.0F).simultaneousMobsAddedPerPlayer(2.0F).ticksBetweenSpawn(160);
   }

   private static TrialSpawnerConfig.Builder trialChamberBase() {
      return TrialSpawnerConfig.builder().simultaneousMobs(3.0F).simultaneousMobsAddedPerPlayer(0.5F).ticksBetweenSpawn(20);
   }

   private record Keys(ResourceKey<TrialSpawnerConfig> normal, ResourceKey<TrialSpawnerConfig> ominous) {
      public static TrialSpawnerConfigs.Keys of(final String id) {
         return new TrialSpawnerConfigs.Keys(TrialSpawnerConfigs.registryKey(id + "/normal"), TrialSpawnerConfigs.registryKey(id + "/ominous"));
      }
   }
}
