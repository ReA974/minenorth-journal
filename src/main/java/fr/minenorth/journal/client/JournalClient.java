package fr.minenorth.journal.client;

import fr.minenorth.journal.Edition;
import net.minecraft.client.Minecraft;

/** Point d'entrée client (appelé par le paquet d'ouverture). */
public final class JournalClient {
    private JournalClient() {}

    public static void open(boolean edit, Edition e) {
        Minecraft.getInstance().setScreen(edit ? new JournalEditScreen(e) : new JournalReadScreen(e));
    }
}
