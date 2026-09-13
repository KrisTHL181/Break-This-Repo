package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface VariantMutator extends UnaryOperator<Variant> {
   VariantMutator.VariantProperty<Quadrant> X_ROT = Variant::withXRot;
   VariantMutator.VariantProperty<Quadrant> Y_ROT = Variant::withYRot;
   VariantMutator.VariantProperty<Quadrant> Z_ROT = Variant::withZRot;
   VariantMutator.VariantProperty<Identifier> MODEL = Variant::withModel;
   VariantMutator.VariantProperty<Boolean> UV_LOCK = Variant::withUvLock;

   default VariantMutator then(VariantMutator p_395828_) {
      return p_397265_ -> p_395828_.apply(this.apply(p_397265_));
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface VariantProperty<T> {
      Variant apply(Variant var1, T var2);

      default VariantMutator withValue(T p_393997_) {
         return p_393814_ -> this.apply(p_393814_, p_393997_);
      }
   }
}
