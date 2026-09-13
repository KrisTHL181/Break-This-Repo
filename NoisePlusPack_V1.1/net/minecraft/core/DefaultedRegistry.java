package net.minecraft.core;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface DefaultedRegistry<T> extends Registry<T> {
   @Override
   @NonNull Identifier getKey(T var1);

   @Override
   @NonNull T getValue(@Nullable Identifier var1);

   @Override
   @NonNull T byId(int var1);

   Identifier getDefaultKey();
}
