package net.minecraft.server.packs.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;

public abstract class PackDetector<T> {
   private final DirectoryValidator validator;

   protected PackDetector(final DirectoryValidator validator) {
      this.validator = validator;
   }

   public @Nullable T detectPackResources(final Path content, final List<ForbiddenSymlinkInfo> issues) throws IOException {
      Path targetContext = content;

      BasicFileAttributes attributes;
      try {
         attributes = Files.readAttributes(content, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException e) {
         return null;
      }

      if (attributes.isSymbolicLink()) {
         this.validator.validateSymlink(content, issues);
         if (!issues.isEmpty()) {
            return null;
         }

         targetContext = Files.readSymbolicLink(content);
         attributes = Files.readAttributes(targetContext, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      }

      if (attributes.isDirectory()) {
         this.validator.validateKnownDirectory(targetContext, issues);
         if (!issues.isEmpty()) {
            return null;
         } else {
            return !Files.isRegularFile(targetContext.resolve("pack.mcmeta")) ? null : this.createDirectoryPack(targetContext);
         }
      } else {
         return attributes.isRegularFile() && targetContext.getFileName().toString().endsWith(".zip") ? this.createZipPack(targetContext) : null;
      }
   }

   protected abstract @Nullable T createZipPack(final Path content) throws IOException;

   protected abstract @Nullable T createDirectoryPack(final Path content) throws IOException;
}
