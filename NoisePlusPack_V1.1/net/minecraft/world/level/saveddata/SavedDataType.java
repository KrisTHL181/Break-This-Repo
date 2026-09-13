package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;

public record SavedDataType<T extends SavedData>(String id, Supplier<T> constructor, Codec<T> codec, DataFixTypes dataFixType) {
   @Override
   public boolean equals(Object p_393064_) {
      return p_393064_ instanceof SavedDataType<?> saveddatatype && this.id.equals(saveddatatype.id);
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return "SavedDataType[" + this.id + "]";
   }
}
