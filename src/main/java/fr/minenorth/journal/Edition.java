package fr.minenorth.journal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Contenu d'un journal (stocké dans le NBT de l'objet, donc copiable et échangeable) : articles avec image et encarts publicitaires. */
public record Edition(String title, String date, String author, List<Article> articles, List<Ad> ads) {
    public static final int MAX_ARTICLES = 4, MAX_ADS = 3, THEMES = 4;
    public static final int MAX_TITLE = 32, MAX_ART_TITLE = 48, MAX_BODY = 700, MAX_AUTHOR = 80, MAX_IMG = 64, MAX_AD_NAME = 32, MAX_AD_TEXT = 90;
    private static final String TAG = "Edition";

    /** image = identifiant d'objet (« minecraft:diamond », ou celui d'un autre mod), vide = sans image. */
    public record Article(String title, String body, String image) {}
    /** Encart publicitaire : annonceur, texte, image (objet) et thème de couleur (0 rouge, 1 bleu, 2 vert, 3 or). */
    public record Ad(String name, String text, String image, int theme) {}

    public static Edition empty() { return new Edition("", "", "", new ArrayList<>(), new ArrayList<>()); }

    public boolean blank() { return title.isEmpty() && articles.isEmpty() && ads.isEmpty(); }

    public static String clean(String s, int max) {
        if (s == null) return "";
        s = s.replace("\u00a7", "").replace('\r', ' ').trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Identifiant d'objet valide et existant, sinon chaîne vide. */
    public static String cleanImage(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        if (t.startsWith(ImageStore.PREFIX)) {
            String n = ImageStore.cleanName(t);
            return !n.isEmpty() && ImageStore.exists(n) ? ImageStore.PREFIX + n : "";
        }
        ResourceLocation id = ResourceLocation.tryParse(t);
        return id != null && ForgeRegistries.ITEMS.containsKey(id) && !"air".equals(id.getPath()) ? id.toString() : "";
    }

    public static Edition read(ItemStack st) {
        CompoundTag root = st.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) return empty();
        CompoundTag t = root.getCompound(TAG);
        List<Article> list = new ArrayList<>();
        ListTag l = t.getList("articles", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size() && i < MAX_ARTICLES; i++) {
            CompoundTag c = l.getCompound(i);
            list.add(new Article(c.getString("t"), c.getString("b"), c.getString("i")));
        }
        List<Ad> ads = new ArrayList<>();
        ListTag a = t.getList("ads", Tag.TAG_COMPOUND);
        for (int i = 0; i < a.size() && i < MAX_ADS; i++) {
            CompoundTag c = a.getCompound(i);
            ads.add(new Ad(c.getString("n"), c.getString("t"), c.getString("i"), Math.floorMod(c.getInt("c"), THEMES)));
        }
        return new Edition(t.getString("title"), t.getString("date"), t.getString("author"), list, ads);
    }

    public void write(ItemStack st) {
        CompoundTag t = new CompoundTag();
        t.putString("title", title); t.putString("date", date); t.putString("author", author);
        ListTag l = new ListTag();
        for (Article a : articles) {
            CompoundTag c = new CompoundTag();
            c.putString("t", a.title()); c.putString("b", a.body()); c.putString("i", a.image());
            l.add(c);
        }
        t.put("articles", l);
        ListTag ls = new ListTag();
        for (Ad a : ads) {
            CompoundTag c = new CompoundTag();
            c.putString("n", a.name()); c.putString("t", a.text()); c.putString("i", a.image()); c.putInt("c", a.theme());
            ls.add(c);
        }
        t.put("ads", ls);
        st.getOrCreateTag().put(TAG, t);
    }

    public void encode(FriendlyByteBuf b) {
        b.writeUtf(title, 64); b.writeUtf(date, 32); b.writeUtf(author, 100);
        b.writeVarInt(articles.size());
        for (Article a : articles) { b.writeUtf(a.title(), 100); b.writeUtf(a.body(), 1500); b.writeUtf(a.image(), 100); }
        b.writeVarInt(ads.size());
        for (Ad a : ads) { b.writeUtf(a.name(), 80); b.writeUtf(a.text(), 200); b.writeUtf(a.image(), 100); b.writeVarInt(a.theme()); }
    }

    public static Edition decode(FriendlyByteBuf b) {
        String title = b.readUtf(64), date = b.readUtf(32), author = b.readUtf(100);
        int n = Math.min(b.readVarInt(), MAX_ARTICLES);
        List<Article> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(new Article(b.readUtf(100), b.readUtf(1500), b.readUtf(100)));
        int m = Math.min(b.readVarInt(), MAX_ADS);
        List<Ad> ads = new ArrayList<>();
        for (int i = 0; i < m; i++) ads.add(new Ad(b.readUtf(80), b.readUtf(200), b.readUtf(100), Math.floorMod(b.readVarInt(), THEMES)));
        return new Edition(title, date, author, list, ads);
    }
}
