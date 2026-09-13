package net.minecraft.util.filefix.virtualfilesystem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNoSuchFileException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNotDirectoryException;
import org.jspecify.annotations.Nullable;

final class DirectoryNode extends Node {
   private final Map<String, Node> childNodes = new HashMap<>();

   public DirectoryNode(final CopyOnWriteFSPath path) {
      super(path);
   }

   public Collection<Node> children() {
      return Collections.unmodifiableCollection(this.childNodes.values());
   }

   public void addChild(final Node child) {
      String name = Objects.requireNonNull(child.name());
      this.childNodes.put(name, child);
      child.setParent(this);
   }

   void removeChild(final String name) {
      this.childNodes.remove(name);
   }

   public @Nullable Node getChild(final String name) {
      return this.childNodes.get(name);
   }

   public DirectoryNode directoryByPath(final CopyOnWriteFSPath path) throws CowFSNoSuchFileException, CowFSNotDirectoryException {
      if (this.byPath(path) instanceof DirectoryNode result) {
         return result;
      } else {
         throw new CowFSNotDirectoryException(path + " was a file, expected directory");
      }
   }

   public FileNode fileByPath(final CopyOnWriteFSPath path) throws CowFSNoSuchFileException {
      if (this.byPathOrNull(path) instanceof FileNode result) {
         return result;
      } else {
         throw new CowFSNoSuchFileException(path.toString());
      }
   }

   public Node byPath(final CopyOnWriteFSPath path) throws CowFSNoSuchFileException {
      Node node = this.byPathOrNull(path);
      if (node != null) {
         return node;
      } else {
         throw new CowFSNoSuchFileException(path.toString());
      }
   }

   public @Nullable Node byPathOrNull(final CopyOnWriteFSPath path) {
      int nameCount = path.getNameCount();
      DirectoryNode directory = this;

      for (int i = 0; i < nameCount; i++) {
         String name = path.getName(i).toString();
         if (!name.equals(".")) {
            if (name.equals("..")) {
               DirectoryNode parent = directory.parent;
               if (parent != null) {
                  directory = parent;
               }
            } else {
               Node nextNode = directory.getChild(name);
               if (!(nextNode instanceof DirectoryNode nextDirectory)) {
                  return i == nameCount - 1 ? nextNode : null;
               }

               directory = nextDirectory;
            }
         }
      }

      return directory;
   }
}
