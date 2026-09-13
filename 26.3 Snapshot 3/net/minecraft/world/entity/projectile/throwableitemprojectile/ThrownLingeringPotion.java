package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownLingeringPotion extends AbstractThrownPotion {
   public ThrownLingeringPotion(final EntityType<? extends ThrownLingeringPotion> type, final Level level) {
      super(type, level);
   }

   public ThrownLingeringPotion(final Level level, final LivingEntity owner, final ItemStack itemStack) {
      super(EntityTypes.LINGERING_POTION, level, owner, itemStack);
   }

   public ThrownLingeringPotion(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
      super(EntityTypes.LINGERING_POTION, level, x, y, z, itemStack);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.LINGERING_POTION;
   }

   @Override
   public void onHitAsPotion(final ServerLevel level, final ItemStack potionItem, final HitResult hitResult) {
      AreaEffectCloud cloud;
      if (hitResult instanceof EntityHitResult entityHitResult) {
         cloud = new AreaEffectCloud(this.level(), entityHitResult.getEntity().getX(), entityHitResult.getEntity().getY(), entityHitResult.getEntity().getZ());
      } else {
         cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
      }

      if (this.getOwner() instanceof LivingEntity livingEntity) {
         cloud.setOwner(livingEntity);
      }

      cloud.setRadius(3.0F);
      cloud.setRadiusOnUse(-0.5F);
      cloud.setDuration(600);
      cloud.setWaitTime(10);
      cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
      cloud.applyComponentsFromItemStack(potionItem);
      level.addFreshEntity(cloud);
   }
}
