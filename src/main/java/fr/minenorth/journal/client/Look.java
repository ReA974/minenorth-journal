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
        if (!a.image().isEmpty()) { image(g, a.image(), x + 5, y + (h - 24) / 2, 1.5f); tx = x + 34; }
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
