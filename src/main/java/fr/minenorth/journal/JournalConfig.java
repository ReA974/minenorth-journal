package fr.minenorth.journal;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.Supplier;

/** config/minenorth_journal.toml (serveur dédié). */
public final class JournalConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> COMPANY;
    private static final ForgeConfigSpec.IntValue PRICE, MAX_PHOTOS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMPANY = b.comment("Nom de l'entreprise (mod Entreprises) dont les membres peuvent rédiger un journal. Vide = seuls les opérateurs.")
                .define("entreprisePresse", "");
        PRICE = b.comment("Prix d'un journal pris au kiosque, en centimes (0 = gratuit). Payé par carte (EuroBank) et versé au COMPTE de l'entreprise de presse",
                        "si le journal a été rédigé par l'un de ses membres ; sinon (rédigé par un opérateur) au trésor public.")
                .defineInRange("prixKiosqueCentimes", 0, 0, 100000);
        MAX_PHOTOS = b.comment("Nombre maximum d'images qu'un journaliste peut importer lui-même (dossier config/minenorth_journal/images/).")
                .defineInRange("maxImagesParJoueur", 20, 0, 500);
        SPEC = b.build();
    }

    private JournalConfig() {}

    private static <T> T val(Supplier<T> s, T def) {
        try { return s.get(); } catch (IllegalStateException e) { return def; }
    }

    public static String company() { return val(COMPANY::get, ""); }
    public static int priceCents() { return val(PRICE::get, 0); }
    public static int maxPhotosPerPlayer() { return val(MAX_PHOTOS::get, 20); }
}
