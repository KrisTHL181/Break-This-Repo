package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

@FunctionalInterface
public interface SearchTree<T> {
   static <T> SearchTree<T> empty() {
      return text -> List.of();
   }

   static <T> SearchTree<T> plainText(final List<T> elements, final Function<T, Stream<String>> idGetter) {
      if (elements.isEmpty()) {
         return empty();
      }

      SuffixArray<T> tree = new SuffixArray<>();

      for (T element : elements) {
         idGetter.apply(element).forEach(elementId -> tree.add(element, elementId.toLowerCase(Locale.ROOT)));
      }

      tree.generate();
      return tree::search;
   }

   List<T> search(String text);
}
