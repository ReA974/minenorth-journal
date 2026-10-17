package fr.minenorth.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Contenu d'un journal (stocké dans le NBT de l'objet, donc copiable et échangeable). */
public record Edition(String title, String date, String author, List<Article> articles) {
    public static final int MAX_ARTICLES = 4, MAX_TITLE = 32, MAX_ART_TITLE = 48, MAX_BODY = 700, MAX_AUTHOR = 48;
    private static final String TAG = "Edition";

    public record Article(String title, String body) {}

    public static Edition empty() { return new Edition("", "", "", new ArrayList<>()); }

    public boolean blank() { return title.isEmpty() && articles.isEmpty(); }

    public static String clean(String s, int max) {
        if (s == null) return "";
        s = s.replace("\u00a7", "").replace('\r', ' ').trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static Edition read(ItemStack st) {
        CompoundTag root = st.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) return empty();
        CompoundTag t = root.getCompound(TAG);
        List<Article> list = new ArrayList<>();
        ListTag l = t.getList("articles", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size() && i < MAX_ARTICLES; i++) list.add(new Article(l.getCompound(i).getString("t"), l.getCompound(i).getString("b")));
        return new Edition(t.getString("title"), t.getString("date"), t.getString("author"), list);
    }

    public void write(ItemStack st) {
        CompoundTag t = new CompoundTag();
        t.putString("title", title); t.putString("date", date); t.putString("author", author);
        ListTag l = new ListTag();
        for (Article a : articles) {
            CompoundTag c = new CompoundTag();
            c.putString("t", a.title()); c.putString("b", a.body());
            l.add(c);
        }
        t.put("articles", l);
        st.getOrCreateTag().put(TAG, t);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeUtf(title, 64); b.writeUtf(date, 32); b.writeUtf(author, 64);
        b.writeVarInt(articles.size());
        for (Article a : articles) { b.writeUtf(a.title(), 100); b.writeUtf(a.body(), 1500); }
    }

    public static Edition decode(FriendlyByteBuf b) {
        String title = b.readUtf(64), date = b.readUtf(32), author = b.readUtf(64);
        int n = Math.min(b.readVarInt(), MAX_ARTICLES);
        List<Article> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(new Article(b.readUtf(100), b.readUtf(1500)));
        return new Edition(title, date, author, list);
    }
}
