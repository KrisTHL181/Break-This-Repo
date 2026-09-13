package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class MapBannerBlockPosFormatFix extends DataFix {
   public MapBannerBlockPosFormatFix(Schema p_333145_) {
      super(p_333145_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> opticfinder = type.findField("data");
      OpticFinder<?> opticfinder1 = opticfinder.type().findField("banners");
      OpticFinder<?> opticfinder2 = DSL.typeFinder(((ListType)opticfinder1.type()).getElement());
      return this.fixTypeEverywhereTyped(
         "MapBannerBlockPosFormatFix",
         type,
         p_390309_ -> p_390309_.updateTyped(
            opticfinder,
            p_390303_ -> p_390303_.updateTyped(
               opticfinder1,
               p_390305_ -> p_390305_.updateTyped(
                  opticfinder2, p_331309_ -> p_331309_.update(DSL.remainderFinder(), p_328913_ -> p_328913_.update("Pos", ExtraDataFixUtils::fixBlockPos))
               )
            )
         )
      );
   }
}
