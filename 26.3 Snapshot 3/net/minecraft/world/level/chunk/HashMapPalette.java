package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
   private final CrudeIncrementalIntIdentityHashBiMap<T> values;
   private final int bits;

   public HashMapPalette(final int bits, final List<T> values) {
      this(bits);
      values.forEach(this.values::add);
   }

   public HashMapPalette(final int bits) {
      this(bits, CrudeIncrementalIntIdentityHashBiMap.create(1 << bits));
   }

   private HashMapPalette(final int bits, final CrudeIncrementalIntIdentityHashBiMap<T> values) {
      this.bits = bits;
      this.values = values;
   }

   public static <A> Palette<A> create(final int bits, final List<A> paletteEntries) {
      return new HashMapPalette<>(bits, paletteEntries);
   }

   @Override
   public int idFor(final T value, final PaletteResize<T> resizeHandler) {
      int id = this.values.getId(value);
      if (id == -1) {
         id = this.values.add(value);
         if (id >= 1 << this.bits) {
            id = resizeHandler.onResize(this.bits + 1, value);
         }
      }

      return id;
   }

   @Override
   public boolean maybeHas(final Predicate<T> predicate) {
      for (int i = 0; i < this.getSize(); i++) {
         if (predicate.test(this.values.byId(i))) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(final int index) {
      T value = this.values.byId(index);
      if (value == null) {
         throw new MissingPaletteEntryException(index);
      } else {
         return value;
      }
   }

   @Override
   public void read(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
      this.values.clear();
      int size = buffer.readVarInt();

      for (int i = 0; i < size; i++) {
         this.values.add(globalMap.byIdOrThrow(buffer.readVarInt()));
      }
   }

   @Override
   public void write(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
      int size = this.getSize();
      buffer.writeVarInt(size);

      for (int i = 0; i < size; i++) {
         buffer.writeVarInt(globalMap.getId(this.values.byId(i)));
      }
   }

   @Override
   public int getSerializedSize(final IdMap<T> globalMap) {
      int size = VarInt.getByteSize(this.getSize());

      for (int i = 0; i < this.getSize(); i++) {
         size += VarInt.getByteSize(globalMap.getId(this.values.byId(i)));
      }

      return size;
   }

   public List<T> getEntries() {
      ArrayList<T> list = new ArrayList<>();
      this.values.iterator().forEachRemaining(list::add);
      return list;
   }

   @Override
   public int getSize() {
      return this.values.size();
   }

   @Override
   public Palette<T> copy() {
      return new HashMapPalette<>(this.bits, this.values.copy());
   }
}
