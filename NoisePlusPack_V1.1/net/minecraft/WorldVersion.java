package net.minecraft;

import java.util.Date;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.world.level.storage.DataVersion;

public interface WorldVersion {
   DataVersion dataVersion();

   String id();

   String name();

   int protocolVersion();

   PackFormat packVersion(PackType var1);

   Date buildTime();

   boolean stable();

   record Simple(
      String id,
      String name,
      DataVersion dataVersion,
      int protocolVersion,
      PackFormat resourcePackVersion,
      PackFormat datapackVersion,
      Date buildTime,
      boolean stable
   ) implements WorldVersion {
      @Override
      public PackFormat packVersion(PackType p_408128_) {
         return switch (p_408128_) {
            case CLIENT_RESOURCES -> this.resourcePackVersion;
            case SERVER_DATA -> this.datapackVersion;
         };
      }
   }
}
