package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MaceItem extends Item {
   private static final int DEFAULT_ATTACK_DAMAGE = 5;
   private static final float DEFAULT_ATTACK_SPEED = -3.4F;
   public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5F;
   private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0F;
   public static final float SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5F;
   private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7F;

   public MaceItem(Item.Properties p_329217_) {
      super(p_329217_);
   }

   public static ItemAttributeModifiers createAttributes() {
      return ItemAttributeModifiers.builder()
         .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
         .build();
   }

   public static Tool createToolProperties() {
      return new Tool(List.of(), 1.0F, 2, false);
   }

   @Override
   public void hurtEnemy(ItemStack p_329476_, LivingEntity p_332492_, LivingEntity p_333391_) {
      if (canSmashAttack(p_333391_)) {
         ServerLevel serverlevel = (ServerLevel)p_333391_.level();
         p_333391_.setDeltaMovement(p_333391_.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
         if (p_333391_ instanceof ServerPlayer serverplayer) {
            serverplayer.currentImpulseImpactPos = this.calculateImpactPosition(serverplayer);
            serverplayer.setIgnoreFallDamageFromCurrentImpulse(true);
            serverplayer.connection.send(new ClientboundSetEntityMotionPacket(serverplayer));
         }

         if (p_332492_.onGround()) {
            if (p_333391_ instanceof ServerPlayer serverplayer1) {
               serverplayer1.setSpawnExtraParticlesOnFall(true);
            }

            SoundEvent soundevent = p_333391_.fallDistance > 5.0 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            serverlevel.playSound(null, p_333391_.getX(), p_333391_.getY(), p_333391_.getZ(), soundevent, p_333391_.getSoundSource(), 1.0F, 1.0F);
         } else {
            serverlevel.playSound(
               null, p_333391_.getX(), p_333391_.getY(), p_333391_.getZ(), SoundEvents.MACE_SMASH_AIR, p_333391_.getSoundSource(), 1.0F, 1.0F
            );
         }

         knockback(serverlevel, p_333391_, p_332492_);
      }
   }

   private Vec3 calculateImpactPosition(ServerPlayer p_365711_) {
      return p_365711_.isIgnoringFallDamageFromCurrentImpulse()
            && p_365711_.currentImpulseImpactPos != null
            && p_365711_.currentImpulseImpactPos.y <= p_365711_.position().y
         ? p_365711_.currentImpulseImpactPos
         : p_365711_.position();
   }

   @Override
   public void postHurtEnemy(ItemStack p_344750_, LivingEntity p_344000_, LivingEntity p_342605_) {
      if (canSmashAttack(p_342605_)) {
         p_342605_.resetFallDistance();
      }
   }

   @Override
   public float getAttackDamageBonus(Entity p_344513_, float p_333106_, DamageSource p_345351_) {
      if (p_345351_.getDirectEntity() instanceof LivingEntity livingentity) {
         if (!canSmashAttack(livingentity)) {
            return 0.0F;
         }

         double d3 = 3.0;
         double d0 = 8.0;
         double d1 = livingentity.fallDistance;
         double d2;
         if (d1 <= 3.0) {
            d2 = 4.0 * d1;
         } else if (d1 <= 8.0) {
            d2 = 12.0 + 2.0 * (d1 - 3.0);
         } else {
            d2 = 22.0 + d1 - 8.0;
         }

         return livingentity.level() instanceof ServerLevel serverlevel
            ? (float)(d2 + EnchantmentHelper.modifyFallBasedDamage(serverlevel, livingentity.getWeaponItem(), p_344513_, p_345351_, 0.0F) * d1)
            : (float)d2;
      } else {
         return 0.0F;
      }
   }

   private static void knockback(Level p_332228_, Entity p_335011_, Entity p_364373_) {
      p_332228_.levelEvent(2013, p_364373_.getOnPos(), 750);
      p_332228_.getEntitiesOfClass(LivingEntity.class, p_364373_.getBoundingBox().inflate(3.5), knockbackPredicate(p_335011_, p_364373_))
         .forEach(p_341573_ -> {
            Vec3 vec3 = p_341573_.position().subtract(p_364373_.position());
            double d0 = getKnockbackPower(p_335011_, p_341573_, vec3);
            Vec3 vec31 = vec3.normalize().scale(d0);
            if (d0 > 0.0) {
               p_341573_.push(vec31.x, 0.7F, vec31.z);
               if (p_341573_ instanceof ServerPlayer serverplayer) {
                  serverplayer.connection.send(new ClientboundSetEntityMotionPacket(serverplayer));
               }
            }
         });
   }

   private static Predicate<LivingEntity> knockbackPredicate(Entity p_334480_, Entity p_363363_) {
      return p_449807_ -> {
         boolean flag = !p_449807_.isSpectator();
         boolean flag1 = p_449807_ != p_334480_ && p_449807_ != p_363363_;
         boolean flag2 = !p_334480_.isAlliedTo(p_449807_);
         boolean flag3 = !(
            p_449807_ instanceof TamableAnimal tamableanimal
               && p_363363_ instanceof LivingEntity livingentity
               && tamableanimal.isTame()
               && tamableanimal.isOwnedBy(livingentity)
         );
         boolean flag5 = !(p_449807_ instanceof ArmorStand armorstand && armorstand.isMarker());
         boolean flag6 = p_363363_.distanceToSqr(p_449807_) <= Math.pow(3.5, 2.0);
         boolean flag4 = !(p_449807_ instanceof Player player && player.isCreative() && player.getAbilities().flying);
         return flag && flag1 && flag2 && flag3 && flag5 && flag6 && flag4;
      };
   }

   private static double getKnockbackPower(Entity p_361841_, LivingEntity p_334129_, Vec3 p_335583_) {
      return (3.5 - p_335583_.length()) * 0.7F * (p_361841_.fallDistance > 5.0 ? 2 : 1) * (1.0 - p_334129_.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
   }

   public static boolean canSmashAttack(LivingEntity p_345213_) {
      return p_345213_.fallDistance > 1.5 && !p_345213_.isFallFlying();
   }

   @Override
   public @Nullable DamageSource getItemDamageSource(LivingEntity p_450398_) {
      return canSmashAttack(p_450398_) ? p_450398_.damageSources().mace(p_450398_) : super.getItemDamageSource(p_450398_);
   }
}
