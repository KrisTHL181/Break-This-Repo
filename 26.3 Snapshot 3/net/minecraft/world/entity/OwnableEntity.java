package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface OwnableEntity {
   @Nullable EntityReference<LivingEntity> getOwnerReference();

   Level level();

   default @Nullable LivingEntity getOwner() {
      return EntityReference.getLivingEntity(this.getOwnerReference(), this.level());
   }

   default @Nullable LivingEntity getRootOwner() {
      Set<Object> seen = new ObjectArraySet();
      LivingEntity owner = this.getOwner();
      seen.add(this);

      while (owner instanceof OwnableEntity) {
         OwnableEntity ownableOwner = (OwnableEntity)owner;
         LivingEntity ownersOwner = ownableOwner.getOwner();
         if (seen.contains(ownersOwner)) {
            return null;
         }

         seen.add(owner);
         owner = ownableOwner.getOwner();
      }

      return owner;
   }
}
