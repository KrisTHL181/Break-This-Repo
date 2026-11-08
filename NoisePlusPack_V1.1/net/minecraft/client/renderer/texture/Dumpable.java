package net.minecraft.client.renderer.texture;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Dumpable {
   void dumpContents(Identifier var1, Path var2) throws IOException;
}
