package net.minecraft.util.profiling.jfr.event;

import java.net.SocketAddress;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.PacketSent")
@Label("Network Packet Sent")
@DontObfuscate
public class PacketSentEvent extends PacketEvent {
   public static final String NAME = "minecraft.PacketSent";
   public static final EventType TYPE = EventType.getEventType(PacketSentEvent.class);

   public PacketSentEvent(String p_300751_, String p_331230_, String p_335798_, SocketAddress p_195595_, int p_195593_) {
      super(p_300751_, p_331230_, p_335798_, p_195595_, p_195593_);
   }
}
