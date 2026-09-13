package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserWhiteList extends StoredUserList<NameAndId, UserWhiteListEntry> {
   public UserWhiteList(final File file, final NotificationService notificationService) {
      super(file, notificationService);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(final JsonObject object) {
      return new UserWhiteListEntry(object);
   }

   public boolean isWhiteListed(final NameAndId user) {
      return this.contains(user);
   }

   public boolean add(final UserWhiteListEntry infos) {
      if (super.add(infos)) {
         if (infos.getUser() != null) {
            this.notificationService.playerAddedToAllowlist(infos.getUser());
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(final NameAndId user) {
      if (super.remove(user)) {
         this.notificationService.playerRemovedFromAllowlist(user);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (UserWhiteListEntry user : this.getEntries()) {
         if (user.getUser() != null) {
            this.notificationService.playerRemovedFromAllowlist(user.getUser());
         }
      }

      super.clear();
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   protected String getKeyForUser(final NameAndId user) {
      return user.id().toString();
   }
}
