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
      BlockPos p_134573_,
      Identifier p_454382_,
      Identifier p_459114_,
      Identifier p_457873_,
      String p_134577_,
      JigsawBlockEntity.JointType p_134578_,
      int p_309767_,
      int p_310524_
   ) {
      this.pos = p_134573_;
      this.name = p_454382_;
      this.target = p_459114_;
      this.pool = p_457873_;
      this.finalState = p_134577_;
      this.joint = p_134578_;
      this.selectionPriority = p_309767_;
      this.placementPriority = p_310524_;
   }

   private ServerboundSetJigsawBlockPacket(FriendlyByteBuf p_179766_) {
      this.pos = p_179766_.readBlockPos();
      this.name = p_179766_.readIdentifier();
      this.target = p_179766_.readIdentifier();
      this.pool = p_179766_.readIdentifier();
      this.finalState = p_179766_.readUtf();
      this.joint = JigsawBlockEntity.JointType.CODEC.byName(p_179766_.readUtf(), JigsawBlockEntity.JointType.ALIGNED);
      this.selectionPriority = p_179766_.readVarInt();
      this.placementPriority = p_179766_.readVarInt();
   }

   private void write(FriendlyByteBuf p_134587_) {
      p_134587_.writeBlockPos(this.pos);
      p_134587_.writeIdentifier(this.name);
      p_134587_.writeIdentifier(this.target);
      p_134587_.writeIdentifier(this.pool);
      p_134587_.writeUtf(this.finalState);
      p_134587_.writeUtf(this.joint.getSerializedName());
      p_134587_.writeVarInt(this.selectionPriority);
      p_134587_.writeVarInt(this.placementPriority);
   }

   @Override
   public PacketType<ServerboundSetJigsawBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_JIGSAW_BLOCK;
   }

   public void handle(ServerGamePacketListener p_134584_) {
      p_134584_.handleSetJigsawBlock(this);
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
