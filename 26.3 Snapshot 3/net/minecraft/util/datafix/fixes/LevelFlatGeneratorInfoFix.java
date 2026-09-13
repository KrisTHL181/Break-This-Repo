package net.minecraft.util.datafix.fixes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.math.NumberUtils;

public class LevelFlatGeneratorInfoFix extends DataFix {
   private static final String GENERATOR_OPTIONS = "generatorOptions";
   @VisibleForTesting
   static final String DEFAULT = "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
   private static final Splitter SPLITTER = Splitter.on(';').limit(5);
   private static final Splitter LAYER_SPLITTER = Splitter.on(',');
   private static final Splitter OLD_AMOUNT_SPLITTER = Splitter.on('x').limit(2);
   private static final Splitter AMOUNT_SPLITTER = Splitter.on('*').limit(2);
   private static final Splitter BLOCK_SPLITTER = Splitter.on(':').limit(3);

   public LevelFlatGeneratorInfoFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "LevelFlatGeneratorInfoFix", this.getInputSchema().getType(References.LEVEL), input -> input.update(DSL.remainderFinder(), this::fix)
      );
   }

   private Dynamic<?> fix(final Dynamic<?> input) {
      return input.get("generatorName").asString("").equalsIgnoreCase("flat")
         ? input.update(
            "generatorOptions", options -> (Dynamic)DataFixUtils.orElse(options.asString().map(this::fixString).map(options::createString).result(), options)
         )
         : input;
   }

   @VisibleForTesting
   String fixString(final String generatorOptions) {
      if (generatorOptions.isEmpty()) {
         return "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
      }

      Iterator<String> parts = SPLITTER.split(generatorOptions).iterator();
      String firstPart = parts.next();
      int version;
      String layerInfo;
      if (parts.hasNext()) {
         version = NumberUtils.toInt(firstPart, 0);
         layerInfo = parts.next();
      } else {
         version = 0;
         layerInfo = firstPart;
      }

      if (version >= 0 && version <= 3) {
         StringBuilder result = new StringBuilder();
         Splitter heightSplitter = version < 3 ? OLD_AMOUNT_SPLITTER : AMOUNT_SPLITTER;
         result.append(StreamSupport.<String>stream(LAYER_SPLITTER.split(layerInfo).spliterator(), false).map(layerString -> {
            List<String> list = heightSplitter.splitToList(layerString);
            int height;
            String layerType;
            if (list.size() == 2) {
               height = NumberUtils.toInt(list.get(0));
               layerType = list.get(1);
            } else {
               height = 1;
               layerType = list.get(0);
            }

            List<String> layerParts = BLOCK_SPLITTER.splitToList(layerType);
            int nameIndex = layerParts.get(0).equals("minecraft") ? 1 : 0;
            String blockString = layerParts.get(nameIndex);
            int blockId = version == 3 ? EntityBlockStateFix.getBlockId("minecraft:" + blockString) : NumberUtils.toInt(blockString, 0);
            int dataIndex = nameIndex + 1;
            int data = layerParts.size() > dataIndex ? NumberUtils.toInt(layerParts.get(dataIndex), 0) : 0;
            return (height == 1 ? "" : height + "*") + BlockStateData.getTag(blockId << 4 | data).get("Name").asString("");
         }).collect(Collectors.joining(",")));

         while (parts.hasNext()) {
            result.append(';').append(parts.next());
         }

         return result.toString();
      } else {
         return "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;1;village";
      }
   }
}
