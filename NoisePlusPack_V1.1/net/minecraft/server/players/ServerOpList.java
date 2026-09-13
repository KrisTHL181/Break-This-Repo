package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class ServerOpList extends StoredUserList<NameAndId, ServerOpListEntry> {
   public ServerOpList(File p_11345_, NotificationService p_422398_) {
      super(p_11345_, p_422398_);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(JsonObject p_11348_) {
      return new ServerOpListEntry(p_11348_);
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   public boolean add(ServerOpListEntry p_430726_) {
      if (super.add(p_430726_)) {
         if (p_430726_.getUser() != null) {
            this.notificationService.playerOped(p_430726_);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(NameAndId p_424121_) {
      ServerOpListEntry serveroplistentry = this.get(p_424121_);
      if (super.remove(p_424121_)) {
         if (serveroplistentry != null) {
            this.notificationService.playerDeoped(serveroplistentry);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (ServerOpListEntry serveroplistentry : this.getEntries()) {
         if (serveroplistentry.getUser() != null) {
            this.notificationService.playerDeoped(serveroplistentry);
         }
      }

      super.clear();
   }

   public boolean canBypassPlayerLimit(NameAndId p_428882_) {
      ServerOpListEntry serveroplistentry = this.get(p_428882_);
      return serveroplistentry != null ? serveroplistentry.getBypassesPlayerLimit() : false;
   }

   protected String getKeyForUser(NameAndId p_431721_) {
      return p_431721_.id().toString();
   }
}
