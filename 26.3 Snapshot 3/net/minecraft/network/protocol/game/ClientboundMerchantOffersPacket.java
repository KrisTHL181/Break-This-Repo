package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.trading.MerchantOffers;

public class ClientboundMerchantOffersPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMerchantOffersPacket> STREAM_CODEC = Packet.codec(
      ClientboundMerchantOffersPacket::write, ClientboundMerchantOffersPacket::new
   );
   private final int containerId;
   private final MerchantOffers offers;
   private final int villagerLevel;
   private final int villagerXp;
   private final boolean showProgress;
   private final boolean canRestock;

   public ClientboundMerchantOffersPacket(
      final int containerId, final MerchantOffers offers, final int merchantLevel, final int merchantXp, final boolean showProgress, final boolean canRestock
   ) {
      this.containerId = containerId;
      this.offers = offers.copy();
      this.villagerLevel = merchantLevel;
      this.villagerXp = merchantXp;
      this.showProgress = showProgress;
      this.canRestock = canRestock;
   }

   private ClientboundMerchantOffersPacket(final RegistryFriendlyByteBuf input) {
      this.containerId = input.readContainerId();
      this.offers = MerchantOffers.STREAM_CODEC.decode(input);
      this.villagerLevel = input.readVarInt();
      this.villagerXp = input.readVarInt();
      this.showProgress = input.readBoolean();
      this.canRestock = input.readBoolean();
   }

   private void write(final RegistryFriendlyByteBuf output) {
      output.writeContainerId(this.containerId);
      MerchantOffers.STREAM_CODEC.encode(output, this.offers);
      output.writeVarInt(this.villagerLevel);
      output.writeVarInt(this.villagerXp);
      output.writeBoolean(this.showProgress);
      output.writeBoolean(this.canRestock);
   }

   @Override
   public PacketType<ClientboundMerchantOffersPacket> type() {
      return GamePacketTypes.CLIENTBOUND_MERCHANT_OFFERS;
   }

   public void handle(final ClientGamePacketListener listener) {
      listener.handleMerchantOffers(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public MerchantOffers getOffers() {
      return this.offers;
   }

   public int getVillagerLevel() {
      return this.villagerLevel;
   }

   public int getVillagerXp() {
      return this.villagerXp;
   }

   public boolean showProgress() {
      return this.showProgress;
   }

   public boolean canRestock() {
      return this.canRestock;
   }
}
