package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserBanList extends StoredUserList<NameAndId, UserBanListEntry> {
   public UserBanList(final File file, final NotificationService notificationService) {
      super(file, notificationService);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(final JsonObject object) {
      return new UserBanListEntry(object);
   }

   public boolean isBanned(final NameAndId user) {
      return this.contains(user);
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   protected String getKeyForUser(final NameAndId user) {
      return user.id().toString();
   }

   public boolean add(final UserBanListEntry infos) {
      if (super.add(infos)) {
         if (infos.getUser() != null) {
            this.notificationService.playerBanned(infos);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(final NameAndId user) {
      if (super.remove(user)) {
         this.notificationService.playerUnbanned(user);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (UserBanListEntry user : this.getEntries()) {
         if (user.getUser() != null) {
            this.notificationService.playerUnbanned(user.getUser());
         }
      }

      super.clear();
   }
}
