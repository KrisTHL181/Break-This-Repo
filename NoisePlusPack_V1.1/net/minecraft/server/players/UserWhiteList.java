package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserWhiteList extends StoredUserList<NameAndId, UserWhiteListEntry> {
   public UserWhiteList(File p_11449_, NotificationService p_424868_) {
      super(p_11449_, p_424868_);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(JsonObject p_11452_) {
      return new UserWhiteListEntry(p_11452_);
   }

   public boolean isWhiteListed(NameAndId p_430432_) {
      return this.contains(p_430432_);
   }

   public boolean add(UserWhiteListEntry p_427014_) {
      if (super.add(p_427014_)) {
         if (p_427014_.getUser() != null) {
            this.notificationService.playerAddedToAllowlist(p_427014_.getUser());
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(NameAndId p_430929_) {
      if (super.remove(p_430929_)) {
         this.notificationService.playerRemovedFromAllowlist(p_430929_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (UserWhiteListEntry userwhitelistentry : this.getEntries()) {
         if (userwhitelistentry.getUser() != null) {
            this.notificationService.playerRemovedFromAllowlist(userwhitelistentry.getUser());
         }
      }

      super.clear();
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   protected String getKeyForUser(NameAndId p_424595_) {
      return p_424595_.id().toString();
   }
}
