package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public class GlobalPalette<T> implements Palette<T> {
   private final IdMap<T> registry;

   public GlobalPalette(final IdMap<T> registry) {
      this.registry = registry;
   }

   @Override
   public int idFor(final T value, final PaletteResize<T> resizeHandler) {
      int id = this.registry.getId(value);
      return id == -1 ? 0 : id;
   }

   @Override
   public boolean maybeHas(final Predicate<T> predicate) {
      return true;
   }

   @Override
   public T valueFor(final int index) {
      T value = this.registry.byId(index);
      if (value == null) {
         throw new MissingPaletteEntryException(index);
      } else {
         return value;
      }
   }

   @Override
   public void read(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
   }

   @Override
   public void write(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
   }

   @Override
   public int getSerializedSize(final IdMap<T> globalMap) {
      return 0;
   }

   @Override
   public int getSize() {
      return this.registry.size();
   }

   @Override
   public Palette<T> copy() {
      return this;
   }
}
