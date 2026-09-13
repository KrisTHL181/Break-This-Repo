package com.mojang.datafixers.types.families;

import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.functions.PointFree;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ListAlgebra implements Algebra {
   private final String name;
   private final List<RewriteResult<?, ?>> views;
   private int hashCode;

   public ListAlgebra(String name, List<RewriteResult<?, ?>> views) {
      this.name = name;
      this.views = views;
   }

   @Override
   public RewriteResult<?, ?> apply(int index) {
      return this.views.get(index);
   }

   @Override
   public String toString() {
      return this.toString(0);
   }

   @Override
   public String toString(int level) {
      String wrap = "\n" + PointFree.indent(level + 1);
      return "Algebra["
         + this.name
         + wrap
         + this.views.stream().map(view -> view.view().function().toString(level + 1)).collect(Collectors.joining(wrap))
         + "\n"
         + PointFree.indent(level)
         + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof ListAlgebra that) ? false : Objects.equals(this.views, that.views);
      }
   }

   @Override
   public int hashCode() {
      if (this.hashCode == 0) {
         this.hashCode = this.views.hashCode();
      }

      return this.hashCode;
   }
}
