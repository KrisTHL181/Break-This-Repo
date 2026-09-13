package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class ServerboundSetJigsawBlockPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetJigsawBlockPacket> STREAM_CODEC = Packet.codec(
      ServerboundSetJigsawBlockPacket::write, ServerboundSetJigsawBlockPacket::new
   );
   private final BlockPos pos;
   private final Identifier name;
   private final Identifier target;
   private final Identifier pool;
   private final String finalState;
   private final JigsawBlockEntity.JointType joint;
   private final int selectionPriority;
   private final int placementPriority;

   public ServerboundSetJigsawBlockPacket(
      final BlockPos blockPos,
      final Identifier name,
      final Identifier target,
      final Identifier pool,
      final String finalState,
      final JigsawBlockEntity.JointType joint,
      final int selectionPriority,
      final int placementPriority
   ) {
      this.pos = blockPos;
      this.name = name;
      this.target = target;
      this.pool = pool;
      this.finalState = finalState;
      this.joint = joint;
      this.selectionPriority = selectionPriority;
      this.placementPriority = placementPriority;
   }

   private ServerboundSetJigsawBlockPacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.name = input.readIdentifier();
      this.target = input.readIdentifier();
      this.pool = input.readIdentifier();
      this.finalState = input.readUtf();
      this.joint = JigsawBlockEntity.JointType.CODEC.byName(input.readUtf(), JigsawBlockEntity.JointType.ALIGNED);
      this.selectionPriority = input.readVarInt();
      this.placementPriority = input.readVarInt();
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeIdentifier(this.name);
      output.writeIdentifier(this.target);
      output.writeIdentifier(this.pool);
      output.writeUtf(this.finalState);
      output.writeUtf(this.joint.getSerializedName());
      output.writeVarInt(this.selectionPriority);
      output.writeVarInt(this.placementPriority);
   }

   @Override
   public PacketType<ServerboundSetJigsawBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_JIGSAW_BLOCK;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSetJigsawBlock(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public Identifier getName() {
      return this.name;
   }

   public Identifier getTarget() {
      return this.target;
   }

   public Identifier getPool() {
      return this.pool;
   }

   public String getFinalState() {
      return this.finalState;
   }

   public JigsawBlockEntity.JointType getJoint() {
      return this.joint;
   }

   public int getSelectionPriority() {
      return this.selectionPriority;
   }

   public int getPlacementPriority() {
      return this.placementPriority;
   }
}
