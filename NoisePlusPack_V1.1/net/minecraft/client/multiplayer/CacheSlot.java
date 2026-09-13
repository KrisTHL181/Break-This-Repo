package net.minecraft.client.multiplayer;

import java.util.function.Function;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
   private final Function<C, D> operation;
   private @Nullable C context;
   private @Nullable D value;

   public CacheSlot(Function<C, D> p_395525_) {
      this.operation = p_395525_;
   }

   public D compute(C p_393567_) {
      if (p_393567_ == this.context && this.value != null) {
         return this.value;
      }

      D d = this.operation.apply(p_393567_);
      this.value = d;
      this.context = p_393567_;
      p_393567_.registerForCleaning(this);
      return d;
   }

   public void clear() {
      this.value = null;
      this.context = null;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
      void registerForCleaning(CacheSlot<C, ?> var1);
   }
}
