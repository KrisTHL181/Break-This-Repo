package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import net.minecraft.server.notifications.NotificationService;
import org.jspecify.annotations.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
   public IpBanList(File p_11036_, NotificationService p_422825_) {
      super(p_11036_, p_422825_);
   }

   @Override
   protected StoredUserEntry<String> createEntry(JsonObject p_11038_) {
      return new IpBanListEntry(p_11038_);
   }

   public boolean isBanned(SocketAddress p_11042_) {
      String s = this.getIpFromAddress(p_11042_);
      return this.contains(s);
   }

   public boolean isBanned(String p_11040_) {
      return this.contains(p_11040_);
   }

   public @Nullable IpBanListEntry get(SocketAddress p_11044_) {
      String s = this.getIpFromAddress(p_11044_);
      return this.get(s);
   }

   private String getIpFromAddress(SocketAddress p_11046_) {
      String s = p_11046_.toString();
      if (s.contains("/")) {
         s = s.substring(s.indexOf(47) + 1);
      }

      if (s.contains(":")) {
         s = s.substring(0, s.indexOf(58));
      }

      return s;
   }

   public boolean add(IpBanListEntry p_425579_) {
      if (super.add(p_425579_)) {
         if (p_425579_.getUser() != null) {
            this.notificationService.ipBanned(p_425579_);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean remove(String p_430248_) {
      if (super.remove(p_430248_)) {
         this.notificationService.ipUnbanned(p_430248_);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void clear() {
      for (IpBanListEntry ipbanlistentry : this.getEntries()) {
         if (ipbanlistentry.getUser() != null) {
            this.notificationService.ipUnbanned(ipbanlistentry.getUser());
         }
      }

      super.clear();
   }
}
