package net.minecraft.world.level.entity;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public interface UUIDLookup<IdentifiedType extends UniquelyIdentifyable> {
   @Nullable IdentifiedType lookup(UUID var1);
}
