package fr.minenorth.journal.client;

import fr.minenorth.journal.Edition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Lecture d'un journal : une couverture puis un article par page (image en tête), un encart publicitaire en bas de chaque page. */
public class JournalReadScreen extends Screen {
    private static final int W = 300, H = 240;
    private static final int PAPER = 0xFFEDE3C8, INK = 0xFF2A2018, RULE = 0xFF6B5A40, FRAME = 0xFF3A2C1C;
    private static final int AD_H = 40;

    private final Edition e;
    private int page, left, top;

    public JournalReadScreen(Edition e) {
        super(Component.literal("Journal"));
        this.e = e;
    }

    private int pages() { return 1 + e.articles().size(); }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(new FlatButton(left + 10, top + H - 22, 40, 16, Component.literal("<"), () -> { if (page > 0) page--; }));
        addRenderableWidget(new FlatButton(left + 56, top + H - 22, 40, 16, Component.literal(">"), () -> { if (page < pages() - 1) page++; }));
        addRenderableWidget(new FlatButton(left + W - 90, top + H - 22, 80, 16, Component.literal("Fermer"), this::onClose));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta < 0 && page < pages() - 1) page++;
        else if (delta > 0 && page > 0) page--;
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, FRAME);
        g.fill(left, top, left + W, top + H, PAPER);
        int cx = left + W / 2;
        int bottom = top + H - 28;
        boolean hasAd = !e.ads().isEmpty();
        int adTop = bottom - AD_H;
        int limit = hasAd ? adTop - 4 : bottom;
        if (page == 0) {
            String title = e.title().isEmpty() ? "Le Journal" : e.title();
            g.pose().pushPose();
            g.pose().scale(2f, 2f, 1f);
            g.drawCenteredString(font, title, cx / 2, (top + 24) / 2, INK);
            g.pose().popPose();
            g.fill(left + 20, top + 54, left + W - 20, top + 56, RULE);
            if (!e.date().isEmpty()) g.drawCenteredString(font, "Édition du " + e.date(), cx, top + 62, INK);
            if (!e.author().isEmpty()) g.drawCenteredString(font, font.plainSubstrByWidth(e.author(), W - 30), cx, top + 75, RULE);
            int y = top + 96;
            for (int i = 0; i < e.articles().size() && y < limit - 8; i++) {
                String t = e.articles().get(i).title();
                g.drawString(font, (i + 1) + ". " + font.plainSubstrByWidth(t.isEmpty() ? "Article" : t, W - 70), left + 24, y, INK, false);
                y += 12;
            }
            if (hasAd) Look.ad(g, e.ads().get(0), left + 12, adTop, W - 24, AD_H);
        } else {
            Edition.Article a = e.articles().get(page - 1);
            g.drawCenteredString(font, font.plainSubstrByWidth(a.title(), W - 30), cx, top + 12, INK);
            g.fill(left + 20, top + 25, left + W - 20, top + 26, RULE);
            int y = top + 32;
            if (!a.image().isEmpty()) {
                y += Look.picture(g, a.image(), left + 20, y, W - 40, 64) + 6;
            }
            List<FormattedCharSequence> lines = font.split(Component.literal(a.body()), W - 40);
            for (FormattedCharSequence l : lines) {
                if (y > limit - 10) break;
                g.drawString(font, l, left + 20, y, INK, false);
                y += 10;
            }
            if (hasAd) Look.ad(g, e.ads().get((page - 1 + 1) % e.ads().size()), left + 12, adTop, W - 24, AD_H);
        }
        g.drawString(font, (page + 1) + " / " + pages(), left + 104, top + H - 18, RULE, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
