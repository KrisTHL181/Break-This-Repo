package com.mojang.datafixers;

public interface FamilyOptic<A, B> {
   TypedOptic<?, ?, A, B> apply(int var1);
}
