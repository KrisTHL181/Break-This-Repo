package net.minecraft.util.filefix.fixes;

import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.filefix.FileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;

public class ResourcePackLocationFileFix extends FileFix {
   public ResourcePackLocationFileFix(final Schema schema) {
      super(schema);
   }

   @Override
   public void makeFixer() {
      this.addFileFixOperation(FileFixOperations.move("resources.zip", "resourcepacks/resources.zip"));
   }
}
