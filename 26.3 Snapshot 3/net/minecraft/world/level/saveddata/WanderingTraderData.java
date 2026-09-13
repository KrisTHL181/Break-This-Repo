package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;

public class WanderingTraderData extends SavedData {
   public static final Codec<WanderingTraderData> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.INT.optionalFieldOf("spawn_delay", 24000).forGetter(data -> data.spawnDelay),
            Codec.INT.optionalFieldOf("spawn_chance", 25).forGetter(data -> data.spawnChance)
         )
         .apply(i, WanderingTraderData::new)
   );
   public static final SavedDataType<WanderingTraderData> TYPE = new SavedDataType<>(
      Identifier.withDefaultNamespace("wandering_trader"), WanderingTraderData::new, CODEC, DataFixTypes.SAVED_DATA_WANDERING_TRADER
   );
   private int spawnDelay;
   private int spawnChance;

   public WanderingTraderData() {
      this(24000, 25);
   }

   public WanderingTraderData(final int spawnDelay, final int spawnChance) {
      this.spawnDelay = spawnDelay;
      this.spawnChance = spawnChance;
   }

   public int spawnDelay() {
      return this.spawnDelay;
   }

   public void setSpawnDelay(final int spawnDelay) {
      if (this.spawnDelay != spawnDelay) {
         this.spawnDelay = spawnDelay;
         this.setDirty(true);
      }
   }

   public int spawnChance() {
      return this.spawnChance;
   }

   public void setSpawnChance(final int spawnChance) {
      if (this.spawnChance != spawnChance) {
         this.spawnChance = spawnChance;
         this.setDirty(true);
      }
   }
}
