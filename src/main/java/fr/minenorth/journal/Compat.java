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

    /** Id de l'entreprise de presse (config) si le joueur en est membre, sinon 0. */
    public static int pressCompanyOf(ServerPlayer p) {
        String company = JournalConfig.company().trim();
        if (company.isEmpty() || !ModList.get().isLoaded("minenorthapi")) return 0;
        try {
            Object service = Class.forName("fr.minenorth.api.MineNorth").getMethod("companies").invoke(null);
            Class<?> svc = Class.forName("fr.minenorth.api.CompanyService");
            int id = (int) svc.getMethod("idOf", MinecraftServer.class, String.class).invoke(service, p.server, company);
            return id != 0 && (boolean) svc.getMethod("isMember", ServerPlayer.class, int.class, boolean.class).invoke(service, p, id, false) ? id : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Nom actuel d'une entreprise active, « » sinon. */
    public static String companyName(MinecraftServer s, int id) {
        try {
            Object service = Class.forName("fr.minenorth.api.MineNorth").getMethod("companies").invoke(null);
            Object n = Class.forName("fr.minenorth.api.CompanyService").getMethod("name", MinecraftServer.class, int.class).invoke(service, s, id);
            return n instanceof String str ? str : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Entreprise qui touche la vente de ce journal : celle mémorisée par le serveur à la rédaction, SEULEMENT si c'est bien l'entreprise de presse
     * de la config (un journal falsifié ne peut pas détourner l'argent vers une autre entreprise). 0 = trésor public.
     */
    public static int pressCompanyOf(MinecraftServer s, net.minecraft.world.item.ItemStack journal) {
        if (!ModList.get().isLoaded("minenorthapi") || journal.getTag() == null) return 0;
        int stored = journal.getTag().getCompound("Edition").getInt("co");
        String configured = JournalConfig.company().trim();
        if (stored == 0 || configured.isEmpty()) return 0;
        try {
            Object service = Class.forName("fr.minenorth.api.MineNorth").getMethod("companies").invoke(null);
            int press = (int) Class.forName("fr.minenorth.api.CompanyService").getMethod("idOf", MinecraftServer.class, String.class).invoke(service, s, configured);
            return press == stored ? press : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Paiement par carte du joueur vers le COMPTE de l'entreprise. Vrai = payé, faux = refusé (le joueur est prévenu), null = impossible (pas de compte
     * entreprise, mod absent) : l'appelant se rabat sur le trésor.
     */
    public static Boolean payCompany(ServerPlayer p, int company, long cents, String label) {
        try {
            java.util.UUID account = (java.util.UUID) Class.forName("fr.minenorth.entreprises.api.EntrepriseApi")
                    .getMethod("accountOf", MinecraftServer.class, int.class).invoke(null, p.server, company);
            if (account == null) return null;
            Object bank = Class.forName("fr.minenorth.api.MineNorth").getMethod("bank").invoke(null);
            Class<?> svc = Class.forName("fr.minenorth.api.BankService");
            Object check = svc.getMethod("check", ServerPlayer.class, long.class).invoke(bank, p, cents);
            if (!(boolean) check.getClass().getMethod("ok").invoke(check)) return refuse(p, check);
            Object res = svc.getMethod("transfer", MinecraftServer.class, java.util.UUID.class, java.util.UUID.class, long.class, String.class, String.class, String.class)
                    .invoke(bank, p.server, p.getUUID(), account, cents, "PAYMENT", label.length() > 60 ? label.substring(0, 60) : label, "Kiosque");
            return (boolean) res.getClass().getMethod("ok").invoke(res) ? Boolean.TRUE : refuse(p, res);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean refuse(ServerPlayer p, Object payResult) {
        String msg = "Paiement refusé (carte bancaire sur vous ?)";
        try { Object m = payResult.getClass().getMethod("message").invoke(payResult); if (m instanceof String str && !str.isBlank()) msg = str; } catch (Throwable ignored) {}
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("§c" + msg), true);
        return Boolean.FALSE;
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
