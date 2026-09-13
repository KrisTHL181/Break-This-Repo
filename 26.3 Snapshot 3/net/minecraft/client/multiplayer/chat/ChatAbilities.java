package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;

public class ChatAbilities {
   public static final ChatAbilities NO_RESTRICTIONS = new ChatAbilities(Set.of());
   private final Set<ChatRestriction> restrictionReasons;
   private final PermissionSet permissions;
   private final Predicate<GuiMessage> visibleMessagesFilter;

   private ChatAbilities(final Set<ChatRestriction> restrictionReasons) {
      this.restrictionReasons = restrictionReasons;
      Set<Permission> permissionSet = new HashSet<>(Permissions.CHAT_PERMISSIONS);

      for (ChatRestriction restrictionReason : restrictionReasons) {
         restrictionReason.modifyPermissions(permissionSet);
      }

      this.permissions = Set.copyOf(permissionSet)::contains;
      this.visibleMessagesFilter = selectVisibleMessages(this);
   }

   private static Predicate<GuiMessage> selectVisibleMessages(final ChatAbilities chatAbilities) {
      com.google.common.collect.ImmutableSet.Builder<GuiMessageSource> visibleSourcesBuilder = ImmutableSet.builder();
      visibleSourcesBuilder.add(GuiMessageSource.SYSTEM_CLIENT);
      if (chatAbilities.canReceivePlayerMessages()) {
         visibleSourcesBuilder.add(GuiMessageSource.PLAYER);
      }

      if (chatAbilities.canReceiveSystemMessages()) {
         visibleSourcesBuilder.add(GuiMessageSource.SYSTEM_SERVER);
      }

      ImmutableSet<GuiMessageSource> visibleSources = visibleSourcesBuilder.build();
      return guiMessage -> visibleSources.contains(guiMessage.source());
   }

   public boolean hasAnyRestrictions() {
      return !this.restrictionReasons.isEmpty();
   }

   public Stream<ChatRestriction> restrictions() {
      return this.restrictionReasons.stream();
   }

   public PermissionSet permissions() {
      return this.permissions;
   }

   public boolean canSendMessages() {
      return this.permissions.hasPermission(Permissions.CHAT_SEND_MESSAGES);
   }

   public boolean canSendCommands() {
      return this.permissions.hasPermission(Permissions.CHAT_SEND_COMMANDS);
   }

   public boolean canReceivePlayerMessages() {
      return this.permissions.hasPermission(Permissions.CHAT_RECEIVE_PLAYER_MESSAGES);
   }

   public boolean canReceiveSystemMessages() {
      return this.permissions.hasPermission(Permissions.CHAT_RECEIVE_SYSTEM_MESSAGES);
   }

   public Predicate<GuiMessage> visibleMessagesFilter() {
      return this.visibleMessagesFilter;
   }

   public static class Builder {
      private final Set<ChatRestriction> restrictions = new HashSet<>();

      public ChatAbilities.Builder addRestriction(final ChatRestriction restriction) {
         this.restrictions.add(restriction);
         return this;
      }

      public ChatAbilities build() {
         return this.restrictions.isEmpty() ? ChatAbilities.NO_RESTRICTIONS : new ChatAbilities(Set.copyOf(this.restrictions));
      }
   }
}
