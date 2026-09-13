package net.minecraft.server.permissions;

import java.util.Set;

public class Permissions {
   public static final Permission COMMANDS_MODERATOR = new Permission.HasCommandLevel(PermissionLevel.MODERATORS);
   public static final Permission COMMANDS_GAMEMASTER = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);
   public static final Permission COMMANDS_ADMIN = new Permission.HasCommandLevel(PermissionLevel.ADMINS);
   public static final Permission COMMANDS_OWNER = new Permission.HasCommandLevel(PermissionLevel.OWNERS);
   public static final Permission COMMANDS_ENTITY_SELECTORS = Permission.Atom.create("commands/entity_selectors");
   public static final Permission CHAT_SEND_MESSAGES = Permission.Atom.create("chat/send_messages");
   public static final Permission CHAT_SEND_COMMANDS = Permission.Atom.create("chat/send_commands");
   public static final Permission CHAT_RECEIVE_PLAYER_MESSAGES = Permission.Atom.create("chat/receive_player_messages");
   public static final Permission CHAT_RECEIVE_SYSTEM_MESSAGES = Permission.Atom.create("chat/receive_system_messages");
   public static final Set<Permission> CHAT_PERMISSIONS = Set.of(
      CHAT_SEND_MESSAGES, CHAT_SEND_COMMANDS, CHAT_RECEIVE_PLAYER_MESSAGES, CHAT_RECEIVE_SYSTEM_MESSAGES
   );
}
