package net.minecraft.util.filefix.virtualfilesystem;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Util;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSCreationException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSSymlinkException;
import org.apache.commons.io.file.PathUtils;
import org.slf4j.Logger;

public class CopyOnWriteFileSystem extends FileSystem {
   private static final Set<String> FILE_ATTRIBUTE_VIEWS = Set.of("basic");
   private static final Logger LOGGER = LogUtils.getLogger();
   private final CopyOnWriteFileStore store;
   private final CopyOnWriteFSProvider provider;
   private final Path baseDirectory;
   private final PathMatcher skippedPaths;
   private final Path tmpDirectory;
   private final CopyOnWriteFSPath rootPath;
   private final AtomicInteger tmpFileIndex = new AtomicInteger();
   private DirectoryNode fileTree;

   private CopyOnWriteFileSystem(final String name, final Path baseDirectory, final Path tmpDirectory, final PathMatcher skippedPaths) throws IOException {
      this.baseDirectory = baseDirectory;
      this.tmpDirectory = tmpDirectory;
      this.skippedPaths = skippedPaths;
      this.provider = new CopyOnWriteFSProvider(this);
      this.store = new CopyOnWriteFileStore(name, this);
      this.rootPath = this.getPath("/");
      this.fileTree = this.buildFileTreeFrom(baseDirectory);
   }

   public static CopyOnWriteFileSystem create(final String name, final Path baseDirectory, final Path tmpDirectory, final PathMatcher skippedPaths) throws IOException {
      if (Files.exists(tmpDirectory)) {
         throw new CowFSCreationException("Temporary directory already exists: " + tmpDirectory);
      }

      CopyOnWriteFileSystem fileSystem = new CopyOnWriteFileSystem(name, baseDirectory, tmpDirectory, skippedPaths);
      Files.createDirectory(tmpDirectory);
      return fileSystem;
   }

   private DirectoryNode buildFileTreeFrom(final Path baseDirectory) throws IOException {
      final DirectoryNode fileTree = new DirectoryNode(this.rootPath);
      Files.walkFileTree(baseDirectory, new SimpleFileVisitor<Path>() {
         public FileVisitResult visitFile(final Path realPath, final BasicFileAttributes attrs) throws IOException {
            checkAttributes(realPath, attrs);
            if (CopyOnWriteFileSystem.this.skippedPaths.matches(realPath)) {
               return FileVisitResult.CONTINUE;
            }

            CopyOnWriteFSPath cowPath = this.toCowPath(realPath);
            DirectoryNode parentNode = fileTree.directoryByPath(Objects.requireNonNull(cowPath.getParent()));
            parentNode.addChild(new FileNode(cowPath, realPath, false));
            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult preVisitDirectory(final Path realPath, final BasicFileAttributes attrs) throws IOException {
            checkAttributes(realPath, attrs);
            if (CopyOnWriteFileSystem.this.skippedPaths.matches(realPath)) {
               return FileVisitResult.SKIP_SUBTREE;
            }

            if (realPath.equals(baseDirectory)) {
               return FileVisitResult.CONTINUE;
            }

            CopyOnWriteFSPath cowPath = this.toCowPath(realPath);
            DirectoryNode parentNode = fileTree.directoryByPath(Objects.requireNonNull(cowPath.getParent()));
            parentNode.addChild(new DirectoryNode(cowPath));
            return FileVisitResult.CONTINUE;
         }

         private static void checkAttributes(final Path realPath, final BasicFileAttributes attrs) throws CowFSCreationException {
            if (!attrs.isRegularFile() && !attrs.isDirectory()) {
               throw new CowFSSymlinkException("Cannot build copy-on-write file system when symlink is present: " + realPath);
            }

            if (!Files.isWritable(realPath)) {
               throw new CowFSCreationException("Cannot build copy-on-write file system, missing write access for file: " + realPath);
            }
         }

         private CopyOnWriteFSPath toCowPath(final Path realPath) {
            return fileTree.path().resolve(baseDirectory.relativize(realPath).toString());
         }
      });
      return fileTree;
   }

   @VisibleForTesting
   protected void resetFileTreeToBaseFolderContent() throws IOException {
      this.fileTree = this.buildFileTreeFrom(this.baseDirectory);
   }

   public CopyOnWriteFSProvider provider() {
      return this.provider;
   }

   @Override
   public void close() throws IOException {
      if (Files.exists(this.tmpDirectory)) {
         PathUtils.deleteDirectory(this.tmpDirectory);
      }
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public boolean isReadOnly() {
      return false;
   }

   @Override
   public String getSeparator() {
      return this.backingFileSystem().getSeparator();
   }

   @Override
   public Iterable<Path> getRootDirectories() {
      return List.of(this.rootPath());
   }

   @Override
   public Iterable<FileStore> getFileStores() {
      return List.of(this.store);
   }

   @Override
   public Set<String> supportedFileAttributeViews() {
      return FILE_ATTRIBUTE_VIEWS;
   }

   public CopyOnWriteFSPath getPath(final String first, final String... more) {
      return CopyOnWriteFSPath.of(this, first, more);
   }

   @Override
   public PathMatcher getPathMatcher(final String syntaxAndPattern) {
      throw new UnsupportedOperationException();
   }

   @Override
   public UserPrincipalLookupService getUserPrincipalLookupService() {
      throw new UnsupportedOperationException();
   }

   @Override
   public WatchService newWatchService() {
      throw new UnsupportedOperationException();
   }

   public CopyOnWriteFileStore store() {
      return this.store;
   }

   public CopyOnWriteFSPath rootPath() {
      return this.rootPath;
   }

   DirectoryNode fileTree() {
      return this.fileTree;
   }

   public Path baseDirectory() {
      return this.baseDirectory;
   }

   public Path tmpDirectory() {
      return this.tmpDirectory;
   }

   Path createTemporaryFilePath() {
      return this.tmpDirectory.resolve("tmp_" + this.tmpFileIndex.incrementAndGet());
   }

   public FileSystem backingFileSystem() {
      return this.tmpDirectory.getFileSystem();
   }

   public CopyOnWriteFileSystem.Moves collectMoveOperations(final Path outPath) {
      CopyOnWriteFileSystem.Moves result = new CopyOnWriteFileSystem.Moves(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
      this.collectMoveOperations(outPath, this.fileTree, result);
      return result;
   }

   private void collectMoveOperations(final Path outPath, final DirectoryNode folder, final CopyOnWriteFileSystem.Moves result) {
      for (Node childNode : folder.children()) {
         Path target = outPath.resolve(Objects.requireNonNull(childNode.name()));
         switch (childNode) {
            case FileNode fileNode:
               FileMove move = new FileMove(fileNode.storagePath(), target);
               if (fileNode.isCopy) {
                  result.copiedFiles.add(move);
               } else {
                  result.preexistingFiles.add(move);
               }
               break;
            case DirectoryNode directoryNode:
               result.directories.add(target);
               this.collectMoveOperations(target, directoryNode, result);
               break;
            default:
               throw new MatchException(null, null);
         }
      }
   }

   public static void createDirectories(final List<Path> directories) throws IOException {
      for (Path directory : directories) {
         Files.createDirectory(directory);
      }
   }

   public static void hardLinkFiles(final List<FileMove> moves) throws IOException {
      for (FileMove move : moves) {
         if (!Files.exists(move.to())) {
            if (!Files.isRegularFile(move.from())) {
               throw new IllegalStateException("Not a regular file: " + move.from());
            }

            Files.createLink(move.to(), move.from());
         }
      }
   }

   public static void moveFiles(final List<FileMove> moves) throws IOException {
      for (FileMove move : moves) {
         Files.move(move.from(), move.to());
      }
   }

   public static void moveFilesWithRetry(final List<FileMove> moves, final CopyOption... options) throws IOException {
      for (FileMove move : moves) {
         if (Files.exists(move.from()) || !Files.exists(move.to())) {
            if (!Files.isRegularFile(move.from())) {
               throw new IOException("Not a regular file: " + move.from());
            }

            Files.move(move.from(), move.to(), options);
         }
      }
   }

   public static List<FileMove> tryRevertMoves(final List<FileMove> moves, final CopyOption... options) {
      List<FileMove> failedMoves = new ArrayList<>();

      for (FileMove move : moves) {
         if (Files.exists(move.to()) || !Files.exists(move.from())) {
            if (Files.isRegularFile(move.to())) {
               boolean success = Util.safeMoveFile(move.to(), move.from(), options);
               if (success) {
                  LOGGER.info("Reverted move from {} to {}", move.from(), move.to());
               } else {
                  LOGGER.error("Failed to revert move from {} to {}", move.from(), move.to());
                  failedMoves.add(move);
               }
            } else {
               LOGGER.error("Skipping reverting move from {} to {} as it's not a file", move.from(), move.to());
               failedMoves.add(move);
            }
         }
      }

      if (failedMoves.isEmpty()) {
         LOGGER.info("Successfully reverted back to previous world state");
      } else {
         LOGGER.error("Completed reverting with errors");
      }

      return failedMoves;
   }

   public record Moves(List<Path> directories, List<FileMove> copiedFiles, List<FileMove> preexistingFiles) {
   }
}
