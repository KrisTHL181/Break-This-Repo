package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.stream.LongStream;
import net.minecraft.util.Mth;

public class BitStorageAlignFix extends DataFix {
   private static final int BIT_TO_LONG_SHIFT = 6;
   private static final int HEIGHTMAP_BITS = 9;
   private static final int HEIGHTMAP_SIZE = 256;

   public BitStorageAlignFix(final Schema schema) {
      super(schema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      Type<?> levelType = chunkType.findFieldType("Level");
      OpticFinder<?> levelFinder = DSL.fieldFinder("Level", levelType);
      OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
      Type<?> sectionType = ((ListType)sectionsFinder.type()).getElement();
      OpticFinder<?> sectionFinder = DSL.typeFinder(sectionType);
      Type<Pair<String, Dynamic<?>>> blockStateType = DSL.named(References.BLOCK_STATE.typeName(), DSL.remainderType());
      OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder = DSL.fieldFinder("Palette", DSL.list(blockStateType));
      return this.fixTypeEverywhereTyped(
         "BitStorageAlignFix",
         chunkType,
         this.getOutputSchema().getType(References.CHUNK),
         chunk -> chunk.updateTyped(levelFinder, level -> this.updateHeightmaps(updateSections(sectionsFinder, sectionFinder, paletteFinder, level)))
      );
   }

   private Typed<?> updateHeightmaps(final Typed<?> level) {
      return level.update(
         DSL.remainderFinder(),
         tag -> tag.update("Heightmaps", heightmaps -> heightmaps.updateMapValues(e -> e.mapSecond(heightmap -> updateBitStorage(tag, heightmap, 256, 9))))
      );
   }

   private static Typed<?> updateSections(
      final OpticFinder<?> sectionsFinder,
      final OpticFinder<?> sectionFinder,
      final OpticFinder<List<Pair<String, Dynamic<?>>>> paletteFinder,
      final Typed<?> level
   ) {
      return level.updateTyped(
         sectionsFinder,
         sections -> sections.updateTyped(
            sectionFinder,
            section -> {
               int bits = section.getOptional(paletteFinder).map(palette -> Math.max(4, DataFixUtils.ceillog2(palette.size()))).orElse(0);
               return bits != 0 && !Mth.isPowerOfTwo(bits)
                  ? section.update(DSL.remainderFinder(), tag -> tag.update("BlockStates", states -> updateBitStorage(tag, states, 4096, bits)))
                  : section;
            }
         )
      );
   }

   private static Dynamic<?> updateBitStorage(final Dynamic<?> tag, final Dynamic<?> storage, final int size, final int bits) {
      long[] input = storage.asLongStream().toArray();
      long[] output = addPadding(size, bits, input);
      return tag.createLongList(LongStream.of(output));
   }

   public static long[] addPadding(final int size, final int bits, final long[] data) {
      int dataLength = data.length;
      if (dataLength == 0) {
         return data;
      }

      long mask = (1L << bits) - 1L;
      int valuesPerLong = 64 / bits;
      int requiredLength = (size + valuesPerLong - 1) / valuesPerLong;
      long[] result = new long[requiredLength];
      int outputDataIndex = 0;
      int outputStart = 0;
      long outputData = 0L;
      int currentIndex = 0;
      long current = data[0];
      long next = dataLength > 1 ? data[1] : 0L;

      for (int index = 0; index < size; index++) {
         int position = index * bits;
         int startData = position >> 6;
         int endData = (index + 1) * bits - 1 >> 6;
         int startBit = position ^ startData << 6;
         if (startData != currentIndex) {
            current = next;
            next = startData + 1 < dataLength ? data[startData + 1] : 0L;
            currentIndex = startData;
         }

         long valueToInsert;
         if (startData == endData) {
            valueToInsert = current >>> startBit & mask;
         } else {
            int shiftBits = 64 - startBit;
            valueToInsert = (current >>> startBit | next << shiftBits) & mask;
         }

         int outputEnd = outputStart + bits;
         if (outputEnd >= 64) {
            result[outputDataIndex++] = outputData;
            outputData = valueToInsert;
            outputStart = bits;
         } else {
            outputData |= valueToInsert << outputStart;
            outputStart = outputEnd;
         }
      }

      if (outputData != 0L) {
         result[outputDataIndex] = outputData;
      }

      return result;
   }
}
