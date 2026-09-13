package net.minecraft.client.gui.narration;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface NarrationElementOutput {
    default void add(final NarratedElementType type, final Component contents) {
        this.add(type, NarrationThunk.from(contents.getString()));
    }

    default void add(final NarratedElementType type, final String contents) {
        this.add(type, NarrationThunk.from(contents));
    }

    default void add(final NarratedElementType type, final Component... contents) {
        this.add(type, NarrationThunk.from(ImmutableList.copyOf(contents)));
    }

    void add(final NarratedElementType type, final NarrationThunk<?> contents);

    NarrationElementOutput nest();
}