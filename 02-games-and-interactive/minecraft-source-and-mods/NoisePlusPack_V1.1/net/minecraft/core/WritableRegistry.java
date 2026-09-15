package net.minecraft.core;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface WritableRegistry<T> extends Registry<T> {
   Holder.Reference<T> register(ResourceKey<T> var1, T var2, RegistrationInfo var3);

   void bindTag(TagKey<T> var1, List<Holder<T>> var2);

   boolean isEmpty();

   HolderGetter<T> createRegistrationLookup();
}
