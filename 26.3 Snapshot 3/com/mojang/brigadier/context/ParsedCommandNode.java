package com.mojang.brigadier.context;

import com.mojang.brigadier.tree.CommandNode;
import java.util.Objects;

public class ParsedCommandNode<S> {
   private final CommandNode<S> node;
   private final StringRange range;

   public ParsedCommandNode(CommandNode<S> node, StringRange range) {
      this.node = node;
      this.range = range;
   }

   public CommandNode<S> getNode() {
      return this.node;
   }

   public StringRange getRange() {
      return this.range;
   }

   @Override
   public String toString() {
      return this.node + "@" + this.range;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ParsedCommandNode<?> that = (ParsedCommandNode<?>)o;
         return Objects.equals(this.node, that.node) && Objects.equals(this.range, that.range);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.node, this.range);
   }
}
