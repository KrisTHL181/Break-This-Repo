package com.mojang.datafixers.types.families;

import com.mojang.datafixers.FamilyOptic;
import com.mojang.datafixers.TypedOptic;
import com.mojang.datafixers.types.Type;
import java.util.function.IntFunction;

public interface TypeFamily {
   Type<?> apply(int var1);

   static <A, B> FamilyOptic<A, B> familyOptic(IntFunction<TypedOptic<?, ?, A, B>> optics) {
      return optics::apply;
   }
}
