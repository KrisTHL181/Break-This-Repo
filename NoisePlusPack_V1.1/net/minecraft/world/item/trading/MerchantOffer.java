package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class MerchantOffer {
   public static final Codec<MerchantOffer> CODEC = RecordCodecBuilder.create(
      p_327696_ -> p_327696_.group(
            ItemCost.CODEC.fieldOf("buy").forGetter(p_328146_ -> p_328146_.baseCostA),
            ItemCost.CODEC.lenientOptionalFieldOf("buyB").forGetter(p_329936_ -> p_329936_.costB),
            ItemStack.CODEC.fieldOf("sell").forGetter(p_330911_ -> p_330911_.result),
            Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(p_329708_ -> p_329708_.uses),
            Codec.INT.lenientOptionalFieldOf("maxUses", 4).forGetter(p_334393_ -> p_334393_.maxUses),
            Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(p_334163_ -> p_334163_.rewardExp),
            Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(p_331018_ -> p_331018_.specialPriceDiff),
            Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(p_334425_ -> p_334425_.demand),
            Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", 0.0F).forGetter(p_335604_ -> p_335604_.priceMultiplier),
            Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(p_334362_ -> p_334362_.xp)
         )
         .apply(p_327696_, MerchantOffer::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MerchantOffer> STREAM_CODEC = StreamCodec.of(
      MerchantOffer::writeToStream, MerchantOffer::createFromStream
   );
   private final ItemCost baseCostA;
   private final Optional<ItemCost> costB;
   private final ItemStack result;
   private int uses;
   private final int maxUses;
   private final boolean rewardExp;
   private int specialPriceDiff;
   private int demand;
   private final float priceMultiplier;
   private final int xp;

   private MerchantOffer(
      ItemCost p_329205_,
      Optional<ItemCost> p_330242_,
      ItemStack p_45334_,
      int p_45337_,
      int p_45338_,
      boolean p_336032_,
      int p_45339_,
      int p_335600_,
      float p_45340_,
      int p_332893_
   ) {
      this.baseCostA = p_329205_;
      this.costB = p_330242_;
      this.result = p_45334_;
      this.uses = p_45337_;
      this.maxUses = p_45338_;
      this.rewardExp = p_336032_;
      this.specialPriceDiff = p_45339_;
      this.demand = p_335600_;
      this.priceMultiplier = p_45340_;
      this.xp = p_332893_;
   }

   public MerchantOffer(ItemCost p_329227_, ItemStack p_45327_, int p_45330_, int p_45331_, float p_45332_) {
      this(p_329227_, Optional.empty(), p_45327_, p_45330_, p_45331_, p_45332_);
   }

   public MerchantOffer(ItemCost p_329055_, Optional<ItemCost> p_335735_, ItemStack p_45321_, int p_45323_, int p_45324_, float p_45325_) {
      this(p_329055_, p_335735_, p_45321_, 0, p_45323_, p_45324_, p_45325_);
   }

   public MerchantOffer(ItemCost p_335696_, Optional<ItemCost> p_334891_, ItemStack p_45342_, int p_45345_, int p_45346_, int p_45347_, float p_45348_) {
      this(p_335696_, p_334891_, p_45342_, p_45345_, p_45346_, p_45347_, p_45348_, 0);
   }

   public MerchantOffer(
      ItemCost p_330366_, Optional<ItemCost> p_332494_, ItemStack p_335597_, int p_333040_, int p_328201_, int p_334322_, float p_333357_, int p_332973_
   ) {
      this(p_330366_, p_332494_, p_335597_, p_333040_, p_328201_, true, 0, p_332973_, p_333357_, p_334322_);
   }

   private MerchantOffer(MerchantOffer p_301741_) {
      this(
         p_301741_.baseCostA,
         p_301741_.costB,
         p_301741_.result.copy(),
         p_301741_.uses,
         p_301741_.maxUses,
         p_301741_.rewardExp,
         p_301741_.specialPriceDiff,
         p_301741_.demand,
         p_301741_.priceMultiplier,
         p_301741_.xp
      );
   }

   public ItemStack getBaseCostA() {
      return this.baseCostA.itemStack();
   }

   public ItemStack getCostA() {
      return this.baseCostA.itemStack().copyWithCount(this.getModifiedCostCount(this.baseCostA));
   }

   private int getModifiedCostCount(ItemCost p_330475_) {
      int i = p_330475_.count();
      int j = Math.max(0, Mth.floor(i * this.demand * this.priceMultiplier));
      return Mth.clamp(i + j + this.specialPriceDiff, 1, p_330475_.itemStack().getMaxStackSize());
   }

   public ItemStack getCostB() {
      return this.costB.map(ItemCost::itemStack).orElse(ItemStack.EMPTY);
   }

   public ItemCost getItemCostA() {
      return this.baseCostA;
   }

   public Optional<ItemCost> getItemCostB() {
      return this.costB;
   }

   public ItemStack getResult() {
      return this.result;
   }

   public void updateDemand() {
      this.demand = this.demand + this.uses - (this.maxUses - this.uses);
   }

   public ItemStack assemble() {
      return this.result.copy();
   }

   public int getUses() {
      return this.uses;
   }

   public void resetUses() {
      this.uses = 0;
   }

   public int getMaxUses() {
      return this.maxUses;
   }

   public void increaseUses() {
      this.uses++;
   }

   public int getDemand() {
      return this.demand;
   }

   public void addToSpecialPriceDiff(int p_45354_) {
      this.specialPriceDiff += p_45354_;
   }

   public void resetSpecialPriceDiff() {
      this.specialPriceDiff = 0;
   }

   public int getSpecialPriceDiff() {
      return this.specialPriceDiff;
   }

   public void setSpecialPriceDiff(int p_45360_) {
      this.specialPriceDiff = p_45360_;
   }

   public float getPriceMultiplier() {
      return this.priceMultiplier;
   }

   public int getXp() {
      return this.xp;
   }

   public boolean isOutOfStock() {
      return this.uses >= this.maxUses;
   }

   public void setToOutOfStock() {
      this.uses = this.maxUses;
   }

   public boolean needsRestock() {
      return this.uses > 0;
   }

   public boolean shouldRewardExp() {
      return this.rewardExp;
   }

   public boolean satisfiedBy(ItemStack p_45356_, ItemStack p_45357_) {
      if (this.baseCostA.test(p_45356_) && p_45356_.getCount() >= this.getModifiedCostCount(this.baseCostA)) {
         return !this.costB.isPresent() ? p_45357_.isEmpty() : this.costB.get().test(p_45357_) && p_45357_.getCount() >= this.costB.get().count();
      } else {
         return false;
      }
   }

   public boolean take(ItemStack p_45362_, ItemStack p_45363_) {
      if (!this.satisfiedBy(p_45362_, p_45363_)) {
         return false;
      }

      p_45362_.shrink(this.getCostA().getCount());
      if (!this.getCostB().isEmpty()) {
         p_45363_.shrink(this.getCostB().getCount());
      }

      return true;
   }

   public MerchantOffer copy() {
      return new MerchantOffer(this);
   }

   private static void writeToStream(RegistryFriendlyByteBuf p_331919_, MerchantOffer p_333750_) {
      ItemCost.STREAM_CODEC.encode(p_331919_, p_333750_.getItemCostA());
      ItemStack.STREAM_CODEC.encode(p_331919_, p_333750_.getResult());
      ItemCost.OPTIONAL_STREAM_CODEC.encode(p_331919_, p_333750_.getItemCostB());
      p_331919_.writeBoolean(p_333750_.isOutOfStock());
      p_331919_.writeInt(p_333750_.getUses());
      p_331919_.writeInt(p_333750_.getMaxUses());
      p_331919_.writeInt(p_333750_.getXp());
      p_331919_.writeInt(p_333750_.getSpecialPriceDiff());
      p_331919_.writeFloat(p_333750_.getPriceMultiplier());
      p_331919_.writeInt(p_333750_.getDemand());
   }

   public static MerchantOffer createFromStream(RegistryFriendlyByteBuf p_335331_) {
      ItemCost itemcost = ItemCost.STREAM_CODEC.decode(p_335331_);
      ItemStack itemstack = ItemStack.STREAM_CODEC.decode(p_335331_);
      Optional<ItemCost> optional = ItemCost.OPTIONAL_STREAM_CODEC.decode(p_335331_);
      boolean flag = p_335331_.readBoolean();
      int i = p_335331_.readInt();
      int j = p_335331_.readInt();
      int k = p_335331_.readInt();
      int l = p_335331_.readInt();
      float f = p_335331_.readFloat();
      int i1 = p_335331_.readInt();
      MerchantOffer merchantoffer = new MerchantOffer(itemcost, optional, itemstack, i, j, k, f, i1);
      if (flag) {
         merchantoffer.setToOutOfStock();
      }

      merchantoffer.setSpecialPriceDiff(l);
      return merchantoffer;
   }
}
