package net.minecraft.server.permissions;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

public class PermissionSetUnion implements PermissionSet {
   private final ReferenceSet<PermissionSet> permissions = new ReferenceArraySet();

   PermissionSetUnion(final PermissionSet first, final PermissionSet second) {
      this.permissions.add(first);
      this.permissions.add(second);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(final ReferenceSet<PermissionSet> oldPermissions, final PermissionSet other) {
      this.permissions.addAll(oldPermissions);
      this.permissions.add(other);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(final ReferenceSet<PermissionSet> oldPermissions, final ReferenceSet<PermissionSet> other) {
      this.permissions.addAll(oldPermissions);
      this.permissions.addAll(other);
      this.ensureNoUnionsWithinUnions();
   }

   @Override
   public boolean hasPermission(final Permission permission) {
      ObjectIterator var2 = this.permissions.iterator();

      while (var2.hasNext()) {
         PermissionSet set = (PermissionSet)var2.next();
         if (set.hasPermission(permission)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public PermissionSet union(final PermissionSet other) {
      return other instanceof PermissionSetUnion otherUnion
         ? new PermissionSetUnion(this.permissions, otherUnion.permissions)
         : new PermissionSetUnion(this.permissions, other);
   }

   @VisibleForTesting
   public ReferenceSet<PermissionSet> getPermissions() {
      return new ReferenceArraySet(this.permissions);
   }

   private void ensureNoUnionsWithinUnions() {
      ObjectIterator var1 = this.permissions.iterator();

      while (var1.hasNext()) {
         PermissionSet set = (PermissionSet)var1.next();
         if (set instanceof PermissionSetUnion) {
            throw new IllegalArgumentException("Cannot have PermissionSetUnion within another PermissionSetUnion");
         }
      }
   }
}
