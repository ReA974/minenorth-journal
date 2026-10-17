package fr.minenorth.journal.client;

import fr.minenorth.journal.Edition;
import fr.minenorth.journal.JournalNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Rédaction : titre du journal et jusqu'à 4 articles (titre + texte). Le serveur revérifie le droit et assainit les textes. */
public class JournalEditScreen extends Screen {
    private static final int W = 360, H = 250;

    private final String[] titles = new String[Edition.MAX_ARTICLES], bodies = new String[Edition.MAX_ARTICLES];
    private String journalTitle;
    private int current;
    private int left, top;
    private EditBox titleBox, articleTitle;
    private MultiLineEditBox body;

    public JournalEditScreen(Edition e) {
        super(Component.literal("Rédaction"));
        journalTitle = e.title();
        for (int i = 0; i < Edition.MAX_ARTICLES; i++) {
            titles[i] = i < e.articles().size() ? e.articles().get(i).title() : "";
            bodies[i] = i < e.articles().size() ? e.articles().get(i).body() : "";
        }
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        titleBox = new EditBox(font, left + 80, top + 12, W - 100, 16, Component.literal("Titre du journal"));
        titleBox.setMaxLength(Edition.MAX_TITLE);
        titleBox.setValue(journalTitle);
        titleBox.setResponder(v -> journalTitle = v);
        addRenderableWidget(titleBox);
        for (int i = 0; i < Edition.MAX_ARTICLES; i++) {
            int idx = i;
            addRenderableWidget(new FlatButton(left + 10 + i * 62, top + 38, 58, 16, Component.literal("Article " + (i + 1)), idx == current ? 0xFF2E7D32 : 0xFF5A4630, () -> select(idx)));
        }
        articleTitle = new EditBox(font, left + 10, top + 62, W - 20, 16, Component.literal("Titre de l'article"));
        articleTitle.setMaxLength(Edition.MAX_ART_TITLE);
        articleTitle.setValue(titles[current]);
        articleTitle.setResponder(v -> titles[current] = v);
        addRenderableWidget(articleTitle);
        body = new MultiLineEditBox(font, left + 10, top + 84, W - 20, 120, Component.literal("Texte de l'article…"), Component.empty());
        body.setCharacterLimit(Edition.MAX_BODY);
        body.setValue(bodies[current]);
        body.setValueListener(v -> bodies[current] = v);
        addRenderableWidget(body);
        addRenderableWidget(new FlatButton(left + 10, top + H - 30, 160, 20, Component.literal("Enregistrer l'édition"), 0xFF2E7D32, this::save));
        addRenderableWidget(new FlatButton(left + W - 170, top + H - 30, 160, 20, Component.literal("Annuler"), this::onClose));
    }

    private void select(int i) {
        current = i;
        rebuildWidgets();
    }

    private void save() {
        List<Edition.Article> list = new ArrayList<>();
        for (int i = 0; i < Edition.MAX_ARTICLES; i++) {
            if (!titles[i].isBlank() || !bodies[i].isBlank()) list.add(new Edition.Article(titles[i], bodies[i]));
        }
        JournalNet.CHANNEL.sendToServer(new JournalNet.SavePacket(new Edition(journalTitle, "", "", list)));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF3A2C1C);
        g.fill(left, top, left + W, top + H, 0xFFEDE3C8);
        g.drawString(font, "Journal :", left + 10, top + 16, 0xFF2A2018, false);
        g.drawString(font, "Titre de l'article", left + 10, top + 55, 0xFF6B5A40, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
