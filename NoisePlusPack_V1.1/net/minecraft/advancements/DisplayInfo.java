package net.minecraft.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public class DisplayInfo {
   public static final Codec<DisplayInfo> CODEC = RecordCodecBuilder.create(
      p_309653_ -> p_309653_.group(
            ItemStack.STRICT_CODEC.fieldOf("icon").forGetter(DisplayInfo::getIcon),
            ComponentSerialization.CODEC.fieldOf("title").forGetter(DisplayInfo::getTitle),
            ComponentSerialization.CODEC.fieldOf("description").forGetter(DisplayInfo::getDescription),
            ClientAsset.ResourceTexture.CODEC.optionalFieldOf("background").forGetter(DisplayInfo::getBackground),
            AdvancementType.CODEC.optionalFieldOf("frame", AdvancementType.TASK).forGetter(DisplayInfo::getType),
            Codec.BOOL.optionalFieldOf("show_toast", true).forGetter(DisplayInfo::shouldShowToast),
            Codec.BOOL.optionalFieldOf("announce_to_chat", true).forGetter(DisplayInfo::shouldAnnounceChat),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(DisplayInfo::isHidden)
         )
         .apply(p_309653_, DisplayInfo::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DisplayInfo> STREAM_CODEC = StreamCodec.ofMember(
      DisplayInfo::serializeToNetwork, DisplayInfo::fromNetwork
   );
   private final Component title;
   private final Component description;
   private final ItemStack icon;
   private final Optional<ClientAsset.ResourceTexture> background;
   private final AdvancementType type;
   private final boolean showToast;
   private final boolean announceChat;
   private final boolean hidden;
   private float x;
   private float y;

   public DisplayInfo(
      ItemStack p_14969_,
      Component p_14970_,
      Component p_14971_,
      Optional<ClientAsset.ResourceTexture> p_310626_,
      AdvancementType p_309985_,
      boolean p_14974_,
      boolean p_14975_,
      boolean p_14976_
   ) {
      this.title = p_14970_;
      this.description = p_14971_;
      this.icon = p_14969_;
      this.background = p_310626_;
      this.type = p_309985_;
      this.showToast = p_14974_;
      this.announceChat = p_14975_;
      this.hidden = p_14976_;
   }

   public void setLocation(float p_14979_, float p_14980_) {
      this.x = p_14979_;
      this.y = p_14980_;
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getDescription() {
      return this.description;
   }

   public ItemStack getIcon() {
      return this.icon;
   }

   public Optional<ClientAsset.ResourceTexture> getBackground() {
      return this.background;
   }

   public AdvancementType getType() {
      return this.type;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public boolean shouldShowToast() {
      return this.showToast;
   }

   public boolean shouldAnnounceChat() {
      return this.announceChat;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   private void serializeToNetwork(RegistryFriendlyByteBuf p_331479_) {
      ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_331479_, this.title);
      ComponentSerialization.TRUSTED_STREAM_CODEC.encode(p_331479_, this.description);
      ItemStack.STREAM_CODEC.encode(p_331479_, this.icon);
      p_331479_.writeEnum(this.type);
      int i = 0;
      if (this.background.isPresent()) {
         i |= 1;
      }

      if (this.showToast) {
         i |= 2;
      }

      if (this.hidden) {
         i |= 4;
      }

      p_331479_.writeInt(i);
      this.background.map(ClientAsset::id).ifPresent(p_331479_::writeIdentifier);
      p_331479_.writeFloat(this.x);
      p_331479_.writeFloat(this.y);
   }

   private static DisplayInfo fromNetwork(RegistryFriendlyByteBuf p_330340_) {
      Component component = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_330340_);
      Component component1 = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(p_330340_);
      ItemStack itemstack = ItemStack.STREAM_CODEC.decode(p_330340_);
      AdvancementType advancementtype = p_330340_.readEnum(AdvancementType.class);
      int i = p_330340_.readInt();
      Optional<ClientAsset.ResourceTexture> optional = (i & 1) != 0
         ? Optional.of(new ClientAsset.ResourceTexture(p_330340_.readIdentifier()))
         : Optional.empty();
      boolean flag = (i & 2) != 0;
      boolean flag1 = (i & 4) != 0;
      DisplayInfo displayinfo = new DisplayInfo(itemstack, component, component1, optional, advancementtype, flag, false, flag1);
      displayinfo.setLocation(p_330340_.readFloat(), p_330340_.readFloat());
      return displayinfo;
   }
}
