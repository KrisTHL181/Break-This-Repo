package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class BlockNameFlatteningFix extends DataFix {
   public BlockNameFlatteningFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   public TypeRewriteRule makeRule() {
      Type<?> blockType = this.getInputSchema().getType(References.BLOCK_NAME);
      Type<?> newBlockType = this.getOutputSchema().getType(References.BLOCK_NAME);
      Type<Pair<String, Either<Integer, String>>> expectedBlockType = DSL.named(
         References.BLOCK_NAME.typeName(), DSL.or(DSL.intType(), NamespacedSchema.namespacedString())
      );
      Type<Pair<String, String>> expectedNewBlockType = DSL.named(References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
      if (Objects.equals(blockType, expectedBlockType) && Objects.equals(newBlockType, expectedNewBlockType)) {
         return this.fixTypeEverywhere(
            "BlockNameFlatteningFix",
            expectedBlockType,
            expectedNewBlockType,
            ops -> block -> block.mapSecond(
               choice -> (String)choice.map(BlockStateData::upgradeBlock, name -> BlockStateData.upgradeBlock(NamespacedSchema.ensureNamespaced(name)))
            )
         );
      } else {
         throw new IllegalStateException("Expected and actual types don't match.");
      }
   }
}
