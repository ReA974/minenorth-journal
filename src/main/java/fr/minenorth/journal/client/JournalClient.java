package fr.minenorth.journal.client;

import fr.minenorth.journal.Edition;
import net.minecraft.client.Minecraft;

/** Point d'entrée client (appelé par le paquet d'ouverture). */
public final class JournalClient {
    private JournalClient() {}

    public static void open(boolean edit, Edition e, java.util.List<String> photos) {
        Minecraft.getInstance().setScreen(edit ? new JournalEditScreen(e, photos) : new JournalReadScreen(e));
    }

    /** Liste des photos mise à jour par le serveur (après un import) : l'écran de choix la reprend et sélectionne la nouvelle image. */
    public static void photosUpdated(java.util.List<String> photos, String added) {
        ImagePickerScreen s = ImagePickerScreen.current;
        if (s != null) s.photosUpdated(photos, added);
    }
}
