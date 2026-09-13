package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2551 extends NamespacedSchema {
   public V2551(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         false,
         References.WORLD_GEN_SETTINGS,
         () -> DSL.fields(
            "dimensions",
            DSL.compoundList(
               DSL.constType(namespacedString()),
               DSL.fields(
                  "generator",
                  DSL.taggedChoiceLazy(
                     "type",
                     DSL.string(),
                     ImmutableMap.of(
                        "minecraft:debug",
                        DSL::remainder,
                        "minecraft:flat",
                        (Supplier<TypeTemplate>)() -> DSL.optionalFields(
                           "settings",
                           DSL.optionalFields(
                              "biome", References.BIOME.in(schema), "layers", DSL.list(DSL.optionalFields("block", References.BLOCK_NAME.in(schema)))
                           )
                        ),
                        "minecraft:noise",
                        (Supplier<TypeTemplate>)() -> DSL.optionalFields(
                           "biome_source",
                           DSL.taggedChoiceLazy(
                              "type",
                              DSL.string(),
                              ImmutableMap.of(
                                 "minecraft:fixed",
                                 (Supplier<TypeTemplate>)() -> DSL.fields("biome", References.BIOME.in(schema)),
                                 "minecraft:multi_noise",
                                 (Supplier<TypeTemplate>)() -> DSL.list(DSL.fields("biome", References.BIOME.in(schema))),
                                 "minecraft:checkerboard",
                                 (Supplier<TypeTemplate>)() -> DSL.fields("biomes", DSL.list(References.BIOME.in(schema))),
                                 "minecraft:vanilla_layered",
                                 DSL::remainder,
                                 "minecraft:the_end",
                                 DSL::remainder
                              )
                           ),
                           "settings",
                           DSL.or(
                              DSL.constType(DSL.string()),
                              DSL.optionalFields("default_block", References.BLOCK_NAME.in(schema), "default_fluid", References.BLOCK_NAME.in(schema))
                           )
                        )
                     )
                  )
               )
            )
         )
      );
   }
}
