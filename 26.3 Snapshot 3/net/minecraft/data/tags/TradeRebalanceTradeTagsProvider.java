package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.trading.TradeRebalanceVillagerTrades;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.VillagerTrades;

public class TradeRebalanceTradeTagsProvider extends TagsProvider<VillagerTrade> {
   public TradeRebalanceTradeTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.VILLAGER_TRADE, lookupProvider);
   }

   @Override
   protected void addTags(final HolderLookup.Provider registries) {
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_1, true)
         .add(
            VillagerTrades.LIBRARIAN_1_PAPER_EMERALD,
            VillagerTrades.LIBRARIAN_1_EMERALD_BOOKSHELF,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_2, true)
         .add(
            VillagerTrades.LIBRARIAN_2_BOOK_EMERALD,
            VillagerTrades.LIBRARIAN_2_EMERALD_LANTERN,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_3, true)
         .add(
            VillagerTrades.LIBRARIAN_3_INK_SAC_EMERALD,
            VillagerTrades.LIBRARIAN_3_EMERALD_GLASS,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_4, true)
         .add(VillagerTrades.LIBRARIAN_4_WRITABLE_BOOK_EMERALD, VillagerTrades.LIBRARIAN_4_EMERALD_CLOCK, VillagerTrades.LIBRARIAN_4_EMERALD_COMPASS);
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_5, true)
         .add(
            VillagerTrades.LIBRARIAN_5_EMERALD_YELLOW_CANDLE,
            VillagerTrades.LIBRARIAN_5_EMERALD_RED_CANDLE,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_DESERT_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_JUNGLE_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_PLAINS_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_SAVANNA_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_SNOW_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_SWAMP_ENCHANTED_BOOK,
            TradeRebalanceVillagerTrades.LIBRARIAN_5_EMERALD_AND_BOOK_TAIGA_ENCHANTED_BOOK
         );
      this.tag(VillagerTradeTags.ARMORER_LEVEL_1, true)
         .add(VillagerTrades.COMMON_SMITH_1_COAL_EMERALD, TradeRebalanceVillagerTrades.ARMORER_1_IRON_INGOT_EMERALD);
      this.tag(VillagerTradeTags.ARMORER_LEVEL_2, true)
         .add(
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_IRON_BOOTS_GROUP_1,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_IRON_CHESTPLATE_GROUP_1,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_IRON_LEGGINGS_GROUP_1,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_IRON_HELMET_GROUP_1,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_HELMET_GROUP_2,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_BOOTS_GROUP_2,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_CHESTPLATE_GROUP_2,
            TradeRebalanceVillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS_GROUP_2
         );
      this.tag(VillagerTradeTags.ARMORER_LEVEL_3, true)
         .add(VillagerTrades.ARMORER_3_LAVA_BUCKET_EMERALD, VillagerTrades.ARMORER_3_EMERALD_SHIELD, TradeRebalanceVillagerTrades.ARMORER_3_EMERALD_BELL);
      this.tag(VillagerTradeTags.ARMORER_LEVEL_4, true)
         .add(
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_LEGGINGS_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_CHESTPLATE_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_BOOTS_SNOW,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_IRON_HELMET_SNOW,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_BOOTS_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_HELMET_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_LEGGINGS_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_ENCHANTED_CHAINMAIL_CHESTPLATE_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_AND_DIAMOND_BOOTS_DIAMOND_LEGGINGS_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_AND_DIAMOND_LEGGINGS_DIAMOND_CHESTPLATE_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_AND_DIAMOND_HELMET_DIAMOND_BOOTS_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_4_EMERALD_AND_DIAMOND_CHESTPLATE_DIAMOND_HELMET_TAIGA
         );
      this.tag(VillagerTradeTags.ARMORER_LEVEL_5, true)
         .add(
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_DESERT,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_PLAINS,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_SAVANNA,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_BOOTS_SNOW,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_HELMET_SNOW,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_CHAINMAIL_HELMET_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_CHAINMAIL_BOOTS_JUNGLE,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_CHAINMAIL_HELMET_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_CHAINMAIL_BOOTS_SWAMP,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_CHESTPLATE_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_5_EMERALD_AND_DIAMOND_DIAMOND_LEGGINGS_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_5_DIAMOND_BLOCK_EMERALD_TAIGA,
            TradeRebalanceVillagerTrades.ARMORER_5_IRON_BLOCK_EMERALD_NON_TAIGA
         );
   }
}
