package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ClientboundUpdateAttributesPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateAttributesPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ClientboundUpdateAttributesPacket::getEntityId,
      ClientboundUpdateAttributesPacket.AttributeSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list(128)),
      ClientboundUpdateAttributesPacket::getValues,
      ClientboundUpdateAttributesPacket::new
   );
   private final int entityId;
   private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes;

   public ClientboundUpdateAttributesPacket(final int entityId, final Collection<AttributeInstance> values) {
      this.entityId = entityId;
      this.attributes = Lists.newArrayList();

      for (AttributeInstance value : values) {
         this.attributes.add(new ClientboundUpdateAttributesPacket.AttributeSnapshot(value.getAttribute(), value.getBaseValue(), value.getModifiers()));
      }
   }

   private ClientboundUpdateAttributesPacket(final int entityId, final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes) {
      this.entityId = entityId;
      this.attributes = attributes;
   }

   @Override
   public PacketType<ClientboundUpdateAttributesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_UPDATE_ATTRIBUTES;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleUpdateAttributes(this);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public List<ClientboundUpdateAttributesPacket.AttributeSnapshot> getValues() {
      return this.attributes;
   }

   public record AttributeSnapshot(Holder<Attribute> attribute, double base, Collection<AttributeModifier> modifiers) {
      public static final StreamCodec<ByteBuf, AttributeModifier> MODIFIER_STREAM_CODEC = StreamCodec.composite(
         Identifier.STREAM_CODEC,
         AttributeModifier::id,
         ByteBufCodecs.DOUBLE,
         AttributeModifier::amount,
         AttributeModifier.Operation.STREAM_CODEC,
         AttributeModifier::operation,
         AttributeModifier::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateAttributesPacket.AttributeSnapshot> STREAM_CODEC = StreamCodec.composite(
         Attribute.STREAM_CODEC,
         ClientboundUpdateAttributesPacket.AttributeSnapshot::attribute,
         ByteBufCodecs.DOUBLE,
         ClientboundUpdateAttributesPacket.AttributeSnapshot::base,
         MODIFIER_STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)),
         ClientboundUpdateAttributesPacket.AttributeSnapshot::modifiers,
         ClientboundUpdateAttributesPacket.AttributeSnapshot::new
      );
   }
}
