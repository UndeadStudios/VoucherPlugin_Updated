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

public class VoucherPlugin extends JavaPlugin implements TabExecutor, Listener {

    private static final String VOUCHER_KEY = "voucher_command";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("createvoucher").setExecutor(this);
        getCommand("givevoucher").setExecutor(this);
        getCommand("givevoucher").setTabCompleter(this);

        Bukkit.getPluginManager().addPermission(new Permission("voucher.use"));
        Bukkit.getPluginManager().addPermission(new Permission("voucher.give"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <command> [args]");
            return true;
        }

        if (command.getName().equalsIgnoreCase("createvoucher")) {
            return handleCreateVoucher(sender, args);
        } else if (command.getName().equalsIgnoreCase("givevoucher")) {
            return handleGiveVoucher(sender, args);
        }
        return false;
    }

    private boolean handleCreateVoucher(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voucher.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to create vouchers.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /createvoucher <voucher_name> <command> <lore...>");
            return true;
        }

        String voucherName = args[0];
        String voucherCommand = args[1];
        List<String> lore = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        saveVoucherData(voucherName, voucherCommand, lore);

        sender.sendMessage(ChatColor.GREEN + "Voucher '" + voucherName + "' created successfully!");
        return true;
    }

    private boolean handleGiveVoucher(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voucher.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to give vouchers.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /givevoucher <player> <voucher_name>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        String voucherName = args[1];
        if (!getConfig().contains(VOUCHER_KEY + "." + voucherName)) {
            sender.sendMessage(ChatColor.RED + "Voucher '" + voucherName + "' does not exist.");
            return true;
        }

        ItemStack voucher = createVoucher(voucherName);
        target.getInventory().addItem(voucher);

        sender.sendMessage(ChatColor.GREEN + "Voucher '" + voucherName + "' given to " + target.getName());
        return true;
    }

    private void saveVoucherData(String voucherName, String voucherCommand, List<String> lore) {
        getConfig().set(VOUCHER_KEY + "." + voucherName + ".command", voucherCommand);
        getConfig().set(VOUCHER_KEY + "." + voucherName + ".name", ChatColor.GOLD + voucherName);
        getConfig().set(VOUCHER_KEY + "." + voucherName + ".lore", lore);
        saveConfig();
    }

    private ItemStack createVoucher(String name) {
        ItemStack voucher = new ItemStack(Material.PAPER);
        ItemMeta meta = voucher.getItemMeta();
        if (meta == null) return voucher;

        String commandToExecute = getConfig().getString(VOUCHER_KEY + "." + name + ".command");
        String itemName = getConfig().getString(VOUCHER_KEY + "." + name + ".name");
        List<String> lore = getConfig().getStringList(VOUCHER_KEY + "." + name + ".lore");

        meta.setDisplayName(itemName != null ? itemName : ChatColor.GOLD + name);
        meta.setLore(lore != null && !lore.isEmpty() ? lore : Collections.singletonList(ChatColor.GRAY + "Right-click to use this voucher!"));
        voucher.setItemMeta(meta);

        return voucher;
    }

    @EventHandler
    public void onPlayerUseVoucher(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();
        for (String key : getConfig().getConfigurationSection(VOUCHER_KEY).getKeys(false)) {
            String voucherName = getConfig().getString(VOUCHER_KEY + "." + key + ".name");
            if (voucherName != null && ChatColor.stripColor(voucherName).equals(ChatColor.stripColor(displayName))) {
                String commandToExecute = getConfig().getString(VOUCHER_KEY + "." + key + ".command");
                if (commandToExecute != null) {
                    player.performCommand(commandToExecute.replace("%player%", player.getName()));
                    player.getInventory().removeItem(item); // Remove the used voucher
                    player.sendMessage(ChatColor.GREEN + "You used the voucher: " + displayName);
                    event.setCancelled(true);
                }
                break;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null; // Suggest online players
        } else if (args.length == 2 && command.getName().equalsIgnoreCase("givevoucher")) {
            return new ArrayList<>(getConfig().getConfigurationSection(VOUCHER_KEY).getKeys(false));
        }
        return Collections.emptyList();
    }
}
