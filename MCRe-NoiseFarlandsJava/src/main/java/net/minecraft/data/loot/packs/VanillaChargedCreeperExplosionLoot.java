package net.minecraft.data.loot.packs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public record VanillaChargedCreeperExplosionLoot(HolderLookup.Provider registries) implements LootTableSubProvider {
    private static final List<VanillaChargedCreeperExplosionLoot.Entry> ENTRIES = List.of(
        new VanillaChargedCreeperExplosionLoot.Entry(BuiltInLootTables.CHARGED_CREEPER_PIGLIN, EntityTypes.PIGLIN, Items.PIGLIN_HEAD),
        new VanillaChargedCreeperExplosionLoot.Entry(BuiltInLootTables.CHARGED_CREEPER_CREEPER, EntityTypes.CREEPER, Items.CREEPER_HEAD),
        new VanillaChargedCreeperExplosionLoot.Entry(BuiltInLootTables.CHARGED_CREEPER_SKELETON, EntityTypes.SKELETON, Items.SKELETON_SKULL),
        new VanillaChargedCreeperExplosionLoot.Entry(
            BuiltInLootTables.CHARGED_CREEPER_WITHER_SKELETON, EntityTypes.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL
        ),
        new VanillaChargedCreeperExplosionLoot.Entry(BuiltInLootTables.CHARGED_CREEPER_ZOMBIE, EntityTypes.ZOMBIE, Items.ZOMBIE_HEAD)
    );

    @Override
    public void generate(final BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        HolderGetter<EntityType<?>> entityTypes = this.registries.lookupOrThrow(Registries.ENTITY_TYPE);
        List<LootPoolEntryContainer.Builder<?>> alternatives = new ArrayList<>(ENTRIES.size());

        for (VanillaChargedCreeperExplosionLoot.Entry entry : ENTRIES) {
            output.accept(
                entry.lootTable,
                LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(entry.item)))
            );
            LootItemCondition.Builder predicate = LootItemEntityPropertyCondition.hasProperties(
                LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(entityTypes, entry.entityType))
            );
            alternatives.add(NestedLootTable.lootTableReference(entry.lootTable).when(predicate));
        }

        output.accept(
            BuiltInLootTables.CHARGED_CREEPER,
            LootTable.lootTable()
                .withPool(
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(AlternativesEntry.alternatives(alternatives.toArray(LootPoolEntryContainer.Builder[]::new)))
                )
        );
    }

    private record Entry(ResourceKey<LootTable> lootTable, EntityType<?> entityType, Item item) {
    }
}