package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;

public class OminousBannerBlockEntityRenameFix extends NamedEntityFix {
   public OminousBannerBlockEntityRenameFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "OminousBannerBlockEntityRenameFix", References.BLOCK_ENTITY, "minecraft:banner");
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      OpticFinder<?> customNameF = entity.getType().findField("CustomName");
      OpticFinder<Pair<String, String>> textComponentF = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return entity.updateTyped(
         customNameF,
         customName -> customName.update(
            textComponentF,
            pair -> pair.mapSecond(name -> name.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\""))
         )
      );
   }
}
