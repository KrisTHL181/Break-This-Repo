package dev.miru.helper;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class KitUtil {
   public static Button button(Component c, Component t, Button.OnPress op, Integer w, Integer h, Integer x, Integer y) {
      if (c == null) {
         throw new IllegalArgumentException("Button text cannot be null");
      }

      if (op == null) {
         throw new IllegalArgumentException("Button action cannot be null");
      }

      Button.Builder builder = Button.builder(c, op);
      if (x != null && y != null) {
         builder.pos(x, y);
         if (w != null && h != null) {
            builder.size(w, h);
            if (t != null) {
               builder.tooltip(Tooltip.create(t));
            }

            return builder.build();
         } else {
            throw new IllegalArgumentException("Size cannot be null");
         }
      } else {
         throw new IllegalArgumentException("Position cannot be null");
      }
   }

   public static Button button(String c, String t, Button.OnPress op, Integer w, Integer h, Integer x, Integer y) {
      return button(comLit(c), comLit(t), op, w, h, x, y);
   }

   public static Button button(Component c, Component t, Button.OnPress op) {
      Button b = Button.builder(c, op).build();
      if (t != null) {
         b.setTooltip(tooltip(t));
      }

      return b;
   }

   public static Button button(String c, String t, Button.OnPress op) {
      return button(comLit(c), comLit(t), op);
   }

   public static EditBox editbox(Font f, String v, Component c, Component t, Integer w, Integer h, Integer x, Integer y, Integer m) {
      EditBox eb = new EditBox(f, x, y, w, h, c);
      if (t != null) {
         eb.setTooltip(tooltip(t));
      }

      if (m != null) {
         eb.setMaxLength(m);
      }

      if (v != null) {
         eb.setValue(v);
      }

      return eb;
   }

   public static EditBox editbox(Font f, String v, Component t, Integer w, Integer h, Integer x, Integer y) {
      return editbox(f, v, Component.literal(v), t, w, h, x, y, null);
   }

   public static MultiLineEditBox multilineeditBox(
      Font f, Integer x, Integer y, Integer w, Integer h, Integer c, Integer cs, Boolean s, Boolean b, Boolean d, Component ct, Component ph
   ) {
      MultiLineEditBox.Builder multiLineEditBoxBuilder = MultiLineEditBox.builder();
      if (x != null) {
         multiLineEditBoxBuilder.setX(x);
      }

      if (y != null) {
         multiLineEditBoxBuilder.setY(y);
      }

      if (b != null) {
         multiLineEditBoxBuilder.setShowBackground(b);
      }

      if (d != null) {
         multiLineEditBoxBuilder.setShowDecorations(d);
      }

      if (c != null) {
         multiLineEditBoxBuilder.setTextColor(c);
      }

      if (s != null) {
         multiLineEditBoxBuilder.setTextShadow(s);
      }

      if (cs != null) {
         multiLineEditBoxBuilder.setCursorColor(cs);
      }

      if (ph != null) {
         multiLineEditBoxBuilder.setPlaceholder(ph);
      }

      return multiLineEditBoxBuilder.build(f, w, h, ct);
   }

   public static Component comLit(String s) {
      return Component.literal(s);
   }

   public static Component comTrans(String s) {
      return Component.translatable(s);
   }

   public static Component comLitToTrans(Component component) {
      return component.toString().startsWith("translate") ? component : comTrans(component.getString());
   }

   public static Component comTransToLit(Component component) {
      return component.toString().startsWith("literal") ? component : Component.literal(component.getString());
   }

   public static Tooltip tooltip(String s) {
      return Tooltip.create(comLit(s));
   }

   public static Tooltip tooltip(Component c) {
      return Tooltip.create(c);
   }
}
