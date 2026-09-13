package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class BowItem extends ProjectileWeaponItem {
   public static final int MAX_DRAW_DURATION = 20;
   public static final int DEFAULT_RANGE = 15;

   public BowItem(Item.Properties p_40660_) {
      super(p_40660_);
   }

   @Override
   public boolean releaseUsing(ItemStack p_40667_, Level p_40668_, LivingEntity p_40669_, int p_40670_) {
      if (p_40669_ instanceof Player player) {
         ItemStack itemstack = player.getProjectile(p_40667_);
         if (itemstack.isEmpty()) {
            return false;
         }

         int i = this.getUseDuration(p_40667_, p_40669_) - p_40670_;
         float f = getPowerForTime(i);
         if (f < 0.1) {
            return false;
         }

         List<ItemStack> list = draw(p_40667_, itemstack, player);
         if (p_40668_ instanceof ServerLevel serverlevel && !list.isEmpty()) {
            this.shoot(serverlevel, player, player.getUsedItemHand(), p_40667_, list, f * 3.0F, 1.0F, f == 1.0F, null);
         }

         p_40668_.playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            1.0F,
            1.0F / (p_40668_.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F
         );
         player.awardStat(Stats.ITEM_USED.get(this));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void shootProjectile(
      LivingEntity p_329327_, Projectile p_335269_, int p_331005_, float p_332731_, float p_332848_, float p_332058_, @Nullable LivingEntity p_335061_
   ) {
      p_335269_.shootFromRotation(p_329327_, p_329327_.getXRot(), p_329327_.getYRot() + p_332058_, 0.0F, p_332731_, p_332848_);
   }

   public static float getPowerForTime(int p_40662_) {
      float f = p_40662_ / 20.0F;
      f = (f * f + f * 2.0F) / 3.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      return f;
   }

   @Override
   public int getUseDuration(ItemStack p_40680_, LivingEntity p_344246_) {
      return 72000;
   }

   @Override
   public ItemUseAnimation getUseAnimation(ItemStack p_40678_) {
      return ItemUseAnimation.BOW;
   }

   @Override
   public InteractionResult use(Level p_40672_, Player p_40673_, InteractionHand p_40674_) {
      ItemStack itemstack = p_40673_.getItemInHand(p_40674_);
      boolean flag = !p_40673_.getProjectile(itemstack).isEmpty();
      if (!p_40673_.hasInfiniteMaterials() && !flag) {
         return InteractionResult.FAIL;
      }

      p_40673_.startUsingItem(p_40674_);
      return InteractionResult.CONSUME;
   }

   @Override
   public Predicate<ItemStack> getAllSupportedProjectiles() {
      return ARROW_ONLY;
   }

   @Override
   public int getDefaultProjectileRange() {
      return 15;
   }
}
