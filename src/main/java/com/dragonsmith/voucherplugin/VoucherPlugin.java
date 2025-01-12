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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class VoucherPlugin extends JavaPlugin implements TabExecutor, Listener {

    private static final String VOUCHER_KEY = "vouchers";

    @Override
    public void onEnable() {
        // Register commands
        Objects.requireNonNull(getCommand("createvoucher")).setExecutor(this);
        Objects.requireNonNull(getCommand("givevoucher")).setExecutor(this);
        Objects.requireNonNull(getCommand("givevoucher")).setTabCompleter(this);

        // Register permissions
        Bukkit.getPluginManager().addPermission(new Permission("voucher.use"));
        Bukkit.getPluginManager().addPermission(new Permission("voucher.give"));

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, this);

        // Save default config if none exists
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <command> [args]");
            return true;
        }

        if (command.getName().equalsIgnoreCase("createvoucher")) {
            // Create voucher
            return handleCreateVoucher(sender, args);
        } else if (command.getName().equalsIgnoreCase("givevoucher")) {
            // Give voucher
            return handleGiveVoucher(sender, args);
        }
        return false;
    }

    private boolean handleCreateVoucher(CommandSender sender, String[] args) {
        if (!sender.hasPermission("voucher.give")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to create vouchers.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /createvoucher <voucher_name> <command...>");
            return true;
        }

        String voucherName = args[0];
        String voucherCommand = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Automatically generate the lore
        List<String> lore = Collections.singletonList(ChatColor.GRAY + "Runs: " + ChatColor.YELLOW + voucherCommand);

        // Save the voucher data to the configuration file
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

        // Check if the voucher exists in the config
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
        getConfig().set(VOUCHER_KEY + "." + voucherName + ".name", ChatColor.GOLD + voucherName); // Color-coded name
        getConfig().set(VOUCHER_KEY + "." + voucherName + ".lore", lore); // Automatically generated lore
        saveConfig();
    }

    private ItemStack createVoucher(String name) {
        ItemStack voucher = new ItemStack(Material.PAPER);
        ItemMeta meta = voucher.getItemMeta();
        if (meta == null) return voucher;

        // Get the data associated with this voucher from the config
        String commandToExecute = getConfig().getString(VOUCHER_KEY + "." + name + ".command");
        String itemName = getConfig().getString(VOUCHER_KEY + "." + name + ".name");
        List<String> lore = getConfig().getStringList(VOUCHER_KEY + "." + name + ".lore");

        meta.setDisplayName(itemName);
        meta.setLore(lore); // Set the lore with "Runs: <command>"
        voucher.setItemMeta(meta);

        return voucher;
    }

@EventHandler
public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    // Ensure the event is a right-click and the player is holding an item
    if (item == null || item.getType() != Material.PAPER) return;

    ItemMeta meta = item.getItemMeta();
    if (meta == null || !meta.hasDisplayName()) return;

    // Debug: Check the name of the item clicked
    getLogger().info("Voucher clicked: " + meta.getDisplayName());

    String voucherName = ChatColor.stripColor(meta.getDisplayName());
    if (!getConfig().contains(VOUCHER_KEY + "." + voucherName)) return;

    String commandToExecute = getConfig().getString(VOUCHER_KEY + "." + voucherName + ".command");
    if (commandToExecute == null) return;

    // Replace placeholder
    commandToExecute = commandToExecute.replace("%player_name%", player.getName());

    // Execute the command
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
    player.sendMessage(ChatColor.GREEN + "Voucher used! Executing: " + commandToExecute);
    item.setAmount(item.getAmount() - 1);

    // Cancel the event to prevent item use
    event.setCancelled(true);
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
