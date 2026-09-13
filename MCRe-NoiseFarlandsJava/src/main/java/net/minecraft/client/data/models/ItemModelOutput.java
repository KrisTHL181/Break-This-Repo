package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
    default void accept(final Item item, final ItemModel.Unbaked generator) {
        this.accept(item, generator, ClientItem.Properties.DEFAULT);
    }

    void accept(Item item, ItemModel.Unbaked generator, ClientItem.Properties properties);

    void copy(Item donor, Item acceptor);
}