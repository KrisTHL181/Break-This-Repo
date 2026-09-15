package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.util.worldupdate.UpgradeProgress;

public interface FileFixOperation {
   void fix(final Path baseDirectory, final UpgradeProgress upgradeProgress) throws IOException;
}
