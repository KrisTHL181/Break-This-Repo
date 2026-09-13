package net.minecraft.world.entity.projectile.throwableitemprojectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class ThrownLingeringPotion extends AbstractThrownPotion {
   public ThrownLingeringPotion(EntityType<? extends ThrownLingeringPotion> p_460856_, Level p_460642_) {
      super(p_460856_, p_460642_);
   }

   public ThrownLingeringPotion(Level p_460136_, LivingEntity p_457042_, ItemStack p_457342_) {
      super(EntityType.LINGERING_POTION, p_460136_, p_457042_, p_457342_);
   }

   public ThrownLingeringPotion(Level p_451698_, double p_452904_, double p_458094_, double p_450411_, ItemStack p_453311_) {
      super(EntityType.LINGERING_POTION, p_451698_, p_452904_, p_458094_, p_450411_, p_453311_);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.LINGERING_POTION;
   }

   @Override
   public void onHitAsPotion(ServerLevel p_452872_, ItemStack p_459089_, HitResult p_457248_) {
      AreaEffectCloud areaeffectcloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
      if (this.getOwner() instanceof LivingEntity livingentity) {
         areaeffectcloud.setOwner(livingentity);
      }

      areaeffectcloud.setRadius(3.0F);
      areaeffectcloud.setRadiusOnUse(-0.5F);
      areaeffectcloud.setDuration(600);
      areaeffectcloud.setWaitTime(10);
      areaeffectcloud.setRadiusPerTick(-areaeffectcloud.getRadius() / areaeffectcloud.getDuration());
      areaeffectcloud.applyComponentsFromItemStack(p_459089_);
      p_452872_.addFreshEntity(areaeffectcloud);
   }
}
