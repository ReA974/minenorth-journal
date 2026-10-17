package fr.minenorth.journal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Bouton plat aux couleurs du papier journal. */
public class FlatButton extends AbstractButton {
    private final Runnable action;
    private final int color;

    public FlatButton(int x, int y, int w, int h, Component label, Runnable action) { this(x, y, w, h, label, 0xFF5A4630, action); }

    public FlatButton(int x, int y, int w, int h, Component label, int color, Runnable action) {
        super(x, y, w, h, label);
        this.action = action;
        this.color = color;
    }

    @Override public void onPress() { action.run(); }
    @Override protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }

    @Override
    public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        int bg = isHoveredOrFocused() ? lighten(color) : color;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.drawString(f, getMessage(), getX() + (width - f.width(getMessage())) / 2, getY() + (height - 8) / 2, 0xFFFFFFFF, false);
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 30), g = Math.min(255, ((c >> 8) & 255) + 30), b = Math.min(255, (c & 255) + 30);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
