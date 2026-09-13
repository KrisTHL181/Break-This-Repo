package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class StructureFeatureIndexSavedData extends SavedData {
   private final LongSet all;
   private final LongSet remaining;
   private static final Codec<LongSet> LONG_SET = Codec.LONG_STREAM.xmap(LongOpenHashSet::toSet, LongCollection::longStream);
   public static final Codec<StructureFeatureIndexSavedData> CODEC = RecordCodecBuilder.create(
      p_395424_ -> p_395424_.group(
            LONG_SET.fieldOf("All").forGetter(p_396607_ -> p_396607_.all), LONG_SET.fieldOf("Remaining").forGetter(p_393898_ -> p_393898_.remaining)
         )
         .apply(p_395424_, StructureFeatureIndexSavedData::new)
   );

   public static SavedDataType<StructureFeatureIndexSavedData> type(String p_394660_) {
      return new SavedDataType<>(p_394660_, StructureFeatureIndexSavedData::new, CODEC, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES);
   }

   private StructureFeatureIndexSavedData(LongSet p_163532_, LongSet p_163533_) {
      this.all = p_163532_;
      this.remaining = p_163533_;
   }

   public StructureFeatureIndexSavedData() {
      this(new LongOpenHashSet(), new LongOpenHashSet());
   }

   public void addIndex(long p_73366_) {
      this.all.add(p_73366_);
      this.remaining.add(p_73366_);
      this.setDirty();
   }

   public boolean hasStartIndex(long p_73370_) {
      return this.all.contains(p_73370_);
   }

   public boolean hasUnhandledIndex(long p_73374_) {
      return this.remaining.contains(p_73374_);
   }

   public void removeIndex(long p_73376_) {
      if (this.remaining.remove(p_73376_)) {
         this.setDirty();
      }
   }

   public LongSet getAll() {
      return this.all;
   }
}
