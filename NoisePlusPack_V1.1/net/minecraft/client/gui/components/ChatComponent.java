package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_CHAT_HISTORY = 100;
   private static final int MESSAGE_INDENT = 4;
   private static final int BOTTOM_MARGIN = 40;
   private static final int TOOLTIP_MAX_WIDTH = 210;
   private static final int TIME_BEFORE_MESSAGE_DELETION = 60;
   private static final Component DELETED_CHAT_MESSAGE = Component.translatable("chat.deleted_marker").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   public static final int MESSAGE_BOTTOM_TO_MESSAGE_TOP = 8;
   public static final Identifier QUEUE_EXPAND_ID = Identifier.withDefaultNamespace("internal/expand_chat_queue");
   private static final Style QUEUE_EXPAND_TEXT_STYLE = Style.EMPTY
      .withClickEvent(new ClickEvent.Custom(QUEUE_EXPAND_ID, Optional.empty()))
      .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.queue.tooltip")));
   final Minecraft minecraft;
   private final ArrayListDeque<String> recentChat = new ArrayListDeque<>(100);
   private final List<GuiMessage> allMessages = Lists.newArrayList();
   private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
   private int chatScrollbarPos;
   private boolean newMessageSinceScroll;
   private ChatComponent.@Nullable Draft latestDraft;
   private @Nullable ChatScreen preservedScreen;
   private final List<ChatComponent.DelayedMessageDeletion> messageDeletionQueue = new ArrayList<>();

   public ChatComponent(Minecraft p_93768_) {
      this.minecraft = p_93768_;
      this.recentChat.addAll(p_93768_.commandHistory().history());
   }

   public void tick() {
      if (!this.messageDeletionQueue.isEmpty()) {
         this.processMessageDeletionQueue();
      }
   }

   private int forEachLine(ChatComponent.AlphaCalculator p_454794_, ChatComponent.LineConsumer p_406479_) {
      int i = this.getLinesPerPage();
      int j = 0;

      for (int k = Math.min(this.trimmedMessages.size() - this.chatScrollbarPos, i) - 1; k >= 0; k--) {
         int l = k + this.chatScrollbarPos;
         GuiMessage.Line guimessage$line = this.trimmedMessages.get(l);
         float f = p_454794_.calculate(guimessage$line);
         if (f > 1.0E-5F) {
            j++;
            p_406479_.accept(guimessage$line, k, f);
         }
      }

      return j;
   }

   public void render(GuiGraphics p_453098_, Font p_460468_, int p_459575_, int p_457862_, int p_453530_, boolean p_454503_, boolean p_459511_) {
      p_453098_.pose().pushMatrix();
      this.render(
         p_454503_
            ? new ChatComponent.DrawingFocusedGraphicsAccess(p_453098_, p_460468_, p_457862_, p_453530_, p_459511_)
            : new ChatComponent.DrawingBackgroundGraphicsAccess(p_453098_),
         p_453098_.guiHeight(),
         p_459575_,
         p_454503_
      );
      p_453098_.pose().popMatrix();
   }

   public void captureClickableText(ActiveTextCollector p_459682_, int p_450155_, int p_458921_, boolean p_450711_) {
      this.render(new ChatComponent.ClickableTextOnlyGraphicsAccess(p_459682_), p_450155_, p_458921_, p_450711_);
   }

   private void render(final ChatComponent.ChatGraphicsAccess p_454435_, int p_283491_, int p_282406_, boolean p_328818_) {
      if (!this.isChatHidden()) {
         int i = this.trimmedMessages.size();
         if (i > 0) {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("chat");
            float f = (float)this.getScale();
            int j = Mth.ceil(this.getWidth() / f);
            final int k = Mth.floor((p_283491_ - 40) / f);
            final float f1 = this.minecraft.options.chatOpacity().get().floatValue() * 0.9F + 0.1F;
            float f2 = this.minecraft.options.textBackgroundOpacity().get().floatValue();
            int l = 9;
            int i1 = 8;
            double d0 = this.minecraft.options.chatLineSpacing().get();
            final int j1 = (int)(9.0 * (d0 + 1.0));
            final int k1 = (int)Math.round(8.0 * (d0 + 1.0) - 4.0 * d0);
            long l1 = this.minecraft.getChatListener().queueSize();
            ChatComponent.AlphaCalculator chatcomponent$alphacalculator = p_328818_
               ? ChatComponent.AlphaCalculator.FULLY_VISIBLE
               : ChatComponent.AlphaCalculator.timeBased(p_282406_);
            p_454435_.updatePose(p_447966_ -> {
               p_447966_.scale(f, f);
               p_447966_.translate(4.0F, 0.0F);
            });
            this.forEachLine(chatcomponent$alphacalculator, (p_447962_, p_447963_, p_447964_) -> {
               int j4 = k - p_447963_ * j1;
               int k4 = j4 - j1;
               p_454435_.fill(-4, k4, j + 4 + 4, j4, ARGB.black(p_447964_ * f2));
            });
            if (l1 > 0L) {
               p_454435_.fill(-2, k, j + 4, k + 9, ARGB.black(f2));
            }

            int i2 = this.forEachLine(chatcomponent$alphacalculator, new ChatComponent.LineConsumer() {
               boolean hoveredOverCurrentMessage;

               @Override
               public void accept(GuiMessage.Line p_458643_, int p_459782_, float p_460027_) {
                  int j4 = k - p_459782_ * j1;
                  int k4 = j4 - j1;
                  int l4 = j4 - k1;
                  boolean flag = p_454435_.handleMessage(l4, p_460027_ * f1, p_458643_.content());
                  this.hoveredOverCurrentMessage |= flag;
                  boolean flag1;
                  if (p_458643_.endOfEntry()) {
                     flag1 = this.hoveredOverCurrentMessage;
                     this.hoveredOverCurrentMessage = false;
                  } else {
                     flag1 = false;
                  }

                  GuiMessageTag guimessagetag = p_458643_.tag();
                  if (guimessagetag != null) {
                     p_454435_.handleTag(-4, k4, -2, j4, p_460027_ * f1, guimessagetag);
                     if (guimessagetag.icon() != null) {
                        int i5 = p_458643_.getTagIconLeft(ChatComponent.this.minecraft.font);
                        int j5 = l4 + 9;
                        p_454435_.handleTagIcon(i5, j5, flag1, guimessagetag, guimessagetag.icon());
                     }
                  }
               }
            });
            if (l1 > 0L) {
               int j2 = k + 9;
               Component component = Component.translatable("chat.queue", l1).setStyle(QUEUE_EXPAND_TEXT_STYLE);
               p_454435_.handleMessage(j2 - 8, 0.5F * f1, component.getVisualOrderText());
            }

            if (p_328818_) {
               int l3 = i * j1;
               int i4 = i2 * j1;
               int k2 = this.chatScrollbarPos * i4 / i - k;
               int l2 = i4 * i4 / l3;
               if (l3 != i4) {
                  int i3 = k2 > 0 ? 170 : 96;
                  int j3 = this.newMessageSinceScroll ? 13382451 : 3355562;
                  int k3 = j + 4;
                  p_454435_.fill(k3, -k2, k3 + 2, -k2 - l2, ARGB.color(i3, j3));
                  p_454435_.fill(k3 + 2, -k2, k3 + 1, -k2 - l2, ARGB.color(i3, 13421772));
               }
            }

            profilerfiller.pop();
         }
      }
   }

   private boolean isChatHidden() {
      return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
   }

   public void clearMessages(boolean p_93796_) {
      this.minecraft.getChatListener().flushQueue();
      this.messageDeletionQueue.clear();
      this.trimmedMessages.clear();
      this.allMessages.clear();
      if (p_93796_) {
         this.recentChat.clear();
         this.recentChat.addAll(this.minecraft.commandHistory().history());
      }
   }

   public void addMessage(Component p_93786_) {
      this.addMessage(p_93786_, null, this.minecraft.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system());
   }

   public void addMessage(Component p_241484_, @Nullable MessageSignature p_241323_, @Nullable GuiMessageTag p_241297_) {
      GuiMessage guimessage = new GuiMessage(this.minecraft.gui.getGuiTicks(), p_241484_, p_241323_, p_241297_);
      this.logChatMessage(guimessage);
      this.addMessageToDisplayQueue(guimessage);
      this.addMessageToQueue(guimessage);
   }

   private void logChatMessage(GuiMessage p_328461_) {
      String s = p_328461_.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
      String s1 = Optionull.map(p_328461_.tag(), GuiMessageTag::logTag);
      if (s1 != null) {
         LOGGER.info("[{}] [CHAT] {}", s1, s);
      } else {
         LOGGER.info("[CHAT] {}", s);
      }
   }

   private void addMessageToDisplayQueue(GuiMessage p_332173_) {
      int i = Mth.floor(this.getWidth() / this.getScale());
      List<FormattedCharSequence> list = p_332173_.splitLines(this.minecraft.font, i);
      boolean flag = this.isChatFocused();

      for (int j = 0; j < list.size(); j++) {
         FormattedCharSequence formattedcharsequence = list.get(j);
         if (flag && this.chatScrollbarPos > 0) {
            this.newMessageSinceScroll = true;
            this.scrollChat(1);
         }

         boolean flag1 = j == list.size() - 1;
         this.trimmedMessages.addFirst(new GuiMessage.Line(p_332173_.addedTime(), formattedcharsequence, p_332173_.tag(), flag1));
      }

      while (this.trimmedMessages.size() > 100) {
         this.trimmedMessages.removeLast();
      }
   }

   private void addMessageToQueue(GuiMessage p_330648_) {
      this.allMessages.addFirst(p_330648_);

      while (this.allMessages.size() > 100) {
         this.allMessages.removeLast();
      }
   }

   private void processMessageDeletionQueue() {
      int i = this.minecraft.gui.getGuiTicks();
      this.messageDeletionQueue.removeIf(p_250713_ -> i >= p_250713_.deletableAfter() ? this.deleteMessageOrDelay(p_250713_.signature()) == null : false);
   }

   public void deleteMessage(MessageSignature p_241324_) {
      ChatComponent.DelayedMessageDeletion chatcomponent$delayedmessagedeletion = this.deleteMessageOrDelay(p_241324_);
      if (chatcomponent$delayedmessagedeletion != null) {
         this.messageDeletionQueue.add(chatcomponent$delayedmessagedeletion);
      }
   }

   private ChatComponent.@Nullable DelayedMessageDeletion deleteMessageOrDelay(MessageSignature p_251812_) {
      int i = this.minecraft.gui.getGuiTicks();
      ListIterator<GuiMessage> listiterator = this.allMessages.listIterator();

      while (listiterator.hasNext()) {
         GuiMessage guimessage = listiterator.next();
         if (p_251812_.equals(guimessage.signature())) {
            int j = guimessage.addedTime() + 60;
            if (i >= j) {
               listiterator.set(this.createDeletedMarker(guimessage));
               this.refreshTrimmedMessages();
               return null;
            }

            return new ChatComponent.DelayedMessageDeletion(p_251812_, j);
         }
      }

      return null;
   }

   private GuiMessage createDeletedMarker(GuiMessage p_249789_) {
      return new GuiMessage(p_249789_.addedTime(), DELETED_CHAT_MESSAGE, null, GuiMessageTag.system());
   }

   public void rescaleChat() {
      this.resetChatScroll();
      this.refreshTrimmedMessages();
   }

   private void refreshTrimmedMessages() {
      this.trimmedMessages.clear();

      for (GuiMessage guimessage : Lists.reverse(this.allMessages)) {
         this.addMessageToDisplayQueue(guimessage);
      }
   }

   public ArrayListDeque<String> getRecentChat() {
      return this.recentChat;
   }

   public void addRecentChat(String p_93784_) {
      if (!p_93784_.equals(this.recentChat.peekLast())) {
         if (this.recentChat.size() >= 100) {
            this.recentChat.removeFirst();
         }

         this.recentChat.addLast(p_93784_);
      }

      if (p_93784_.startsWith("/")) {
         this.minecraft.commandHistory().addCommand(p_93784_);
      }
   }

   public void resetChatScroll() {
      this.chatScrollbarPos = 0;
      this.newMessageSinceScroll = false;
   }

   public void scrollChat(int p_205361_) {
      this.chatScrollbarPos += p_205361_;
      int i = this.trimmedMessages.size();
      if (this.chatScrollbarPos > i - this.getLinesPerPage()) {
         this.chatScrollbarPos = i - this.getLinesPerPage();
      }

      if (this.chatScrollbarPos <= 0) {
         this.chatScrollbarPos = 0;
         this.newMessageSinceScroll = false;
      }
   }

   public boolean isChatFocused() {
      return this.minecraft.screen instanceof ChatScreen;
   }

   private int getWidth() {
      return getWidth(this.minecraft.options.chatWidth().get());
   }

   private int getHeight() {
      return getHeight(this.isChatFocused() ? this.minecraft.options.chatHeightFocused().get() : this.minecraft.options.chatHeightUnfocused().get());
   }

   private double getScale() {
      return this.minecraft.options.chatScale().get();
   }

   public static int getWidth(double p_93799_) {
      int i = 320;
      int j = 40;
      return Mth.floor(p_93799_ * 280.0 + 40.0);
   }

   public static int getHeight(double p_93812_) {
      int i = 180;
      int j = 20;
      return Mth.floor(p_93812_ * 160.0 + 20.0);
   }

   public static double defaultUnfocusedPct() {
      int i = 180;
      int j = 20;
      return 70.0 / (getHeight(1.0) - 20);
   }

   public int getLinesPerPage() {
      return this.getHeight() / this.getLineHeight();
   }

   private int getLineHeight() {
      return (int)(9.0 * (this.minecraft.options.chatLineSpacing().get() + 1.0));
   }

   public void saveAsDraft(String p_422280_) {
      boolean flag = p_422280_.startsWith("/");
      this.latestDraft = new ChatComponent.Draft(p_422280_, flag ? ChatComponent.ChatMethod.COMMAND : ChatComponent.ChatMethod.MESSAGE);
   }

   public void discardDraft() {
      this.latestDraft = null;
   }

   public <T extends ChatScreen> T createScreen(ChatComponent.ChatMethod p_423358_, ChatScreen.ChatConstructor<T> p_424105_) {
      return this.latestDraft != null && p_423358_.isDraftRestorable(this.latestDraft)
         ? p_424105_.create(this.latestDraft.text(), true)
         : p_424105_.create(p_423358_.prefix(), false);
   }

   public void openScreen(ChatComponent.ChatMethod p_427648_, ChatScreen.ChatConstructor<?> p_427995_) {
      this.minecraft.setScreen(this.createScreen(p_427648_, (ChatScreen.ChatConstructor<Screen>)p_427995_));
   }

   public void preserveCurrentChatScreen() {
      if (this.minecraft.screen instanceof ChatScreen chatscreen) {
         this.preservedScreen = chatscreen;
      }
   }

   public @Nullable ChatScreen restoreChatScreen() {
      ChatScreen chatscreen = this.preservedScreen;
      this.preservedScreen = null;
      return chatscreen;
   }

   public ChatComponent.State storeState() {
      return new ChatComponent.State(List.copyOf(this.allMessages), List.copyOf(this.recentChat), List.copyOf(this.messageDeletionQueue));
   }

   public void restoreState(ChatComponent.State p_330708_) {
      this.recentChat.clear();
      this.recentChat.addAll(p_330708_.history);
      this.messageDeletionQueue.clear();
      this.messageDeletionQueue.addAll(p_330708_.delayedMessageDeletions);
      this.allMessages.clear();
      this.allMessages.addAll(p_330708_.messages);
      this.refreshTrimmedMessages();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface AlphaCalculator {
      ChatComponent.AlphaCalculator FULLY_VISIBLE = p_460520_ -> 1.0F;

      static ChatComponent.AlphaCalculator timeBased(int p_459736_) {
         return p_450969_ -> {
            int i = p_459736_ - p_450969_.addedTime();
            double d0 = i / 200.0;
            d0 = 1.0 - d0;
            d0 *= 10.0;
            d0 = Mth.clamp(d0, 0.0, 1.0);
            d0 *= d0;
            return (float)d0;
         };
      }

      float calculate(GuiMessage.Line var1);
   }

   @OnlyIn(Dist.CLIENT)
   public interface ChatGraphicsAccess {
      void updatePose(Consumer<Matrix3x2f> var1);

      void fill(int var1, int var2, int var3, int var4, int var5);

      boolean handleMessage(int var1, float var2, FormattedCharSequence var3);

      void handleTag(int var1, int var2, int var3, int var4, float var5, GuiMessageTag var6);

      void handleTagIcon(int var1, int var2, boolean var3, GuiMessageTag var4, GuiMessageTag.Icon var5);
   }

   @OnlyIn(Dist.CLIENT)
   public enum ChatMethod {
      MESSAGE("") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft p_427449_) {
            return true;
         }
      },
      COMMAND("/") {
         @Override
         public boolean isDraftRestorable(ChatComponent.Draft p_424071_) {
            return this == p_424071_.chatMethod;
         }
      };

      private final String prefix;

      ChatMethod(final String p_427929_) {
         this.prefix = p_427929_;
      }

      public String prefix() {
         return this.prefix;
      }

      public abstract boolean isDraftRestorable(ChatComponent.Draft var1);
   }

   @OnlyIn(Dist.CLIENT)
   static class ClickableTextOnlyGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final ActiveTextCollector output;

      public ClickableTextOnlyGraphicsAccess(ActiveTextCollector p_459594_) {
         this.output = p_459594_;
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_457754_) {
         ActiveTextCollector.Parameters activetextcollector$parameters = this.output.defaultParameters();
         Matrix3x2f matrix3x2f = new Matrix3x2f(activetextcollector$parameters.pose());
         p_457754_.accept(matrix3x2f);
         this.output.defaultParameters(activetextcollector$parameters.withPose(matrix3x2f));
      }

      @Override
      public void fill(int p_454725_, int p_455250_, int p_456132_, int p_453204_, int p_456429_) {
      }

      @Override
      public boolean handleMessage(int p_460878_, float p_459568_, FormattedCharSequence p_460810_) {
         this.output.accept(TextAlignment.LEFT, 0, p_460878_, p_460810_);
         return false;
      }

      @Override
      public void handleTag(int p_453921_, int p_453520_, int p_452777_, int p_452128_, float p_456338_, GuiMessageTag p_459947_) {
      }

      @Override
      public void handleTagIcon(int p_454574_, int p_450520_, boolean p_458306_, GuiMessageTag p_452330_, GuiMessageTag.Icon p_455478_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   record DelayedMessageDeletion(MessageSignature signature, int deletableAfter) {
   }

   @OnlyIn(Dist.CLIENT)
   public record Draft(String text, ChatComponent.ChatMethod chatMethod) {
   }

   @OnlyIn(Dist.CLIENT)
   static class DrawingBackgroundGraphicsAccess implements ChatComponent.ChatGraphicsAccess {
      private final GuiGraphics graphics;
      private final ActiveTextCollector textRenderer;
      private ActiveTextCollector.Parameters parameters;

      public DrawingBackgroundGraphicsAccess(GuiGraphics p_453947_) {
         this.graphics = p_453947_;
         this.textRenderer = p_453947_.textRenderer(GuiGraphics.HoveredTextEffects.NONE, null);
         this.parameters = this.textRenderer.defaultParameters();
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_459885_) {
         p_459885_.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
      }

      @Override
      public void fill(int p_451673_, int p_456346_, int p_454400_, int p_458693_, int p_450893_) {
         this.graphics.fill(p_451673_, p_456346_, p_454400_, p_458693_, p_450893_);
      }

      @Override
      public boolean handleMessage(int p_460347_, float p_452818_, FormattedCharSequence p_454578_) {
         this.textRenderer.accept(TextAlignment.LEFT, 0, p_460347_, this.parameters.withOpacity(p_452818_), p_454578_);
         return false;
      }

      @Override
      public void handleTag(int p_455783_, int p_458356_, int p_453318_, int p_450870_, float p_455951_, GuiMessageTag p_454431_) {
         int i = ARGB.color(p_455951_, p_454431_.indicatorColor());
         this.graphics.fill(p_455783_, p_458356_, p_453318_, p_450870_, i);
      }

      @Override
      public void handleTagIcon(int p_457556_, int p_451833_, boolean p_450571_, GuiMessageTag p_456428_, GuiMessageTag.Icon p_453258_) {
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DrawingFocusedGraphicsAccess implements ChatComponent.ChatGraphicsAccess, Consumer<Style> {
      private final GuiGraphics graphics;
      private final Font font;
      private final ActiveTextCollector textRenderer;
      private ActiveTextCollector.Parameters parameters;
      private final int globalMouseX;
      private final int globalMouseY;
      private final Vector2f localMousePos = new Vector2f();
      private @Nullable Style hoveredStyle;
      private final boolean changeCursorOnInsertions;

      public DrawingFocusedGraphicsAccess(GuiGraphics p_450344_, Font p_456648_, int p_458887_, int p_458076_, boolean p_456016_) {
         this.graphics = p_450344_;
         this.font = p_456648_;
         this.textRenderer = p_450344_.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR, this);
         this.globalMouseX = p_458887_;
         this.globalMouseY = p_458076_;
         this.changeCursorOnInsertions = p_456016_;
         this.parameters = this.textRenderer.defaultParameters();
         this.updateLocalMousePos();
      }

      private void updateLocalMousePos() {
         this.graphics.pose().invert(new Matrix3x2f()).transformPosition(this.globalMouseX, this.globalMouseY, this.localMousePos);
      }

      @Override
      public void updatePose(Consumer<Matrix3x2f> p_455251_) {
         p_455251_.accept(this.graphics.pose());
         this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
         this.updateLocalMousePos();
      }

      @Override
      public void fill(int p_457603_, int p_454567_, int p_460020_, int p_454180_, int p_456487_) {
         this.graphics.fill(p_457603_, p_454567_, p_460020_, p_454180_, p_456487_);
      }

      public void accept(Style p_450657_) {
         this.hoveredStyle = p_450657_;
      }

      @Override
      public boolean handleMessage(int p_457933_, float p_454330_, FormattedCharSequence p_452767_) {
         this.hoveredStyle = null;
         this.textRenderer.accept(TextAlignment.LEFT, 0, p_457933_, this.parameters.withOpacity(p_454330_), p_452767_);
         if (this.changeCursorOnInsertions && this.hoveredStyle != null && this.hoveredStyle.getInsertion() != null) {
            this.graphics.requestCursor(CursorTypes.POINTING_HAND);
         }

         return this.hoveredStyle != null;
      }

      private boolean isMouseOver(int p_457295_, int p_450912_, int p_459411_, int p_453873_) {
         return ActiveTextCollector.isPointInRectangle(this.localMousePos.x, this.localMousePos.y, p_457295_, p_450912_, p_459411_, p_453873_);
      }

      @Override
      public void handleTag(int p_456518_, int p_457943_, int p_453583_, int p_451657_, float p_456783_, GuiMessageTag p_451852_) {
         int i = ARGB.color(p_456783_, p_451852_.indicatorColor());
         this.graphics.fill(p_456518_, p_457943_, p_453583_, p_451657_, i);
         if (this.isMouseOver(p_456518_, p_457943_, p_453583_, p_451657_)) {
            this.showTooltip(p_451852_);
         }
      }

      @Override
      public void handleTagIcon(int p_460153_, int p_456884_, boolean p_451883_, GuiMessageTag p_457511_, GuiMessageTag.Icon p_459914_) {
         int i = p_456884_ - p_459914_.height - 1;
         int j = p_460153_ + p_459914_.width;
         boolean flag = this.isMouseOver(p_460153_, i, j, p_456884_);
         if (flag) {
            this.showTooltip(p_457511_);
         }

         if (p_451883_ || flag) {
            p_459914_.draw(this.graphics, p_460153_, i);
         }
      }

      private void showTooltip(GuiMessageTag p_453767_) {
         if (p_453767_.text() != null) {
            this.graphics.setTooltipForNextFrame(this.font, this.font.split(p_453767_.text(), 210), this.globalMouseX, this.globalMouseY);
         }
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface LineConsumer {
      void accept(GuiMessage.Line var1, int var2, float var3);
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      final List<GuiMessage> messages;
      final List<String> history;
      final List<ChatComponent.DelayedMessageDeletion> delayedMessageDeletions;

      public State(List<GuiMessage> p_334723_, List<String> p_331396_, List<ChatComponent.DelayedMessageDeletion> p_332733_) {
         this.messages = p_334723_;
         this.history = p_331396_;
         this.delayedMessageDeletions = p_332733_;
      }
   }
}
