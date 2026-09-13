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
   SAVED_DATA_CUSTOM_BOSS_EVENTS(References.SAVED_DATA_CUSTOM_BOSS_EVENTS),
   SAVED_DATA_ENDER_DRAGON_FIGHT(References.SAVED_DATA_ENDER_DRAGON_FIGHT),
   SAVED_DATA_GAME_RULES(References.SAVED_DATA_GAME_RULES),
   SAVED_DATA_FORCED_CHUNKS(References.SAVED_DATA_TICKETS),
   SAVED_DATA_MAP_DATA(References.SAVED_DATA_MAP_DATA),
   SAVED_DATA_MAP_INDEX(References.SAVED_DATA_MAP_INDEX),
   SAVED_DATA_RAIDS(References.SAVED_DATA_RAIDS),
   SAVED_DATA_RANDOM_SEQUENCES(References.SAVED_DATA_RANDOM_SEQUENCES),
   SAVED_DATA_SCHEDULED_EVENTS(References.SAVED_DATA_SCHEDULED_EVENTS),
   SAVED_DATA_SCOREBOARD(References.SAVED_DATA_SCOREBOARD),
   SAVED_DATA_STOPWATCHES(References.SAVED_DATA_STOPWATCHES),
   SAVED_DATA_STRUCTURE_FEATURE_INDICES(References.SAVED_DATA_STRUCTURE_FEATURE_INDICES),
   SAVED_DATA_WANDERING_TRADER(References.SAVED_DATA_WANDERING_TRADER),
   SAVED_DATA_WEATHER(References.SAVED_DATA_WEATHER),
   SAVED_DATA_WORLD_BORDER(References.SAVED_DATA_WORLD_BORDER),
   SAVED_DATA_WORLD_CLOCKS(References.SAVED_DATA_WORLD_CLOCKS),
   SAVED_DATA_WORLD_GEN_SETTINGS(References.SAVED_DATA_WORLD_GEN_SETTINGS),
   ADVANCEMENTS(References.ADVANCEMENTS),
   POI_CHUNK(References.POI_CHUNK),
   WORLD_GEN_SETTINGS(References.WORLD_GEN_SETTINGS),
   ENTITY_CHUNK(References.ENTITY_CHUNK),
   DEBUG_PROFILE(References.DEBUG_PROFILE);

   public static final Set<TypeReference> TYPES_FOR_LEVEL_LIST;
   private final TypeReference type;

   DataFixTypes(final TypeReference type) {
      this.type = type;
   }

   private static int currentVersion() {
      return SharedConstants.getCurrentVersion().dataVersion().version();
   }

   public <A> Codec<A> wrapCodec(final Codec<A> codec, final DataFixer dataFixer, final int defaultVersion) {
      return new Codec<A>() {
         public <T> DataResult<T> encode(final A input, final DynamicOps<T> ops, final T prefix) {
            return codec.encode(input, ops, prefix)
               .flatMap(data -> ops.mergeToMap(data, ops.createString("DataVersion"), ops.createInt(DataFixTypes.currentVersion())));
         }

         public <T> DataResult<Pair<A, T>> decode(final DynamicOps<T> ops, final T input) {
            int fromVersion = ops.get(input, "DataVersion").flatMap(ops::getNumberValue).map(Number::intValue).result().orElse(defaultVersion);
            Dynamic<T> dataWithoutVersion = new Dynamic(ops, ops.remove(input, "DataVersion"));
            Dynamic<T> fixedData = DataFixTypes.this.updateToCurrentVersion(dataFixer, dataWithoutVersion, fromVersion);
            return codec.decode(fixedData);
         }
      };
   }

   public <T> Dynamic<T> update(final DataFixer fixerUpper, final Dynamic<T> input, final int fromVersion, final int toVersion) {
      return fixerUpper.update(this.type, input, fromVersion, toVersion);
   }

   public <T> Dynamic<T> updateToCurrentVersion(final DataFixer fixerUpper, final Dynamic<T> input, final int dataVersion) {
      return this.update(fixerUpper, input, dataVersion, currentVersion());
   }

   public CompoundTag update(final DataFixer fixer, final CompoundTag tag, final int fromVersion, final int toVersion) {
      return (CompoundTag)this.update(fixer, new Dynamic(NbtOps.INSTANCE, tag), fromVersion, toVersion).getValue();
   }

   public CompoundTag updateToCurrentVersion(final DataFixer fixer, final CompoundTag tag, final int fromVersion) {
      return this.update(fixer, tag, fromVersion, currentVersion());
   }

   static {
      TYPES_FOR_LEVEL_LIST = Set.of(LEVEL_SUMMARY.type);
   }
}
