package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;

public class PrimedTntBlockStateFixer extends NamedEntityWriteReadFix {
   public PrimedTntBlockStateFixer(Schema p_310798_) {
      super(p_310798_, true, "PrimedTnt BlockState fixer", References.ENTITY, "minecraft:tnt");
   }

   private static <T> Dynamic<T> renameFuse(Dynamic<T> p_313041_) {
      Optional<Dynamic<T>> optional = p_313041_.get("Fuse").get().result();
      return optional.isPresent() ? p_313041_.set("fuse", optional.get()) : p_313041_;
   }

   private static <T> Dynamic<T> insertBlockState(Dynamic<T> p_309485_) {
      return p_309485_.set("block_state", p_309485_.createMap(Map.of(p_309485_.createString("Name"), p_309485_.createString("minecraft:tnt"))));
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_310859_) {
      return renameFuse(insertBlockState(p_310859_));
   }
}
