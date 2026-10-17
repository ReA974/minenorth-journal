package fr.minenorth.journal;

import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Photos du journal : fichiers PNG que l'admin dépose dans config/minenorth_journal/images/ (nom en minuscules : lettres, chiffres, - et _).
 * Les journalistes les choisissent par leur nom (« photo:nom ») ; les clients les reçoivent du serveur à la demande. Taille limitée.
 */
public final class ImageStore {
    private ImageStore() {}

    public static final long MAX_BYTES = 262_144;
    public static final String PREFIX = "photo:";

    private static Path dir() { return FMLPaths.CONFIGDIR.get().resolve("minenorth_journal").resolve("images"); }

    /** Nom valide (assaini) ou chaîne vide. */
    public static String cleanName(String s) {
        if (s == null) return "";
        String n = s.trim().toLowerCase(Locale.ROOT);
        if (n.startsWith(PREFIX)) n = n.substring(PREFIX.length());
        return n.matches("[a-z0-9_-]{1,32}") ? n : "";
    }

    /** Noms des photos disponibles (crée le dossier s'il manque). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.forEach(f -> {
                    String fn = f.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!fn.endsWith(".png")) return;
                    String n = cleanName(fn.substring(0, fn.length() - 4));
                    try {
                        if (!n.isEmpty() && Files.size(f) <= MAX_BYTES) out.add(n);
                    } catch (IOException ignored) {}
                });
            }
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    /** Nombre de photos dont le nom commence par ce préfixe (« pseudo_ »). */
    public static int countWithPrefix(String prefix) {
        int n = 0;
        for (String s : names()) if (s.startsWith(prefix)) n++;
        return n;
    }

    /** Enregistre un PNG déjà vérifié sous un nom déjà assaini, sans écraser un fichier existant. */
    public static boolean save(String name, byte[] png) {
        String n = cleanName(name);
        if (n.isEmpty() || png.length > MAX_BYTES) return false;
        try {
            Files.createDirectories(dir());
            Files.write(dir().resolve(n + ".png"), png, java.nio.file.StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean exists(String name) { return !cleanName(name).isEmpty() && names().contains(cleanName(name)); }

    @Nullable
    public static byte[] load(String name) {
        String n = cleanName(name);
        if (n.isEmpty()) return null;
        try {
            Path f = dir().resolve(n + ".png");
            if (!Files.isRegularFile(f) || Files.size(f) > MAX_BYTES) return null;
            return Files.readAllBytes(f);
        } catch (IOException e) {
            return null;
        }
    }
}
