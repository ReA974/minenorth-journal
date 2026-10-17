package fr.minenorth.journal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Lien facultatif avec le mod Entreprises (API MineNorth) par réflexion : sans lui, seuls les opérateurs rédigent. */
public final class Compat {
    private Compat() {}

    private static Method companies, idOf, isMember, displayName;

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

    /** Nom RP « Prénom Nom » (mod Identité, via l'API MineNorth) ; repli : le pseudo Minecraft. */
    public static String rpName(ServerPlayer p) {
        String fallback = p.getGameProfile().getName();
        if (!ModList.get().isLoaded("minenorthapi")) return fallback;
        try {
            if (displayName == null) displayName = Class.forName("fr.minenorth.api.MineNorth").getMethod("displayName", MinecraftServer.class, java.util.UUID.class);
            Object name = displayName.invoke(null, p.server, p.getUUID());
            return name instanceof String str && !str.isBlank() ? str : fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    /** Paie par carte (API MineNorth, versé au trésor). Vrai si c'est gratuit ou payé ; sinon prévient le joueur. */
    public static boolean charge(ServerPlayer p, long cents, String source) {
        if (cents <= 0) return true;
        if (!ModList.get().isLoaded("minenorthapi")) return true;
        try {
            Object bank = Class.forName("fr.minenorth.api.MineNorth").getMethod("bank").invoke(null);
            Method charge = Class.forName("fr.minenorth.api.BankService").getMethod("charge", ServerPlayer.class, long.class, String.class);
            Object res = charge.invoke(bank, p, cents, source);
            boolean ok = (boolean) res.getClass().getMethod("ok").invoke(res);
            if (!ok) {
                String msg = "Paiement refusé (carte bancaire sur vous ?)";
                try { Object m = res.getClass().getMethod("message").invoke(res); if (m instanceof String str && !str.isBlank()) msg = str; } catch (Throwable ignored) {}
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("§c" + msg), true);
            }
            return ok;
        } catch (Throwable t) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("§cPaiement indisponible."), true);
            return false;
        }
    }
}
