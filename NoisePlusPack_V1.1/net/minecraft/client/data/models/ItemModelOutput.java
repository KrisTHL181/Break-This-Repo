package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
   default void accept(Item p_375406_, ItemModel.Unbaked p_376490_) {
      this.accept(p_375406_, p_376490_, ClientItem.Properties.DEFAULT);
   }

   void accept(Item var1, ItemModel.Unbaked var2, ClientItem.Properties var3);

   void copy(Item var1, Item var2);
}
