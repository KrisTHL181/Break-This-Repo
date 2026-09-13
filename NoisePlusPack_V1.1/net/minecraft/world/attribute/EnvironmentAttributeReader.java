package net.minecraft.world.attribute;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface EnvironmentAttributeReader {
   EnvironmentAttributeReader EMPTY = new EnvironmentAttributeReader() {
      @Override
      public <Value> Value getDimensionValue(EnvironmentAttribute<Value> p_450947_) {
         return p_450947_.defaultValue();
      }

      @Override
      public <Value> Value getValue(EnvironmentAttribute<Value> p_456179_, Vec3 p_454439_, @Nullable SpatialAttributeInterpolator p_455486_) {
         return p_456179_.defaultValue();
      }
   };

   <Value> Value getDimensionValue(EnvironmentAttribute<Value> var1);

   default <Value> Value getValue(EnvironmentAttribute<Value> p_453225_, BlockPos p_454314_) {
      return this.getValue(p_453225_, Vec3.atCenterOf(p_454314_));
   }

   default <Value> Value getValue(EnvironmentAttribute<Value> p_454259_, Vec3 p_458543_) {
      return this.getValue(p_454259_, p_458543_, null);
   }

   <Value> Value getValue(EnvironmentAttribute<Value> var1, Vec3 var2, @Nullable SpatialAttributeInterpolator var3);
}
