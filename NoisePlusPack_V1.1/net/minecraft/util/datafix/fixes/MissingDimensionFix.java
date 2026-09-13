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
   public MissingDimensionFix(Schema p_16420_, boolean p_16421_) {
      super(p_16420_, p_16421_);
   }

   protected static <A> Type<Pair<A, Dynamic<?>>> fields(String p_16439_, Type<A> p_16440_) {
      return DSL.and(DSL.field(p_16439_, p_16440_), DSL.remainderType());
   }

   protected static <A> Type<Pair<Either<A, Unit>, Dynamic<?>>> optionalFields(String p_16447_, Type<A> p_16448_) {
      return DSL.and(DSL.optional(DSL.field(p_16447_, p_16448_)), DSL.remainderType());
   }

   protected static <A1, A2> Type<Pair<Either<A1, Unit>, Pair<Either<A2, Unit>, Dynamic<?>>>> optionalFields(
      String p_16442_, Type<A1> p_16443_, String p_16444_, Type<A2> p_16445_
   ) {
      return DSL.and(DSL.optional(DSL.field(p_16442_, p_16443_)), DSL.optional(DSL.field(p_16444_, p_16445_)), DSL.remainderType());
   }

   protected TypeRewriteRule makeRule() {
      Schema schema = this.getInputSchema();
      Type<?> type = DSL.taggedChoiceType(
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
      CompoundListType<String, ?> compoundlisttype = DSL.compoundList(NamespacedSchema.namespacedString(), fields("generator", type));
      Type<?> type1 = DSL.and(compoundlisttype, DSL.remainderType());
      Type<?> type2 = schema.getType(References.WORLD_GEN_SETTINGS);
      FieldFinder<?> fieldfinder = new FieldFinder("dimensions", type1);
      if (!type2.findFieldType("dimensions").equals(type1)) {
         throw new IllegalStateException();
      }

      OpticFinder<? extends List<? extends Pair<String, ?>>> opticfinder = compoundlisttype.finder();
      return this.fixTypeEverywhereTyped(
         "MissingDimensionFix", type2, p_16426_ -> p_16426_.updateTyped(fieldfinder, p_145517_ -> p_145517_.updateTyped(opticfinder, p_326611_ -> {
            if (!(p_326611_.getValue() instanceof List)) {
               throw new IllegalStateException("List exptected");
            } else if (((List)p_326611_.getValue()).isEmpty()) {
               Dynamic<?> dynamic = (Dynamic<?>)p_16426_.get(DSL.remainderFinder());
               Dynamic<?> dynamic1 = this.recreateSettings(dynamic);
               return (Typed)DataFixUtils.orElse(compoundlisttype.readTyped(dynamic1).result().map(Pair::getFirst), p_326611_);
            } else {
               return p_326611_;
            }
         }))
      );
   }

   protected static Type<? extends Pair<? extends Either<? extends Pair<? extends Either<?, Unit>, ? extends Pair<? extends Either<? extends List<? extends Pair<? extends Either<?, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>>, Unit>, Dynamic<?>>> flatType(
      Schema p_185131_
   ) {
      return optionalFields(
         "settings",
         optionalFields("biome", p_185131_.getType(References.BIOME), "layers", DSL.list(optionalFields("block", p_185131_.getType(References.BLOCK_NAME))))
      );
   }

   private <T> Dynamic<T> recreateSettings(Dynamic<T> p_16437_) {
      long i = p_16437_.get("seed").asLong(0L);
      return new Dynamic(p_16437_.getOps(), WorldGenSettingsFix.vanillaLevels(p_16437_, i, WorldGenSettingsFix.defaultOverworld(p_16437_, i), false));
   }
}
