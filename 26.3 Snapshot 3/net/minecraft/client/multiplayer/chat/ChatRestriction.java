package net.minecraft.client.multiplayer.chat;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.CommonLinks;

public enum ChatRestriction {
   CHAT_AND_COMMANDS_DISABLED_BY_OPTIONS(
      Component.translatable("chat_restriction.chat_and_commands_disabled_by_options"), Optional.of(ChatRestriction.Action.GO_TO_CHAT_SETTINGS)
   ) {
      @Override
      public void modifyPermissions(final Set<Permission> permissionSet) {
         ChatRestriction.disableCommands(permissionSet);
         ChatRestriction.disablePlayerMessages(permissionSet);
      }
   },
   CHAT_DISABLED_BY_OPTIONS(Component.translatable("chat_restriction.chat_disabled_by_options"), Optional.of(ChatRestriction.Action.GO_TO_CHAT_SETTINGS)) {
      @Override
      public void modifyPermissions(final Set<Permission> permissionSet) {
         ChatRestriction.disablePlayerMessages(permissionSet);
      }
   },
   DISABLED_BY_LAUNCHER(Component.translatable("chat_restriction.disabled_by_launcher"), Optional.empty()) {
      @Override
      public void modifyPermissions(final Set<Permission> permissionSet) {
         ChatRestriction.disablePlayerMessages(permissionSet);
      }
   },
   DISABLED_BY_PROFILE(Component.translatable("chat_restriction.disabled_by_profile"), Optional.of(ChatRestriction.Action.GO_TO_ACCOUNT)) {
      @Override
      public void modifyPermissions(final Set<Permission> permissionSet) {
         ChatRestriction.disablePlayerMessages(permissionSet);
      }
   };

   private final Component display;
   private final Optional<ChatRestriction.Action> action;

   ChatRestriction(final Component display, final Optional<ChatRestriction.Action> action) {
      this.display = display;
      this.action = action;
   }

   public Component display() {
      return this.display;
   }

   public Optional<ChatRestriction.Action> action() {
      return this.action;
   }

   private static void disablePlayerMessages(final Set<Permission> permissionSet) {
      permissionSet.remove(Permissions.CHAT_SEND_MESSAGES);
      permissionSet.remove(Permissions.CHAT_RECEIVE_PLAYER_MESSAGES);
   }

   private static void disableCommands(final Set<Permission> permissionSet) {
      permissionSet.remove(Permissions.CHAT_SEND_COMMANDS);
      permissionSet.remove(Permissions.CHAT_RECEIVE_SYSTEM_MESSAGES);
   }

   public abstract void modifyPermissions(Set<Permission> permissionSet);

   public record Action(Component title, BiConsumer<Minecraft, Screen> runnable) {
      public static final ChatRestriction.Action GO_TO_ACCOUNT = new ChatRestriction.Action(
         Component.translatable("chat_restriction.disabled_by_profile.action"),
         (minecraft, screen) -> ConfirmLinkScreen.confirmLinkNow(screen, CommonLinks.ACCOUNT_SETTINGS, true)
      );
      public static final ChatRestriction.Action GO_TO_CHAT_SETTINGS = new ChatRestriction.Action(
         Component.translatable("chat_restriction.chat_disabled_by_options.action"),
         (minecraft, screen) -> minecraft.gui.setScreen(new ChatOptionsScreen(screen, minecraft.options))
      );
   }
}
