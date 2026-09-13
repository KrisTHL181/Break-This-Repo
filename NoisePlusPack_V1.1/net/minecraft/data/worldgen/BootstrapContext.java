package net.minecraft.data.worldgen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface BootstrapContext<T> {
   Holder.Reference<T> register(ResourceKey<T> var1, T var2, Lifecycle var3);

   default Holder.Reference<T> register(ResourceKey<T> p_333375_, T p_328645_) {
      return this.register(p_333375_, p_328645_, Lifecycle.stable());
   }

   <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> var1);
}
