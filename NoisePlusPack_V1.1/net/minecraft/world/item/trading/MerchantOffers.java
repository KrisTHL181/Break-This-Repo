package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public static final Codec<MerchantOffers> CODEC = MerchantOffer.CODEC
      .listOf()
      .optionalFieldOf("Recipes", List.of())
      .xmap(MerchantOffers::new, Function.identity())
      .codec();
   public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffers> STREAM_CODEC = MerchantOffer.STREAM_CODEC
      .apply(ByteBufCodecs.collection(MerchantOffers::new));

   public MerchantOffers() {
   }

   private MerchantOffers(int p_220323_) {
      super(p_220323_);
   }

   private MerchantOffers(Collection<MerchantOffer> p_331802_) {
      super(p_331802_);
   }

   public @Nullable MerchantOffer getRecipeFor(ItemStack p_45390_, ItemStack p_45391_, int p_45392_) {
      if (p_45392_ > 0 && p_45392_ < this.size()) {
         MerchantOffer merchantoffer1 = this.get(p_45392_);
         return merchantoffer1.satisfiedBy(p_45390_, p_45391_) ? merchantoffer1 : null;
      }

      for (int i = 0; i < this.size(); i++) {
         MerchantOffer merchantoffer = this.get(i);
         if (merchantoffer.satisfiedBy(p_45390_, p_45391_)) {
            return merchantoffer;
         }
      }

      return null;
   }

   public MerchantOffers copy() {
      MerchantOffers merchantoffers = new MerchantOffers(this.size());

      for (MerchantOffer merchantoffer : this) {
         merchantoffers.add(merchantoffer.copy());
      }

      return merchantoffers;
   }
}
