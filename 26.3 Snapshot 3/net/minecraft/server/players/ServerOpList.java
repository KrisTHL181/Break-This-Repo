package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class ServerOpList extends StoredUserList<NameAndId, ServerOpListEntry> {
   public ServerOpList(final File file, final NotificationService notificationService) {
      super(file, notificationService);
   }

   @Override
   protected StoredUserEntry<NameAndId> createEntry(final JsonObject object) {
      return new ServerOpListEntry(object);
   }

   @Override
   public String[] getUserList() {
      return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
   }

   public boolean add(final ServerOpListEntry infos) {
      if (super.add(infos)) {
         if (infos.getUser() != null) {
            this.notificationService.playerOped(infos);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(final NameAndId user) {
      ServerOpListEntry entry = this.get(user);
      if (super.remove(user)) {
         if (entry != null) {
            this.notificationService.playerDeoped(entry);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (ServerOpListEntry user : this.getEntries()) {
         if (user.getUser() != null) {
            this.notificationService.playerDeoped(user);
         }
      }

      super.clear();
   }

   public boolean canBypassPlayerLimit(final NameAndId user) {
      ServerOpListEntry entry = this.get(user);
      return entry != null ? entry.getBypassesPlayerLimit() : false;
   }

   protected String getKeyForUser(final NameAndId user) {
      return user.id().toString();
   }
}
