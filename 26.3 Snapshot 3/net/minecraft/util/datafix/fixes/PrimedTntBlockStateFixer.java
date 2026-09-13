package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;

public class PrimedTntBlockStateFixer extends NamedEntityWriteReadFix {
   public PrimedTntBlockStateFixer(final Schema outputSchema) {
      super(outputSchema, true, "PrimedTnt BlockState fixer", References.ENTITY, "minecraft:tnt");
   }

   private static <T> Dynamic<T> renameFuse(final Dynamic<T> input) {
      Optional<Dynamic<T>> fuseValue = input.get("Fuse").get().result();
      return fuseValue.isPresent() ? input.set("fuse", fuseValue.get()) : input;
   }

   private static <T> Dynamic<T> insertBlockState(final Dynamic<T> input) {
      return input.set("block_state", input.createMap(Map.of(input.createString("Name"), input.createString("minecraft:tnt"))));
   }

   @Override
   protected <T> Dynamic<T> fix(final Dynamic<T> input) {
      return renameFuse(insertBlockState(input));
   }
}
