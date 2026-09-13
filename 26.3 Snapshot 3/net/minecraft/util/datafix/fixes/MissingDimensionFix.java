package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.FieldFinder;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList.CompoundListType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.List;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class MissingDimensionFix extends DataFix {
   public MissingDimensionFix(final Schema schema, final boolean changesType) {
      super(schema, changesType);
   }

   protected static <A> Type<Pair<A, Dynamic<?>>> fields(final String name, final Type<A> type) {
      return DSL.and(DSL.field(name, type), DSL.remainderType());
   }

   protected static <A> Type<Pair<Either<A, Unit>, Dynamic<?>>> optionalFields(final String name, final Type<A> type) {
      return DSL.and(DSL.optional(DSL.field(name, type)), DSL.remainderType());
   }

   protected static <A1, A2> Type<Pair<Either<A1, Unit>, Pair<Either<A2, Unit>, Dynamic<?>>>> optionalFields(
      final String name1, final Type<A1> type1, final String name2, final Type<A2> type2
   ) {
      return DSL.and(DSL.optional(DSL.field(name1, type1)), DSL.optional(DSL.field(name2, type2)), DSL.remainderType());
   }

   protected TypeRewriteRule makeRule() {
      Schema schema = this.getInputSchema();
      Type<?> generatorType = DSL.taggedChoiceType(
         "type",
         DSL.string(),
         ImmutableMap.of(
            "minecraft:debug",
            DSL.remainderType(),
            "minecraft:flat",
            flatType(schema),
            "minecraft:noise",
            optionalFields(
               "biome_source",
               DSL.taggedChoiceType(
                  "type",
                  DSL.string(),
                  ImmutableMap.of(
                     "minecraft:fixed",
                     fields("biome", schema.getType(References.BIOME)),
                     "minecraft:multi_noise",
                     DSL.list(fields("biome", schema.getType(References.BIOME))),
                     "minecraft:checkerboard",
                     fields("biomes", DSL.list(schema.getType(References.BIOME))),
                     "minecraft:vanilla_layered",
                     DSL.remainderType(),
                     "minecraft:the_end",
                     DSL.remainderType()
                  )
               ),
               "settings",
               DSL.or(
                  DSL.string(), optionalFields("default_block", schema.getType(References.BLOCK_NAME), "default_fluid", schema.getType(References.BLOCK_NAME))
               )
            )
         )
      );
      CompoundListType<String, ?> dimensionsType = DSL.compoundList(NamespacedSchema.namespacedString(), fields("generator", generatorType));
      Type<?> expectedDimensionsType = DSL.and(dimensionsType, DSL.remainderType());
      Type<?> settings = schema.getType(References.WORLD_GEN_SETTINGS);
      FieldFinder<?> dimensionsFinder = new FieldFinder("dimensions", expectedDimensionsType);
      if (!settings.findFieldType("dimensions").equals(expectedDimensionsType)) {
         throw new IllegalStateException();
      }

      OpticFinder<? extends List<? extends Pair<String, ?>>> dimensionListFinder = dimensionsType.finder();
      return this.fixTypeEverywhereTyped(
         "MissingDimensionFix",
         settings,
         input -> input.updateTyped(dimensionsFinder, dimensions -> dimensions.updateTyped(dimensionListFinder, generators -> {
            if (!(generators.getValue() instanceof List)) {
               throw new IllegalStateException("List exptected");
            } else if (((List)generators.getValue()).isEmpty()) {
               Dynamic<?> tag = (Dynamic<?>)input.get(DSL.remainderFinder());
               Dynamic<?> newDimensions = this.recreateSettings(tag);
               return (Typed)DataFixUtils.orElse(dimensionsType.readTyped(newDimensions).result().map(Pair::getFirst), generators);
            } else {
               return generators;
            }
         }))
      );
   }

   protected static Type<? extends Pair<? extends Either<? extends Pair<? extends Either<?, Unit>, ? extends Pair<? extends Either<? extends List<? extends Pair<? extends Either<?, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>> flatType(
      final Schema schema
   ) {
      return optionalFields(
         "settings",
         optionalFields("biome", schema.getType(References.BIOME), "layers", DSL.list(optionalFields("block", schema.getType(References.BLOCK_NAME))))
      );
   }

   private <T> Dynamic<T> recreateSettings(final Dynamic<T> tag) {
      long seed = tag.get("seed").asLong(0L);
      return new Dynamic(tag.getOps(), WorldGenSettingsFix.vanillaLevels(tag, seed, WorldGenSettingsFix.defaultOverworld(tag, seed), false));
   }
}
