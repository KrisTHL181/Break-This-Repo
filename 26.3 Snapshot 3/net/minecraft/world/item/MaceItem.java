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

   public MaceItem(final Item.Properties properties) {
      super(properties);
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
   public void hurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
      if (canSmashAttack(attacker)) {
         ServerLevel level = (ServerLevel)attacker.level();
         attacker.setDeltaMovement(attacker.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
         attacker.setIgnoreFallDamageFromCurrentImpulse(true, this.calculateImpactPosition(attacker));
         if (attacker instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
         }

         if (mob.onGround()) {
            if (attacker instanceof ServerPlayer player) {
               player.setSpawnExtraParticlesOnFall(true);
            }

            SoundEvent sound = attacker.fallDistance > 5.0 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), sound, attacker.getSoundSource(), 1.0F, 1.0F);
         } else {
            level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.MACE_SMASH_AIR, attacker.getSoundSource(), 1.0F, 1.0F);
         }

         knockback(level, attacker, mob);
      }
   }

   private Vec3 calculateImpactPosition(final LivingEntity attacker) {
      return attacker.isIgnoringFallDamageFromCurrentImpulse() && attacker.currentImpulseImpactPos.y <= attacker.position().y
         ? attacker.currentImpulseImpactPos
         : attacker.position();
   }

   @Override
   public void postHurtEnemy(final ItemStack itemStack, final LivingEntity mob, final LivingEntity attacker) {
      if (canSmashAttack(attacker)) {
         attacker.resetFallDistance();
      }
   }

   @Override
   public float getAttackDamageBonus(final Entity victim, final float ignoredDamage, final DamageSource damageSource) {
      if (damageSource.getDirectEntity() instanceof LivingEntity attacker) {
         if (!canSmashAttack(attacker)) {
            return 0.0F;
         }

         double fallHeightThreshold1 = 3.0;
         double fallHeightThreshold2 = 8.0;
         double fallDistance = attacker.fallDistance;
         double damage;
         if (fallDistance <= 3.0) {
            damage = 4.0 * fallDistance;
         } else if (fallDistance <= 8.0) {
            damage = 12.0 + 2.0 * (fallDistance - 3.0);
         } else {
            damage = 22.0 + fallDistance - 8.0;
         }

         return attacker.level() instanceof ServerLevel level
            ? (float)(damage + EnchantmentHelper.modifyFallBasedDamage(level, attacker.getWeaponItem(), victim, damageSource, 0.0F) * fallDistance)
            : (float)damage;
      } else {
         return 0.0F;
      }
   }

   private static void knockback(final Level level, final Entity attacker, final Entity entity) {
      level.levelEvent(2013, entity.getOnPos(), 750);
      level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.5), knockbackPredicate(attacker, entity)).forEach(nearby -> {
         Vec3 direction = nearby.position().subtract(entity.position());
         double knockbackPower = getKnockbackPower(attacker, nearby, direction);
         Vec3 knockbackVector = direction.normalize().scale(knockbackPower);
         if (knockbackPower > 0.0) {
            nearby.push(knockbackVector.x, 0.7F, knockbackVector.z);
            if (nearby instanceof ServerPlayer otherPlayer) {
               otherPlayer.connection.send(new ClientboundSetEntityMotionPacket(otherPlayer));
            }
         }
      });
   }

   private static Predicate<LivingEntity> knockbackPredicate(final Entity attacker, final Entity entity) {
      return nearby -> {
         boolean notSpectator = !nearby.isSpectator();
         boolean notPlayer = nearby != attacker && nearby != entity;
         boolean notAlliedToPlayer = !attacker.isAlliedTo(nearby);
         boolean notTamedByPlayer = !(
            nearby instanceof TamableAnimal animal && entity instanceof LivingEntity livingAttacker && animal.isTame() && animal.isOwnedBy(livingAttacker)
         );
         boolean notArmorStand = !(nearby instanceof ArmorStand armorStand && armorStand.isMarker());
         boolean withinRange = entity.distanceToSqr(nearby) <= Math.pow(3.5, 2.0);
         boolean notFlyingInCreative = !(nearby instanceof Player player && player.isCreative() && player.getAbilities().flying);
         return notSpectator && notPlayer && notAlliedToPlayer && notTamedByPlayer && notArmorStand && withinRange && notFlyingInCreative;
      };
   }

   private static double getKnockbackPower(final Entity attacker, final LivingEntity nearby, final Vec3 direction) {
      return (3.5 - direction.length()) * 0.7F * (attacker.fallDistance > 5.0 ? 2 : 1) * (1.0 - nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
   }

   public static boolean canSmashAttack(final LivingEntity attacker) {
      return attacker.fallDistance > 1.5 && !attacker.isFallFlying();
   }

   @Override
   public @Nullable DamageSource getItemDamageSource(final LivingEntity attacker) {
      return canSmashAttack(attacker) ? attacker.damageSources().mace(attacker) : super.getItemDamageSource(attacker);
   }
}
