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

   public HashMapPalette(int p_187905_, List<T> p_428768_) {
      this(p_187905_);
      p_428768_.forEach(this.values::add);
   }

   public HashMapPalette(int p_199916_) {
      this(p_199916_, CrudeIncrementalIntIdentityHashBiMap.create(1 << p_199916_));
   }

   private HashMapPalette(int p_187909_, CrudeIncrementalIntIdentityHashBiMap<T> p_425599_) {
      this.bits = p_187909_;
      this.values = p_425599_;
   }

   public static <A> Palette<A> create(int p_187913_, List<A> p_187916_) {
      return new HashMapPalette<>(p_187913_, p_187916_);
   }

   @Override
   public int idFor(T p_62673_, PaletteResize<T> p_430263_) {
      int i = this.values.getId(p_62673_);
      if (i == -1) {
         i = this.values.add(p_62673_);
         if (i >= 1 << this.bits) {
            i = p_430263_.onResize(this.bits + 1, p_62673_);
         }
      }

      return i;
   }

   @Override
   public boolean maybeHas(Predicate<T> p_62675_) {
      for (int i = 0; i < this.getSize(); i++) {
         if (p_62675_.test(this.values.byId(i))) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int p_62671_) {
      T t = this.values.byId(p_62671_);
      if (t == null) {
         throw new MissingPaletteEntryException(p_62671_);
      } else {
         return t;
      }
   }

   @Override
   public void read(FriendlyByteBuf p_62679_, IdMap<T> p_428164_) {
      this.values.clear();
      int i = p_62679_.readVarInt();

      for (int j = 0; j < i; j++) {
         this.values.add(p_428164_.byIdOrThrow(p_62679_.readVarInt()));
      }
   }

   @Override
   public void write(FriendlyByteBuf p_62684_, IdMap<T> p_423871_) {
      int i = this.getSize();
      p_62684_.writeVarInt(i);

      for (int j = 0; j < i; j++) {
         p_62684_.writeVarInt(p_423871_.getId(this.values.byId(j)));
      }
   }

   @Override
   public int getSerializedSize(IdMap<T> p_424534_) {
      int i = VarInt.getByteSize(this.getSize());

      for (int j = 0; j < this.getSize(); j++) {
         i += VarInt.getByteSize(p_424534_.getId(this.values.byId(j)));
      }

      return i;
   }

   public List<T> getEntries() {
      ArrayList<T> arraylist = new ArrayList<>();
      this.values.iterator().forEachRemaining(arraylist::add);
      return arraylist;
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
