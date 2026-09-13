package net.minecraft.server.level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public record TicketType(long timeout, @TicketType.Flags int flags) {
   public static final long NO_TIMEOUT = 0L;
   public static final int FLAG_PERSIST = 1;
   public static final int FLAG_LOADING = 2;
   public static final int FLAG_SIMULATION = 4;
   public static final int FLAG_KEEP_DIMENSION_ACTIVE = 8;
   public static final int FLAG_CAN_EXPIRE_IF_UNLOADED = 16;
   public static final TicketType PLAYER_SPAWN = register("player_spawn", 20L, 2);
   public static final TicketType SPAWN_SEARCH = register("spawn_search", 1L, 2);
   public static final TicketType DRAGON = register("dragon", 0L, 6);
   public static final TicketType PLAYER_LOADING = register("player_loading", 0L, 2);
   public static final TicketType PLAYER_SIMULATION = register("player_simulation", 0L, 12);
   public static final TicketType FORCED = register("forced", 0L, 15);
   public static final TicketType PORTAL = register("portal", 300L, 15);
   public static final TicketType ENDER_PEARL = register("ender_pearl", 40L, 14);
   public static final TicketType UNKNOWN = register("unknown", 1L, 18);

   private static TicketType register(String p_395985_, long p_395702_, @TicketType.Flags int p_426031_) {
      return Registry.register(BuiltInRegistries.TICKET_TYPE, p_395985_, new TicketType(p_395702_, p_426031_));
   }

   public boolean persist() {
      return (this.flags & 1) != 0;
   }

   public boolean doesLoad() {
      return (this.flags & 2) != 0;
   }

   public boolean doesSimulate() {
      return (this.flags & 4) != 0;
   }

   public boolean shouldKeepDimensionActive() {
      return (this.flags & 8) != 0;
   }

   public boolean canExpireIfUnloaded() {
      return (this.flags & 16) != 0;
   }

   public boolean hasTimeout() {
      return this.timeout != 0L;
   }

   @Retention(RetentionPolicy.CLASS)
   @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
   public @interface Flags {
   }
}
