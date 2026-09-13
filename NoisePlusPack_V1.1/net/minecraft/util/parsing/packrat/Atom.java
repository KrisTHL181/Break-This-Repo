package net.minecraft.util.parsing.packrat;

public record Atom<T>(String name) {
   @Override
   public String toString() {
      return "<" + this.name + ">";
   }

   public static <T> Atom<T> of(String p_335186_) {
      return new Atom<>(p_335186_);
   }
}
