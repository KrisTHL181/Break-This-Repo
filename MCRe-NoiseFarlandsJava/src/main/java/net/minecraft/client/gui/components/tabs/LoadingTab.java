package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingTab implements Tab {
    private final Component title;
    private final Component loadingTitle;
    protected final LinearLayout layout = LinearLayout.vertical();

    public LoadingTab(final Font font, final Component title, final Component loadingTitle) {
        this.title = title;
        this.loadingTitle = loadingTitle;
        LoadingDotsWidget loadingDotsWidget = new LoadingDotsWidget(font, loadingTitle);
        this.layout.defaultCellSetting().alignVerticallyMiddle().alignHorizontallyCenter();
        this.layout.addChild(loadingDotsWidget, layoutSettings -> layoutSettings.paddingBottom(30));
    }

    @Override
    public Component getTabTitle() {
        return this.title;
    }

    @Override
    public Component getTabExtraNarration() {
        return this.loadingTitle;
    }

    @Override
    public void visitChildren(final Consumer<AbstractWidget> childrenConsumer) {
        this.layout.visitWidgets(childrenConsumer);
    }

    @Override
    public void doLayout(final ScreenRectangle screenRectangle) {
        this.layout.arrangeElements();
        FrameLayout.alignInRectangle(this.layout, screenRectangle, 0.5F, 0.5F);
    }

    @Override
    public Layout getLayout() {
        return this.layout;
    }
}