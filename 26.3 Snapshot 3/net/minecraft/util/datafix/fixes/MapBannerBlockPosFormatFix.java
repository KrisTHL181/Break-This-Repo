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
   public MapBannerBlockPosFormatFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA);
      OpticFinder<?> dataF = type.findField("data");
      OpticFinder<?> bannersF = dataF.type().findField("banners");
      OpticFinder<?> bannerF = DSL.typeFinder(((ListType)bannersF.type()).getElement());
      return this.fixTypeEverywhereTyped(
         "MapBannerBlockPosFormatFix",
         type,
         input -> input.updateTyped(
            dataF,
            data -> data.updateTyped(
               bannersF,
               banners -> banners.updateTyped(
                  bannerF, banner -> banner.update(DSL.remainderFinder(), bannerTag -> bannerTag.update("Pos", ExtraDataFixUtils::fixBlockPos))
               )
            )
         )
      );
   }
}
