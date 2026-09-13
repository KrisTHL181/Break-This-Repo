package net.minecraft.server.permissions;

import java.util.function.Predicate;

public record PermissionProviderCheck<T extends PermissionSetSupplier>(PermissionCheck test) implements Predicate<T> {
   public boolean test(T p_454486_) {
      return this.test.check(p_454486_.permissions());
   }
}
