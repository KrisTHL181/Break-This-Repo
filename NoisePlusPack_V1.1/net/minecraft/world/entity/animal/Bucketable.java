package net.minecraft.world.entity.animal;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public interface Bucketable {
   boolean fromBucket();

   void setFromBucket(boolean var1);

   void saveToBucketTag(ItemStack var1);

   void loadFromBucketTag(CompoundTag var1);

   ItemStack getBucketItemStack();

   SoundEvent getPickupSound();

   @Deprecated
   static void saveDefaultDataToBucketTag(Mob p_148823_, ItemStack p_148824_) {
      p_148824_.copyFrom(DataComponents.CUSTOM_NAME, p_148823_);
      CustomData.update(DataComponents.BUCKET_ENTITY_DATA, p_148824_, p_335779_ -> {
         if (p_148823_.isNoAi()) {
            p_335779_.putBoolean("NoAI", p_148823_.isNoAi());
         }

         if (p_148823_.isSilent()) {
            p_335779_.putBoolean("Silent", p_148823_.isSilent());
         }

         if (p_148823_.isNoGravity()) {
            p_335779_.putBoolean("NoGravity", p_148823_.isNoGravity());
         }

         if (p_148823_.hasGlowingTag()) {
            p_335779_.putBoolean("Glowing", p_148823_.hasGlowingTag());
         }

         if (p_148823_.isInvulnerable()) {
            p_335779_.putBoolean("Invulnerable", p_148823_.isInvulnerable());
         }

         p_335779_.putFloat("Health", p_148823_.getHealth());
      });
   }

   @Deprecated
   static void loadDefaultDataFromBucketTag(Mob p_148826_, CompoundTag p_148827_) {
      p_148827_.getBoolean("NoAI").ifPresent(p_148826_::setNoAi);
      p_148827_.getBoolean("Silent").ifPresent(p_148826_::setSilent);
      p_148827_.getBoolean("NoGravity").ifPresent(p_148826_::setNoGravity);
      p_148827_.getBoolean("Glowing").ifPresent(p_148826_::setGlowingTag);
      p_148827_.getBoolean("Invulnerable").ifPresent(p_148826_::setInvulnerable);
      p_148827_.getFloat("Health").ifPresent(p_148826_::setHealth);
   }

   static <T extends LivingEntity & Bucketable> Optional<InteractionResult> bucketMobPickup(Player p_148829_, InteractionHand p_148830_, T p_148831_) {
      ItemStack itemstack = p_148829_.getItemInHand(p_148830_);
      if (itemstack.getItem() == Items.WATER_BUCKET && p_148831_.isAlive()) {
         p_148831_.playSound(p_148831_.getPickupSound(), 1.0F, 1.0F);
         ItemStack itemstack1 = p_148831_.getBucketItemStack();
         p_148831_.saveToBucketTag(itemstack1);
         ItemStack itemstack2 = ItemUtils.createFilledResult(itemstack, p_148829_, itemstack1, false);
         p_148829_.setItemInHand(p_148830_, itemstack2);
         Level level = p_148831_.level();
         if (!level.isClientSide()) {
            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)p_148829_, itemstack1);
         }

         p_148831_.discard();
         return Optional.of(InteractionResult.SUCCESS);
      } else {
         return Optional.empty();
      }
   }
}
