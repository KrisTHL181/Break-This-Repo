package net.minecraft.server.players;

import com.google.gson.JsonObject;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public class ServerOpListEntry extends StoredUserEntry<NameAndId> {
   private final LevelBasedPermissionSet permissions;
   private final boolean bypassesPlayerLimit;

   public ServerOpListEntry(NameAndId p_422946_, LevelBasedPermissionSet p_451375_, boolean p_11362_) {
      super(p_422946_);
      this.permissions = p_451375_;
      this.bypassesPlayerLimit = p_11362_;
   }

   public ServerOpListEntry(JsonObject p_11358_) {
      super(NameAndId.fromJson(p_11358_));
      PermissionLevel permissionlevel = p_11358_.has("level") ? PermissionLevel.byId(p_11358_.get("level").getAsInt()) : PermissionLevel.ALL;
      this.permissions = LevelBasedPermissionSet.forLevel(permissionlevel);
      this.bypassesPlayerLimit = p_11358_.has("bypassesPlayerLimit") && p_11358_.get("bypassesPlayerLimit").getAsBoolean();
   }

   public LevelBasedPermissionSet permissions() {
      return this.permissions;
   }

   public boolean getBypassesPlayerLimit() {
      return this.bypassesPlayerLimit;
   }

   @Override
   protected void serialize(JsonObject p_11365_) {
      if (this.getUser() != null) {
         this.getUser().appendTo(p_11365_);
         p_11365_.addProperty("level", this.permissions.level().id());
         p_11365_.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
      }
   }
}
