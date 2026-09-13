package com.mojang.renderpearl.api.pipeline;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ShaderSource {
   @Nullable String get(Identifier id, ShaderType type);
}
