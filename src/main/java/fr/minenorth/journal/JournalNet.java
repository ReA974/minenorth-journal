package fr.minenorth.journal;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/** Réseau du journal : le serveur ouvre l'écran (lecture ou rédaction), le client renvoie le texte rédigé (revérifié et assaini par le serveur). */
public final class JournalNet {
    private JournalNet() {}

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthJournal.MOD_ID, "network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenPacket.class, OpenPacket::encode, OpenPacket::decode, OpenPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, SavePacket.class, SavePacket::encode, SavePacket::decode, SavePacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, ImageRequestPacket.class, ImageRequestPacket::encode, ImageRequestPacket::decode, ImageRequestPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, ImageDataPacket.class, ImageDataPacket::encode, ImageDataPacket::decode, ImageDataPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, UploadChunkPacket.class, UploadChunkPacket::encode, UploadChunkPacket::decode, UploadChunkPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, PhotoListPacket.class, PhotoListPacket::encode, PhotoListPacket::decode, PhotoListPacket::handle, java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Client -> serveur : un morceau (30 Ko au plus) d'une image à importer ; tout est revérifié par {@link PhotoUpload}. */
    public record UploadChunkPacket(String name, int index, int total, byte[] data) {
        static void encode(UploadChunkPacket p, FriendlyByteBuf b) { b.writeUtf(p.name, 64); b.writeVarInt(p.index); b.writeVarInt(p.total); b.writeByteArray(p.data); }
        static UploadChunkPacket decode(FriendlyByteBuf b) { return new UploadChunkPacket(b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readByteArray(PhotoUpload.CHUNK)); }
        static void handle(UploadChunkPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> { ServerPlayer sp = c.get().getSender(); if (sp != null) PhotoUpload.chunk(sp, p.name, p.index, p.total, p.data); });
            c.get().setPacketHandled(true);
        }
    }

    /** Serveur -> client : liste à jour des photos (après un import) et nom de la photo qui vient d'être ajoutée. */
    public record PhotoListPacket(List<String> photos, String added) {
        static void encode(PhotoListPacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.photos.size());
            for (String n : p.photos) b.writeUtf(n, 40);
            b.writeUtf(p.added, 40);
        }
        static PhotoListPacket decode(FriendlyByteBuf b) {
            int n = Math.min(b.readVarInt(), 500);
            List<String> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(b.readUtf(40));
            return new PhotoListPacket(l, b.readUtf(40));
        }
        static void handle(PhotoListPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.journal.client.JournalClient.photosUpdated(p.photos, p.added)));
            c.get().setPacketHandled(true);
        }
    }

    public static void open(ServerPlayer p, boolean edit, Edition e) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenPacket(edit, e, edit ? ImageStore.names() : List.of()));
    }

    public record OpenPacket(boolean edit, Edition edition, List<String> photos) {
        static void encode(OpenPacket p, FriendlyByteBuf b) {
            b.writeBoolean(p.edit); p.edition.encode(b);
            b.writeVarInt(p.photos.size());
            for (String n : p.photos) b.writeUtf(n, 40);
        }
        static OpenPacket decode(FriendlyByteBuf b) {
            boolean edit = b.readBoolean();
            Edition e = Edition.decode(b);
            int n = Math.min(b.readVarInt(), 200);
            List<String> photos = new ArrayList<>();
            for (int i = 0; i < n; i++) photos.add(b.readUtf(40));
            return new OpenPacket(edit, e, photos);
        }
        static void handle(OpenPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.journal.client.JournalClient.open(p.edit, p.edition, p.photos)));
            c.get().setPacketHandled(true);
        }
    }

    /** Client -> serveur : demande le fichier d'une photo (nom assaini et limité aux photos du dossier). */
    public record ImageRequestPacket(String name) {
        static void encode(ImageRequestPacket p, FriendlyByteBuf b) { b.writeUtf(p.name, 40); }
        static ImageRequestPacket decode(FriendlyByteBuf b) { return new ImageRequestPacket(b.readUtf(40)); }
        static void handle(ImageRequestPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                ServerPlayer sp = c.get().getSender();
                if (sp == null || !allowImageRequest(sp)) return;
                byte[] data = ImageStore.load(p.name);
                if (data != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new ImageDataPacket(ImageStore.cleanName(p.name), data));
            });
            c.get().setPacketHandled(true);
        }
    }

    /** Fenêtre glissante de demandes de photos par joueur : 40 demandes / 10 s (un journal affiche 7 images au plus). WeakHashMap : rien à nettoyer. */
    private static final java.util.Map<ServerPlayer, long[]> IMAGE_RATE = new java.util.WeakHashMap<>();

    private static boolean allowImageRequest(ServerPlayer sp) {
        long now = sp.server.getTickCount();
        long[] w = IMAGE_RATE.computeIfAbsent(sp, k -> new long[]{now, 0});
        if (now - w[0] > 200) { w[0] = now; w[1] = 0; }
        return ++w[1] <= 40;
    }

    /** Serveur -> client : contenu PNG d'une photo. */
    public record ImageDataPacket(String name, byte[] data) {
        static void encode(ImageDataPacket p, FriendlyByteBuf b) { b.writeUtf(p.name, 40); b.writeByteArray(p.data); }
        static ImageDataPacket decode(FriendlyByteBuf b) { return new ImageDataPacket(b.readUtf(40), b.readByteArray((int) ImageStore.MAX_BYTES)); }
        static void handle(ImageDataPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.journal.client.PhotoCache.receive(p.name, p.data)));
            c.get().setPacketHandled(true);
        }
    }

    /** Client -> serveur : texte rédigé. Le serveur vérifie le droit, l'objet en main, les longueurs, et appose date et signature. */
    public record SavePacket(Edition edition) {
        static void encode(SavePacket p, FriendlyByteBuf b) { p.edition.encode(b); }
        static SavePacket decode(FriendlyByteBuf b) { return new SavePacket(Edition.decode(b)); }
        static void handle(SavePacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> { ServerPlayer sp = c.get().getSender(); if (sp != null) save(sp, p.edition); });
            c.get().setPacketHandled(true);
        }
    }

    private static void save(ServerPlayer p, Edition in) {
        ItemStack st = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(st.getItem() instanceof JournalItem) || !Compat.isJournalist(p)) return;
        List<Edition.Article> arts = new ArrayList<>();
        for (Edition.Article a : in.articles()) {
            if (arts.size() >= Edition.MAX_ARTICLES) break;
            String t = Edition.clean(a.title(), Edition.MAX_ART_TITLE), b = Edition.clean(a.body(), Edition.MAX_BODY);
            if (!t.isEmpty() || !b.isEmpty()) arts.add(new Edition.Article(t, b, Edition.cleanImage(a.image())));
        }
        List<Edition.Ad> ads = new ArrayList<>();
        for (Edition.Ad a : in.ads()) {
            if (ads.size() >= Edition.MAX_ADS) break;
            String n = Edition.clean(a.name(), Edition.MAX_AD_NAME), t = Edition.clean(a.text(), Edition.MAX_AD_TEXT);
            if (!n.isEmpty() || !t.isEmpty()) ads.add(new Edition.Ad(n, t, Edition.cleanImage(a.image()), Math.floorMod(a.theme(), Edition.THEMES)));
        }
        String title = Edition.clean(in.title(), Edition.MAX_TITLE);
        if (title.isEmpty() && arts.isEmpty()) { p.displayClientMessage(Component.literal("§cÉcrivez au moins un titre ou un article."), true); return; }
        String company = JournalConfig.company().trim();
        String author = "Par " + Compat.rpName(p) + (company.isEmpty() ? "" : " — " + company);
        new Edition(title, new SimpleDateFormat("dd/MM/yyyy").format(new Date()), Edition.clean(author, Edition.MAX_AUTHOR), arts, ads).write(st);
        // Entreprise qui touchera les ventes au kiosque : posée par le serveur (jamais par le client), seulement si l'auteur est de l'entreprise de presse.
        int co = Compat.pressCompanyOf(p);
        if (co != 0 && st.getTag() != null) {
            var tag = st.getTag().getCompound("Edition");
            tag.putInt("co", co);
            tag.putString("cn", Compat.companyName(p.server, co));
        }
        p.getInventory().setChanged();
        p.displayClientMessage(Component.literal("§aÉdition enregistrée sur " + st.getCount() + " exemplaire(s)."), true);
    }
}
