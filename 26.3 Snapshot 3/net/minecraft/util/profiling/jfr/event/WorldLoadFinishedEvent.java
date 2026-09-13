package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name("minecraft.LoadWorld")
@Label("Create/Load World")
@Category({"Minecraft", "World Generation"})
@StackTrace(false)
public class WorldLoadFinishedEvent extends Event {
   public static final String EVENT_NAME = "minecraft.LoadWorld";
   public static final EventType TYPE = EventType.getEventType(WorldLoadFinishedEvent.class);
}
