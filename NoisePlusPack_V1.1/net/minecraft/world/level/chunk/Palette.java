package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public interface Palette<T> {
   int idFor(T var1, PaletteResize<T> var2);

   boolean maybeHas(Predicate<T> var1);

   T valueFor(int var1);

   void read(FriendlyByteBuf var1, IdMap<T> var2);

   void write(FriendlyByteBuf var1, IdMap<T> var2);

   int getSerializedSize(IdMap<T> var1);

   int getSize();

   Palette<T> copy();

   interface Factory {
      <A> Palette<A> create(int var1, List<A> var2);
   }
}
