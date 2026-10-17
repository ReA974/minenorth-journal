package fr.minenorth.journal;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.Supplier;

/** config/minenorth_journal.toml (serveur dédié). */
public final class JournalConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> COMPANY;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        COMPANY = b.comment("Nom de l'entreprise (mod Entreprises) dont les membres peuvent rédiger un journal. Vide = seuls les opérateurs.")
                .define("entreprisePresse", "");
        SPEC = b.build();
    }

    private JournalConfig() {}

    private static <T> T val(Supplier<T> s, T def) {
        try { return s.get(); } catch (IllegalStateException e) { return def; }
    }

    public static String company() { return val(COMPANY::get, ""); }
}
