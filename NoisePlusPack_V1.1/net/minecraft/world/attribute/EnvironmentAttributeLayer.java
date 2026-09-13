package net.minecraft.world.attribute;

import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public sealed interface EnvironmentAttributeLayer<Value>
   permits EnvironmentAttributeLayer.Constant,
   EnvironmentAttributeLayer.TimeBased,
   EnvironmentAttributeLayer.Positional {
   @FunctionalInterface
   non-sealed interface Constant<Value> extends EnvironmentAttributeLayer<Value> {
      Value applyConstant(Value var1);
   }

   @FunctionalInterface
   non-sealed interface Positional<Value> extends EnvironmentAttributeLayer<Value> {
      Value applyPositional(Value var1, Vec3 var2, @Nullable SpatialAttributeInterpolator var3);
   }

   @FunctionalInterface
   non-sealed interface TimeBased<Value> extends EnvironmentAttributeLayer<Value> {
      Value applyTimeBased(Value var1, int var2);
   }
}
