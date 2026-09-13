package net.minecraft.world.item.trading;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class TradeSets {
   public static final ResourceKey<TradeSet> WANDERING_TRADER_BUYING = resourceKey("wandering_trader/buying");
   public static final ResourceKey<TradeSet> WANDERING_TRADER_COMMON = resourceKey("wandering_trader/common");
   public static final ResourceKey<TradeSet> WANDERING_TRADER_UNCOMMON = resourceKey("wandering_trader/uncommon");
   public static final ResourceKey<TradeSet> ARMORER_LEVEL_1 = resourceKey("armorer/level_1");
   public static final ResourceKey<TradeSet> ARMORER_LEVEL_2 = resourceKey("armorer/level_2");
   public static final ResourceKey<TradeSet> ARMORER_LEVEL_3 = resourceKey("armorer/level_3");
   public static final ResourceKey<TradeSet> ARMORER_LEVEL_4 = resourceKey("armorer/level_4");
   public static final ResourceKey<TradeSet> ARMORER_LEVEL_5 = resourceKey("armorer/level_5");
   public static final ResourceKey<TradeSet> BUTCHER_LEVEL_1 = resourceKey("butcher/level_1");
   public static final ResourceKey<TradeSet> BUTCHER_LEVEL_2 = resourceKey("butcher/level_2");
   public static final ResourceKey<TradeSet> BUTCHER_LEVEL_3 = resourceKey("butcher/level_3");
   public static final ResourceKey<TradeSet> BUTCHER_LEVEL_4 = resourceKey("butcher/level_4");
   public static final ResourceKey<TradeSet> BUTCHER_LEVEL_5 = resourceKey("butcher/level_5");
   public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_1 = resourceKey("cartographer/level_1");
   public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_2 = resourceKey("cartographer/level_2");
   public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_3 = resourceKey("cartographer/level_3");
   public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_4 = resourceKey("cartographer/level_4");
   public static final ResourceKey<TradeSet> CARTOGRAPHER_LEVEL_5 = resourceKey("cartographer/level_5");
   public static final ResourceKey<TradeSet> CLERIC_LEVEL_1 = resourceKey("cleric/level_1");
   public static final ResourceKey<TradeSet> CLERIC_LEVEL_2 = resourceKey("cleric/level_2");
   public static final ResourceKey<TradeSet> CLERIC_LEVEL_3 = resourceKey("cleric/level_3");
   public static final ResourceKey<TradeSet> CLERIC_LEVEL_4 = resourceKey("cleric/level_4");
   public static final ResourceKey<TradeSet> CLERIC_LEVEL_5 = resourceKey("cleric/level_5");
   public static final ResourceKey<TradeSet> FARMER_LEVEL_1 = resourceKey("farmer/level_1");
   public static final ResourceKey<TradeSet> FARMER_LEVEL_2 = resourceKey("farmer/level_2");
   public static final ResourceKey<TradeSet> FARMER_LEVEL_3 = resourceKey("farmer/level_3");
   public static final ResourceKey<TradeSet> FARMER_LEVEL_4 = resourceKey("farmer/level_4");
   public static final ResourceKey<TradeSet> FARMER_LEVEL_5 = resourceKey("farmer/level_5");
   public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_1 = resourceKey("fisherman/level_1");
   public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_2 = resourceKey("fisherman/level_2");
   public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_3 = resourceKey("fisherman/level_3");
   public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_4 = resourceKey("fisherman/level_4");
   public static final ResourceKey<TradeSet> FISHERMAN_LEVEL_5 = resourceKey("fisherman/level_5");
   public static final ResourceKey<TradeSet> FLETCHER_LEVEL_1 = resourceKey("fletcher/level_1");
   public static final ResourceKey<TradeSet> FLETCHER_LEVEL_2 = resourceKey("fletcher/level_2");
   public static final ResourceKey<TradeSet> FLETCHER_LEVEL_3 = resourceKey("fletcher/level_3");
   public static final ResourceKey<TradeSet> FLETCHER_LEVEL_4 = resourceKey("fletcher/level_4");
   public static final ResourceKey<TradeSet> FLETCHER_LEVEL_5 = resourceKey("fletcher/level_5");
   public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_1 = resourceKey("leatherworker/level_1");
   public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_2 = resourceKey("leatherworker/level_2");
   public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_3 = resourceKey("leatherworker/level_3");
   public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_4 = resourceKey("leatherworker/level_4");
   public static final ResourceKey<TradeSet> LEATHERWORKER_LEVEL_5 = resourceKey("leatherworker/level_5");
   public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_1 = resourceKey("librarian/level_1");
   public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_2 = resourceKey("librarian/level_2");
   public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_3 = resourceKey("librarian/level_3");
   public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_4 = resourceKey("librarian/level_4");
   public static final ResourceKey<TradeSet> LIBRARIAN_LEVEL_5 = resourceKey("librarian/level_5");
   public static final ResourceKey<TradeSet> MASON_LEVEL_1 = resourceKey("mason/level_1");
   public static final ResourceKey<TradeSet> MASON_LEVEL_2 = resourceKey("mason/level_2");
   public static final ResourceKey<TradeSet> MASON_LEVEL_3 = resourceKey("mason/level_3");
   public static final ResourceKey<TradeSet> MASON_LEVEL_4 = resourceKey("mason/level_4");
   public static final ResourceKey<TradeSet> MASON_LEVEL_5 = resourceKey("mason/level_5");
   public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_1 = resourceKey("shepherd/level_1");
   public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_2 = resourceKey("shepherd/level_2");
   public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_3 = resourceKey("shepherd/level_3");
   public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_4 = resourceKey("shepherd/level_4");
   public static final ResourceKey<TradeSet> SHEPHERD_LEVEL_5 = resourceKey("shepherd/level_5");
   public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_1 = resourceKey("toolsmith/level_1");
   public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_2 = resourceKey("toolsmith/level_2");
   public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_3 = resourceKey("toolsmith/level_3");
   public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_4 = resourceKey("toolsmith/level_4");
   public static final ResourceKey<TradeSet> TOOLSMITH_LEVEL_5 = resourceKey("toolsmith/level_5");
   public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_1 = resourceKey("weaponsmith/level_1");
   public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_2 = resourceKey("weaponsmith/level_2");
   public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_3 = resourceKey("weaponsmith/level_3");
   public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_4 = resourceKey("weaponsmith/level_4");
   public static final ResourceKey<TradeSet> WEAPONSMITH_LEVEL_5 = resourceKey("weaponsmith/level_5");

   public static Holder<TradeSet> bootstrap(final BootstrapContext<TradeSet> context) {
      register(context, WANDERING_TRADER_BUYING, VillagerTradeTags.WANDERING_TRADER_BUYING);
      register(context, WANDERING_TRADER_COMMON, VillagerTradeTags.WANDERING_TRADER_COMMON, ConstantValue.exactly(5.0F));
      register(context, WANDERING_TRADER_UNCOMMON, VillagerTradeTags.WANDERING_TRADER_UNCOMMON);
      register(context, ARMORER_LEVEL_1, VillagerTradeTags.ARMORER_LEVEL_1);
      register(context, ARMORER_LEVEL_2, VillagerTradeTags.ARMORER_LEVEL_2);
      register(context, ARMORER_LEVEL_3, VillagerTradeTags.ARMORER_LEVEL_3);
      register(context, ARMORER_LEVEL_4, VillagerTradeTags.ARMORER_LEVEL_4);
      register(context, ARMORER_LEVEL_5, VillagerTradeTags.ARMORER_LEVEL_5);
      register(context, BUTCHER_LEVEL_1, VillagerTradeTags.BUTCHER_LEVEL_1);
      register(context, BUTCHER_LEVEL_2, VillagerTradeTags.BUTCHER_LEVEL_2);
      register(context, BUTCHER_LEVEL_3, VillagerTradeTags.BUTCHER_LEVEL_3);
      register(context, BUTCHER_LEVEL_4, VillagerTradeTags.BUTCHER_LEVEL_4);
      register(context, BUTCHER_LEVEL_5, VillagerTradeTags.BUTCHER_LEVEL_5);
      register(context, CARTOGRAPHER_LEVEL_1, VillagerTradeTags.CARTOGRAPHER_LEVEL_1);
      register(context, CARTOGRAPHER_LEVEL_2, VillagerTradeTags.CARTOGRAPHER_LEVEL_2);
      register(context, CARTOGRAPHER_LEVEL_3, VillagerTradeTags.CARTOGRAPHER_LEVEL_3);
      register(context, CARTOGRAPHER_LEVEL_4, VillagerTradeTags.CARTOGRAPHER_LEVEL_4);
      register(context, CARTOGRAPHER_LEVEL_5, VillagerTradeTags.CARTOGRAPHER_LEVEL_5);
      register(context, CLERIC_LEVEL_1, VillagerTradeTags.CLERIC_LEVEL_1);
      register(context, CLERIC_LEVEL_2, VillagerTradeTags.CLERIC_LEVEL_2);
      register(context, CLERIC_LEVEL_3, VillagerTradeTags.CLERIC_LEVEL_3);
      register(context, CLERIC_LEVEL_4, VillagerTradeTags.CLERIC_LEVEL_4);
      register(context, CLERIC_LEVEL_5, VillagerTradeTags.CLERIC_LEVEL_5);
      register(context, FARMER_LEVEL_1, VillagerTradeTags.FARMER_LEVEL_1);
      register(context, FARMER_LEVEL_2, VillagerTradeTags.FARMER_LEVEL_2);
      register(context, FARMER_LEVEL_3, VillagerTradeTags.FARMER_LEVEL_3);
      register(context, FARMER_LEVEL_4, VillagerTradeTags.FARMER_LEVEL_4);
      register(context, FARMER_LEVEL_5, VillagerTradeTags.FARMER_LEVEL_5);
      register(context, FISHERMAN_LEVEL_1, VillagerTradeTags.FISHERMAN_LEVEL_1);
      register(context, FISHERMAN_LEVEL_2, VillagerTradeTags.FISHERMAN_LEVEL_2);
      register(context, FISHERMAN_LEVEL_3, VillagerTradeTags.FISHERMAN_LEVEL_3);
      register(context, FISHERMAN_LEVEL_4, VillagerTradeTags.FISHERMAN_LEVEL_4);
      register(context, FISHERMAN_LEVEL_5, VillagerTradeTags.FISHERMAN_LEVEL_5);
      register(context, FLETCHER_LEVEL_1, VillagerTradeTags.FLETCHER_LEVEL_1);
      register(context, FLETCHER_LEVEL_2, VillagerTradeTags.FLETCHER_LEVEL_2);
      register(context, FLETCHER_LEVEL_3, VillagerTradeTags.FLETCHER_LEVEL_3);
      register(context, FLETCHER_LEVEL_4, VillagerTradeTags.FLETCHER_LEVEL_4);
      register(context, FLETCHER_LEVEL_5, VillagerTradeTags.FLETCHER_LEVEL_5);
      register(context, LEATHERWORKER_LEVEL_1, VillagerTradeTags.LEATHERWORKER_LEVEL_1);
      register(context, LEATHERWORKER_LEVEL_2, VillagerTradeTags.LEATHERWORKER_LEVEL_2);
      register(context, LEATHERWORKER_LEVEL_3, VillagerTradeTags.LEATHERWORKER_LEVEL_3);
      register(context, LEATHERWORKER_LEVEL_4, VillagerTradeTags.LEATHERWORKER_LEVEL_4);
      register(context, LEATHERWORKER_LEVEL_5, VillagerTradeTags.LEATHERWORKER_LEVEL_5);
      register(context, LIBRARIAN_LEVEL_1, VillagerTradeTags.LIBRARIAN_LEVEL_1);
      register(context, LIBRARIAN_LEVEL_2, VillagerTradeTags.LIBRARIAN_LEVEL_2);
      register(context, LIBRARIAN_LEVEL_3, VillagerTradeTags.LIBRARIAN_LEVEL_3);
      register(context, LIBRARIAN_LEVEL_4, VillagerTradeTags.LIBRARIAN_LEVEL_4);
      register(context, LIBRARIAN_LEVEL_5, VillagerTradeTags.LIBRARIAN_LEVEL_5, ConstantValue.exactly(3.0F));
      register(context, MASON_LEVEL_1, VillagerTradeTags.MASON_LEVEL_1);
      register(context, MASON_LEVEL_2, VillagerTradeTags.MASON_LEVEL_2);
      register(context, MASON_LEVEL_3, VillagerTradeTags.MASON_LEVEL_3);
      register(context, MASON_LEVEL_4, VillagerTradeTags.MASON_LEVEL_4);
      register(context, MASON_LEVEL_5, VillagerTradeTags.MASON_LEVEL_5);
      register(context, SHEPHERD_LEVEL_1, VillagerTradeTags.SHEPHERD_LEVEL_1);
      register(context, SHEPHERD_LEVEL_2, VillagerTradeTags.SHEPHERD_LEVEL_2);
      register(context, SHEPHERD_LEVEL_3, VillagerTradeTags.SHEPHERD_LEVEL_3);
      register(context, SHEPHERD_LEVEL_4, VillagerTradeTags.SHEPHERD_LEVEL_4);
      register(context, SHEPHERD_LEVEL_5, VillagerTradeTags.SHEPHERD_LEVEL_5);
      register(context, TOOLSMITH_LEVEL_1, VillagerTradeTags.TOOLSMITH_LEVEL_1);
      register(context, TOOLSMITH_LEVEL_2, VillagerTradeTags.TOOLSMITH_LEVEL_2);
      register(context, TOOLSMITH_LEVEL_3, VillagerTradeTags.TOOLSMITH_LEVEL_3);
      register(context, TOOLSMITH_LEVEL_4, VillagerTradeTags.TOOLSMITH_LEVEL_4);
      register(context, TOOLSMITH_LEVEL_5, VillagerTradeTags.TOOLSMITH_LEVEL_5);
      register(context, WEAPONSMITH_LEVEL_1, VillagerTradeTags.WEAPONSMITH_LEVEL_1);
      register(context, WEAPONSMITH_LEVEL_2, VillagerTradeTags.WEAPONSMITH_LEVEL_2);
      register(context, WEAPONSMITH_LEVEL_3, VillagerTradeTags.WEAPONSMITH_LEVEL_3);
      register(context, WEAPONSMITH_LEVEL_4, VillagerTradeTags.WEAPONSMITH_LEVEL_4);
      return register(context, WEAPONSMITH_LEVEL_5, VillagerTradeTags.WEAPONSMITH_LEVEL_5);
   }

   public static Holder.Reference<TradeSet> register(
      final BootstrapContext<TradeSet> context, final ResourceKey<TradeSet> resourceKey, final TagKey<VillagerTrade> tradeTag
   ) {
      return register(context, resourceKey, tradeTag, ConstantValue.exactly(2.0F));
   }

   public static Holder.Reference<TradeSet> register(
      final BootstrapContext<TradeSet> context,
      final ResourceKey<TradeSet> resourceKey,
      final TagKey<VillagerTrade> tradeTag,
      final NumberProvider numberProvider
   ) {
      return context.register(
         resourceKey,
         new TradeSet(
            context.lookup(Registries.VILLAGER_TRADE).getOrThrow(tradeTag),
            numberProvider,
            false,
            Optional.of(resourceKey.identifier().withPrefix("trade_set/"))
         )
      );
   }

   public static ResourceKey<TradeSet> resourceKey(final String path) {
      return ResourceKey.create(Registries.TRADE_SET, Identifier.withDefaultNamespace(path));
   }
}
