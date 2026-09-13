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

   public DirectoryValidator(PathMatcher p_299405_) {
      this.symlinkTargetAllowList = p_299405_;
   }

   public void validateSymlink(Path p_289934_, List<ForbiddenSymlinkInfo> p_289972_) throws IOException {
      Path path = Files.readSymbolicLink(p_289934_);
      if (!this.symlinkTargetAllowList.matches(path)) {
         p_289972_.add(new ForbiddenSymlinkInfo(p_289934_, path));
      }
   }

   public List<ForbiddenSymlinkInfo> validateSymlink(Path p_299520_) throws IOException {
      List<ForbiddenSymlinkInfo> list = new ArrayList<>();
      this.validateSymlink(p_299520_, list);
      return list;
   }

   public List<ForbiddenSymlinkInfo> validateDirectory(Path p_301110_, boolean p_298035_) throws IOException {
      List<ForbiddenSymlinkInfo> list = new ArrayList<>();

      BasicFileAttributes basicfileattributes;
      try {
         basicfileattributes = Files.readAttributes(p_301110_, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException nosuchfileexception) {
         return list;
      }

      if (basicfileattributes.isRegularFile()) {
         throw new IOException("Path " + p_301110_ + " is not a directory");
      }

      if (basicfileattributes.isSymbolicLink()) {
         if (!p_298035_) {
            this.validateSymlink(p_301110_, list);
            return list;
         }

         p_301110_ = Files.readSymbolicLink(p_301110_);
      }

      this.validateKnownDirectory(p_301110_, list);
      return list;
   }

   public void validateKnownDirectory(Path p_297387_, final List<ForbiddenSymlinkInfo> p_298980_) throws IOException {
      Files.walkFileTree(p_297387_, new SimpleFileVisitor<Path>() {
         private void validateSymlink(Path p_289935_, BasicFileAttributes p_289941_) throws IOException {
            if (p_289941_.isSymbolicLink()) {
               DirectoryValidator.this.validateSymlink(p_289935_, p_298980_);
            }
         }

         public FileVisitResult preVisitDirectory(Path p_289946_, BasicFileAttributes p_289950_) throws IOException {
            this.validateSymlink(p_289946_, p_289950_);
            return super.preVisitDirectory(p_289946_, p_289950_);
         }

         public FileVisitResult visitFile(Path p_289986_, BasicFileAttributes p_289991_) throws IOException {
            this.validateSymlink(p_289986_, p_289991_);
            return super.visitFile(p_289986_, p_289991_);
         }
      });
   }
}
