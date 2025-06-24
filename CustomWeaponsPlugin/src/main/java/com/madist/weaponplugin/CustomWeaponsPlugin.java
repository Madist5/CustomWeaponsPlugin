package com.madist.weaponplugin;

import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class CustomWeaponsPlugin extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Long> shadowInvisCooldown = new HashMap<>();
    private final Map<UUID, Long> ghostCloneCooldown = new HashMap<>();
    private final Map<UUID, Long> wardenFreezeCooldown = new HashMap<>();
    private final Map<UUID, Long> wardenBoomCooldown = new HashMap<>();
    private final Map<UUID, Long> wardenMendCooldown = new HashMap<>();
    private final Map<UUID, Integer> pyroBowShots = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<String, Map<UUID, BossBar>> playerBars = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("locateitem").setExecutor(this);
    }

    private boolean isShadowWalkerBlade(ItemStack item) {
        return item != null && item.getType() == Material.NETHERITE_SWORD && item.getItemMeta() != null && item.getItemMeta().getDisplayName().contains("ShadowWalker Blade");
    }

    private boolean isWardenAxe(ItemStack item) {
        return item != null && item.getType() == Material.NETHERITE_AXE && item.getItemMeta() != null && item.getItemMeta().getDisplayName().contains("Warden's Final Blow");
    }

    private boolean isPyroBow(ItemStack item) {
        return item != null && item.getType() == Material.BOW && item.getItemMeta() != null && item.getItemMeta().getDisplayName().contains("Pyro Bow");
    }

    private boolean hasNamedItem(Player player, String namePart) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains(namePart)) {
                return true;
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains(namePart)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("Usage: /locateitem <name>");
            return true;
        }

        String namePart = String.join(" ", args);
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (hasNamedItem(target, namePart)) {
                sender.sendMessage("§aFound '" + namePart + "' held by §b" + target.getName() + "§a at location §e" +
                        target.getLocation().getBlockX() + ", " +
                        target.getLocation().getBlockY() + ", " +
                        target.getLocation().getBlockZ());
                return true;
            }
        }

        sender.sendMessage("§cNo player currently holds an item named '" + namePart + "'.");
        return true;
    }

    private void showBossBar(Player player, String ability, long duration, BarColor color) {
        BossBar bar = Bukkit.createBossBar("§e" + ability + " Cooldown", color, BarStyle.SOLID);
        bar.addPlayer(player);

        playerBars.computeIfAbsent(ability, k -> new HashMap<>()).put(player.getUniqueId(), bar);

        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= duration || !player.isOnline()) {
                    bar.removeAll();
                    playerBars.get(ability).remove(player.getUniqueId());
                    cancel();
                    return;
                }
                double progress = (double) (duration - elapsed) / duration;
                bar.setProgress(progress);
                elapsed += 1000;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void sendCooldownBar(Player player, String ability, long remaining, BarColor color) {
        long seconds = remaining / 1000;
        player.sendActionBar("§e" + ability + " Cooldown: §c" + seconds + "s");
        showBossBar(player, ability, remaining, color);
    }

    // All other events remain unchanged (omitted here for brevity)

    private LivingEntity getNearestLivingEntity(Player player) {
        double radius = 10.0;
        Location loc = player.getLocation();
        List<Entity> nearby = (List<Entity>) loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity && entity != player) {
                return (LivingEntity) entity;
            }
        }
        return null;
    }
}