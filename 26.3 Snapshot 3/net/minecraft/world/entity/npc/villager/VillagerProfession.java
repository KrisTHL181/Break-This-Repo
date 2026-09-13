package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public record VillagerProfession(
   Component name,
   Predicate<Holder<PoiType>> heldJobSite,
   Predicate<Holder<PoiType>> acquirableJobSite,
   ImmutableSet<Item> requestedItems,
   ImmutableSet<Block> secondaryPoi,
   @Nullable SoundEvent workSound,
   Int2ObjectMap<ResourceKey<TradeSet>> tradeSetsByLevel
) {
   public static final Predicate<Holder<PoiType>> ALL_ACQUIRABLE_JOBS = holder -> holder.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
   public static final ResourceKey<VillagerProfession> NONE = createKey("none");
   public static final ResourceKey<VillagerProfession> ARMORER = createKey("armorer");
   public static final ResourceKey<VillagerProfession> BUTCHER = createKey("butcher");
   public static final ResourceKey<VillagerProfession> CARTOGRAPHER = createKey("cartographer");
   public static final ResourceKey<VillagerProfession> CLERIC = createKey("cleric");
   public static final ResourceKey<VillagerProfession> FARMER = createKey("farmer");
   public static final ResourceKey<VillagerProfession> FISHERMAN = createKey("fisherman");
   public static final ResourceKey<VillagerProfession> FLETCHER = createKey("fletcher");
   public static final ResourceKey<VillagerProfession> LEATHERWORKER = createKey("leatherworker");
   public static final ResourceKey<VillagerProfession> LIBRARIAN = createKey("librarian");
   public static final ResourceKey<VillagerProfession> MASON = createKey("mason");
   public static final ResourceKey<VillagerProfession> NITWIT = createKey("nitwit");
   public static final ResourceKey<VillagerProfession> SHEPHERD = createKey("shepherd");
   public static final ResourceKey<VillagerProfession> TOOLSMITH = createKey("toolsmith");
   public static final ResourceKey<VillagerProfession> WEAPONSMITH = createKey("weaponsmith");

   private static ResourceKey<VillagerProfession> createKey(final String name) {
      return ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace(name));
   }

   private static VillagerProfession register(
      final Registry<VillagerProfession> registry,
      final ResourceKey<VillagerProfession> name,
      final ResourceKey<PoiType> jobSite,
      final @Nullable SoundEvent workSound,
      final Int2ObjectMap<ResourceKey<TradeSet>> trades
   ) {
      return register(registry, name, poiType -> poiType.is(jobSite), poiType -> poiType.is(jobSite), workSound, trades);
   }

   private static VillagerProfession register(
      final Registry<VillagerProfession> registry,
      final ResourceKey<VillagerProfession> name,
      final Predicate<Holder<PoiType>> heldJobSite,
      final Predicate<Holder<PoiType>> acquirableJobSite,
      final @Nullable SoundEvent workSound
   ) {
      return register(registry, name, heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound, Int2ObjectMap.ofEntries(new Entry[0]));
   }

   private static VillagerProfession register(
      final Registry<VillagerProfession> registry,
      final ResourceKey<VillagerProfession> name,
      final Predicate<Holder<PoiType>> heldJobSite,
      final Predicate<Holder<PoiType>> acquirableJobSite,
      final @Nullable SoundEvent workSound,
      final Int2ObjectMap<ResourceKey<TradeSet>> trades
   ) {
      return register(registry, name, heldJobSite, acquirableJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound, trades);
   }

   private static VillagerProfession register(
      final Registry<VillagerProfession> registry,
      final ResourceKey<VillagerProfession> name,
      final ResourceKey<PoiType> jobSite,
      final ImmutableSet<Item> requestedItems,
      final ImmutableSet<Block> secondaryPoi,
      final @Nullable SoundEvent workSound,
      final Int2ObjectMap<ResourceKey<TradeSet>> trades
   ) {
      return register(registry, name, poiType -> poiType.is(jobSite), poiType -> poiType.is(jobSite), requestedItems, secondaryPoi, workSound, trades);
   }

   private static VillagerProfession register(
      final Registry<VillagerProfession> registry,
      final ResourceKey<VillagerProfession> name,
      final Predicate<Holder<PoiType>> heldJobSite,
      final Predicate<Holder<PoiType>> acquirableJobSite,
      final ImmutableSet<Item> requestedItems,
      final ImmutableSet<Block> secondaryPoi,
      final @Nullable SoundEvent workSound,
      final Int2ObjectMap<ResourceKey<TradeSet>> trades
   ) {
      return Registry.register(
         registry,
         name,
         new VillagerProfession(
            Component.translatable("entity." + name.identifier().getNamespace() + ".villager." + name.identifier().getPath()),
            heldJobSite,
            acquirableJobSite,
            requestedItems,
            secondaryPoi,
            workSound,
            trades
         )
      );
   }

   public static VillagerProfession bootstrap(final Registry<VillagerProfession> registry) {
      register(registry, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, null);
      register(
         registry,
         ARMORER,
         PoiTypes.ARMORER,
         SoundEvents.VILLAGER_WORK_ARMORER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.ARMORER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.ARMORER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.ARMORER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.ARMORER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.ARMORER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         BUTCHER,
         PoiTypes.BUTCHER,
         SoundEvents.VILLAGER_WORK_BUTCHER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.BUTCHER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.BUTCHER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.BUTCHER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.BUTCHER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.BUTCHER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         CARTOGRAPHER,
         PoiTypes.CARTOGRAPHER,
         SoundEvents.VILLAGER_WORK_CARTOGRAPHER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.CARTOGRAPHER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.CARTOGRAPHER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.CARTOGRAPHER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.CARTOGRAPHER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.CARTOGRAPHER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         CLERIC,
         PoiTypes.CLERIC,
         SoundEvents.VILLAGER_WORK_CLERIC,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.CLERIC_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.CLERIC_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.CLERIC_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.CLERIC_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.CLERIC_LEVEL_5)
            }
         )
      );
      register(
         registry,
         FARMER,
         PoiTypes.FARMER,
         ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL),
         ImmutableSet.of(Blocks.FARMLAND),
         SoundEvents.VILLAGER_WORK_FARMER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.FARMER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.FARMER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.FARMER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.FARMER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.FARMER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         FISHERMAN,
         PoiTypes.FISHERMAN,
         SoundEvents.VILLAGER_WORK_FISHERMAN,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.FISHERMAN_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.FISHERMAN_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.FISHERMAN_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.FISHERMAN_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.FISHERMAN_LEVEL_5)
            }
         )
      );
      register(
         registry,
         FLETCHER,
         PoiTypes.FLETCHER,
         SoundEvents.VILLAGER_WORK_FLETCHER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.FLETCHER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.FLETCHER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.FLETCHER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.FLETCHER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.FLETCHER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         LEATHERWORKER,
         PoiTypes.LEATHERWORKER,
         SoundEvents.VILLAGER_WORK_LEATHERWORKER,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.LEATHERWORKER_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.LEATHERWORKER_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.LEATHERWORKER_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.LEATHERWORKER_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.LEATHERWORKER_LEVEL_5)
            }
         )
      );
      register(
         registry,
         LIBRARIAN,
         PoiTypes.LIBRARIAN,
         SoundEvents.VILLAGER_WORK_LIBRARIAN,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.LIBRARIAN_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.LIBRARIAN_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.LIBRARIAN_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.LIBRARIAN_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.LIBRARIAN_LEVEL_5)
            }
         )
      );
      register(
         registry,
         MASON,
         PoiTypes.MASON,
         SoundEvents.VILLAGER_WORK_MASON,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.MASON_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.MASON_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.MASON_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.MASON_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.MASON_LEVEL_5)
            }
         )
      );
      register(registry, NITWIT, PoiType.NONE, PoiType.NONE, null);
      register(
         registry,
         SHEPHERD,
         PoiTypes.SHEPHERD,
         SoundEvents.VILLAGER_WORK_SHEPHERD,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.SHEPHERD_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.SHEPHERD_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.SHEPHERD_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.SHEPHERD_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.SHEPHERD_LEVEL_5)
            }
         )
      );
      register(
         registry,
         TOOLSMITH,
         PoiTypes.TOOLSMITH,
         SoundEvents.VILLAGER_WORK_TOOLSMITH,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.TOOLSMITH_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.TOOLSMITH_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.TOOLSMITH_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.TOOLSMITH_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.TOOLSMITH_LEVEL_5)
            }
         )
      );
      return register(
         registry,
         WEAPONSMITH,
         PoiTypes.WEAPONSMITH,
         SoundEvents.VILLAGER_WORK_WEAPONSMITH,
         Int2ObjectMap.ofEntries(
            new Entry[]{
               Int2ObjectMap.entry(1, TradeSets.WEAPONSMITH_LEVEL_1),
               Int2ObjectMap.entry(2, TradeSets.WEAPONSMITH_LEVEL_2),
               Int2ObjectMap.entry(3, TradeSets.WEAPONSMITH_LEVEL_3),
               Int2ObjectMap.entry(4, TradeSets.WEAPONSMITH_LEVEL_4),
               Int2ObjectMap.entry(5, TradeSets.WEAPONSMITH_LEVEL_5)
            }
         )
      );
   }

   public @Nullable ResourceKey<TradeSet> getTrades(final int level) {
      return (ResourceKey<TradeSet>)this.tradeSetsByLevel.get(level);
   }
}
