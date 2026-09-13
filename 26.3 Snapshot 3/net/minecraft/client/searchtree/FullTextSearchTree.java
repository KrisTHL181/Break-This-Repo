package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

public class FullTextSearchTree<T> extends IdSearchTree<T> {
   private final SearchTree<T> plainTextSearchTree;

   public FullTextSearchTree(final Function<T, Stream<String>> nameGetter, final Function<T, Stream<Identifier>> idGetter, final List<T> contents) {
      super(idGetter, contents);
      this.plainTextSearchTree = SearchTree.plainText(contents, nameGetter);
   }

   @Override
   protected List<T> searchPlainText(final String text) {
      return this.plainTextSearchTree.search(text);
   }

   @Override
   protected List<T> searchIdentifier(final String namespace, final String path) {
      List<T> namespaces = this.identifierSearchTree.searchNamespace(namespace);
      List<T> paths = this.identifierSearchTree.searchPath(path);
      List<T> names = this.plainTextSearchTree.search(path);
      Iterator<T> mergedPathsAndNames = new MergingUniqueIterator<T>(paths.iterator(), names.iterator(), this.additionOrder);
      return ImmutableList.copyOf(new IntersectionIterator<T>(namespaces.iterator(), mergedPathsAndNames, this.additionOrder));
   }
}
