package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class TradeSet {
   public static final Codec<TradeSet> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.VILLAGER_TRADE).fieldOf("trades").forGetter(tradeSet -> tradeSet.trades),
            NumberProviders.DIRECT_CODEC.fieldOf("amount").forGetter(tradeSet -> tradeSet.amount),
            Codec.BOOL.optionalFieldOf("allow_duplicates", false).forGetter(tradeSet -> tradeSet.allowDuplicates),
            Identifier.CODEC.optionalFieldOf("random_sequence").forGetter(t -> t.randomSequence)
         )
         .apply(i, TradeSet::new)
   );
   private final HolderSet<VillagerTrade> trades;
   private final NumberProvider amount;
   private final boolean allowDuplicates;
   private final Optional<Identifier> randomSequence;

   public TradeSet(final HolderSet<VillagerTrade> trades, final NumberProvider amount, final boolean allowDuplicates, final Optional<Identifier> randomSequence) {
      this.trades = trades;
      this.amount = amount;
      this.allowDuplicates = allowDuplicates;
      this.randomSequence = randomSequence;
   }

   public HolderSet<VillagerTrade> getTrades() {
      return this.trades;
   }

   public int calculateNumberOfTrades(final LootContext lootContext) {
      return this.amount.getInt(lootContext);
   }

   public boolean allowDuplicates() {
      return this.allowDuplicates;
   }

   public Optional<Identifier> randomSequence() {
      return this.randomSequence;
   }
}
