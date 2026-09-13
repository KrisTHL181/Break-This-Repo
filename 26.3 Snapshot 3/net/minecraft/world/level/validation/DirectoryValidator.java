package net.minecraft.world.level.validation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryValidator {
   private final PathMatcher symlinkTargetAllowList;

   public DirectoryValidator(final PathMatcher symlinkTargetAllowList) {
      this.symlinkTargetAllowList = symlinkTargetAllowList;
   }

   public void validateSymlink(final Path path, final List<ForbiddenSymlinkInfo> issues) throws IOException {
      Path target = Files.readSymbolicLink(path);
      if (!this.symlinkTargetAllowList.matches(target)) {
         issues.add(new ForbiddenSymlinkInfo(path, target));
      }
   }

   public List<ForbiddenSymlinkInfo> validateSymlink(final Path path) throws IOException {
      List<ForbiddenSymlinkInfo> result = new ArrayList<>();
      this.validateSymlink(path, result);
      return result;
   }

   public List<ForbiddenSymlinkInfo> validateDirectory(Path directory, final boolean allowTopSymlink) throws IOException {
      List<ForbiddenSymlinkInfo> issues = new ArrayList<>();

      BasicFileAttributes targetAttributes;
      try {
         targetAttributes = Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException e) {
         return issues;
      }

      if (targetAttributes.isRegularFile()) {
         throw new IOException("Path " + directory + " is not a directory");
      }

      if (targetAttributes.isSymbolicLink()) {
         if (!allowTopSymlink) {
            this.validateSymlink(directory, issues);
            return issues;
         }

         directory = Files.readSymbolicLink(directory);
      }

      this.validateKnownDirectory(directory, issues);
      return issues;
   }

   public void validateKnownDirectory(final Path directory, final List<ForbiddenSymlinkInfo> issues) throws IOException {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
         private void validateSymlink(final Path path, final BasicFileAttributes attrs) throws IOException {
            if (attrs.isSymbolicLink()) {
               DirectoryValidator.this.validateSymlink(path, issues);
            }
         }

         public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            this.validateSymlink(dir, attrs);
            return super.preVisitDirectory(dir, attrs);
         }

         public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            this.validateSymlink(file, attrs);
            return super.visitFile(file, attrs);
         }
      });
   }
}
