package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IdSearchTree<T> implements SearchTree<T> {
   protected final Comparator<T> additionOrder;
   protected final IdentifierSearchTree<T> identifierSearchTree;

   public IdSearchTree(Function<T, Stream<Identifier>> p_235167_, List<T> p_235168_) {
      ToIntFunction<T> tointfunction = Util.createIndexLookup(p_235168_);
      this.additionOrder = Comparator.comparingInt(tointfunction);
      this.identifierSearchTree = IdentifierSearchTree.create(p_235168_, p_235167_);
   }

   @Override
   public List<T> search(String p_235173_) {
      int i = p_235173_.indexOf(58);
      return i == -1 ? this.searchPlainText(p_235173_) : this.searchIdentifier(p_235173_.substring(0, i).trim(), p_235173_.substring(i + 1).trim());
   }

   protected List<T> searchPlainText(String p_235169_) {
      return this.identifierSearchTree.searchPath(p_235169_);
   }

   protected List<T> searchIdentifier(String p_457047_, String p_454707_) {
      List<T> list = this.identifierSearchTree.searchNamespace(p_457047_);
      List<T> list1 = this.identifierSearchTree.searchPath(p_454707_);
      return ImmutableList.copyOf(new IntersectionIterator<T>(list.iterator(), list1.iterator(), this.additionOrder));
   }
}
