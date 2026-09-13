package net.minecraft.client.renderer.texture;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;

public interface Dumpable {
   void dumpContents(Identifier selfId, Path dir) throws IOException;
}
