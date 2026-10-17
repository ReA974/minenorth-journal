package fr.minenorth.journal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import fr.minenorth.journal.Edition;

import java.util.List;

/** Dessin partagé : image d'objet et encart publicitaire. */
final class Look {
    private Look() {}

    static final int[] AD_BG = {0xFFF3C9C4, 0xFFC9DCF3, 0xFFCDEBCB, 0xFFF3E3A8};
    static final int[] AD_EDGE = {0xFFB3261E, 0xFF1F5FA8, 0xFF2E7D32, 0xFFB8860B};
    static final String[] THEME_NAMES = {"Rouge", "Bleu", "Vert", "Or"};

    static ItemStack stack(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        Item it = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        return it == null ? ItemStack.EMPTY : new ItemStack(it);
    }

    /** Image dans une boîte bw x bh (centrée) : photo du serveur ou icône d'objet agrandie. Renvoie la hauteur occupée. */
    static int picture(GuiGraphics g, String id, int x, int y, int bw, int bh) {
        if (id == null || id.isEmpty()) return 0;
        if (id.startsWith("photo:")) {
            PhotoCache.Photo p = PhotoCache.get(id.substring(6));
            if (p == null) {
                g.fill(x, y, x + bw, y + bh, 0xFF8A826C);
                g.drawCenteredString(Minecraft.getInstance().font, "…", x + bw / 2, y + bh / 2 - 4, 0xFFFFFFFF);
                return bh;
            }
            float k = Math.min((float) bw / p.w(), (float) bh / p.h());
            int dw = Math.max(1, Math.round(p.w() * k)), dh = Math.max(1, Math.round(p.h() * k));
            g.blit(p.tex(), x + (bw - dw) / 2, y + (bh - dh) / 2, dw, dh, 0f, 0f, p.w(), p.h(), p.w(), p.h());
            return bh;
        }
        float s = Math.min(bw, bh) / 16f;
        image(g, id, x + (int) ((bw - 16 * s) / 2), y + (int) ((bh - 16 * s) / 2), s);
        return bh;
    }

    /** Image d'objet agrandie (x échelle), coin haut gauche en x, y. */
    static void image(GuiGraphics g, String id, int x, int y, float scale) {
        ItemStack st = stack(id);
        if (st.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(st, 0, 0);
        g.pose().popPose();
    }

    /** Encart publicitaire de largeur w et hauteur h. */
    static void ad(GuiGraphics g, Edition.Ad a, int x, int y, int w, int h) {
        Font f = Minecraft.getInstance().font;
        g.fill(x, y, x + w, y + h, AD_EDGE[a.theme()]);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, AD_BG[a.theme()]);
        int tx = x + 6;
        if (!a.image().isEmpty()) { picture(g, a.image(), x + 4, y + 4, h - 8, h - 8); tx = x + h; }
        g.drawString(f, Component.literal("PUB · " + a.name()).withStyle(net.minecraft.ChatFormatting.BOLD), tx, y + 4, AD_EDGE[a.theme()], false);
        List<FormattedCharSequence> lines = f.split(Component.literal(a.text()), x + w - 6 - tx);
        int ly = y + 15;
        for (FormattedCharSequence l : lines) {
            if (ly > y + h - 10) break;
            g.drawString(f, l, tx, ly, 0xFF2A2018, false);
            ly += 9;
        }
    }
}
