package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.fixes.References;

public class V1451_6 extends NamespacedSchema {
   public static final String SPECIAL_OBJECTIVE_MARKER = "_special";
   protected static final HookFunction UNPACK_OBJECTIVE_ID = new HookFunction() {
      public <T> T apply(final DynamicOps<T> ops, final T value) {
         Dynamic<T> input = new Dynamic(ops, value);
         return (T)((Dynamic)DataFixUtils.orElse(
               input.get("CriteriaName")
                  .asString()
                  .result()
                  .map(name -> {
                     int colonPos = name.indexOf(58);
                     if (colonPos < 0) {
                        return Pair.of("_special", name);
                     }

                     try {
                        Identifier statType = Identifier.bySeparator(name.substring(0, colonPos), '.');
                        Identifier statId = Identifier.bySeparator(name.substring(colonPos + 1), '.');
                        return Pair.of(statType.toString(), statId.toString());
                     } catch (Exception e) {
                        return Pair.of("_special", name);
                     }
                  })
                  .map(
                     explodedId -> input.set(
                        "CriteriaType",
                        input.createMap(
                           ImmutableMap.of(
                              input.createString("type"),
                              input.createString((String)explodedId.getFirst()),
                              input.createString("id"),
                              input.createString((String)explodedId.getSecond())
                           )
                        )
                     )
                  ),
               input
            ))
            .getValue();
      }
   };
   protected static final HookFunction REPACK_OBJECTIVE_ID = new HookFunction() {
      public <T> T apply(final DynamicOps<T> ops, final T value) {
         Dynamic<T> input = new Dynamic(ops, value);
         Optional<Dynamic<T>> repackedId = input.get("CriteriaType")
            .get()
            .result()
            .flatMap(
               type -> {
                  Optional<String> statType = type.get("type").asString().result();
                  Optional<String> statId = type.get("id").asString().result();
                  if (statType.isPresent() && statId.isPresent()) {
                     String unpackedType = statType.get();
                     return unpackedType.equals("_special")
                        ? Optional.of(input.createString(statId.get()))
                        : Optional.of(type.createString(V1451_6.packNamespacedWithDot(unpackedType) + ":" + V1451_6.packNamespacedWithDot(statId.get())));
                  } else {
                     return Optional.empty();
                  }
               }
            );
         return (T)((Dynamic)DataFixUtils.orElse(repackedId.map(id -> input.set("CriteriaName", id).remove("CriteriaType")), input)).getValue();
      }
   };

   public V1451_6(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      Supplier<TypeTemplate> ITEM_STATS = () -> DSL.compoundList(References.ITEM_NAME.in(schema), DSL.constType(DSL.intType()));
      schema.registerType(
         false,
         References.STATS,
         () -> DSL.optionalFields(
            "stats",
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(schema), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:crafted", ITEM_STATS.get()),
                  Pair.of("minecraft:used", ITEM_STATS.get()),
                  Pair.of("minecraft:broken", ITEM_STATS.get()),
                  Pair.of("minecraft:picked_up", ITEM_STATS.get()),
                  Pair.of("minecraft:dropped", ITEM_STATS.get()),
                  Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(schema), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(schema), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
               }
            )
         )
      );
      Map<String, Supplier<TypeTemplate>> criterionTypes = createCriterionTypes(schema);
      schema.registerType(
         false,
         References.OBJECTIVE,
         () -> DSL.hook(
            DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), criterionTypes), "DisplayName", References.TEXT_COMPONENT.in(schema)),
            UNPACK_OBJECTIVE_ID,
            REPACK_OBJECTIVE_ID
         )
      );
   }

   protected static Map<String, Supplier<TypeTemplate>> createCriterionTypes(final Schema schema) {
      Supplier<TypeTemplate> itemCriterion = () -> DSL.optionalFields("id", References.ITEM_NAME.in(schema));
      Supplier<TypeTemplate> blockCriterion = () -> DSL.optionalFields("id", References.BLOCK_NAME.in(schema));
      Supplier<TypeTemplate> entityCriterion = () -> DSL.optionalFields("id", References.ENTITY_NAME.in(schema));
      Map<String, Supplier<TypeTemplate>> criterionTypes = Maps.newHashMap();
      criterionTypes.put("minecraft:mined", blockCriterion);
      criterionTypes.put("minecraft:crafted", itemCriterion);
      criterionTypes.put("minecraft:used", itemCriterion);
      criterionTypes.put("minecraft:broken", itemCriterion);
      criterionTypes.put("minecraft:picked_up", itemCriterion);
      criterionTypes.put("minecraft:dropped", itemCriterion);
      criterionTypes.put("minecraft:killed", entityCriterion);
      criterionTypes.put("minecraft:killed_by", entityCriterion);
      criterionTypes.put("minecraft:custom", () -> DSL.optionalFields("id", DSL.constType(namespacedString())));
      criterionTypes.put("_special", () -> DSL.optionalFields("id", DSL.constType(DSL.string())));
      return criterionTypes;
   }

   public static String packNamespacedWithDot(final String location) {
      Identifier parsedLoc = Identifier.tryParse(location);
      return parsedLoc != null ? parsedLoc.getNamespace() + "." + parsedLoc.getPath() : location;
   }
}
