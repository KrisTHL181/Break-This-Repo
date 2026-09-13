package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;

public class OminousBannerBlockEntityRenameFix extends NamedEntityFix {
   public OminousBannerBlockEntityRenameFix(Schema p_16548_, boolean p_16549_) {
      super(p_16548_, p_16549_, "OminousBannerBlockEntityRenameFix", References.BLOCK_ENTITY, "minecraft:banner");
   }

   @Override
   protected Typed<?> fix(Typed<?> p_16551_) {
      OpticFinder<?> opticfinder = p_16551_.getType().findField("CustomName");
      OpticFinder<Pair<String, String>> opticfinder1 = DSL.typeFinder(this.getInputSchema().getType(References.TEXT_COMPONENT));
      return p_16551_.updateTyped(
         opticfinder,
         p_394137_ -> p_394137_.update(
            opticfinder1,
            p_392661_ -> p_392661_.mapSecond(
               p_397041_ -> p_397041_.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"")
            )
         )
      );
   }
}
