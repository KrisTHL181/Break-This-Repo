package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
   private final T[] values;
   private final int bits;
   private int size;

   private LinearPalette(final int bits, final List<T> paletteEntries) {
      this.values = (T[])(new Object[1 << bits]);
      this.bits = bits;
      Validate.isTrue(
         paletteEntries.size() <= this.values.length,
         "Can't initialize LinearPalette of size %d with %d entries",
         new Object[]{this.values.length, paletteEntries.size()}
      );

      for (int i = 0; i < paletteEntries.size(); i++) {
         this.values[i] = paletteEntries.get(i);
      }

      this.size = paletteEntries.size();
   }

   private LinearPalette(final T[] values, final int bits, final int size) {
      this.values = values;
      this.bits = bits;
      this.size = size;
   }

   public static <A> Palette<A> create(final int bits, final List<A> paletteEntries) {
      return new LinearPalette<>(bits, paletteEntries);
   }

   @Override
   public int idFor(final T value, final PaletteResize<T> resizeHandler) {
      for (int i = 0; i < this.size; i++) {
         if (this.values[i] == value) {
            return i;
         }
      }

      int index = this.size;
      if (index < this.values.length) {
         this.values[index] = value;
         this.size++;
         return index;
      } else {
         return resizeHandler.onResize(this.bits + 1, value);
      }
   }

   @Override
   public boolean maybeHas(final Predicate<T> predicate) {
      for (int i = 0; i < this.size; i++) {
         if (predicate.test(this.values[i])) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(final int index) {
      if (index >= 0 && index < this.size) {
         return this.values[index];
      } else {
         throw new MissingPaletteEntryException(index);
      }
   }

   @Override
   public void read(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
      this.size = buffer.readVarInt();

      for (int i = 0; i < this.size; i++) {
         this.values[i] = globalMap.byIdOrThrow(buffer.readVarInt());
      }
   }

   @Override
   public void write(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
      buffer.writeVarInt(this.size);

      for (int i = 0; i < this.size; i++) {
         buffer.writeVarInt(globalMap.getId(this.values[i]));
      }
   }

   @Override
   public int getSerializedSize(final IdMap<T> globalMap) {
      int result = VarInt.getByteSize(this.getSize());

      for (int i = 0; i < this.getSize(); i++) {
         result += VarInt.getByteSize(globalMap.getId(this.values[i]));
      }

      return result;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public Palette<T> copy() {
      return new LinearPalette<>((T[])((Object[])this.values.clone()), this.bits, this.size);
   }
}
