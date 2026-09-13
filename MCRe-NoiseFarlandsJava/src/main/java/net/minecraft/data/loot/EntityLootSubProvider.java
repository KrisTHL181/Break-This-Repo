package net.minecraft.data.loot;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityEquipmentPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.SheepPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;

public abstract class EntityLootSubProvider implements LootTableSubProvider {
    protected final HolderLookup.Provider registries;
    private final FeatureFlagSet allowed;
    private final FeatureFlagSet required;
    private final Map<EntityType<?>, Map<ResourceKey<LootTable>, LootTable.Builder>> map = Maps.newHashMap();

    protected final AnyOfCondition.Builder shouldSmeltLoot() {
        HolderLookup.RegistryLookup<Enchantment> enchantmentsRegistry = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return AnyOfCondition.anyOf(
            LootItemEntityPropertyCondition.hasProperties(
                LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().flags(EntityFlagsPredicate.Builder.flags().setOnFire(true))
            ),
            LootItemEntityPropertyCondition.hasProperties(
                LootContext.EntityTarget.DIRECT_ATTACKER,
                EntityPredicate.Builder.entity()
                    .equipment(
                        EntityEquipmentPredicate.Builder.equipment()
                            .mainhand(
                                ItemPredicate.Builder.item()
                                    .withComponents(
                                        DataComponentMatchers.Builder.components()
                                            .partial(
                                                DataComponentPredicates.ENCHANTMENTS,
                                                EnchantmentsPredicate.enchantments(
                                                    List.of(
                                                        new EnchantmentPredicate(
                                                            enchantmentsRegistry.getOrThrow(EnchantmentTags.SMELTS_LOOT), MinMaxBounds.Ints.ANY
                                                        )
                                                    )
                                                )
                                            )
                                            .build()
                                    )
                            )
                    )
            )
        );
    }

    protected EntityLootSubProvider(final FeatureFlagSet enabledFeatures, final HolderLookup.Provider registries) {
        this(enabledFeatures, enabledFeatures, registries);
    }

    protected EntityLootSubProvider(final FeatureFlagSet allowed, final FeatureFlagSet required, final HolderLookup.Provider registries) {
        this.allowed = allowed;
        this.required = required;
        this.registries = registries;
    }

    public static LootPool.Builder createSheepDispatchPool(final ColorCollection<ResourceKey<LootTable>> tableNames) {
        AlternativesEntry.Builder variants = AlternativesEntry.alternatives();

        for (DyeColor color : DyeColor.VALUES) {
            variants = variants.otherwise(
                NestedLootTable.lootTableReference(tableNames.pick(color))
                    .when(
                        LootItemEntityPropertyCondition.hasProperties(
                            LootContext.EntityTarget.THIS,
                            EntityPredicate.Builder.entity()
                                .components(DataComponentExactPredicate.expect(DataComponents.SHEEP_COLOR, color))
                                .sheep(SheepPredicate.hasWool())
                        )
                    )
            );
        }

        return LootPool.lootPool().add(variants);
    }

    public abstract void generate();

    @Override
    public void generate(final BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        this.generate();
        Set<ResourceKey<LootTable>> seen = new HashSet<>();
        BuiltInRegistries.ENTITY_TYPE
            .listElements()
            .forEach(
                holder -> {
                    EntityType<?> type = holder.value();
                    if (type.isEnabled(this.allowed)) {
                        Optional<ResourceKey<LootTable>> defaultLootTable = type.getDefaultLootTable();
                        if (defaultLootTable.isPresent()) {
                            Map<ResourceKey<LootTable>, LootTable.Builder> builders = this.map.remove(type);
                            if (type.isEnabled(this.required) && (builders == null || !builders.containsKey(defaultLootTable.get()))) {
                                throw new IllegalStateException(
                                    String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", defaultLootTable.get(), holder.key().identifier())
                                );
                            }

                            if (builders != null) {
                                builders.forEach(
                                    (id, builder) -> {
                                        if (!seen.add((ResourceKey<LootTable>)id)) {
                                            throw new IllegalStateException(
                                                String.format(Locale.ROOT, "Duplicate loottable '%s' for '%s'", id, holder.key().identifier())
                                            );
                                        }

                                        output.accept((ResourceKey<LootTable>)id, builder);
                                    }
                                );
                            }
                        } else {
                            Map<ResourceKey<LootTable>, LootTable.Builder> builders = this.map.remove(type);
                            if (builders != null) {
                                throw new IllegalStateException(
                                    String.format(
                                        Locale.ROOT,
                                        "Weird loottables '%s' for '%s', not a LivingEntity so should not have loot",
                                        builders.keySet().stream().map(r -> r.identifier().toString()).collect(Collectors.joining(",")),
                                        holder.key().identifier()
                                    )
                                );
                            }
                        }
                    }
                }
            );
        if (!this.map.isEmpty()) {
            throw new IllegalStateException("Created loot tables for entities not supported by datapack: " + this.map.keySet());
        }
    }

    protected LootItemCondition.Builder killedByFrog(final HolderGetter<EntityType<?>> entityTypes) {
        return DamageSourceCondition.hasDamageSource(
            DamageSourcePredicate.Builder.damageType().source(EntityPredicate.Builder.entity().of(entityTypes, EntityTypes.FROG))
        );
    }

    protected LootItemCondition.Builder killedByFrogVariant(
        final HolderGetter<EntityType<?>> entityTypes, final HolderGetter<FrogVariant> frogVariants, final ResourceKey<FrogVariant> variant
    ) {
        return DamageSourceCondition.hasDamageSource(
            DamageSourcePredicate.Builder.damageType()
                .source(
                    EntityPredicate.Builder.entity()
                        .of(entityTypes, EntityTypes.FROG)
                        .components(DataComponentExactPredicate.expect(DataComponents.FROG_VARIANT, frogVariants.getOrThrow(variant)))
                )
        );
    }

    protected void add(final EntityType<?> type, final LootTable.Builder builder) {
        this.add(type, type.getDefaultLootTable().orElseThrow(() -> new IllegalStateException("Entity " + type + " has no loot table")), builder);
    }

    protected void add(final EntityType<?> type, final ResourceKey<LootTable> lootTable, final LootTable.Builder builder) {
        this.map.computeIfAbsent(type, k -> new HashMap<>()).put(lootTable, builder);
    }
}