package fr.minenorth.journal;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** MineNorth Journal : éditions papier rédigées par les journalistes (entreprise de presse configurable) et lues par tous. */
@Mod(MineNorthJournal.MOD_ID)
public class MineNorthJournal {
    public static final String MOD_ID = "minenorthjournal";

    public MineNorthJournal() {
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        JournalNet.register();
        // Config du serveur dédié seulement, dans config/ (le client ne crée aucun fichier).
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, JournalConfig.SPEC, "minenorth_journal.toml");
        }
    }
}
