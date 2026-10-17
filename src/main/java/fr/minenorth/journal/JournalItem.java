package fr.minenorth.journal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Journal : clic droit = lecture ; accroupi + clic droit = rédaction (journalistes). Un journal vierge porte tout le tas :
 * la rédaction s'applique à tous les exemplaires tenus, que l'on peut ensuite vendre (boutiques) ou distribuer.
 */
public class JournalItem extends Item {
    public JournalItem() { super(new Item.Properties().stacksTo(16)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(st);
        boolean edit = p.isShiftKeyDown();
        if (edit && !Compat.isJournalist(p)) {
            p.displayClientMessage(Component.literal("§cSeuls les journalistes rédigent un journal."), true);
            return InteractionResultHolder.fail(st);
        }
        Edition e = Edition.read(st);
        if (!edit && e.blank()) {
            p.displayClientMessage(Component.literal("§7Ce journal est vierge."), true);
            return InteractionResultHolder.fail(st);
        }
        JournalNet.open(p, edit, e);
        return InteractionResultHolder.success(st);
    }

    @Override
    public Component getName(ItemStack st) {
        Edition e = Edition.read(st);
        Component base = Component.translatable(getDescriptionId());
        return e.title().isEmpty() ? base : Component.empty().append(base).append(" : ").append(e.title());
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        Edition e = Edition.read(st);
        if (!e.date().isEmpty()) tip.add(Component.literal("§7Édition du " + e.date()));
        if (!e.author().isEmpty()) tip.add(Component.literal("§7" + e.author()));
        tip.add(Component.literal("§8Clic droit : lire · accroupi + clic droit : rédiger (journalistes)"));
    }
}
