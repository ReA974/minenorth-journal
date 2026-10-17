package fr.minenorth.journal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Lien facultatif avec le mod Entreprises (API MineNorth) par réflexion : sans lui, seuls les opérateurs rédigent. */
public final class Compat {
    private Compat() {}

    private static Method companies, idOf, isMember;

    /** Journaliste : opérateur (niveau 2), ou membre (patron / employé) de l'entreprise de presse de la config. */
    public static boolean isJournalist(ServerPlayer p) {
        if (p.hasPermissions(2)) return true;
        String company = JournalConfig.company().trim();
        if (company.isEmpty() || !ModList.get().isLoaded("minenorthapi")) return false;
        try {
            if (companies == null) {
                companies = Class.forName("fr.minenorth.api.MineNorth").getMethod("companies");
                Class<?> svc = Class.forName("fr.minenorth.api.CompanyService");
                idOf = svc.getMethod("idOf", MinecraftServer.class, String.class);
                isMember = svc.getMethod("isMember", ServerPlayer.class, int.class, boolean.class);
            }
            Object service = companies.invoke(null);
            int id = (int) idOf.invoke(service, p.server, company);
            return id != 0 && (boolean) isMember.invoke(service, p, id, false);
        } catch (Throwable t) {
            return false;
        }
    }
}
