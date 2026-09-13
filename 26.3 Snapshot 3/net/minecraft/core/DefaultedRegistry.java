package net.minecraft.core;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface DefaultedRegistry<T> extends Registry<T> {
   @Override
   @NonNull Identifier getKey(T thing);

   @Override
   @NonNull T getValue(@Nullable Identifier key);

   @Override
   @NonNull T byId(int id);

   Identifier getDefaultKey();
}
