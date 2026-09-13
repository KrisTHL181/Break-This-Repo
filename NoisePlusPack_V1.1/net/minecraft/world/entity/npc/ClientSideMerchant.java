package net.minecraft.world.entity.npc;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

public class ClientSideMerchant implements Merchant {
   private final Player source;
   private MerchantOffers offers = new MerchantOffers();
   private int xp;

   public ClientSideMerchant(Player p_35344_) {
      this.source = p_35344_;
   }

   @Override
   public Player getTradingPlayer() {
      return this.source;
   }

   @Override
   public void setTradingPlayer(@Nullable Player p_35356_) {
   }

   @Override
   public MerchantOffers getOffers() {
      return this.offers;
   }

   @Override
   public void overrideOffers(MerchantOffers p_35348_) {
      this.offers = p_35348_;
   }

   @Override
   public void notifyTrade(MerchantOffer p_35346_) {
      p_35346_.increaseUses();
   }

   @Override
   public void notifyTradeUpdated(ItemStack p_35358_) {
   }

   @Override
   public boolean isClientSide() {
      return this.source.level().isClientSide();
   }

   @Override
   public boolean stillValid(Player p_377376_) {
      return this.source == p_377376_;
   }

   @Override
   public int getVillagerXp() {
      return this.xp;
   }

   @Override
   public void overrideXp(int p_35360_) {
      this.xp = p_35360_;
   }

   @Override
   public boolean showProgressBar() {
      return true;
   }

   @Override
   public SoundEvent getNotifyTradeSound() {
      return SoundEvents.VILLAGER_YES;
   }
}
