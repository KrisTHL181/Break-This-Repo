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

   void onDisconnect(DisconnectionDetails var1);

   default void onPacketError(Packet p_330857_, Exception p_328275_) throws ReportedException {
      throw PacketUtils.makeReportedException(p_328275_, p_330857_, this);
   }

   default DisconnectionDetails createDisconnectionInfo(Component p_342542_, Throwable p_342140_) {
      return new DisconnectionDetails(p_342542_);
   }

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> p_299735_) {
      return this.isAcceptingMessages();
   }

   default void fillCrashReport(CrashReport p_311292_) {
      CrashReportCategory crashreportcategory = p_311292_.addCategory("Connection");
      crashreportcategory.setDetail("Protocol", () -> this.protocol().id());
      crashreportcategory.setDetail("Flow", () -> this.flow().toString());
      this.fillListenerSpecificCrashDetails(p_311292_, crashreportcategory);
   }

   default void fillListenerSpecificCrashDetails(CrashReport p_343455_, CrashReportCategory p_310872_) {
   }
}
