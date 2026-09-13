package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import net.minecraft.obfuscate.DontObfuscate;

@Name("minecraft.ClientFps")
@Label("Client fps")
@Category({"Minecraft", "Ticking"})
@StackTrace(false)
@Period("1 s")
@DontObfuscate
public class ClientFpsEvent extends Event {
   public static final String EVENT_NAME = "minecraft.ClientFps";
   public static final EventType TYPE = EventType.getEventType(ClientFpsEvent.class);
   @Name("fps")
   @Label("Client fps")
   public final int fps;

   public ClientFpsEvent(int p_457955_) {
      this.fps = p_457955_;
   }

   public static class Fields {
      public static final String FPS = "fps";

      private Fields() {
      }
   }
}
