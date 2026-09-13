package net.minecraft.world.entity.ai.gossip;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum GossipType implements StringRepresentable {
   MAJOR_NEGATIVE("major_negative", -5, 100, 10, 10),
   MINOR_NEGATIVE("minor_negative", -1, 200, 20, 20),
   MINOR_POSITIVE("minor_positive", 1, 25, 1, 5),
   MAJOR_POSITIVE("major_positive", 5, 20, 0, 20),
   TRADING("trading", 1, 25, 2, 20);

   public static final int REPUTATION_CHANGE_PER_EVENT = 25;
   public static final int REPUTATION_CHANGE_PER_EVERLASTING_MEMORY = 20;
   public static final int REPUTATION_CHANGE_PER_TRADE = 2;
   public final String id;
   public final int weight;
   public final int max;
   public final int decayPerDay;
   public final int decayPerTransfer;
   public static final Codec<GossipType> CODEC = StringRepresentable.fromEnum(GossipType::values);

   GossipType(final String p_26284_, final int p_26285_, final int p_26286_, final int p_26287_, final int p_26288_) {
      this.id = p_26284_;
      this.weight = p_26285_;
      this.max = p_26286_;
      this.decayPerDay = p_26287_;
      this.decayPerTransfer = p_26288_;
   }

   @Override
   public String getSerializedName() {
      return this.id;
   }
}
