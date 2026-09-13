package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;

public class EntityMinecartIdentifiersFix extends EntityRenameFix {
   public EntityMinecartIdentifiersFix(Schema p_15479_) {
      super("EntityMinecartIdentifiersFix", p_15479_, true);
   }

   @Override
   protected Pair<String, Typed<?>> fix(String p_336021_, Typed<?> p_331251_) {
      if (!p_336021_.equals("Minecart")) {
         return Pair.of(p_336021_, p_331251_);
      }

      int i = ((Dynamic)p_331251_.getOrCreate(DSL.remainderFinder())).get("Type").asInt(0);

      String s = switch (i) {
         case 1 -> "MinecartChest";
         case 2 -> "MinecartFurnace";
         default -> "MinecartRideable";
      };
      Type<?> type = (Type<?>)this.getOutputSchema().findChoiceType(References.ENTITY).types().get(s);
      return Pair.of(s, Util.writeAndReadTypedOrThrow(p_331251_, type, p_326576_ -> p_326576_.remove("Type")));
   }
}
