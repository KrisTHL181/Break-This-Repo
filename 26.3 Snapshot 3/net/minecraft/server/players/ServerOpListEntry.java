package net.minecraft.server.players;

import com.google.gson.JsonObject;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public class ServerOpListEntry extends StoredUserEntry<NameAndId> {
   private final LevelBasedPermissionSet permissions;
   private final boolean bypassesPlayerLimit;

   public ServerOpListEntry(final NameAndId user, final LevelBasedPermissionSet permissions, final boolean bypassesPlayerLimit) {
      super(user);
      this.permissions = permissions;
      this.bypassesPlayerLimit = bypassesPlayerLimit;
   }

   public ServerOpListEntry(final JsonObject object) {
      super(NameAndId.fromJson(object));
      PermissionLevel level = object.has("level") ? PermissionLevel.byId(object.get("level").getAsInt()) : PermissionLevel.ALL;
      this.permissions = LevelBasedPermissionSet.forLevel(level);
      this.bypassesPlayerLimit = object.has("bypassesPlayerLimit") && object.get("bypassesPlayerLimit").getAsBoolean();
   }

   public LevelBasedPermissionSet permissions() {
      return this.permissions;
   }

   public boolean getBypassesPlayerLimit() {
      return this.bypassesPlayerLimit;
   }

   @Override
   protected void serialize(final JsonObject object) {
      if (this.getUser() != null) {
         this.getUser().appendTo(object);
         object.addProperty("level", this.permissions.level().id());
         object.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
      }
   }
}
