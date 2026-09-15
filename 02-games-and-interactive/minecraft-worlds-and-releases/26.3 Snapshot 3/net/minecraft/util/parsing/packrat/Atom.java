package net.minecraft.util.parsing.packrat;

public record Atom<T>(String name) {
   @Override
   public String toString() {
      return "<" + this.name + ">";
   }

   public static <T> Atom<T> of(final String name) {
      return new Atom<>(name);
   }
}
