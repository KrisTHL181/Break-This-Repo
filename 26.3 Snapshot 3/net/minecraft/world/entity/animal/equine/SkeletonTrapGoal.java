package net.minecraft.world.entity.animal.equine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import org.jspecify.annotations.Nullable;

public class SkeletonTrapGoal extends Goal {
   private final SkeletonHorse horse;

   public SkeletonTrapGoal(final SkeletonHorse horse) {
      this.horse = horse;
   }

   @Override
   public boolean canUse() {
      return this.horse.level().hasNearbyAlivePlayer(this.horse.getX(), this.horse.getY(), this.horse.getZ(), 10.0);
   }

   @Override
   public void tick() {
      ServerLevel level = (ServerLevel)this.horse.level();
      DifficultyInstance difficulty = level.getCurrentDifficultyAt(this.horse.blockPosition());
      this.horse.setTrap(false);
      this.horse.setTamed(true);
      this.horse.setAge(0);
      LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
      if (bolt != null) {
         bolt.snapTo(this.horse.getX(), this.horse.getY(), this.horse.getZ());
         bolt.setVisualOnly(true);
         level.addFreshEntity(bolt);
         Skeleton skeleton = this.createSkeleton(difficulty, this.horse);
         if (skeleton != null) {
            skeleton.startRiding(this.horse);
            level.addFreshEntityWithPassengers(skeleton);

            for (int i = 0; i < 3; i++) {
               AbstractHorse otherHorse = this.createHorse(difficulty);
               if (otherHorse != null) {
                  Skeleton otherSkeleton = this.createSkeleton(difficulty, otherHorse);
                  if (otherSkeleton != null) {
                     otherSkeleton.startRiding(otherHorse);
                     otherHorse.push(this.horse.getRandom().triangle(0.0, 1.1485), 0.0, this.horse.getRandom().triangle(0.0, 1.1485));
                     level.addFreshEntityWithPassengers(otherHorse);
                  }
               }
            }
         }
      }
   }

   private @Nullable AbstractHorse createHorse(final DifficultyInstance difficulty) {
      SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(this.horse.level(), EntitySpawnReason.TRIGGERED);
      if (horse != null) {
         horse.finalizeSpawn((ServerLevel)this.horse.level(), difficulty, EntitySpawnReason.TRIGGERED, null);
         horse.setPos(this.horse.getX(), this.horse.getY(), this.horse.getZ());
         horse.invulnerableTime = 60;
         horse.setPersistenceRequired();
         horse.setTamed(true);
         horse.setAge(0);
      }

      return horse;
   }

   private @Nullable Skeleton createSkeleton(final DifficultyInstance difficulty, final AbstractHorse horse) {
      Skeleton skeleton = EntityTypes.SKELETON.create(horse.level(), EntitySpawnReason.TRIGGERED);
      if (skeleton != null) {
         skeleton.finalizeSpawn((ServerLevel)horse.level(), difficulty, EntitySpawnReason.TRIGGERED, null);
         skeleton.setPos(horse.getX(), horse.getY(), horse.getZ());
         skeleton.invulnerableTime = 60;
         skeleton.setPersistenceRequired();
         if (skeleton.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            skeleton.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
         }

         this.enchant(skeleton, EquipmentSlot.MAINHAND, difficulty);
         this.enchant(skeleton, EquipmentSlot.HEAD, difficulty);
      }

      return skeleton;
   }

   private void enchant(final Skeleton skeleton, final EquipmentSlot slot, final DifficultyInstance difficulty) {
      ItemStack stack = skeleton.getItemBySlot(slot);
      stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
      EnchantmentHelper.enchantItemFromProvider(
         stack, skeleton.level().registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, skeleton.getRandom()
      );
      skeleton.setItemSlot(slot, stack);
   }
}
