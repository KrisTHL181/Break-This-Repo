package net.minecraft.world.entity;

import org.jspecify.annotations.Nullable;

public interface TraceableEntity {
   @Nullable Entity getOwner();
}
