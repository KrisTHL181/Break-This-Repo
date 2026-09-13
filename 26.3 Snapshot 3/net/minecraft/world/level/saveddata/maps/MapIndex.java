package net.minecraft.world.level.saveddata.maps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class MapIndex extends SavedData {
   private static final int NO_MAP_ID = -1;
   public static final Codec<MapIndex> CODEC = RecordCodecBuilder.create(
      i -> i.group(Codec.INT.optionalFieldOf("map", -1).forGetter(m -> m.lastMapId)).apply(i, MapIndex::new)
   );
   public static final SavedDataType<MapIndex> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("maps/last_id"), MapIndex::new, CODEC, DataFixTypes.SAVED_DATA_MAP_INDEX
   );
   private int lastMapId;

   public MapIndex() {
      this(-1);
   }

   public MapIndex(final int lastMapId) {
      this.lastMapId = lastMapId;
   }

   public MapId getNextMapId() {
      MapId id = new MapId(++this.lastMapId);
      this.setDirty();
      return id;
   }
}
