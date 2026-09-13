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
   private static final IntFunction<PermissionLevel> BY_ID = ByIdMap.continuous(p_452528_ -> p_452528_.id, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
   public static final Codec<PermissionLevel> INT_CODEC = Codec.INT.xmap(BY_ID::apply, p_450760_ -> p_450760_.id);
   private final String name;
   private final int id;

   PermissionLevel(final String p_453949_, final int p_458428_) {
      this.name = p_453949_;
      this.id = p_458428_;
   }

   public boolean isEqualOrHigherThan(PermissionLevel p_450593_) {
      return this.id >= p_450593_.id;
   }

   public static PermissionLevel byId(int p_451931_) {
      return BY_ID.apply(p_451931_);
   }

   public int id() {
      return this.id;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }
}
