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
      Set<Object> set = new ObjectArraySet();
      LivingEntity livingentity = this.getOwner();
      set.add(this);

      while (livingentity instanceof OwnableEntity) {
         OwnableEntity ownableentity = (OwnableEntity)livingentity;
         LivingEntity livingentity1 = ownableentity.getOwner();
         if (set.contains(livingentity1)) {
            return null;
         }

         set.add(livingentity);
         livingentity = ownableentity.getOwner();
      }

      return livingentity;
   }
}
