package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

public interface IdentifierSearchTree<T> {
   static <T> IdentifierSearchTree<T> empty() {
      return new IdentifierSearchTree<T>() {
         @Override
         public List<T> searchNamespace(final String namespace) {
            return List.of();
         }

         @Override
         public List<T> searchPath(final String path) {
            return List.of();
         }
      };
   }

   static <T> IdentifierSearchTree<T> create(final List<T> elements, final Function<T, Stream<Identifier>> idGetter) {
      if (elements.isEmpty()) {
         return empty();
      }

      final SuffixArray<T> namespaceTree = new SuffixArray<>();
      final SuffixArray<T> pathTree = new SuffixArray<>();

      for (T element : elements) {
         idGetter.apply(element).forEach(elementId -> {
            namespaceTree.add(element, elementId.getNamespace().toLowerCase(Locale.ROOT));
            pathTree.add(element, elementId.getPath().toLowerCase(Locale.ROOT));
         });
      }

      namespaceTree.generate();
      pathTree.generate();
      return new IdentifierSearchTree<T>() {
         @Override
         public List<T> searchNamespace(final String namespace) {
            return namespaceTree.search(namespace);
         }

         @Override
         public List<T> searchPath(final String path) {
            return pathTree.search(path);
         }
      };
   }

   List<T> searchNamespace(String namespace);

   List<T> searchPath(String path);
}
