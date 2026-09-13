package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;

public interface ValueInput {
   <T> Optional<T> read(String var1, Codec<T> var2);

   @Deprecated
   <T> Optional<T> read(MapCodec<T> var1);

   Optional<ValueInput> child(String var1);

   ValueInput childOrEmpty(String var1);

   Optional<ValueInput.ValueInputList> childrenList(String var1);

   ValueInput.ValueInputList childrenListOrEmpty(String var1);

   <T> Optional<ValueInput.TypedInputList<T>> list(String var1, Codec<T> var2);

   <T> ValueInput.TypedInputList<T> listOrEmpty(String var1, Codec<T> var2);

   boolean getBooleanOr(String var1, boolean var2);

   byte getByteOr(String var1, byte var2);

   int getShortOr(String var1, short var2);

   Optional<Integer> getInt(String var1);

   int getIntOr(String var1, int var2);

   long getLongOr(String var1, long var2);

   Optional<Long> getLong(String var1);

   float getFloatOr(String var1, float var2);

   double getDoubleOr(String var1, double var2);

   Optional<String> getString(String var1);

   String getStringOr(String var1, String var2);

   Optional<int[]> getIntArray(String var1);

   @Deprecated
   HolderLookup.Provider lookup();

   interface TypedInputList<T> extends Iterable<T> {
      boolean isEmpty();

      Stream<T> stream();
   }

   interface ValueInputList extends Iterable<ValueInput> {
      boolean isEmpty();

      Stream<ValueInput> stream();
   }
}
