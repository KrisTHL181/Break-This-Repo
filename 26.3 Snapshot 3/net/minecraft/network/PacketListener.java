package net.minecraft.network;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketUtils;

public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   void onDisconnect(DisconnectionDetails details);

   default void onPacketError(final Packet packet, final Exception cause) throws ReportedException {
      throw PacketUtils.makeReportedException(cause, packet, this);
   }

   default DisconnectionDetails createDisconnectionInfo(final Component reason, final Throwable cause) {
      return new DisconnectionDetails(reason);
   }

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(final Packet<?> packet) {
      return this.isAcceptingMessages();
   }

   default void fillCrashReport(final CrashReport crashReport) {
      CrashReportCategory connection = crashReport.addCategory("Connection");
      connection.setDetail("Protocol", () -> this.protocol().id());
      connection.setDetail("Flow", () -> this.flow().toString());
      this.fillListenerSpecificCrashDetails(crashReport, connection);
   }

   default void fillListenerSpecificCrashDetails(final CrashReport report, final CrashReportCategory connectionDetails) {
   }
}
