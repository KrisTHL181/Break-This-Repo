package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

public class EntityPaintingItemFrameDirectionFix extends DataFix {
   private static final int[][] DIRECTIONS = new int[][]{{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

   public EntityPaintingItemFrameDirectionFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   private Dynamic<?> doFix(Dynamic<?> input, final boolean isPainting, final boolean isItemFrame) {
      if ((isPainting || isItemFrame) && input.get("Facing").asNumber().result().isEmpty()) {
         int direction;
         if (input.get("Direction").asNumber().result().isPresent()) {
            direction = input.get("Direction").asByte((byte)0) % DIRECTIONS.length;
            int[] steps = DIRECTIONS[direction];
            input = input.set("TileX", input.createInt(input.get("TileX").asInt(0) + steps[0]));
            input = input.set("TileY", input.createInt(input.get("TileY").asInt(0) + steps[1]));
            input = input.set("TileZ", input.createInt(input.get("TileZ").asInt(0) + steps[2]));
            input = input.remove("Direction");
            if (isItemFrame && input.get("ItemRotation").asNumber().result().isPresent()) {
               input = input.set("ItemRotation", input.createByte((byte)(input.get("ItemRotation").asByte((byte)0) * 2)));
            }
         } else {
            direction = input.get("Dir").asByte((byte)0) % DIRECTIONS.length;
            input = input.remove("Dir");
         }

         input = input.set("Facing", input.createByte((byte)direction));
      }

      return input;
   }

   public TypeRewriteRule makeRule() {
      Type<?> paintingType = this.getInputSchema().getChoiceType(References.ENTITY, "Painting");
      OpticFinder<?> paintingF = DSL.namedChoice("Painting", paintingType);
      Type<?> itemFrameType = this.getInputSchema().getChoiceType(References.ENTITY, "ItemFrame");
      OpticFinder<?> itemFrameF = DSL.namedChoice("ItemFrame", itemFrameType);
      Type<?> entityType = this.getInputSchema().getType(References.ENTITY);
      TypeRewriteRule paintingRule = this.fixTypeEverywhereTyped(
         "EntityPaintingFix",
         entityType,
         input -> input.updateTyped(paintingF, paintingType, entity -> entity.update(DSL.remainderFinder(), tag -> this.doFix(tag, true, false)))
      );
      TypeRewriteRule itemFrameRule = this.fixTypeEverywhereTyped(
         "EntityItemFrameFix",
         entityType,
         input -> input.updateTyped(itemFrameF, itemFrameType, entity -> entity.update(DSL.remainderFinder(), tag -> this.doFix(tag, false, true)))
      );
      return TypeRewriteRule.seq(paintingRule, itemFrameRule);
   }
}
