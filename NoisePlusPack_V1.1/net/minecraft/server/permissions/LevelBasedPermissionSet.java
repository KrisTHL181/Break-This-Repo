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
   default boolean hasPermission(Permission p_452231_) {
      if (p_452231_ instanceof Permission.HasCommandLevel permission$hascommandlevel) {
         return this.level().isEqualOrHigherThan(permission$hascommandlevel.level());
      } else {
         return p_452231_.equals(Permissions.COMMANDS_ENTITY_SELECTORS) ? this.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS) : false;
      }
   }

   @Override
   default PermissionSet union(PermissionSet p_457355_) {
      if (p_457355_ instanceof LevelBasedPermissionSet levelbasedpermissionset) {
         return this.level().isEqualOrHigherThan(levelbasedpermissionset.level()) ? levelbasedpermissionset : this;
      } else {
         return PermissionSet.super.union(p_457355_);
      }
   }

   static LevelBasedPermissionSet forLevel(PermissionLevel p_450467_) {
      return switch (p_450467_) {
         case ALL -> ALL;
         case MODERATORS -> MODERATOR;
         case GAMEMASTERS -> GAMEMASTER;
         case ADMINS -> ADMIN;
         case OWNERS -> OWNER;
      };
   }

   private static LevelBasedPermissionSet create(final PermissionLevel p_453715_) {
      return new LevelBasedPermissionSet() {
         @Override
         public PermissionLevel level() {
            return p_453715_;
         }

         @Override
         public String toString() {
            return "permission level: " + p_453715_.name();
         }
      };
   }
}
