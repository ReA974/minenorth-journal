package fr.minenorth.journal.client;

import fr.minenorth.journal.Edition;
import fr.minenorth.journal.JournalNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Rédaction : titre, jusqu'à 4 articles (titre, image, texte) et 3 encarts publicitaires. Le serveur revérifie le droit et assainit tout. */
public class JournalEditScreen extends Screen {
    private static final int W = 380, H = 290, ART = Edition.MAX_ARTICLES, ADS = Edition.MAX_ADS;

    private final String[] artTitle = new String[ART], artBody = new String[ART], artImg = new String[ART];
    private final String[] adName = new String[ADS], adText = new String[ADS], adImg = new String[ADS];
    private final int[] adTheme = new int[ADS];
    private String journalTitle;
    /** 0..3 : articles ; 4..6 : encarts publicitaires. */
    private int mode;
    private int left, top;
    private EditBox f1, f2, f3;
    private MultiLineEditBox body;

    /** Photos du serveur (liste modifiable : l'écran de choix la met à jour après un import). */
    private final List<String> photos;

    public JournalEditScreen(Edition e, List<String> photos) {
        super(Component.literal("Rédaction"));
        this.photos = new ArrayList<>(photos);
        journalTitle = e.title();
        for (int i = 0; i < ART; i++) {
            boolean has = i < e.articles().size();
            artTitle[i] = has ? e.articles().get(i).title() : "";
            artBody[i] = has ? e.articles().get(i).body() : "";
            artImg[i] = has ? e.articles().get(i).image() : "";
        }
        for (int i = 0; i < ADS; i++) {
            boolean has = i < e.ads().size();
            adName[i] = has ? e.ads().get(i).name() : "";
            adText[i] = has ? e.ads().get(i).text() : "";
            adImg[i] = has ? e.ads().get(i).image() : "";
            adTheme[i] = has ? e.ads().get(i).theme() : 0;
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        EditBox titleBox = new EditBox(font, left + 80, top + 10, W - 100, 16, Component.literal("Titre du journal"));
        titleBox.setMaxLength(Edition.MAX_TITLE);
        titleBox.setValue(journalTitle);
        titleBox.setResponder(v -> journalTitle = v);
        addRenderableWidget(titleBox);
        for (int i = 0; i < ART + ADS; i++) {
            int idx = i;
            String label = i < ART ? "Art. " + (i + 1) : "Pub " + (i - ART + 1);
            int color = i == mode ? 0xFF2E7D32 : (i < ART ? 0xFF5A4630 : 0xFF8A5A14);
            addRenderableWidget(new FlatButton(left + 10 + i * 52, top + 34, 50, 16, Component.literal(label), color, () -> select(idx)));
        }
        if (mode < ART) buildArticle(); else buildAd(mode - ART);
        addRenderableWidget(new FlatButton(left + 10, top + H - 28, 170, 20, Component.literal("Enregistrer l'édition"), 0xFF2E7D32, this::save));
        addRenderableWidget(new FlatButton(left + W - 180, top + H - 28, 170, 20, Component.literal("Annuler"), this::onClose));
    }

    private EditBox box(int x, int y, int w, int max, String value, java.util.function.Consumer<String> setter, String name) {
        EditBox b = new EditBox(font, x, y, w, 16, Component.literal(name));
        b.setMaxLength(max);
        b.setValue(value);
        b.setResponder(setter);
        addRenderableWidget(b);
        return b;
    }

    private void buildArticle() {
        int i = mode;
        f1 = box(left + 10, top + 72, W - 20, Edition.MAX_ART_TITLE, artTitle[i], v -> artTitle[i] = v, "Titre de l'article");
        f2 = box(left + 10, top + 106, W - 215, Edition.MAX_IMG, artImg[i], v -> artImg[i] = v, "Image");
        f2.setEditable(false);
        addRenderableWidget(new FlatButton(left + W - 195, top + 106, 120, 16, Component.literal("Choisir l'image…"), 0xFF2E7D32, () -> pick(v -> artImg[i] = v)));
        addRenderableWidget(new FlatButton(left + W - 70, top + 106, 60, 16, Component.literal("Retirer"), 0xFF8A3A2A, () -> { artImg[i] = ""; f2.setValue(""); }));
        body = new MultiLineEditBox(font, left + 10, top + 130, W - 20, 120, Component.literal("Texte de l'article…"), Component.empty());
        body.setCharacterLimit(Edition.MAX_BODY);
        body.setValue(artBody[i]);
        body.setValueListener(v -> artBody[i] = v);
        addRenderableWidget(body);
    }

    private void buildAd(int i) {
        f1 = box(left + 10, top + 72, W - 20, Edition.MAX_AD_NAME, adName[i], v -> adName[i] = v, "Annonceur");
        f3 = box(left + 10, top + 106, W - 20, Edition.MAX_AD_TEXT, adText[i], v -> adText[i] = v, "Texte de la publicité");
        f2 = box(left + 10, top + 140, W - 215, Edition.MAX_IMG, adImg[i], v -> adImg[i] = v, "Image");
        f2.setEditable(false);
        addRenderableWidget(new FlatButton(left + W - 195, top + 140, 120, 16, Component.literal("Choisir l'image…"), 0xFF2E7D32, () -> pick(v -> adImg[i] = v)));
        addRenderableWidget(new FlatButton(left + W - 70, top + 140, 60, 16, Component.literal("Retirer"), 0xFF8A3A2A, () -> { adImg[i] = ""; f2.setValue(""); }));
        addRenderableWidget(new FlatButton(left + 10, top + 170, 170, 18, Component.literal("Couleur : " + Look.THEME_NAMES[adTheme[i]]), Look.AD_EDGE[adTheme[i]],
                () -> { adTheme[i] = (adTheme[i] + 1) % Edition.THEMES; rebuildWidgets(); }));
    }

    /** Ouvre le choix de l'image (photos du serveur, import depuis l'ordinateur, objets de l'inventaire) ; le formulaire se reconstruit au retour. */
    private void pick(java.util.function.Consumer<String> setter) {
        Minecraft.getInstance().setScreen(new ImagePickerScreen(this, photos, setter));
    }

    private void select(int i) {
        mode = i;
        rebuildWidgets();
    }

    private void save() {
        List<Edition.Article> arts = new ArrayList<>();
        for (int i = 0; i < ART; i++) {
            if (!artTitle[i].isBlank() || !artBody[i].isBlank()) arts.add(new Edition.Article(artTitle[i], artBody[i], artImg[i]));
        }
        List<Edition.Ad> ads = new ArrayList<>();
        for (int i = 0; i < ADS; i++) {
            if (!adName[i].isBlank() || !adText[i].isBlank()) ads.add(new Edition.Ad(adName[i], adText[i], adImg[i], adTheme[i]));
        }
        JournalNet.CHANNEL.sendToServer(new JournalNet.SavePacket(new Edition(journalTitle, "", "", arts, ads)));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF3A2C1C);
        g.fill(left, top, left + W, top + H, 0xFFEDE3C8);
        int ink = 0xFF2A2018, rule = 0xFF6B5A40;
        g.drawString(font, "Journal :", left + 10, top + 14, ink, false);
        if (mode < ART) {
            g.drawString(font, "Titre de l'article", left + 10, top + 61, rule, false);
            g.drawString(font, "Image (photo, image importée ou objet ; facultatif)", left + 10, top + 95, rule, false);
            if (!artImg[mode].isEmpty()) Look.picture(g, artImg[mode], left + W - 60, top + 52, 48, 40);
        } else {
            int i = mode - ART;
            g.drawString(font, "Annonceur", left + 10, top + 61, rule, false);
            g.drawString(font, "Texte de la publicité", left + 10, top + 95, rule, false);
            g.drawString(font, "Image (photo, image importée ou objet ; facultatif)", left + 10, top + 129, rule, false);
            Look.ad(g, new Edition.Ad(adName[i].isEmpty() ? "Annonceur" : adName[i], adText[i], adImg[i], adTheme[i]), left + 10, top + 196, W - 20, 40);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
