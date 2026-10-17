package fr.minenorth.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Importation d'images par les journalistes : le client envoie un PNG en petits paquets, le serveur le réassemble, le vérifie (signature, taille,
 * dimensions, décodage), le ré-encode proprement (plus aucune donnée du fichier d'origine) puis l'enregistre dans le dossier des photos.
 * Le nom est imposé par le serveur (« pseudo_nom ») ; nombre de photos par joueur, débit et poids sont limités.
 */
public final class PhotoUpload {
    private PhotoUpload() {}

    public static final int CHUNK = 30_000, MAX_CHUNKS = 9, MAX_SIDE = 1024;
    private static final int COOLDOWN_TICKS = 100;

    private static final class Pending {
        final String name;
        final int total;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        int next;
        Pending(String name, int total) { this.name = name; this.total = total; }
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_DONE = new HashMap<>();

    public static void forget(UUID id) { PENDING.remove(id); LAST_DONE.remove(id); }

    private static void say(ServerPlayer p, String msg) { p.displayClientMessage(Component.literal(msg), true); }

    public static void chunk(ServerPlayer p, String rawName, int index, int total, byte[] part) {
        if (!Compat.isJournalist(p)) { say(p, "§cSeuls les journalistes importent des images."); return; }
        if (total < 1 || total > MAX_CHUNKS || index < 0 || index >= total || part.length == 0 || part.length > CHUNK) { PENDING.remove(p.getUUID()); return; }
        long now = p.server.getTickCount();
        if (index == 0) {
            Long last = LAST_DONE.get(p.getUUID());
            if (last != null && now - last < COOLDOWN_TICKS) { say(p, "§7Patientez quelques secondes entre deux imports."); return; }
            PENDING.put(p.getUUID(), new Pending(rawName, total));
        }
        Pending pd = PENDING.get(p.getUUID());
        if (pd == null || pd.total != total || pd.next != index) { PENDING.remove(p.getUUID()); return; }
        if (pd.data.size() + part.length > ImageStore.MAX_BYTES) { PENDING.remove(p.getUUID()); say(p, "§cImage trop lourde (256 Ko maximum)."); return; }
        pd.data.write(part, 0, part.length);
        pd.next++;
        if (pd.next < pd.total) return;
        PENDING.remove(p.getUUID());
        LAST_DONE.put(p.getUUID(), now);
        finish(p, pd.name, pd.data.toByteArray());
    }

    private static String pseudo(ServerPlayer p) {
        String s = p.getGameProfile().getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (s.isEmpty()) s = "joueur";
        return s.length() > 8 ? s.substring(0, 8) : s;
    }

    private static String base(String raw) {
        String s = raw == null ? "" : raw.toLowerCase(java.util.Locale.ROOT).trim();
        if (s.endsWith(".png") || s.endsWith(".jpg")) s = s.substring(0, s.length() - 4);
        else if (s.endsWith(".jpeg")) s = s.substring(0, s.length() - 5);
        s = s.replaceAll("[^a-z0-9_-]", "_").replaceAll("_+", "_");
        if (s.length() > 18) s = s.substring(0, 18);
        return s.isEmpty() ? "image" : s;
    }

    private static void finish(ServerPlayer p, String rawName, byte[] png) {
        if (png.length < 8 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') { say(p, "§cFichier refusé : ce n'est pas un PNG."); return; }
        String prefix = pseudo(p) + "_";
        if (ImageStore.countWithPrefix(prefix) >= JournalConfig.maxPhotosPerPlayer()) {
            say(p, "§cVous avez atteint votre limite de " + JournalConfig.maxPhotosPerPlayer() + " images (demandez à un admin d'en retirer).");
            return;
        }
        byte[] clean;
        try {
            clean = reencode(png);
        } catch (IOException | RuntimeException e) {
            say(p, "§cImage illisible ou trop grande (" + MAX_SIDE + " x " + MAX_SIDE + " pixels maximum).");
            return;
        }
        if (clean == null) { say(p, "§cImage refusée (illisible ou plus de " + MAX_SIDE + " pixels de côté)."); return; }
        if (clean.length > ImageStore.MAX_BYTES) { say(p, "§cImage trop lourde après conversion (256 Ko maximum)."); return; }
        String base = prefix + base(rawName), name = base;
        for (int i = 2; ImageStore.exists(name) && i < 100; i++) name = base + "_" + i;
        if (ImageStore.exists(name) || !ImageStore.save(name, clean)) { say(p, "§cImpossible d'enregistrer l'image."); return; }
        say(p, "§aImage importée : " + name);
        final String added = name;
        JournalNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new JournalNet.PhotoListPacket(ImageStore.names(), added));
    }

    /** Décode (dimensions vérifiées avant le décodage complet) puis ré-encode en PNG RVBA. */
    private static byte[] reencode(byte[] png) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("png");
            if (in == null || !it.hasNext()) return null;
            ImageReader r = it.next();
            try {
                r.setInput(in, true, true);
                int w = r.getWidth(0), h = r.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_SIDE || h > MAX_SIDE) return null;
                BufferedImage src = r.read(0);
                BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                dst.getGraphics().drawImage(src, 0, 0, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (!ImageIO.write(dst, "png", out)) return null;
                return out.toByteArray();
            } finally {
                r.dispose();
            }
        }
    }
}
