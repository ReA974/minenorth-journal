package fr.minenorth.journal.client;

import fr.minenorth.journal.JournalNet;
import fr.minenorth.journal.PhotoUpload;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Importation d'images depuis l'ordinateur du joueur : il dépose ses fichiers PNG ou JPG dans .minecraft/minenorth_journal/photos/, on les réduit
 * si besoin (512 pixels de côté puis moins) pour tenir dans 250 Ko, et on les envoie au serveur qui les revérifie entièrement.
 */
final class ImageImport {
    private ImageImport() {}

    private static final int MAX_BYTES = 250_000, MAX_SIDE = 512, MAX_SOURCE = 16_000_000;

    static Path dir() { return FMLPaths.GAMEDIR.get().resolve("minenorth_journal").resolve("photos"); }

    /** Noms des fichiers PNG / JPG du dossier (créé au besoin). */
    static List<String> files() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(dir());
            try (Stream<Path> s = Files.list(dir())) {
                s.forEach(f -> {
                    String n = f.getFileName().toString();
                    String l = n.toLowerCase(Locale.ROOT);
                    if (Files.isRegularFile(f) && (l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg"))) out.add(n);
                });
            }
        } catch (IOException ignored) {}
        Collections.sort(out);
        return out;
    }

    /** PNG prêt à l'envoi (réduit si nécessaire), ou null si le fichier est illisible ou impossible à réduire assez. */
    @Nullable
    static byte[] prepare(String file) {
        try {
            Path f = dir().resolve(file).normalize();
            if (!f.startsWith(dir()) || !Files.isRegularFile(f) || Files.size(f) > MAX_SOURCE) return null;
            BufferedImage src = ImageIO.read(f.toFile());
            if (src == null) return null;
            int side = MAX_SIDE;
            while (side >= 64) {
                double k = Math.min(1.0, (double) side / Math.max(src.getWidth(), src.getHeight()));
                int w = Math.max(1, (int) Math.round(src.getWidth() * k)), h = Math.max(1, (int) Math.round(src.getHeight() * k));
                BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = dst.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, w, h, null);
                g.dispose();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!ImageIO.write(dst, "png", out)) return null;
                if (out.size() <= MAX_BYTES) return out.toByteArray();
                side = (int) (side * 0.75);
            }
        } catch (IOException | RuntimeException ignored) {}
        return null;
    }

    /** Envoie le PNG au serveur en morceaux de 30 Ko. */
    static void send(String file, byte[] png) {
        int total = (png.length + PhotoUpload.CHUNK - 1) / PhotoUpload.CHUNK;
        if (total < 1 || total > PhotoUpload.MAX_CHUNKS) return;
        String name = file.length() > 60 ? file.substring(0, 60) : file;
        for (int i = 0; i < total; i++) {
            int from = i * PhotoUpload.CHUNK, to = Math.min(png.length, from + PhotoUpload.CHUNK);
            JournalNet.CHANNEL.sendToServer(new JournalNet.UploadChunkPacket(name, i, total, java.util.Arrays.copyOfRange(png, from, to)));
        }
    }
}
