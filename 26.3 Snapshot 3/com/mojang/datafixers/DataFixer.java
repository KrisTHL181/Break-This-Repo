package com.mojang.datafixers;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public interface DataFixer {
   <T> Dynamic<T> update(DSL.TypeReference var1, Dynamic<T> var2, int var3, int var4);

   Schema getSchema(int var1);
}
