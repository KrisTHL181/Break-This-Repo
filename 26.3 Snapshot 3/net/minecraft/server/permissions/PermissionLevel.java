package net.minecraft.server.permissions;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum PermissionLevel implements StringRepresentable {
   ALL("all", 0),
   MODERATORS("moderators", 1),
   GAMEMASTERS("gamemasters", 2),
   ADMINS("admins", 3),
   OWNERS("owners", 4);

   public static final Codec<PermissionLevel> CODEC = StringRepresentable.fromEnum(PermissionLevel::values);
   private static final IntFunction<PermissionLevel> BY_ID = ByIdMap.continuous(level -> level.id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
   public static final Codec<PermissionLevel> INT_CODEC = Codec.INT.xmap(BY_ID::apply, level -> level.id);
   private final String name;
   private final int id;

   PermissionLevel(final String name, final int id) {
      this.name = name;
      this.id = id;
   }

   public boolean isEqualOrHigherThan(final PermissionLevel other) {
      return this.id >= other.id;
   }

   public static PermissionLevel byId(final int level) {
      return BY_ID.apply(level);
   }

   public int id() {
      return this.id;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
