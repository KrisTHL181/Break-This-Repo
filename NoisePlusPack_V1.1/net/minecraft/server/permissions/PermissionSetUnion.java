package net.minecraft.server.permissions;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

public class PermissionSetUnion implements PermissionSet {
   private final ReferenceSet<PermissionSet> permissions = new ReferenceArraySet();

   PermissionSetUnion(PermissionSet p_457083_, PermissionSet p_454255_) {
      this.permissions.add(p_457083_);
      this.permissions.add(p_454255_);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(ReferenceSet<PermissionSet> p_455911_, PermissionSet p_453202_) {
      this.permissions.addAll(p_455911_);
      this.permissions.add(p_453202_);
      this.ensureNoUnionsWithinUnions();
   }

   private PermissionSetUnion(ReferenceSet<PermissionSet> p_460982_, ReferenceSet<PermissionSet> p_460805_) {
      this.permissions.addAll(p_460982_);
      this.permissions.addAll(p_460805_);
      this.ensureNoUnionsWithinUnions();
   }

   @Override
   public boolean hasPermission(Permission p_453572_) {
      ObjectIterator var2 = this.permissions.iterator();

      while (var2.hasNext()) {
         PermissionSet permissionset = (PermissionSet)var2.next();
         if (permissionset.hasPermission(p_453572_)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public PermissionSet union(PermissionSet p_460824_) {
      return p_460824_ instanceof PermissionSetUnion permissionsetunion
         ? new PermissionSetUnion(this.permissions, permissionsetunion.permissions)
         : new PermissionSetUnion(this.permissions, p_460824_);
   }

   @VisibleForTesting
   public ReferenceSet<PermissionSet> getPermissions() {
      return new ReferenceArraySet(this.permissions);
   }

   private void ensureNoUnionsWithinUnions() {
      ObjectIterator var1 = this.permissions.iterator();

      while (var1.hasNext()) {
         PermissionSet permissionset = (PermissionSet)var1.next();
         if (permissionset instanceof PermissionSetUnion) {
            throw new IllegalArgumentException("Cannot have PermissionSetUnion within another PermissionSetUnion");
         }
      }
   }
}
