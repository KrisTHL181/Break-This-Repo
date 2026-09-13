package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.services.request.AbuseReportRequest.ClientInfo;
import com.mojang.authlib.services.request.AbuseReportRequest.RealmInfo;
import com.mojang.authlib.services.request.AbuseReportRequest.ThirdPartyServerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

public record ReportEnvironment(String clientVersion, ReportEnvironment.@Nullable Server server) {
   public static ReportEnvironment local() {
      return create(null);
   }

   public static ReportEnvironment thirdParty(final String ip) {
      return create(new ReportEnvironment.Server.ThirdParty(ip));
   }

   public static ReportEnvironment realm(final RealmsServer realm) {
      return create(new ReportEnvironment.Server.Realm(realm));
   }

   public static ReportEnvironment create(final ReportEnvironment.@Nullable Server server) {
      return new ReportEnvironment(getClientVersion(), server);
   }

   public ClientInfo clientInfo() {
      return new ClientInfo(this.clientVersion, Locale.getDefault().toLanguageTag());
   }

   public @Nullable ThirdPartyServerInfo thirdPartyServerInfo() {
      return this.server instanceof ReportEnvironment.Server.ThirdParty thirdParty ? new ThirdPartyServerInfo(thirdParty.ip) : null;
   }

   public @Nullable RealmInfo realmInfo() {
      return this.server instanceof ReportEnvironment.Server.Realm realm ? new RealmInfo(String.valueOf(realm.realmId()), realm.slotId()) : null;
   }

   private static String getClientVersion() {
      StringBuilder version = new StringBuilder();
      version.append(SharedConstants.getCurrentVersion().id());
      if (Minecraft.checkModStatus().shouldReportAsModified()) {
         version.append(" (modded)");
      }

      return version.toString();
   }

   public interface Server {
      record Realm(long realmId, int slotId) implements ReportEnvironment.Server {
         public Realm(final RealmsServer realm) {
            this(realm.id, realm.activeSlot);
         }
      }

      record ThirdParty(String ip) implements ReportEnvironment.Server {
      }
   }
}
