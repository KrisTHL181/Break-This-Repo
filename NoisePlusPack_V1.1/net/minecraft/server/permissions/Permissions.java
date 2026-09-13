package net.minecraft.server.permissions;

public class Permissions {
   public static final Permission COMMANDS_MODERATOR = new Permission.HasCommandLevel(PermissionLevel.MODERATORS);
   public static final Permission COMMANDS_GAMEMASTER = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);
   public static final Permission COMMANDS_ADMIN = new Permission.HasCommandLevel(PermissionLevel.ADMINS);
   public static final Permission COMMANDS_OWNER = new Permission.HasCommandLevel(PermissionLevel.OWNERS);
   public static final Permission COMMANDS_ENTITY_SELECTORS = Permission.Atom.create("commands/entity_selectors");
}
