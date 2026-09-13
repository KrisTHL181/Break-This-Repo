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

   public ClientSideMerchant(final Player source) {
      this.source = source;
   }

   @Override
   public Player getTradingPlayer() {
      return this.source;
   }

   @Override
   public void setTradingPlayer(final @Nullable Player player) {
   }

   @Override
   public MerchantOffers getOffers() {
      return this.offers;
   }

   @Override
   public void overrideOffers(final MerchantOffers offers) {
      this.offers = offers;
   }

   @Override
   public void notifyTrade(final MerchantOffer offer) {
      offer.increaseUses();
   }

   @Override
   public void notifyTradeUpdated(final ItemStack itemStack) {
   }

   @Override
   public boolean isClientSide() {
      return this.source.level().isClientSide();
   }

   @Override
   public boolean stillValid(final Player player) {
      return this.source == player;
   }

   @Override
   public int getVillagerXp() {
      return this.xp;
   }

   @Override
   public void overrideXp(final int xp) {
      this.xp = xp;
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
