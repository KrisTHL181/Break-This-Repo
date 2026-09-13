package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.Util;

public class OminousBannerRenameFix extends ItemStackTagFix {
   public OminousBannerRenameFix(Schema p_216694_) {
      super(p_216694_, "OminousBannerRenameFix", p_216698_ -> p_216698_.equals("minecraft:white_banner"));
   }

   private <T> Dynamic<T> fixItemStackTag(Dynamic<T> p_216696_) {
      return p_216696_.update(
         "display",
         p_390334_ -> p_390334_.update(
            "Name",
            p_390333_ -> {
               Optional<String> optional = p_390333_.asString().result();
               return optional.isPresent()
                  ? p_390333_.createString(
                     optional.get().replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"")
                  )
                  : p_390333_;
            }
         )
      );
   }

   @Override
   protected Typed<?> fixItemStackTag(Typed<?> p_397496_) {
      return Util.writeAndReadTypedOrThrow(p_397496_, p_397496_.getType(), this::fixItemStackTag);
   }
}
