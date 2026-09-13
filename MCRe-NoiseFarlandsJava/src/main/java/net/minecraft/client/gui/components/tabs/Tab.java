package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Tab {
    Component getTabTitle();

    Component getTabExtraNarration();

    void visitChildren(final Consumer<AbstractWidget> childrenConsumer);

    void doLayout(final ScreenRectangle screenRectangle);

    Layout getLayout();
}