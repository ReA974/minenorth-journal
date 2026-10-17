package fr.minenorth.journal.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Choix de l'image d'un article ou d'une publicité : « Photos » (images du serveur, avec miniatures, et import depuis son ordinateur),
 * « Objets » (les objets de son inventaire) ou « Aucune ». Renvoie l'identifiant retenu (« photo:nom » ou identifiant d'objet) au formulaire.
 */
public class ImagePickerScreen extends Screen {
    private static final int W = 380, H = 250, COLS = 5, CELL_W = 70, CELL_H = 56, ROWS = 3;
    private static final ResourceLocation PREVIEW = new ResourceLocation("minenorthjournal", "import_preview");
    /** Écran ouvert, pour que le paquet de mise à jour de la liste le retrouve. */
    @Nullable static ImagePickerScreen current;

    private final Screen parent;
    private final List<String> photos;
    private final Consumer<String> onPick;
    private int tab;               // 0 photos, 1 objets, 2 import
    private int scroll;
    private int left, top;
    private List<String> files = List.of();
    @Nullable private String selectedFile;
    @Nullable private byte[] prepared;
    private int previewW, previewH;
    private boolean previewReady;
    private String status = "";
    private boolean waitingPick;
    private final List<String> itemIds = new ArrayList<>();

    public ImagePickerScreen(Screen parent, List<String> photos, Consumer<String> onPick) {
        super(Component.literal("Choisir une image"));
        this.parent = parent;
        this.photos = photos;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        current = this;
        left = (width - W) / 2;
        top = (height - H) / 2;
        addRenderableWidget(new FlatButton(left + 10, top + 28, 90, 16, Component.literal("Photos"), tab == 0 ? 0xFF2E7D32 : 0xFF5A4630, () -> setTab(0)));
        addRenderableWidget(new FlatButton(left + 104, top + 28, 110, 16, Component.literal("Objets (inventaire)"), tab == 1 ? 0xFF2E7D32 : 0xFF5A4630, () -> setTab(1)));
        addRenderableWidget(new FlatButton(left + 218, top + 28, 100, 16, Component.literal("Importer…"), tab == 2 ? 0xFF2E7D32 : 0xFF8A5A14, () -> setTab(2)));
        addRenderableWidget(new FlatButton(left + W - 56, top + 28, 46, 16, Component.literal("Aucune"), 0xFF8A3A2A, () -> choose("")));
        addRenderableWidget(new FlatButton(left + W - 90, top + H - 26, 80, 18, Component.literal("Annuler"), this::onClose));
        if (tab == 2) {
            addRenderableWidget(new FlatButton(left + 10, top + H - 26, 120, 18, Component.literal("Ouvrir le dossier"), () -> Util.getPlatform().openFile(ImageImport.dir().toFile())));
            addRenderableWidget(new FlatButton(left + 134, top + H - 26, 110, 18, Component.literal("Envoyer l'image"), 0xFF2E7D32, this::sendSelected));
        }
    }

    private void setTab(int t) {
        tab = t;
        scroll = 0;
        status = "";
        if (t == 1) collectItems();
        if (t == 2) { files = ImageImport.files(); selectedFile = null; prepared = null; previewReady = false; }
        rebuildWidgets();
    }

    private void collectItems() {
        itemIds.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Inventory inv = mc.player.getInventory();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (List<ItemStack> list : List.of(inv.items, inv.offhand, inv.armor)) {
            for (ItemStack s : list) {
                if (s.isEmpty()) continue;
                var key = ForgeRegistries.ITEMS.getKey(s.getItem());
                if (key != null) seen.putIfAbsent(key.toString(), true);
            }
        }
        itemIds.addAll(seen.keySet());
    }

    private void choose(String id) {
        onPick.accept(id);
        onClose();
    }

    /** Mise à jour de la liste des photos par le serveur ; si un import vient d'aboutir, l'image est choisie directement. */
    void photosUpdated(List<String> list, String added) {
        photos.clear();
        photos.addAll(list);
        if (waitingPick && !added.isEmpty()) choose("photo:" + added);
    }

    private void selectFile(String f) {
        selectedFile = f;
        status = "";
        previewReady = false;
        prepared = ImageImport.prepare(f);
        if (prepared == null) { status = "§cImage illisible ou impossible à réduire."; return; }
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(prepared));
            Minecraft.getInstance().getTextureManager().register(PREVIEW, new DynamicTexture(img));
            previewW = img.getWidth();
            previewH = img.getHeight();
            previewReady = true;
            status = "§7" + previewW + " x " + previewH + " px, " + (prepared.length / 1024) + " Ko";
        } catch (Exception e) {
            prepared = null;
            status = "§cImage illisible.";
        }
    }

    private void sendSelected() {
        if (selectedFile == null || prepared == null) { status = "§cChoisissez d'abord un fichier dans la liste."; return; }
        ImageImport.send(selectedFile, prepared);
        waitingPick = true;
        status = "§eEnvoi au serveur…";
    }

    @Override
    public void onClose() {
        current = null;
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        if (current == this) current = null;
        Minecraft.getInstance().getTextureManager().release(PREVIEW);
        super.removed();
    }

    // ------------------------------------------------------------------ dessin

    private int gridX() { return left + 10; }
    private int gridY() { return top + 52; }

    private int itemCols() { return (W - 20) / 24; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF3A2C1C);
        g.fill(left, top, left + W, top + H, 0xFFEDE3C8);
        int ink = 0xFF2A2018, rule = 0xFF6B5A40;
        g.drawString(font, "Choisir une image", left + 10, top + 10, ink, false);
        List<Component> tooltip = null;
        ItemStack tipStack = ItemStack.EMPTY;
        if (tab == 0) {
            if (photos.isEmpty()) g.drawString(font, "Aucune photo sur le serveur : utilisez « Importer… » pour ajouter la vôtre.", gridX(), gridY() + 4, rule, false);
            int first = scroll * COLS;
            for (int i = 0; i < COLS * ROWS && first + i < photos.size(); i++) {
                int x = gridX() + (i % COLS) * CELL_W, y = gridY() + (i / COLS) * CELL_H;
                String name = photos.get(first + i);
                boolean hov = mx >= x && mx < x + CELL_W - 2 && my >= y && my < y + CELL_H - 2;
                g.fill(x, y, x + CELL_W - 2, y + CELL_H - 2, hov ? 0xFFB9A877 : 0xFFD9CCA6);
                Look.picture(g, "photo:" + name, x + 3, y + 3, CELL_W - 8, 38);
                g.drawString(font, font.plainSubstrByWidth(name, CELL_W - 8), x + 4, y + 43, ink, false);
            }
            g.drawString(font, "Molette : défiler · clic : choisir", left + 10, top + H - 22, rule, false);
        } else if (tab == 1) {
            if (itemIds.isEmpty()) g.drawString(font, "Votre inventaire est vide.", gridX(), gridY() + 4, rule, false);
            int cols = itemCols(), perPage = cols * 7;
            int first = scroll * cols;
            for (int i = 0; i < perPage && first + i < itemIds.size(); i++) {
                int x = gridX() + (i % cols) * 24, y = gridY() + (i / cols) * 24;
                boolean hov = mx >= x && mx < x + 22 && my >= y && my < y + 22;
                g.fill(x, y, x + 22, y + 22, hov ? 0xFFB9A877 : 0xFFD9CCA6);
                ItemStack st = Look.stack(itemIds.get(first + i));
                g.renderItem(st, x + 3, y + 3);
                if (hov) tipStack = st;
            }
            g.drawString(font, "Un objet Minecraft s'affiche en icône agrandie. Pour une vraie image, onglet « Photos » ou « Importer… ».", left + 10, top + H - 22, rule, false);
        } else {
            g.drawString(font, "Déposez des fichiers PNG ou JPG dans :", left + 10, top + 52, ink, false);
            g.drawString(font, ".minecraft/minenorth_journal/photos", left + 10, top + 62, 0xFF8A5A14, false);
            if (files.isEmpty()) g.drawString(font, "Dossier vide : « Ouvrir le dossier », glissez vos images, puis revenez ici.", left + 10, top + 80, rule, false);
            int rows = 9;
            for (int i = 0; i < rows && scroll + i < files.size(); i++) {
                int y = top + 78 + i * 13;
                String f = files.get(scroll + i);
                boolean sel = f.equals(selectedFile), hov = mx >= left + 10 && mx < left + 190 && my >= y && my < y + 12;
                g.fill(left + 10, y, left + 190, y + 12, sel ? 0xFF2E7D32 : hov ? 0xFFB9A877 : 0xFFD9CCA6);
                g.drawString(font, font.plainSubstrByWidth(f, 172), left + 14, y + 2, sel ? 0xFFFFFFFF : ink, false);
            }
            int px = left + 200, py = top + 52, pw = W - 210, ph = 120;
            g.fill(px, py, px + pw, py + ph, 0xFF8A826C);
            if (previewReady) {
                float k = Math.min((float) (pw - 4) / previewW, (float) (ph - 4) / previewH);
                int dw = Math.max(1, Math.round(previewW * k)), dh = Math.max(1, Math.round(previewH * k));
                g.blit(PREVIEW, px + (pw - dw) / 2, py + (ph - dh) / 2, dw, dh, 0f, 0f, previewW, previewH, previewW, previewH);
            } else {
                g.drawCenteredString(font, "aperçu", px + pw / 2, py + ph / 2 - 4, 0xFFFFFFFF);
            }
            g.drawString(font, status, px, py + ph + 6, ink, false);
        }
        super.render(g, mx, my, pt);
        if (!tipStack.isEmpty()) g.renderTooltip(font, tipStack, mx, my);
    }

    // ------------------------------------------------------------------ souris

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        if (tab == 0) {
            int first = scroll * COLS;
            for (int i = 0; i < COLS * ROWS && first + i < photos.size(); i++) {
                int x = gridX() + (i % COLS) * CELL_W, y = gridY() + (i / COLS) * CELL_H;
                if (mx >= x && mx < x + CELL_W - 2 && my >= y && my < y + CELL_H - 2) { choose("photo:" + photos.get(first + i)); return true; }
            }
        } else if (tab == 1) {
            int cols = itemCols(), first = scroll * cols;
            for (int i = 0; i < cols * 7 && first + i < itemIds.size(); i++) {
                int x = gridX() + (i % cols) * 24, y = gridY() + (i / cols) * 24;
                if (mx >= x && mx < x + 22 && my >= y && my < y + 22) { choose(itemIds.get(first + i)); return true; }
            }
        } else {
            for (int i = 0; i < 9 && scroll + i < files.size(); i++) {
                int y = top + 78 + i * 13;
                if (mx >= left + 10 && mx < left + 190 && my >= y && my < y + 12) { selectFile(files.get(scroll + i)); return true; }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = tab == 0 ? Math.max(0, (photos.size() + COLS - 1) / COLS - ROWS)
                : tab == 1 ? Math.max(0, (itemIds.size() + itemCols() - 1) / itemCols() - 7)
                : Math.max(0, files.size() - 9);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
