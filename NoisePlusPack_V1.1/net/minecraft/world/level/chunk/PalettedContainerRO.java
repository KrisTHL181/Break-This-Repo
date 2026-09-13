package net.minecraft.world.level.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.network.FriendlyByteBuf;

public interface PalettedContainerRO<T> {
   T get(int var1, int var2, int var3);

   void getAll(Consumer<T> var1);

   void write(FriendlyByteBuf var1);

   int getSerializedSize();

   @VisibleForTesting
   int bitsPerEntry();

   boolean maybeHas(Predicate<T> var1);

   void count(PalettedContainer.CountConsumer<T> var1);

   PalettedContainer<T> copy();

   PalettedContainer<T> recreate();

   PalettedContainerRO.PackedData<T> pack(Strategy<T> var1);

   record PackedData<T>(List<T> paletteEntries, Optional<LongStream> storage, int bitsPerEntry) {
      public static final int UNKNOWN_BITS_PER_ENTRY = -1;

      public PackedData(List<T> p_238381_, Optional<LongStream> p_238382_) {
         this(p_238381_, p_238382_, -1);
      }
   }

   interface Unpacker<T, C extends PalettedContainerRO<T>> {
      DataResult<C> read(Strategy<T> var1, PalettedContainerRO.PackedData<T> var2);
   }
}
