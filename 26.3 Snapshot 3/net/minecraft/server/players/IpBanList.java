package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import net.minecraft.server.notifications.NotificationService;
import org.jspecify.annotations.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(final File file, final NotificationService notificationService) {
      super(file, notificationService);
   }

   @Override
   protected StoredUserEntry<String> createEntry(final JsonObject object) {
      return new IpBanListEntry(object);
   }

   public boolean isBanned(final SocketAddress address) {
      String ip = this.getIpFromAddress(address);
      return this.contains(ip);
   }

   public boolean isBanned(final String ip) {
      return this.contains(ip);
   }

   public @Nullable IpBanListEntry get(final SocketAddress address) {
      String ip = this.getIpFromAddress(address);
      return this.get(ip);
   }

   private String getIpFromAddress(final SocketAddress address) {
      String ip = address.toString();
      if (ip.contains("/")) {
         ip = ip.substring(ip.indexOf(47) + 1);
      }

      if (ip.contains(":")) {
         ip = ip.substring(0, ip.indexOf(58));
      }

      return ip;
   }

   public boolean add(final IpBanListEntry infos) {
      if (super.add(infos)) {
         if (infos.getUser() != null) {
            this.notificationService.ipBanned(infos);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(final String ip) {
      if (super.remove(ip)) {
         this.notificationService.ipUnbanned(ip);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (IpBanListEntry user : this.getEntries()) {
         if (user.getUser() != null) {
            this.notificationService.ipUnbanned(user.getUser());
         }
      }

      super.clear();
   }
}
