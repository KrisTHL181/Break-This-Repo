package com.mojang.datafixers.types.families;

import com.mojang.datafixers.RewriteResult;

public interface Algebra {
   RewriteResult<?, ?> apply(int var1);

   String toString(int var1);
}
