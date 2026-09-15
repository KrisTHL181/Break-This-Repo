package net.minecraft.world.level.entity;

import java.util.UUID;

public interface UniquelyIdentifyable {
   UUID getUUID();

   boolean isRemoved();
}
