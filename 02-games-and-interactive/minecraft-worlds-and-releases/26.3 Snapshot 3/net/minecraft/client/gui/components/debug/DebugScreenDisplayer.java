package net.minecraft.client.gui.components.debug;

import java.util.Collection;
import net.minecraft.resources.Identifier;

public interface DebugScreenDisplayer {
   void addPriorityLine(String line);

   void addLine(String line);

   void addToGroup(final Identifier group, Collection<String> lines);

   void addToGroup(final Identifier group, String lines);
}
