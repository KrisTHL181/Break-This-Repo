package net.minecraft.server.permissions;

public interface PermissionSet {
   PermissionSet NO_PERMISSIONS = permission -> false;
   PermissionSet ALL_PERMISSIONS = permission -> true;

   boolean hasPermission(Permission permission);

   default PermissionSet union(final PermissionSet other) {
      return other instanceof PermissionSetUnion ? other.union(this) : new PermissionSetUnion(this, other);
   }
}
