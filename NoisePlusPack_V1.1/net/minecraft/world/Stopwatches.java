package net.minecraft.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class Stopwatches extends SavedData {
   private static final Codec<Stopwatches> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
      .fieldOf("stopwatches")
      .codec()
      .xmap(Stopwatches::unpack, Stopwatches::pack);
   public static final SavedDataType<Stopwatches> TYPE = new SavedDataType<>("stopwatches", Stopwatches::new, CODEC, DataFixTypes.SAVED_DATA_STOPWATCHES);
   private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap();

   private Stopwatches() {
   }

   private static Stopwatches unpack(Map<Identifier, Long> p_452456_) {
      Stopwatches stopwatches = new Stopwatches();
      long i = currentTime();
      p_452456_.forEach((p_457369_, p_459721_) -> stopwatches.stopwatches.put(p_457369_, new Stopwatch(i, p_459721_)));
      return stopwatches;
   }

   private Map<Identifier, Long> pack() {
      long i = currentTime();
      Map<Identifier, Long> map = new TreeMap<>();
      this.stopwatches.forEach((p_452902_, p_460078_) -> map.put(p_452902_, p_460078_.elapsedMilliseconds(i)));
      return map;
   }

   public @Nullable Stopwatch get(Identifier p_460321_) {
      return this.stopwatches.get(p_460321_);
   }

   public boolean add(Identifier p_460107_, Stopwatch p_450666_) {
      if (this.stopwatches.putIfAbsent(p_460107_, p_450666_) == null) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean update(Identifier p_458161_, UnaryOperator<Stopwatch> p_451099_) {
      if (this.stopwatches.computeIfPresent(p_458161_, (p_456454_, p_451493_) -> p_451099_.apply(p_451493_)) != null) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean remove(Identifier p_450904_) {
      boolean flag = this.stopwatches.remove(p_450904_) != null;
      if (flag) {
         this.setDirty();
      }

      return flag;
   }

   @Override
   public boolean isDirty() {
      return super.isDirty() || !this.stopwatches.isEmpty();
   }

   public List<Identifier> ids() {
      return List.copyOf(this.stopwatches.keySet());
   }

   public static long currentTime() {
      return Util.getMillis();
   }
}
