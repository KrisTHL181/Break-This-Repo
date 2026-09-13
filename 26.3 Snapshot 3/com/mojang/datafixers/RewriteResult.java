package com.mojang.datafixers;

import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.RecursivePoint;
import java.util.BitSet;
import java.util.Objects;

public record RewriteResult<A, B>(View<A, B> view, BitSet recData) {
   public static <A, B> RewriteResult<A, B> create(View<A, B> view, BitSet recData) {
      return new RewriteResult<>(view, recData);
   }

   public static <A> RewriteResult<A, A> nop(Type<A> type) {
      return new RewriteResult<>(View.nopView(type), new BitSet());
   }

   public <C> RewriteResult<C, B> compose(RewriteResult<C, A> that) {
      BitSet newData;
      if (this.view.type() instanceof RecursivePoint.RecursivePointType && that.view.type() instanceof RecursivePoint.RecursivePointType) {
         newData = (BitSet)this.recData.clone();
         newData.or(that.recData);
      } else {
         newData = this.recData;
      }

      return create(this.view.compose(that.view), newData);
   }

   @Override
   public String toString() {
      return "RR[" + this.view + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RewriteResult<?, ?> that = (RewriteResult<?, ?>)o;
         return Objects.equals(this.view, that.view);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.view.hashCode();
   }
}
