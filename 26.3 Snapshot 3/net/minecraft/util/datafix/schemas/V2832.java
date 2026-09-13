package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2832 extends NamespacedSchema {
   public V2832(final int versionKey, final Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(
      final Schema schema, final Map<String, Supplier<TypeTemplate>> entityTypes, final Map<String, Supplier<TypeTemplate>> blockEntityTypes
   ) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         false,
         References.CHUNK,
         () -> DSL.fields(
            "Level",
            DSL.optionalFields(
               "Entities",
               DSL.list(References.ENTITY_TREE.in(schema)),
               "TileEntities",
               DSL.list(DSL.or(References.BLOCK_ENTITY.in(schema), DSL.remainder())),
               "TileTicks",
               DSL.list(DSL.fields("i", References.BLOCK_NAME.in(schema))),
               "Sections",
               DSL.list(
                  DSL.optionalFields(
                     "biomes",
                     DSL.optionalFields("palette", DSL.list(References.BIOME.in(schema))),
                     "block_states",
                     DSL.optionalFields("palette", DSL.list(References.BLOCK_STATE.in(schema)))
                  )
               ),
               "Structures",
               DSL.optionalFields("Starts", DSL.compoundList(References.STRUCTURE_FEATURE.in(schema)))
            )
         )
      );
      schema.registerType(false, References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST, () -> DSL.constType(namespacedString()));
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
                                 (Supplier<TypeTemplate>)() -> DSL.or(
                                    DSL.fields("preset", References.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST.in(schema)),
                                    DSL.list(DSL.fields("biome", References.BIOME.in(schema)))
                                 ),
                                 "minecraft:checkerboard",
                                 (Supplier<TypeTemplate>)() -> DSL.fields("biomes", DSL.list(References.BIOME.in(schema))),
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
