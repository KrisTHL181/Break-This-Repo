package net.minecraft.util.profiling.jfr.event;

import java.net.SocketAddress;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.PacketReceived")
@Label("Network Packet Received")
@DontObfuscate
public class PacketReceivedEvent extends PacketEvent {
   public static final String NAME = "minecraft.PacketReceived";
   public static final EventType TYPE = EventType.getEventType(PacketReceivedEvent.class);

   public PacketReceivedEvent(String p_300302_, String p_329711_, String p_330889_, SocketAddress p_195587_, int p_195585_) {
      super(p_300302_, p_329711_, p_330889_, p_195587_, p_195585_);
   }
}
