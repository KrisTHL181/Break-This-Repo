package net.minecraft.world.item.trading;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.Sum;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class TradeRebalanceVillagerTrades extends VillagerTrades {
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_desert_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_jungle_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_plains_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_savanna_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_snow_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_swamp_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK = resourceKey(
      "librarian/1/emerald_and_book_taiga_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_desert_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_jungle_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_plains_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_savanna_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_snow_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_swamp_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK = resourceKey(
      "librarian/2/emerald_and_book_taiga_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_desert_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_jungle_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_plains_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_savanna_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_snow_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_swamp_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK = resourceKey(
      "librarian/3/emerald_and_book_taiga_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_desert_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_jungle_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_plains_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_savanna_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_snow_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_swamp_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK = resourceKey(
      "librarian/5/emerald_and_book_taiga_enchanted_book"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_1_IRON_INGOT_EMERALD = resourceKey("armorer/1/iron_ingot_emerald");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_IRON_BOOTS_GROUP_1 = resourceKey("armorer/2/emerald_iron_boots_group_1");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_IRON_HELMET_GROUP_1 = resourceKey("armorer/2/emerald_iron_helmet_group_1");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_IRON_LEGGINGS_GROUP_1 = resourceKey("armorer/2/emerald_iron_leggings_group_1");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_IRON_CHESTPLATE_GROUP_1 = resourceKey("armorer/2/emerald_iron_chestplate_group_1");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_BOOTS_GROUP_2 = resourceKey("armorer/2/emerald_chainmail_boots_group_2");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_HELMET_GROUP_2 = resourceKey("armorer/2/emerald_chainmail_helmet_group_2");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS_GROUP_2 = resourceKey("armorer/2/emerald_chainmail_leggings_group_2");
   public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_CHESTPLATE_GROUP_2 = resourceKey("armorer/2/emerald_chainmail_chainmail_group_2");
   public static final ResourceKey<VillagerTrade> ARMORER_3_EMERALD_BELL = resourceKey("armorer/3/emerald_bell");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_DESERT = resourceKey("armorer/4/emerald_enchanted_iron_boots_desert");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_DESERT = resourceKey("armorer/4/emerald_enchanted_iron_helmet_desert");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_DESERT = resourceKey(
      "armorer/4/emerald_enchanted_iron_leggings_desert"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_DESERT = resourceKey(
      "armorer/4/emerald_enchanted_iron_chestplate_desert"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_PLAINS = resourceKey("armorer/4/emerald_enchanted_iron_boots_plains");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_PLAINS = resourceKey("armorer/4/emerald_enchanted_iron_helmet_plains");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_PLAINS = resourceKey(
      "armorer/4/emerald_enchanted_iron_leggings_plains"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_PLAINS = resourceKey(
      "armorer/4/emerald_enchanted_iron_chestplate_plains"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SAVANNA = resourceKey("armorer/4/emerald_enchanted_iron_boots_savanna");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SAVANNA = resourceKey(
      "armorer/4/emerald_enchanted_iron_helmet_savanna"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_SAVANNA = resourceKey(
      "armorer/4/emerald_enchanted_iron_leggings_savanna"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_SAVANNA = resourceKey(
      "armorer/4/emerald_enchanted_iron_chestplate_savanna"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SNOW = resourceKey("armorer/4/emerald_enchanted_iron_boots_snow");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SNOW = resourceKey("armorer/4/emerald_enchanted_iron_helmet_snow");
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_JUNGLE = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_boots_jungle"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_JUNGLE = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_helmet_jungle"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_JUNGLE = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_leggings_jungle"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_JUNGLE = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_chestplate_jungle"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_SWAMP = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_boots_swamp"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_SWAMP = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_helmet_swamp"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_SWAMP = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_leggings_swamp"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_SWAMP = resourceKey(
      "armorer/4/emerald_enchanted_chainmail_chestplate_swamp"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_AND_DIAMOND_BOOTS_DIAMOND_LEGGINGS_TAIGA = resourceKey(
      "armorer/4/emerald_and_diamond_boots_diamond_leggings_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_AND_DIAMOND_LEGGINGS_DIAMOND_CHESTPLATE_TAIGA = resourceKey(
      "armorer/4/emerald_and_diamond_leggings_diamond_chestplate_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_AND_DIAMOND_HELMET_DIAMOND_BOOTS_TAIGA = resourceKey(
      "armorer/4/emerald_and_diamond_helmet_diamond_boots_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_AND_DIAMOND_CHESTPLATE_DIAMOND_HELMET_TAIGA = resourceKey(
      "armorer/4/emerald_and_diamond_chestplate_diamond_helmet_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_DESERT = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_chestplate_desert"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_DESERT = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_leggings_desert"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_PLAINS = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_leggings_plains"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_PLAINS = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_boots_plains"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SAVANNA = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_helmet_savanna"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_SAVANNA = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_chestplate_savanna"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_SNOW = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_boots_snow"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SNOW = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_helmet_snow"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_CHAINMAIL_HELMET_JUNGLE = resourceKey("armorer/5/emerald_chainmail_helmet_jungle");
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_CHAINMAIL_BOOTS_JUNGLE = resourceKey("armorer/5/emerald_chainmail_boots_jungle");
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_CHAINMAIL_HELMET_SWAMP = resourceKey("armorer/5/emerald_chainmail_helmet_swamp");
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_CHAINMAIL_BOOTS_SWAMP = resourceKey("armorer/5/emerald_chainmail_boots_swamp");
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_TAIGA = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_chestplate_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_TAIGA = resourceKey(
      "armorer/5/emerald_and_diamond_diamond_leggings_taiga"
   );
   public static final ResourceKey<VillagerTrade> ARMORER_5_DIAMOND_BLOCK_EMERALD_TAIGA = resourceKey("armorer/5/diamond_block_emerald_taiga");
   public static final ResourceKey<VillagerTrade> ARMORER_5_IRON_BLOCK_EMERALD_NON_TAIGA = resourceKey("armorer/5/iron_block_emerald_non_taiga");

   public static Holder<VillagerTrade> bootstrap(final BootstrapContext<VillagerTrade> context) {
      HolderGetter<Item> items = context.lookup(Registries.ITEM);
      HolderGetter<VillagerType> villagerVariants = context.lookup(Registries.VILLAGER_TYPE);
      HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
      HolderSet<Enchantment> doubleTradePrice = enchantments.getOrThrow(EnchantmentTags.DOUBLE_TRADE_PRICE);
      HolderSet<VillagerType> desertType = villagerTypeHolderSet(villagerVariants, VillagerType.DESERT);
      HolderSet<Enchantment> enchantmentsDesertCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_DESERT_COMMON);
      HolderSet<VillagerType> jungleType = villagerTypeHolderSet(villagerVariants, VillagerType.JUNGLE);
      HolderSet<Enchantment> enchantmentsJungleCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_JUNGLE_COMMON);
      HolderSet<VillagerType> plainsType = villagerTypeHolderSet(villagerVariants, VillagerType.PLAINS);
      HolderSet<Enchantment> enchantmentsPlainsCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_PLAINS_COMMON);
      HolderSet<VillagerType> savannaType = villagerTypeHolderSet(villagerVariants, VillagerType.SAVANNA);
      HolderSet<Enchantment> enchantmentsSavannaCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_SAVANNA_COMMON);
      HolderSet<VillagerType> snowType = villagerTypeHolderSet(villagerVariants, VillagerType.SNOW);
      HolderSet<Enchantment> enchantmentsSnowCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_SNOW_COMMON);
      HolderSet<VillagerType> swampType = villagerTypeHolderSet(villagerVariants, VillagerType.SWAMP);
      HolderSet<Enchantment> enchantmentsSwampCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_SWAMP_COMMON);
      HolderSet<VillagerType> taigaType = villagerTypeHolderSet(villagerVariants, VillagerType.TAIGA);
      HolderSet<Enchantment> enchantmentsTaigaCommon = enchantments.getOrThrow(EnchantmentTags.TRADES_TAIGA_COMMON);
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, desertType, enchantmentsDesertCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, jungleType, enchantmentsJungleCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, plainsType, enchantmentsPlainsCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, savannaType, enchantmentsSavannaCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, snowType, enchantmentsSnowCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, swampType, enchantmentsSwampCommon)
      );
      register(
         context,
         LIBRARIAN_1_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK,
         createLibrarianLevel1EnchantmentTrade(items, doubleTradePrice, taigaType, enchantmentsTaigaCommon)
      );
      register(
         context,
         LIBRARIAN_2_BOOK_EMERALD,
         new VillagerTrade(new TradeCost(Items.BOOK, 4), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, desertType, enchantmentsDesertCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, jungleType, enchantmentsJungleCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, plainsType, enchantmentsPlainsCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, savannaType, enchantmentsSavannaCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, snowType, enchantmentsSnowCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, swampType, enchantmentsSwampCommon)
      );
      register(
         context,
         LIBRARIAN_2_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK,
         createLibrarianLevel2EnchantmentTrade(items, doubleTradePrice, taigaType, enchantmentsTaigaCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, desertType, enchantmentsDesertCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, jungleType, enchantmentsJungleCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, plainsType, enchantmentsPlainsCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, savannaType, enchantmentsSavannaCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, snowType, enchantmentsSnowCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, swampType, enchantmentsSwampCommon)
      );
      register(
         context,
         LIBRARIAN_3_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK,
         createLibrarianLevel3EnchantmentTrade(items, doubleTradePrice, taigaType, enchantmentsTaigaCommon)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
         createLibrarianLevel5EnchantmentTrade(items, doubleTradePrice, desertType, enchantments.getOrThrow(Enchantments.EFFICIENCY), 3)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
         createLibrarianLevel5EnchantmentTrade(items, doubleTradePrice, jungleType, enchantments.getOrThrow(Enchantments.UNBREAKING), 2)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
         createLibrarianLevel5EnchantmentTrade(items, doubleTradePrice, plainsType, enchantments.getOrThrow(Enchantments.PROTECTION), 3)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
         createLibrarianLevel5EnchantmentTrade(items, doubleTradePrice, savannaType, enchantments.getOrThrow(Enchantments.SHARPNESS), 3)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK,
         createLibrarianLevel5EnchantmentTrade(items, doubleTradePrice, taigaType, enchantments.getOrThrow(Enchantments.FORTUNE), 2)
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
         createLibrarianLevel5SpecialEnchantmentTrade(items, doubleTradePrice, snowType, enchantments.getOrThrow(Enchantments.SILK_TOUCH))
      );
      register(
         context,
         LIBRARIAN_5_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
         createLibrarianLevel5SpecialEnchantmentTrade(items, doubleTradePrice, swampType, enchantments.getOrThrow(Enchantments.MENDING))
      );
      register(
         context,
         ARMORER_1_IRON_INGOT_EMERALD,
         new VillagerTrade(
            new TradeCost(Items.IRON_INGOT, 5),
            new ItemStackTemplate(Items.EMERALD),
            12,
            5,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants, List.of(VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA)
               )
            ),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_IRON_BOOTS_GROUP_1,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 4),
            new ItemStackTemplate(Items.IRON_BOOTS),
            12,
            5,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants, List.of(VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA)
               )
            ),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_IRON_HELMET_GROUP_1,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 5),
            new ItemStackTemplate(Items.IRON_HELMET),
            12,
            5,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants, List.of(VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA)
               )
            ),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_IRON_LEGGINGS_GROUP_1,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 7),
            new ItemStackTemplate(Items.IRON_LEGGINGS),
            12,
            5,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants, List.of(VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA)
               )
            ),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_IRON_CHESTPLATE_GROUP_1,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.IRON_CHESTPLATE),
            12,
            5,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants, List.of(VillagerType.DESERT, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.TAIGA)
               )
            ),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_CHAINMAIL_BOOTS_GROUP_2,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 4),
            new ItemStackTemplate(Items.CHAINMAIL_BOOTS),
            12,
            5,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE, VillagerType.SWAMP))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_CHAINMAIL_HELMET_GROUP_2,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 5),
            new ItemStackTemplate(Items.CHAINMAIL_HELMET),
            12,
            5,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE, VillagerType.SWAMP))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS_GROUP_2,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 7),
            new ItemStackTemplate(Items.CHAINMAIL_LEGGINGS),
            12,
            5,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE, VillagerType.SWAMP))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_2_EMERALD_CHAINMAIL_CHESTPLATE_GROUP_2,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE),
            12,
            5,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE, VillagerType.SWAMP))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_3_EMERALD_BELL,
         new VillagerTrade(new TradeCost(Items.EMERALD, 36), new ItemStackTemplate(Items.BELL), 12, 10, 0.2F, Optional.empty(), List.of())
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.IRON_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.IRON_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.IRON_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.IRON_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 11),
            new ItemStackTemplate(Items.IRON_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.IRON_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 13),
            new ItemStackTemplate(Items.IRON_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.IRON_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.IRON_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.IRON_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.IRON_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.IRON_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 11),
            new ItemStackTemplate(Items.IRON_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.IRON_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 13),
            new ItemStackTemplate(Items.IRON_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.IRON_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 2),
            new ItemStackTemplate(Items.IRON_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.IRON_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 3),
            new ItemStackTemplate(Items.IRON_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.IRON_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 5),
            new ItemStackTemplate(Items.IRON_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.IRON_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 7),
            new ItemStackTemplate(Items.IRON_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.IRON_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SNOW,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.IRON_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SNOW))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.FROST_WALKER), 1, Items.IRON_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SNOW,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.IRON_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SNOW))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.AQUA_AFFINITY), 1, Items.IRON_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.CHAINMAIL_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.UNBREAKING), 1, Items.CHAINMAIL_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.CHAINMAIL_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.UNBREAKING), 1, Items.CHAINMAIL_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 11),
            new ItemStackTemplate(Items.CHAINMAIL_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.UNBREAKING), 1, Items.CHAINMAIL_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 13),
            new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.UNBREAKING), 1, Items.CHAINMAIL_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.CHAINMAIL_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.MENDING), 1, Items.CHAINMAIL_BOOTS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.CHAINMAIL_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.MENDING), 1, Items.CHAINMAIL_HELMET)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 11),
            new ItemStackTemplate(Items.CHAINMAIL_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.MENDING), 1, Items.CHAINMAIL_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 13),
            new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.MENDING), 1, Items.CHAINMAIL_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_AND_DIAMOND_BOOTS_DIAMOND_LEGGINGS_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 4),
            Optional.of(new TradeCost(Items.DIAMOND_BOOTS, 1)),
            new ItemStackTemplate(Items.DIAMOND_LEGGINGS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_AND_DIAMOND_LEGGINGS_DIAMOND_CHESTPLATE_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 4),
            Optional.of(new TradeCost(Items.DIAMOND_LEGGINGS, 1)),
            new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_AND_DIAMOND_HELMET_DIAMOND_BOOTS_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 4),
            Optional.of(new TradeCost(Items.DIAMOND_HELMET, 1)),
            new ItemStackTemplate(Items.DIAMOND_BOOTS),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_4_EMERALD_AND_DIAMOND_CHESTPLATE_DIAMOND_HELMET_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 2),
            Optional.of(new TradeCost(Items.DIAMOND_CHESTPLATE, 1)),
            new ItemStackTemplate(Items.DIAMOND_HELMET),
            3,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            List.of()
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 16),
            Optional.of(new TradeCost(Items.DIAMOND, 4)),
            new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.DIAMOND_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_DESERT,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 16),
            Optional.of(new TradeCost(Items.DIAMOND, 3)),
            new ItemStackTemplate(Items.DIAMOND_LEGGINGS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.DESERT))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.THORNS), 1, Items.DIAMOND_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 16),
            Optional.of(new TradeCost(Items.DIAMOND, 3)),
            new ItemStackTemplate(Items.DIAMOND_LEGGINGS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.DIAMOND_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_PLAINS,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 12),
            Optional.of(new TradeCost(Items.DIAMOND, 2)),
            new ItemStackTemplate(Items.DIAMOND_BOOTS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.PLAINS))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROTECTION), 1, Items.DIAMOND_BOOTS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 6),
            Optional.of(new TradeCost(Items.DIAMOND, 2)),
            new ItemStackTemplate(Items.DIAMOND_HELMET),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.DIAMOND_HELMET)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_SAVANNA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            Optional.of(new TradeCost(Items.DIAMOND, 3)),
            new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SAVANNA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BINDING_CURSE), 1, Items.DIAMOND_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_SNOW,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 12),
            Optional.of(new TradeCost(Items.DIAMOND, 2)),
            new ItemStackTemplate(Items.DIAMOND_BOOTS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SNOW))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.FROST_WALKER), 1, Items.DIAMOND_BOOTS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SNOW,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 12),
            Optional.of(new TradeCost(Items.DIAMOND, 3)),
            new ItemStackTemplate(Items.DIAMOND_HELMET),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SNOW))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.AQUA_AFFINITY), 1, Items.DIAMOND_HELMET)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_CHAINMAIL_HELMET_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.CHAINMAIL_HELMET),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 1, Items.CHAINMAIL_HELMET)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_CHAINMAIL_BOOTS_JUNGLE,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.CHAINMAIL_BOOTS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.JUNGLE))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.FEATHER_FALLING), 1, Items.CHAINMAIL_BOOTS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_CHAINMAIL_HELMET_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 9),
            new ItemStackTemplate(Items.CHAINMAIL_HELMET),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.RESPIRATION), 1, Items.CHAINMAIL_HELMET)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_CHAINMAIL_BOOTS_SWAMP,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            new ItemStackTemplate(Items.CHAINMAIL_BOOTS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.SWAMP))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.DEPTH_STRIDER), 1, Items.CHAINMAIL_BOOTS)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 18),
            Optional.of(new TradeCost(Items.DIAMOND, 4)),
            new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), 1, Items.DIAMOND_CHESTPLATE)
         )
      );
      register(
         context,
         ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.EMERALD, 18),
            Optional.of(new TradeCost(Items.DIAMOND, 3)),
            new ItemStackTemplate(Items.DIAMOND_LEGGINGS),
            3,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            enchantedItem(items, enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), 1, Items.DIAMOND_LEGGINGS)
         )
      );
      register(
         context,
         ARMORER_5_DIAMOND_BLOCK_EMERALD_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.DIAMOND_BLOCK, 1),
            new ItemStackTemplate(Items.EMERALD, 42),
            12,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, List.of(VillagerType.TAIGA))),
            List.of()
         )
      );
      return register(
         context,
         ARMORER_5_IRON_BLOCK_EMERALD_NON_TAIGA,
         new VillagerTrade(
            new TradeCost(Items.IRON_BLOCK, 1),
            new ItemStackTemplate(Items.EMERALD, 4),
            12,
            30,
            0.05F,
            villagerTypeRestriction(
               villagerTypeHolderSet(
                  villagerVariants,
                  List.of(VillagerType.DESERT, VillagerType.JUNGLE, VillagerType.PLAINS, VillagerType.SAVANNA, VillagerType.SNOW, VillagerType.SWAMP)
               )
            ),
            List.of()
         )
      );
   }

   private static VillagerTrade createLibrarianLevel1EnchantmentTrade(
      final HolderGetter<Item> items,
      final HolderSet<Enchantment> doubleTradePrice,
      final HolderSet<VillagerType> villagerTypes,
      final HolderSet<Enchantment> enchantments
   ) {
      return new VillagerTrade(
         new TradeCost(Items.EMERALD, 0),
         Optional.of(new TradeCost(Items.BOOK, 1)),
         new ItemStackTemplate(Items.ENCHANTED_BOOK),
         12,
         1,
         0.2F,
         villagerTypeRestriction(villagerTypes),
         enchantedBook(items, enchantments),
         doubleTradePrice
      );
   }

   private static VillagerTrade createLibrarianLevel2EnchantmentTrade(
      final HolderGetter<Item> items,
      final HolderSet<Enchantment> doubleTradePrice,
      final HolderSet<VillagerType> villagerTypes,
      final HolderSet<Enchantment> enchantments
   ) {
      return new VillagerTrade(
         new TradeCost(Items.EMERALD, 0),
         Optional.of(new TradeCost(Items.BOOK, 1)),
         new ItemStackTemplate(Items.ENCHANTED_BOOK),
         12,
         5,
         0.2F,
         villagerTypeRestriction(villagerTypes),
         enchantedBook(items, enchantments),
         doubleTradePrice
      );
   }

   private static VillagerTrade createLibrarianLevel3EnchantmentTrade(
      final HolderGetter<Item> items,
      final HolderSet<Enchantment> doubleTradePrice,
      final HolderSet<VillagerType> villagerTypes,
      final HolderSet<Enchantment> enchantments
   ) {
      return new VillagerTrade(
         new TradeCost(Items.EMERALD, 0),
         Optional.of(new TradeCost(Items.BOOK, 1)),
         new ItemStackTemplate(Items.ENCHANTED_BOOK),
         12,
         10,
         0.2F,
         villagerTypeRestriction(villagerTypes),
         enchantedBook(items, enchantments),
         doubleTradePrice
      );
   }

   private static VillagerTrade createLibrarianLevel5EnchantmentTrade(
      final HolderGetter<Item> items,
      final HolderSet<Enchantment> doubleTradePrice,
      final HolderSet<VillagerType> villagerTypes,
      final Holder<Enchantment> enchantment,
      final int level
   ) {
      return new VillagerTrade(
         new TradeCost(Items.EMERALD, Sum.sum(ConstantValue.exactly(3 * level + 2), UniformGenerator.between(0.0F, 5 + level * 10))),
         Optional.of(new TradeCost(Items.BOOK, 1)),
         new ItemStackTemplate(Items.ENCHANTED_BOOK),
         12,
         30,
         0.2F,
         villagerTypeRestriction(villagerTypes),
         enchantedBook(items, enchantment, level),
         doubleTradePrice
      );
   }

   private static VillagerTrade createLibrarianLevel5SpecialEnchantmentTrade(
      final HolderGetter<Item> items,
      final HolderSet<Enchantment> doubleTradePrice,
      final HolderSet<VillagerType> villagerTypes,
      final Holder<Enchantment> enchantment
   ) {
      return new VillagerTrade(
         new TradeCost(Items.EMERALD, 0),
         Optional.of(new TradeCost(Items.BOOK, 1)),
         new ItemStackTemplate(Items.ENCHANTED_BOOK),
         12,
         30,
         0.2F,
         villagerTypeRestriction(villagerTypes),
         enchantedBook(items, HolderSet.direct(enchantment)),
         doubleTradePrice
      );
   }
}
