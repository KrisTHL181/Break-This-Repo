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
      ClientboundUpdateAttributesPacket.AttributeSnapshot.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ClientboundUpdateAttributesPacket::getValues,
      ClientboundUpdateAttributesPacket::new
   );
   private final int entityId;
   private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes;

   public ClientboundUpdateAttributesPacket(int p_133580_, Collection<AttributeInstance> p_133581_) {
      this.entityId = p_133580_;
      this.attributes = Lists.newArrayList();

      for (AttributeInstance attributeinstance : p_133581_) {
         this.attributes
            .add(
               new ClientboundUpdateAttributesPacket.AttributeSnapshot(
                  attributeinstance.getAttribute(), attributeinstance.getBaseValue(), attributeinstance.getModifiers()
               )
            );
      }
   }

   private ClientboundUpdateAttributesPacket(int p_332663_, List<ClientboundUpdateAttributesPacket.AttributeSnapshot> p_327701_) {
      this.entityId = p_332663_;
      this.attributes = p_327701_;
   }

   @Override
   public PacketType<ClientboundUpdateAttributesPacket> type() {
      return GamePacketTypes.CLIENTBOUND_UPDATE_ATTRIBUTES;
   }

   public void handle(ClientGamePacketListener p_133587_) {
      p_133587_.handleUpdateAttributes(this);
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
