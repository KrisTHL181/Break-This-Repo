package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserBanList extends StoredUserList<NameAndId, UserBanListEntry> {
   public UserBanList(File p_11402_, NotificationService p_424284_) {
      super(p_11402_, p_424284_);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(JsonObject p_11405_) {
      return new UserBanListEntry(p_11405_);
   }

   public boolean isBanned(NameAndId p_427408_) {
      return this.contains(p_427408_);
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   protected String getKeyForUser(NameAndId p_428270_) {
      return p_428270_.id().toString();
   }

   public boolean add(UserBanListEntry p_430672_) {
      if (super.add(p_430672_)) {
         if (p_430672_.getUser() != null) {
            this.notificationService.playerBanned(p_430672_);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(NameAndId p_427933_) {
      if (super.remove(p_427933_)) {
         this.notificationService.playerUnbanned(p_427933_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (UserBanListEntry userbanlistentry : this.getEntries()) {
         if (userbanlistentry.getUser() != null) {
            this.notificationService.playerUnbanned(userbanlistentry.getUser());
         }
      }

      super.clear();
   }
}
