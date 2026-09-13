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

   private LinearPalette(int p_188016_, List<T> p_188018_) {
      this.values = (T[])(new Object[1 << p_188016_]);
      this.bits = p_188016_;
      Validate.isTrue(
         p_188018_.size() <= this.values.length,
         "Can't initialize LinearPalette of size %d with %d entries",
         new Object[]{this.values.length, p_188018_.size()}
      );

      for (int i = 0; i < p_188018_.size(); i++) {
         this.values[i] = p_188018_.get(i);
      }

      this.size = p_188018_.size();
   }

   private LinearPalette(T[] p_199922_, int p_199924_, int p_199925_) {
      this.values = p_199922_;
      this.bits = p_199924_;
      this.size = p_199925_;
   }

   public static <A> Palette<A> create(int p_188020_, List<A> p_188023_) {
      return new LinearPalette<>(p_188020_, p_188023_);
   }

   @Override
   public int idFor(T p_63040_, PaletteResize<T> p_426539_) {
      for (int i = 0; i < this.size; i++) {
         if (this.values[i] == p_63040_) {
            return i;
         }
      }

      int j = this.size;
      if (j < this.values.length) {
         this.values[j] = p_63040_;
         this.size++;
         return j;
      } else {
         return p_426539_.onResize(this.bits + 1, p_63040_);
      }
   }

   @Override
   public boolean maybeHas(Predicate<T> p_63042_) {
      for (int i = 0; i < this.size; i++) {
         if (p_63042_.test(this.values[i])) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int p_63038_) {
      if (p_63038_ >= 0 && p_63038_ < this.size) {
         return this.values[p_63038_];
      } else {
         throw new MissingPaletteEntryException(p_63038_);
      }
   }

   @Override
   public void read(FriendlyByteBuf p_63046_, IdMap<T> p_425100_) {
      this.size = p_63046_.readVarInt();

      for (int i = 0; i < this.size; i++) {
         this.values[i] = p_425100_.byIdOrThrow(p_63046_.readVarInt());
      }
   }

   @Override
   public void write(FriendlyByteBuf p_63049_, IdMap<T> p_430932_) {
      p_63049_.writeVarInt(this.size);

      for (int i = 0; i < this.size; i++) {
         p_63049_.writeVarInt(p_430932_.getId(this.values[i]));
      }
   }

   @Override
   public int getSerializedSize(IdMap<T> p_423999_) {
      int i = VarInt.getByteSize(this.getSize());

      for (int j = 0; j < this.getSize(); j++) {
         i += VarInt.getByteSize(p_423999_.getId(this.values[j]));
      }

      return i;
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
