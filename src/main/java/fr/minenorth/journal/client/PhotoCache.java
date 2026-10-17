package fr.minenorth.journal.client;

import com.mojang.blaze3d.platform.NativeImage;
import fr.minenorth.journal.JournalNet;
import fr.minenorth.journal.MineNorthJournal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Photos du journal reçues du serveur : demandées une seule fois, puis gardées en texture. */
public final class PhotoCache {
    private PhotoCache() {}

    record Photo(ResourceLocation tex, int w, int h) {}

    private static final Map<String, Photo> READY = new HashMap<>();
    private static final Set<String> REQUESTED = new HashSet<>();

    /** La photo si elle est arrivée, sinon null (et la demande part au serveur). */
    @Nullable
    static Photo get(String name) {
        Photo p = READY.get(name);
        if (p == null && REQUESTED.add(name)) JournalNet.CHANNEL.sendToServer(new JournalNet.ImageRequestPacket(name));
        return p;
    }

    /** Appelé par le paquet de données (thread client). */
    public static void receive(String name, byte[] data) {
        try {
            NativeImage img = NativeImage.read(new ByteArrayInputStream(data));
            ResourceLocation rl = new ResourceLocation(MineNorthJournal.MOD_ID, "photo/" + name);
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
            READY.put(name, new Photo(rl, img.getWidth(), img.getHeight()));
        } catch (Exception ignored) {
            // fichier illisible : on n'insiste pas
        }
    }
}
