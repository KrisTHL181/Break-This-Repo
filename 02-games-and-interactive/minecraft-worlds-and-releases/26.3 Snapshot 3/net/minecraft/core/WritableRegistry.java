package net.minecraft.core;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface WritableRegistry<T> extends Registry<T> {
   Holder.Reference<T> register(ResourceKey<T> key, T value, RegistrationInfo registrationInfo);

   void bindTags(Map<TagKey<T>, List<Holder<T>>> pendingTags);

   boolean isEmpty();

   HolderGetter<T> createRegistrationLookup();
}
