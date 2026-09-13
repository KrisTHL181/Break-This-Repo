package net.minecraft.world.item;

import java.util.OptionalInt;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

public interface ProjectileItem {
   Projectile asProjectile(Level var1, Position var2, ItemStack var3, Direction var4);

   default ProjectileItem.DispenseConfig createDispenseConfig() {
      return ProjectileItem.DispenseConfig.DEFAULT;
   }

   default void shoot(Projectile p_328685_, double p_328692_, double p_328907_, double p_334180_, float p_333007_, float p_331671_) {
      p_328685_.shoot(p_328692_, p_328907_, p_334180_, p_333007_, p_331671_);
   }

   record DispenseConfig(ProjectileItem.PositionFunction positionFunction, float uncertainty, float power, OptionalInt overrideDispenseEvent) {
      public static final ProjectileItem.DispenseConfig DEFAULT = builder().build();

      public static ProjectileItem.DispenseConfig.Builder builder() {
         return new ProjectileItem.DispenseConfig.Builder();
      }

      public static class Builder {
         private ProjectileItem.PositionFunction positionFunction = (p_331972_, p_327694_) -> DispenserBlock.getDispensePosition(
            p_331972_, 0.7, new Vec3(0.0, 0.1, 0.0)
         );
         private float uncertainty = 6.0F;
         private float power = 1.1F;
         private OptionalInt overrideDispenseEvent = OptionalInt.empty();

         public ProjectileItem.DispenseConfig.Builder positionFunction(ProjectileItem.PositionFunction p_328427_) {
            this.positionFunction = p_328427_;
            return this;
         }

         public ProjectileItem.DispenseConfig.Builder uncertainty(float p_328001_) {
            this.uncertainty = p_328001_;
            return this;
         }

         public ProjectileItem.DispenseConfig.Builder power(float p_334376_) {
            this.power = p_334376_;
            return this;
         }

         public ProjectileItem.DispenseConfig.Builder overrideDispenseEvent(int p_331932_) {
            this.overrideDispenseEvent = OptionalInt.of(p_331932_);
            return this;
         }

         public ProjectileItem.DispenseConfig build() {
            return new ProjectileItem.DispenseConfig(this.positionFunction, this.uncertainty, this.power, this.overrideDispenseEvent);
         }
      }
   }

   @FunctionalInterface
   interface PositionFunction {
      Position getDispensePosition(BlockSource var1, Direction var2);
   }
}
