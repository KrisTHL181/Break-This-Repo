package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public class GlobalPalette<T> implements Palette<T> {
   private final IdMap<T> registry;

   public GlobalPalette(IdMap<T> p_187897_) {
      this.registry = p_187897_;
   }

   @Override
   public int idFor(T p_62648_, PaletteResize<T> p_424925_) {
      int i = this.registry.getId(p_62648_);
      return i == -1 ? 0 : i;
   }

   @Override
   public boolean maybeHas(Predicate<T> p_62650_) {
      return true;
   }

   @Override
   public T valueFor(int p_62646_) {
      T t = this.registry.byId(p_62646_);
      if (t == null) {
         throw new MissingPaletteEntryException(p_62646_);
      } else {
         return t;
      }
   }

   @Override
   public void read(FriendlyByteBuf p_62654_, IdMap<T> p_427980_) {
   }

   @Override
   public void write(FriendlyByteBuf p_62656_, IdMap<T> p_429113_) {
   }

   @Override
   public int getSerializedSize(IdMap<T> p_424748_) {
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
