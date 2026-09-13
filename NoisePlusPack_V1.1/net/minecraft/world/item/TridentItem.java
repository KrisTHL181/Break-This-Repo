package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TridentItem extends Item implements ProjectileItem {
   public static final int THROW_THRESHOLD_TIME = 10;
   public static final float BASE_DAMAGE = 8.0F;
   public static final float PROJECTILE_SHOOT_POWER = 2.5F;

   public TridentItem(Item.Properties p_43381_) {
      super(p_43381_);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 8.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.9F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
   }

   public static Tool createToolProperties() {
      return new Tool(List.of(), 1.0F, 2, false);
   }

   @Override
   public ItemUseAnimation getUseAnimation(ItemStack p_43417_) {
      return ItemUseAnimation.TRIDENT;
   }

   @Override
   public int getUseDuration(ItemStack p_43419_, LivingEntity p_344216_) {
      return 72000;
   }

   @Override
   public boolean releaseUsing(ItemStack p_43394_, Level p_43395_, LivingEntity p_43396_, int p_43397_) {
      if (p_43396_ instanceof Player player) {
         int i = this.getUseDuration(p_43394_, p_43396_) - p_43397_;
         if (i < 10) {
            return false;
         }

         float f = EnchantmentHelper.getTridentSpinAttackStrength(p_43394_, player);
         if (f > 0.0F && !player.isInWaterOrRain()) {
            return false;
         }

         if (p_43394_.nextDamageWillBreak()) {
            return false;
         }

         Holder<SoundEvent> holder = EnchantmentHelper.pickHighestLevel(p_43394_, EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
         player.awardStat(Stats.ITEM_USED.get(this));
         if (p_43395_ instanceof ServerLevel serverlevel) {
            p_43394_.hurtWithoutBreaking(1, player);
            if (f == 0.0F) {
               ItemStack itemstack = p_43394_.consumeAndReturn(1, player);
               ThrownTrident throwntrident = Projectile.spawnProjectileFromRotation(ThrownTrident::new, serverlevel, itemstack, player, 0.0F, 2.5F, 1.0F);
               if (player.hasInfiniteMaterials()) {
                  throwntrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
               }

               p_43395_.playSound(null, throwntrident, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
               return true;
            }
         }

         if (f > 0.0F) {
            float f7 = player.getYRot();
            float f1 = player.getXRot();
            float f2 = -Mth.sin(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
            float f3 = -Mth.sin(f1 * (float) (Math.PI / 180.0));
            float f4 = Mth.cos(f7 * (float) (Math.PI / 180.0)) * Mth.cos(f1 * (float) (Math.PI / 180.0));
            float f5 = Mth.sqrt(f2 * f2 + f3 * f3 + f4 * f4);
            f2 *= f / f5;
            f3 *= f / f5;
            f4 *= f / f5;
            player.push(f2, f3, f4);
            player.startAutoSpinAttack(20, 8.0F, p_43394_);
            if (player.onGround()) {
               float f6 = 1.1999999F;
               player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
            }

            p_43395_.playSound(null, player, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public InteractionResult use(Level p_43405_, Player p_43406_, InteractionHand p_43407_) {
      ItemStack itemstack = p_43406_.getItemInHand(p_43407_);
      if (itemstack.nextDamageWillBreak()) {
         return InteractionResult.FAIL;
      }

      if (EnchantmentHelper.getTridentSpinAttackStrength(itemstack, p_43406_) > 0.0F && !p_43406_.isInWaterOrRain()) {
         return InteractionResult.FAIL;
      }

      p_43406_.startUsingItem(p_43407_);
      return InteractionResult.CONSUME;
   }

   @Override
   public Projectile asProjectile(Level p_330065_, Position p_333679_, ItemStack p_332911_, Direction p_333212_) {
      ThrownTrident throwntrident = new ThrownTrident(p_330065_, p_333679_.x(), p_333679_.y(), p_333679_.z(), p_332911_.copyWithCount(1));
      throwntrident.pickup = AbstractArrow.Pickup.ALLOWED;
      return throwntrident;
   }
}
