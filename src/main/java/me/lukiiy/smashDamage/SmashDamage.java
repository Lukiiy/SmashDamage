package me.lukiiy.smashDamage;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class SmashDamage extends JavaPlugin implements Listener {
    public final NamespacedKey fatigueKey = new NamespacedKey(this, "fatigue");

    public final Random random = new Random();
    private final Map<Damageable, TextDisplay> fatigueDisplay = new HashMap<>();
    private final Set<Damageable> pulverizing = new HashSet<>();

    @Override
    public void onEnable() {
        setupConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, it -> it.registrar().register(Cmd.register(), "SmashDamage main command"));
    }

    public static SmashDamage getInstance() {
        return JavaPlugin.getPlugin(SmashDamage.class);
    }

    // Config
    public void setupConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    // API??
    public void setFatigue(@NotNull Damageable entity, double value) {
        entity.getPersistentDataContainer().set(fatigueKey, PersistentDataType.DOUBLE, Math.clamp(value, 0, 999.9));
    }

    public double getFatigue(@NotNull Damageable entity) {
        return entity.getPersistentDataContainer().getOrDefault(fatigueKey, PersistentDataType.DOUBLE, 0.0);
    }

    public void displayFatigue(Damageable entity) {
        if (getConfig().getBoolean("fatigueDisplay.disable")) return;
        if (fatigueDisplay.containsKey(entity)) {
            fatigueDisplay.get(entity).setTicksLived(1);
            return;
        }

        int maxTicks = getConfig().getInt("fatigueDisplay.timespan");
        EntitySize size = new EntitySize(entity, getConfig().getDouble("fatigueDisplay.yOffset"));

        TextDisplay display = entity.getWorld().spawn(getSpawn(entity, size), TextDisplay.class, it -> {
            it.setPersistent(false);
            it.text(ColorUtil.formatted(getFatigue(entity)));
            it.setSeeThrough(false);
            it.setShadowed(true);
            it.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            it.setDefaultBackground(false);
            it.setBillboard(Display.Billboard.VERTICAL);
            it.setViewRange(32);
            it.setTeleportDuration(2);
            fatigueDisplay.put(entity, it);
            if (entity instanceof Player p) p.hideEntity(this, it);
        });

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
            if (!entity.isValid() || !display.isValid() || display.getTicksLived() > maxTicks) {
                task.cancel();
                display.remove();
                fatigueDisplay.remove(entity);
                return;
            }

            display.teleport(getSpawn(entity, size));
            display.text(ColorUtil.formatted(getFatigue(entity)));
        }, 1L, 2L);
    }

    public void removeFatigue(Damageable entity) {
        if (fatigueDisplay.containsKey(entity)) fatigueDisplay.get(entity).remove();
    }

    public void pulverize(Damageable entity) {
        double threshold = getConfig().getDouble("pulverizing.effect");

        if (threshold <= 0 || pulverizing.contains(entity)) return;
        pulverizing.add(entity);

        int particles = getConfig().getInt("pulverizing.particles");
        final EntitySize size = new EntitySize(entity);

        entity.getScheduler().runAtFixedRate(this, (task) -> {
            double f = getFatigue(entity);

            if (entity instanceof Player p && p.getGameMode().isInvulnerable()) return;
            if (!entity.isValid() || f < threshold) {
                pulverizing.remove(entity);
                task.cancel();
            }

            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation().add(0, size.height, 0), particles, size.width, size.height / 4, size.width, 0.025);
        }, null, 1, 10);
    }

    // Listener
    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!getConfig().getBoolean("fatigueHud")) return;

        Player p = e.getPlayer();
        p.getScheduler().runAtFixedRate(this, (task) -> {
            if (!p.isOnline()) task.cancel();
            if (p.getGameMode().isInvulnerable()) return;

            p.sendActionBar(ColorUtil.formatted(getFatigue(p)));
        }, null, 1, 10L);
    }

    @EventHandler
    public void kb(EntityKnockbackByEntityEvent e) {
        Damageable entity = e.getEntity();

        if (entity instanceof Boss) return;
        e.setKnockback(e.getKnockback().multiply(getFatigue(entity) / 75 * getConfig().getDouble("multiplier.knockback")));
    }

    @EventHandler
    public void transform(EntityTransformEvent e) {
        if (!(e.getTransformedEntity() instanceof Damageable before)) return;

        double fatigue = getFatigue(before);
        if (fatigue != 0) e.getTransformedEntities().forEach(it -> setFatigue((Damageable) it, fatigue));
    }

    @EventHandler
    public void damage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Damageable entity) || entity instanceof Boss) return;
        double add = e.getFinalDamage() * getConfig().getDouble("multiplier.damageAddition") + random.nextDouble(0, 5);

        if (e instanceof EntityDamageByEntityEvent hasEntity) if (hasEntity.isCritical()) add = add * getConfig().getDouble("multiplier.critical");

        double fatigue = getFatigue(entity) + add;
        setFatigue(entity, fatigue);
        displayFatigue(entity);

        if (fatigue > 121) pulverize(entity);
    }

    @EventHandler
    public void heal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Damageable entity)) return;
        setFatigue(entity, getFatigue(entity) - e.getAmount() * getConfig().getDouble("multiplier.heal"));
    }

    @EventHandler
    public void consume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();

        ItemMeta meta = e.getItem().getItemMeta();
        if (meta == null) return;

        setFatigue(p, getFatigue(p) - meta.getFood().getNutrition() * getConfig().getDouble("multiplier.consume"));
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        setFatigue(e.getPlayer(), 0);
    }

    private Location getSpawn(Entity entity, EntitySize size) {
        Location loc = entity.getLocation().add(0, size.height, 0);
        loc.setYaw(0);
        loc.setPitch(0);

        return loc;
    }
}
