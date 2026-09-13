package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface ClickEvent {
   Codec<ClickEvent> CODEC = ClickEvent.Action.CODEC.dispatch("action", ClickEvent::action, p_389908_ -> p_389908_.codec);

   ClickEvent.Action action();

   enum Action implements StringRepresentable {
      OPEN_URL("open_url", true, ClickEvent.OpenUrl.CODEC),
      OPEN_FILE("open_file", false, ClickEvent.OpenFile.CODEC),
      RUN_COMMAND("run_command", true, ClickEvent.RunCommand.CODEC),
      SUGGEST_COMMAND("suggest_command", true, ClickEvent.SuggestCommand.CODEC),
      SHOW_DIALOG("show_dialog", true, ClickEvent.ShowDialog.CODEC),
      CHANGE_PAGE("change_page", true, ClickEvent.ChangePage.CODEC),
      COPY_TO_CLIPBOARD("copy_to_clipboard", true, ClickEvent.CopyToClipboard.CODEC),
      CUSTOM("custom", true, ClickEvent.Custom.CODEC);

      public static final Codec<ClickEvent.Action> UNSAFE_CODEC = StringRepresentable.fromEnum(ClickEvent.Action::values);
      public static final Codec<ClickEvent.Action> CODEC = UNSAFE_CODEC.validate(ClickEvent.Action::filterForSerialization);
      private final boolean allowFromServer;
      private final String name;
      final MapCodec<? extends ClickEvent> codec;

      Action(final String p_130642_, final boolean p_130643_, final MapCodec<? extends ClickEvent> p_396902_) {
         this.name = p_130642_;
         this.allowFromServer = p_130643_;
         this.codec = p_396902_;
      }

      public boolean isAllowedFromServer() {
         return this.allowFromServer;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public MapCodec<? extends ClickEvent> valueCodec() {
         return this.codec;
      }

      public static DataResult<ClickEvent.Action> filterForSerialization(ClickEvent.Action p_311653_) {
         return !p_311653_.isAllowedFromServer()
            ? DataResult.error(() -> "Click event type not allowed: " + p_311653_)
            : DataResult.success(p_311653_, Lifecycle.stable());
      }
   }

   record ChangePage(int page) implements ClickEvent {
      public static final MapCodec<ClickEvent.ChangePage> CODEC = RecordCodecBuilder.mapCodec(
         p_396959_ -> p_396959_.group(ExtraCodecs.POSITIVE_INT.fieldOf("page").forGetter(ClickEvent.ChangePage::page))
            .apply(p_396959_, ClickEvent.ChangePage::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.CHANGE_PAGE;
      }
   }

   record CopyToClipboard(String value) implements ClickEvent {
      public static final MapCodec<ClickEvent.CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec(
         p_398012_ -> p_398012_.group(Codec.STRING.fieldOf("value").forGetter(ClickEvent.CopyToClipboard::value))
            .apply(p_398012_, ClickEvent.CopyToClipboard::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.COPY_TO_CLIPBOARD;
      }
   }

   record Custom(Identifier id, Optional<Tag> payload) implements ClickEvent {
      public static final MapCodec<ClickEvent.Custom> CODEC = RecordCodecBuilder.mapCodec(
         p_448772_ -> p_448772_.group(
               Identifier.CODEC.fieldOf("id").forGetter(ClickEvent.Custom::id),
               ExtraCodecs.NBT.optionalFieldOf("payload").forGetter(ClickEvent.Custom::payload)
            )
            .apply(p_448772_, ClickEvent.Custom::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.CUSTOM;
      }
   }

   record OpenFile(String path) implements ClickEvent {
      public static final MapCodec<ClickEvent.OpenFile> CODEC = RecordCodecBuilder.mapCodec(
         p_391516_ -> p_391516_.group(Codec.STRING.fieldOf("path").forGetter(ClickEvent.OpenFile::path)).apply(p_391516_, ClickEvent.OpenFile::new)
      );

      public OpenFile(File p_397081_) {
         this(p_397081_.toString());
      }

      public OpenFile(Path p_392820_) {
         this(p_392820_.toFile());
      }

      public File file() {
         return new File(this.path);
      }

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.OPEN_FILE;
      }
   }

   record OpenUrl(URI uri) implements ClickEvent {
      public static final MapCodec<ClickEvent.OpenUrl> CODEC = RecordCodecBuilder.mapCodec(
         p_395349_ -> p_395349_.group(ExtraCodecs.UNTRUSTED_URI.fieldOf("url").forGetter(ClickEvent.OpenUrl::uri)).apply(p_395349_, ClickEvent.OpenUrl::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.OPEN_URL;
      }
   }

   record RunCommand(String command) implements ClickEvent {
      public static final MapCodec<ClickEvent.RunCommand> CODEC = RecordCodecBuilder.mapCodec(
         p_397684_ -> p_397684_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.RunCommand::command))
            .apply(p_397684_, ClickEvent.RunCommand::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.RUN_COMMAND;
      }
   }

   record ShowDialog(Holder<Dialog> dialog) implements ClickEvent {
      public static final MapCodec<ClickEvent.ShowDialog> CODEC = RecordCodecBuilder.mapCodec(
         p_406853_ -> p_406853_.group(Dialog.CODEC.fieldOf("dialog").forGetter(ClickEvent.ShowDialog::dialog)).apply(p_406853_, ClickEvent.ShowDialog::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.SHOW_DIALOG;
      }
   }

   record SuggestCommand(String command) implements ClickEvent {
      public static final MapCodec<ClickEvent.SuggestCommand> CODEC = RecordCodecBuilder.mapCodec(
         p_391836_ -> p_391836_.group(ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(ClickEvent.SuggestCommand::command))
            .apply(p_391836_, ClickEvent.SuggestCommand::new)
      );

      @Override
      public ClickEvent.Action action() {
         return ClickEvent.Action.SUGGEST_COMMAND;
      }
   }
}
