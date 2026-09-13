package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FullTextSearchTree<T> extends IdSearchTree<T> {
   private final SearchTree<T> plainTextSearchTree;

   public FullTextSearchTree(Function<T, Stream<String>> p_235155_, Function<T, Stream<Identifier>> p_235156_, List<T> p_235157_) {
      super(p_235156_, p_235157_);
      this.plainTextSearchTree = SearchTree.plainText(p_235157_, p_235155_);
   }

   @Override
   protected List<T> searchPlainText(String p_235160_) {
      return this.plainTextSearchTree.search(p_235160_);
   }

   @Override
   protected List<T> searchIdentifier(String p_451542_, String p_452559_) {
      List<T> list = this.identifierSearchTree.searchNamespace(p_451542_);
      List<T> list1 = this.identifierSearchTree.searchPath(p_452559_);
      List<T> list2 = this.plainTextSearchTree.search(p_452559_);
      Iterator<T> iterator = new MergingUniqueIterator<T>(list1.iterator(), list2.iterator(), this.additionOrder);
      return ImmutableList.copyOf(new IntersectionIterator<T>(list.iterator(), iterator, this.additionOrder));
   }
}
