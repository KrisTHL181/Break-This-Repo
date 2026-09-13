package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;

public class ServerboundSetStructureBlockPacket implements Packet<ServerGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ServerboundSetStructureBlockPacket> STREAM_CODEC = Packet.codec(
      ServerboundSetStructureBlockPacket::write, ServerboundSetStructureBlockPacket::new
   );
   private static final int FLAG_IGNORE_ENTITIES = 1;
   private static final int FLAG_SHOW_AIR = 2;
   private static final int FLAG_SHOW_BOUNDING_BOX = 4;
   private static final int FLAG_STRICT = 8;
   private final BlockPos pos;
   private final StructureBlockEntity.UpdateType updateType;
   private final StructureMode mode;
   private final String name;
   private final BlockPos offset;
   private final Vec3i size;
   private final Mirror mirror;
   private final Rotation rotation;
   private final String data;
   private final boolean ignoreEntities;
   private final boolean strict;
   private final boolean showAir;
   private final boolean showBoundingBox;
   private final float integrity;
   private final long seed;

   public ServerboundSetStructureBlockPacket(
      final BlockPos pos,
      final StructureBlockEntity.UpdateType updateType,
      final StructureMode mode,
      final String name,
      final BlockPos offset,
      final Vec3i size,
      final Mirror mirror,
      final Rotation rotation,
      final String data,
      final boolean ignoreEntities,
      final boolean strict,
      final boolean showAir,
      final boolean showBoundingBox,
      final float integrity,
      final long seed
   ) {
      this.pos = pos;
      this.updateType = updateType;
      this.mode = mode;
      this.name = name;
      this.offset = offset;
      this.size = size;
      this.mirror = mirror;
      this.rotation = rotation;
      this.data = data;
      this.ignoreEntities = ignoreEntities;
      this.strict = strict;
      this.showAir = showAir;
      this.showBoundingBox = showBoundingBox;
      this.integrity = integrity;
      this.seed = seed;
   }

   private ServerboundSetStructureBlockPacket(final FriendlyByteBuf input) {
      this.pos = input.readBlockPos();
      this.updateType = input.readEnum(StructureBlockEntity.UpdateType.class);
      this.mode = input.readEnum(StructureMode.class);
      this.name = input.readUtf();
      int maxOffset = 48;
      this.offset = new BlockPos(Mth.clamp(input.readByte(), -48, 48), Mth.clamp(input.readByte(), -48, 48), Mth.clamp(input.readByte(), -48, 48));
      int maxSize = 48;
      this.size = new Vec3i(Mth.clamp(input.readByte(), 0, 48), Mth.clamp(input.readByte(), 0, 48), Mth.clamp(input.readByte(), 0, 48));
      this.mirror = input.readEnum(Mirror.class);
      this.rotation = input.readEnum(Rotation.class);
      this.data = input.readUtf(128);
      this.integrity = Mth.clamp(input.readFloat(), 0.0F, 1.0F);
      this.seed = input.readVarLong();
      int flags = input.readByte();
      this.ignoreEntities = (flags & 1) != 0;
      this.strict = (flags & 8) != 0;
      this.showAir = (flags & 2) != 0;
      this.showBoundingBox = (flags & 4) != 0;
   }

   private void write(final FriendlyByteBuf output) {
      output.writeBlockPos(this.pos);
      output.writeEnum(this.updateType);
      output.writeEnum(this.mode);
      output.writeUtf(this.name);
      output.writeByte(this.offset.getX());
      output.writeByte(this.offset.getY());
      output.writeByte(this.offset.getZ());
      output.writeByte(this.size.getX());
      output.writeByte(this.size.getY());
      output.writeByte(this.size.getZ());
      output.writeEnum(this.mirror);
      output.writeEnum(this.rotation);
      output.writeUtf(this.data);
      output.writeFloat(this.integrity);
      output.writeVarLong(this.seed);
      int flags = 0;
      if (this.ignoreEntities) {
         flags |= 1;
      }

      if (this.showAir) {
         flags |= 2;
      }

      if (this.showBoundingBox) {
         flags |= 4;
      }

      if (this.strict) {
         flags |= 8;
      }

      output.writeByte(flags);
   }

   @Override
   public PacketType<ServerboundSetStructureBlockPacket> type() {
      return GamePacketTypes.SERVERBOUND_SET_STRUCTURE_BLOCK;
   }

   public void handle(final ServerGamePacketListener listener) {
      listener.handleSetStructureBlock(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public StructureBlockEntity.UpdateType getUpdateType() {
      return this.updateType;
   }

   public StructureMode getMode() {
      return this.mode;
   }

   public String getName() {
      return this.name;
   }

   public BlockPos getOffset() {
      return this.offset;
   }

   public Vec3i getSize() {
      return this.size;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public String getData() {
      return this.data;
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   public boolean isStrict() {
      return this.strict;
   }

   public boolean isShowAir() {
      return this.showAir;
   }

   public boolean isShowBoundingBox() {
      return this.showBoundingBox;
   }

   public float getIntegrity() {
      return this.integrity;
   }

   public long getSeed() {
      return this.seed;
   }
}
