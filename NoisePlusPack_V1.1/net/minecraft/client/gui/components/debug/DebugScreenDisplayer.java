package net.minecraft.client.gui.components.debug;

import java.util.Collection;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DebugScreenDisplayer {
   void addPriorityLine(String var1);

   void addLine(String var1);

   void addToGroup(Identifier var1, Collection<String> var2);

   void addToGroup(Identifier var1, String var2);
}
