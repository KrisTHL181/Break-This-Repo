package net.minecraft.network.syncher;

import java.util.List;

public interface SyncedDataHolder {
   void onSyncedDataUpdated(EntityDataAccessor<?> var1);

   void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> var1);
}
