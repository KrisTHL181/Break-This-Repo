package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.Util;

public class OminousBannerRenameFix extends ItemStackTagFix {
   public OminousBannerRenameFix(final Schema outputSchema) {
      super(outputSchema, "OminousBannerRenameFix", id -> id.equals("minecraft:white_banner"));
   }

   private <T> Dynamic<T> fixItemStackTag(final Dynamic<T> tag) {
      return tag.update(
         "display",
         display -> display.update(
            "Name",
            name -> {
               Optional<String> string = name.asString().result();
               return string.isPresent()
                  ? name.createString(
                     string.get().replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"")
                  )
                  : name;
            }
         )
      );
   }

   @Override
   protected Typed<?> fixItemStackTag(final Typed<?> tag) {
      return Util.writeAndReadTypedOrThrow(tag, tag.getType(), this::fixItemStackTag);
   }
}
