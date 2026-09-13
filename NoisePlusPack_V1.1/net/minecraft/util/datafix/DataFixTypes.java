package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;

public enum DataFixTypes {
   LEVEL(References.LEVEL),
   LEVEL_SUMMARY(References.LIGHTWEIGHT_LEVEL),
   PLAYER(References.PLAYER),
   CHUNK(References.CHUNK),
   HOTBAR(References.HOTBAR),
   OPTIONS(References.OPTIONS),
   STRUCTURE(References.STRUCTURE),
   STATS(References.STATS),
   SAVED_DATA_COMMAND_STORAGE(References.SAVED_DATA_COMMAND_STORAGE),
   SAVED_DATA_FORCED_CHUNKS(References.SAVED_DATA_TICKETS),
   SAVED_DATA_MAP_DATA(References.SAVED_DATA_MAP_DATA),
   SAVED_DATA_MAP_INDEX(References.SAVED_DATA_MAP_INDEX),
   SAVED_DATA_RAIDS(References.SAVED_DATA_RAIDS),
   SAVED_DATA_RANDOM_SEQUENCES(References.SAVED_DATA_RANDOM_SEQUENCES),
   SAVED_DATA_SCOREBOARD(References.SAVED_DATA_SCOREBOARD),
   SAVED_DATA_STOPWATCHES(References.SAVED_DATA_STOPWATCHES),
   SAVED_DATA_STRUCTURE_FEATURE_INDICES(References.SAVED_DATA_STRUCTURE_FEATURE_INDICES),
   SAVED_DATA_WORLD_BORDER(References.SAVED_DATA_WORLD_BORDER),
   ADVANCEMENTS(References.ADVANCEMENTS),
   POI_CHUNK(References.POI_CHUNK),
   WORLD_GEN_SETTINGS(References.WORLD_GEN_SETTINGS),
   ENTITY_CHUNK(References.ENTITY_CHUNK),
   DEBUG_PROFILE(References.DEBUG_PROFILE);

   public static final Set<TypeReference> TYPES_FOR_LEVEL_LIST;
   private final TypeReference type;

   DataFixTypes(final TypeReference p_14503_) {
      this.type = p_14503_;
   }

   static int currentVersion() {
      return SharedConstants.getCurrentVersion().dataVersion().version();
   }

   public <A> Codec<A> wrapCodec(final Codec<A> p_300412_, final DataFixer p_297748_, final int p_299395_) {
      return new Codec<A>() {
         public <T> DataResult<T> encode(A p_299356_, DynamicOps<T> p_298710_, T p_298085_) {
            return p_300412_.encode(p_299356_, p_298710_, p_298085_)
               .flatMap(p_299755_ -> p_298710_.mergeToMap(p_299755_, p_298710_.createString("DataVersion"), p_298710_.createInt(DataFixTypes.currentVersion())));
         }

         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> p_300000_, T p_300871_) {
            int i = p_300000_.get(p_300871_, "DataVersion").flatMap(p_300000_::getNumberValue).map(Number::intValue).result().orElse(p_299395_);
            Dynamic<T> dynamic = new Dynamic(p_300000_, p_300000_.remove(p_300871_, "DataVersion"));
            Dynamic<T> dynamic1 = DataFixTypes.this.updateToCurrentVersion(p_297748_, dynamic, i);
            return p_300412_.decode(dynamic1);
         }
      };
   }

   public <T> Dynamic<T> update(DataFixer p_265388_, Dynamic<T> p_265179_, int p_265372_, int p_265168_) {
      return p_265388_.update(this.type, p_265179_, p_265372_, p_265168_);
   }

   public <T> Dynamic<T> updateToCurrentVersion(DataFixer p_265085_, Dynamic<T> p_265237_, int p_265099_) {
      return this.update(p_265085_, p_265237_, p_265099_, currentVersion());
   }

   public CompoundTag update(DataFixer p_265128_, CompoundTag p_265422_, int p_265549_, int p_265304_) {
      return (CompoundTag)this.update(p_265128_, new Dynamic(NbtOps.INSTANCE, p_265422_), p_265549_, p_265304_).getValue();
   }

   public CompoundTag updateToCurrentVersion(DataFixer p_265583_, CompoundTag p_265401_, int p_265111_) {
      return this.update(p_265583_, p_265401_, p_265111_, currentVersion());
   }

   static {
      TYPES_FOR_LEVEL_LIST = Set.of(LEVEL_SUMMARY.type);
   }
}
