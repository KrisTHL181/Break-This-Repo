package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class BlockEntityBlockStateFix extends NamedEntityFix {
   public BlockEntityBlockStateFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "BlockEntityBlockStateFix", References.BLOCK_ENTITY, "minecraft:piston");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      Type<?> newType = this.getOutputSchema().getChoiceType(References.BLOCK_ENTITY, "minecraft:piston");
      Type<?> blockStateType = newType.findFieldType("blockState");
      OpticFinder<?> blockStateF = DSL.fieldFinder("blockState", blockStateType);
      Dynamic<?> tag = (Dynamic<?>)entity.get(DSL.remainderFinder());
      int block = tag.get("blockId").asInt(0);
      tag = tag.remove("blockId");
      int data = tag.get("blockData").asInt(0) & 15;
      tag = tag.remove("blockData");
      Dynamic<?> blockStateTag = BlockStateData.getTag(block << 4 | data);
      Typed<?> output = (Typed<?>)newType.pointTyped(entity.getOps()).orElseThrow(() -> new IllegalStateException("Could not create new piston block entity."));
      return output.set(DSL.remainderFinder(), tag)
         .set(
            blockStateF,
            (Typed)((Pair)blockStateType.readTyped(blockStateTag)
                  .result()
                  .orElseThrow(() -> new IllegalStateException("Could not parse newly created block state tag.")))
               .getFirst()
         );
   }
}
