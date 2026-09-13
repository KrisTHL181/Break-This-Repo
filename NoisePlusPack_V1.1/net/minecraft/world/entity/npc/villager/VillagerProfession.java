package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.ImmutableSet;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public record VillagerProfession(
   Component name,
   Predicate<Holder<PoiType>> heldJobSite,
   Predicate<Holder<PoiType>> acquirableJobSite,
   ImmutableSet<Item> requestedItems,
   ImmutableSet<Block> secondaryPoi,
   @Nullable SoundEvent workSound
) {
   public static final Predicate<Holder<PoiType>> ALL_ACQUIRABLE_JOBS = p_456882_ -> p_456882_.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
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

   private static ResourceKey<VillagerProfession> createKey(String p_458280_) {
      return ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace(p_458280_));
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_451838_, ResourceKey<VillagerProfession> p_454597_, ResourceKey<PoiType> p_451406_, @Nullable SoundEvent p_456523_
   ) {
      return register(p_451838_, p_454597_, p_458373_ -> p_458373_.is(p_451406_), p_458036_ -> p_458036_.is(p_451406_), p_456523_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_460987_,
      ResourceKey<VillagerProfession> p_451659_,
      Predicate<Holder<PoiType>> p_451477_,
      Predicate<Holder<PoiType>> p_451897_,
      @Nullable SoundEvent p_454008_
   ) {
      return register(p_460987_, p_451659_, p_451477_, p_451897_, ImmutableSet.of(), ImmutableSet.of(), p_454008_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_454580_,
      ResourceKey<VillagerProfession> p_457254_,
      ResourceKey<PoiType> p_458073_,
      ImmutableSet<Item> p_454610_,
      ImmutableSet<Block> p_455699_,
      @Nullable SoundEvent p_452226_
   ) {
      return register(p_454580_, p_457254_, p_451218_ -> p_451218_.is(p_458073_), p_454390_ -> p_454390_.is(p_458073_), p_454610_, p_455699_, p_452226_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_453251_,
      ResourceKey<VillagerProfession> p_454243_,
      Predicate<Holder<PoiType>> p_458179_,
      Predicate<Holder<PoiType>> p_457965_,
      ImmutableSet<Item> p_451774_,
      ImmutableSet<Block> p_454355_,
      @Nullable SoundEvent p_454937_
   ) {
      return Registry.register(
         p_453251_,
         p_454243_,
         new VillagerProfession(
            Component.translatable("entity." + p_454243_.identifier().getNamespace() + ".villager." + p_454243_.identifier().getPath()),
            p_458179_,
            p_457965_,
            p_451774_,
            p_454355_,
            p_454937_
         )
      );
   }

   public static VillagerProfession bootstrap(Registry<VillagerProfession> p_452291_) {
      register(p_452291_, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, null);
      register(p_452291_, ARMORER, PoiTypes.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER);
      register(p_452291_, BUTCHER, PoiTypes.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER);
      register(p_452291_, CARTOGRAPHER, PoiTypes.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
      register(p_452291_, CLERIC, PoiTypes.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC);
      register(
         p_452291_,
         FARMER,
         PoiTypes.FARMER,
         ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL),
         ImmutableSet.of(Blocks.FARMLAND),
         SoundEvents.VILLAGER_WORK_FARMER
      );
      register(p_452291_, FISHERMAN, PoiTypes.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN);
      register(p_452291_, FLETCHER, PoiTypes.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER);
      register(p_452291_, LEATHERWORKER, PoiTypes.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
      register(p_452291_, LIBRARIAN, PoiTypes.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN);
      register(p_452291_, MASON, PoiTypes.MASON, SoundEvents.VILLAGER_WORK_MASON);
      register(p_452291_, NITWIT, PoiType.NONE, PoiType.NONE, null);
      register(p_452291_, SHEPHERD, PoiTypes.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD);
      register(p_452291_, TOOLSMITH, PoiTypes.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH);
      return register(p_452291_, WEAPONSMITH, PoiTypes.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
   }
}
