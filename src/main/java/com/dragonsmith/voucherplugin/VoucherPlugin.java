package com.dragonsmith.voucherplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VoucherPlugin extends JavaPlugin implements Listener, TabExecutor {

    private static final String VOUCHER_KEY = "voucher_command";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("givevoucher").setExecutor(this);
        getCommand("givevoucher").setTabCompleter(this);

        // Register permissions
        Bukkit.getPluginManager().addPermission(new Permission("voucher.use"));
        Bukkit.getPluginManager().addPermission(new Permission("voucher.give"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("voucher.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /givevoucher <player> <voucher_name> <command>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        String voucherName = args[1];
        String voucherCommand = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        // Save the voucher command to a configuration (or file)
        getConfig().set(VOUCHER_KEY + "." + voucherName, voucherCommand);
        saveConfig();

        ItemStack voucher = createVoucher(voucherName);
        target.getInventory().addItem(voucher);

        sender.sendMessage(ChatColor.GREEN + "Voucher given to " + target.getName());
        return true;
    }

    private ItemStack createVoucher(String name) {
        ItemStack voucher = new ItemStack(Material.PAPER);
        ItemMeta meta = voucher.getItemMeta();
        if (meta == null) return voucher;

        meta.setDisplayName(ChatColor.GOLD + name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "This voucher allows you to execute a command.");
        lore.add(ChatColor.GRAY + "Command: %command%"); // Placeholder for the command
        meta.setLore(lore);

        voucher.setItemMeta(meta);
        return voucher;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ensure the player is holding an item and it is of type PAPER
        if (event.getItem() == null || event.getItem().getType() != Material.PAPER) {
            return;
        }

        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();

        // Check if the item has metadata and a display name
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        // Ensure the player has permission to use the voucher
        if (!event.getPlayer().hasPermission("voucher.use")) {
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to use this voucher.");
            return;
        }

        // Get the lore of the item
        List<String> lore = meta.getLore();
        if (lore == null || lore.size() < 2) {
            return; // Ensure that the lore is properly defined
        }

        // Replace the %command% placeholder with the actual command from the config
        String voucherName = meta.getDisplayName().replace(ChatColor.GOLD.toString(), "");
        String commandToExecute = getConfig().getString(VOUCHER_KEY + "." + voucherName);
        if (commandToExecute == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "This voucher has no command.");
            return;
        }

        // Replace the placeholder with player name
        commandToExecute = commandToExecute.replace("%player_name%", event.getPlayer().getName());

        // Dispatch the command as console (admin rights)
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
            event.getPlayer().sendMessage(ChatColor.GREEN + "Voucher used: " + meta.getDisplayName());

            // Remove the item after use
            event.getItem().setAmount(event.getItem().getAmount() - 1);
        } catch (Exception e) {
            event.getPlayer().sendMessage(ChatColor.RED + "Failed to execute the command.");
        }

        // Cancel the event to prevent item use (if you don't want the paper to be consumed)
        event.setCancelled(true);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null; // Suggest online players
        }
        return Collections.emptyList();
    }
}
