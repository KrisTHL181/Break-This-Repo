package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BlockEntityCustomNameToComponentFix extends DataFix {
   private static final Set<String> NAMEABLE_BLOCK_ENTITIES = Set.of(
      "minecraft:beacon",
      "minecraft:banner",
      "minecraft:brewing_stand",
      "minecraft:chest",
      "minecraft:trapped_chest",
      "minecraft:dispenser",
      "minecraft:dropper",
      "minecraft:enchanting_table",
      "minecraft:furnace",
      "minecraft:hopper",
      "minecraft:shulker_box"
   );

   public BlockEntityCustomNameToComponentFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<String> idFinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      Type<?> inputType = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> outputType = this.getOutputSchema().getType(References.BLOCK_ENTITY);
      Type<?> patchedInputType = ExtraDataFixUtils.patchSubType(inputType, inputType, outputType);
      return this.fixTypeEverywhereTyped(
         "BlockEntityCustomNameToComponentFix",
         inputType,
         outputType,
         input -> {
            Optional<String> id = input.getOptional(idFinder);
            return id.isPresent() && !NAMEABLE_BLOCK_ENTITIES.contains(id.get())
               ? ExtraDataFixUtils.cast(outputType, input)
               : Util.writeAndReadTypedOrThrow(
                  ExtraDataFixUtils.cast(patchedInputType, input), outputType, BlockEntityCustomNameToComponentFix::fixTagCustomName
               );
         }
      );
   }

   public static <T> Dynamic<T> fixTagCustomName(final Dynamic<T> tag) {
      String name = tag.get("CustomName").asString("");
      return name.isEmpty() ? tag.remove("CustomName") : tag.set("CustomName", LegacyComponentDataFixUtils.createPlainTextComponent(tag.getOps(), name));
   }
}
