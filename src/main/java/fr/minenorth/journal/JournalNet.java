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

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineNorthJournal.MOD_ID, "network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, OpenPacket.class, OpenPacket::encode, OpenPacket::decode, OpenPacket::handle);
        CHANNEL.registerMessage(1, SavePacket.class, SavePacket::encode, SavePacket::decode, SavePacket::handle);
    }

    public static void open(ServerPlayer p, boolean edit, Edition e) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenPacket(edit, e));
    }

    public record OpenPacket(boolean edit, Edition edition) {
        static void encode(OpenPacket p, FriendlyByteBuf b) { b.writeBoolean(p.edit); p.edition.encode(b); }
        static OpenPacket decode(FriendlyByteBuf b) { return new OpenPacket(b.readBoolean(), Edition.decode(b)); }
        static void handle(OpenPacket p, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> fr.minenorth.journal.client.JournalClient.open(p.edit, p.edition)));
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
            if (!t.isEmpty() || !b.isEmpty()) arts.add(new Edition.Article(t, b));
        }
        String title = Edition.clean(in.title(), Edition.MAX_TITLE);
        if (title.isEmpty() && arts.isEmpty()) { p.displayClientMessage(Component.literal("§cÉcrivez au moins un titre ou un article."), true); return; }
        String company = JournalConfig.company().trim();
        String author = Edition.clean(company.isEmpty() ? p.getGameProfile().getName() : company, Edition.MAX_AUTHOR);
        new Edition(title, new SimpleDateFormat("dd/MM/yyyy").format(new Date()), author, arts).write(st);
        p.getInventory().setChanged();
        p.displayClientMessage(Component.literal("§aÉdition enregistrée sur " + st.getCount() + " exemplaire(s)."), true);
    }
}
