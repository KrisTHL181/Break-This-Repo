package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenSettingsHeightAndBiomeFix extends DataFix {
   private static final String NAME = "WorldGenSettingsHeightAndBiomeFix";
   public static final String WAS_PREVIOUSLY_INCREASED_KEY = "has_increased_height_already";

   public WorldGenSettingsHeightAndBiomeFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> worldGenSettingsType = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
      OpticFinder<?> dimensionsFinder = worldGenSettingsType.findField("dimensions");
      Type<?> worldGenSettingsTypeNew = this.getOutputSchema().getType(References.WORLD_GEN_SETTINGS);
      Type<?> dimensionsType = worldGenSettingsTypeNew.findFieldType("dimensions");
      return this.fixTypeEverywhereTyped(
         "WorldGenSettingsHeightAndBiomeFix",
         worldGenSettingsType,
         worldGenSettingsTypeNew,
         input -> {
            OptionalDynamic<?> wasIncreasedOpt = ((Dynamic)input.get(DSL.remainderFinder())).get("has_increased_height_already");
            boolean wasExpSnap = wasIncreasedOpt.result().isEmpty();
            boolean wasPreviouslyIncreased = wasIncreasedOpt.asBoolean(true);
            return input.update(DSL.remainderFinder(), tag -> tag.remove("has_increased_height_already"))
               .updateTyped(
                  dimensionsFinder,
                  dimensionsType,
                  dimensions -> Util.writeAndReadTypedOrThrow(
                     dimensions,
                     dimensionsType,
                     dimensionsTag -> dimensionsTag.update(
                        "minecraft:overworld",
                        overworldTag -> overworldTag.update(
                           "generator",
                           generator -> {
                              String generatorType = generator.get("type").asString("");
                              if ("minecraft:noise".equals(generatorType)) {
                                 MutableBoolean isLargeBiomes = new MutableBoolean();
                                 generator = generator.update(
                                    "biome_source",
                                    biomeSource -> {
                                       String type = biomeSource.get("type").asString("");
                                       if ("minecraft:vanilla_layered".equals(type) || wasExpSnap && "minecraft:multi_noise".equals(type)) {
                                          if (biomeSource.get("large_biomes").asBoolean(false)) {
                                             isLargeBiomes.setTrue();
                                          }

                                          return biomeSource.createMap(
                                             ImmutableMap.of(
                                                biomeSource.createString("preset"),
                                                biomeSource.createString("minecraft:overworld"),
                                                biomeSource.createString("type"),
                                                biomeSource.createString("minecraft:multi_noise")
                                             )
                                          );
                                       } else {
                                          return biomeSource;
                                       }
                                    }
                                 );
                                 return isLargeBiomes.booleanValue()
                                    ? generator.update(
                                       "settings",
                                       settings -> "minecraft:overworld".equals(settings.asString(""))
                                          ? settings.createString("minecraft:large_biomes")
                                          : settings
                                    )
                                    : generator;
                              } else if ("minecraft:flat".equals(generatorType)) {
                                 return wasPreviouslyIncreased
                                    ? generator
                                    : generator.update("settings", settings -> settings.update("layers", WorldGenSettingsHeightAndBiomeFix::updateLayers));
                              } else {
                                 return generator;
                              }
                           }
                        )
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> updateLayers(final Dynamic<?> layers) {
      Dynamic<?> airLayer = layers.createMap(
         ImmutableMap.of(layers.createString("height"), layers.createInt(64), layers.createString("block"), layers.createString("minecraft:air"))
      );
      return layers.createList(Stream.concat(Stream.of(airLayer), layers.asStream()));
   }
}
