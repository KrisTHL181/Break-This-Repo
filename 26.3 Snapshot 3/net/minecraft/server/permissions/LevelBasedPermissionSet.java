package net.minecraft.server.permissions;

public interface LevelBasedPermissionSet extends PermissionSet {
   @Deprecated
   LevelBasedPermissionSet ALL = create(PermissionLevel.ALL);
   LevelBasedPermissionSet MODERATOR = create(PermissionLevel.MODERATORS);
   LevelBasedPermissionSet GAMEMASTER = create(PermissionLevel.GAMEMASTERS);
   LevelBasedPermissionSet ADMIN = create(PermissionLevel.ADMINS);
   LevelBasedPermissionSet OWNER = create(PermissionLevel.OWNERS);

   PermissionLevel level();

   @Override
   default boolean hasPermission(final Permission permission) {
      if (permission instanceof Permission.HasCommandLevel levelCheck) {
         return this.level().isEqualOrHigherThan(levelCheck.level());
      } else {
         return permission.equals(Permissions.COMMANDS_ENTITY_SELECTORS) ? this.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS) : false;
      }
   }

   @Override
   default PermissionSet union(final PermissionSet other) {
      if (other instanceof LevelBasedPermissionSet otherSet) {
         return this.level().isEqualOrHigherThan(otherSet.level()) ? otherSet : this;
      } else {
         return PermissionSet.super.union(other);
      }
   }

   static LevelBasedPermissionSet forLevel(final PermissionLevel level) {
      return switch (level) {
         case ALL -> ALL;
         case MODERATORS -> MODERATOR;
         case GAMEMASTERS -> GAMEMASTER;
         case ADMINS -> ADMIN;
         case OWNERS -> OWNER;
      };
   }

   private static LevelBasedPermissionSet create(final PermissionLevel level) {
      return new LevelBasedPermissionSet() {
         @Override
         public PermissionLevel level() {
            return level;
         }

         @Override
         public String toString() {
            return "permission level: " + level.name();
         }
      };
   }
}
