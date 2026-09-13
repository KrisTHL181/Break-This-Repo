package net.minecraft.data.worldgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface BootstrapContextAccess {
   <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key);

   @Deprecated
   <S> Stream<Holder.Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key);
}
