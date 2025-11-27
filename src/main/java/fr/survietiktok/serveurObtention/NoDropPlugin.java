package fr.survietiktok.serveurObtention;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NoDropPlugin extends JavaPlugin implements Listener {

    private Set<Material> disabledBlocks;

    @Override
    public void onEnable() {
        // Génère la config si elle n'existe pas
        saveDefaultConfig();
        loadDisabledBlocks();

        // Enregistre les listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("NoDropBlocks démarré avec " + disabledBlocks.size() + " blocs dans la liste.");
    }

    private void loadDisabledBlocks() {
        disabledBlocks = new HashSet<>();
        FileConfiguration config = getConfig();

        List<String> list = config.getStringList("disabled-blocks");
        if (list == null) {
            getLogger().warning("Aucune clé 'disabled-blocks' trouvée dans config.yml");
            return;
        }

        for (String entry : list) {
            if (entry == null) continue;
            String name = entry.trim().toUpperCase();
            if (name.isEmpty()) continue;

            try {
                Material mat = Material.valueOf(name);
                if (mat.isBlock()) {
                    disabledBlocks.add(mat);
                } else {
                    getLogger().warning("Matériau non-block dans disabled-blocks: " + name);
                }
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Matériau inconnu dans disabled-blocks: " + name);
            }
        }
    }

    private boolean isDisabled(Material material) {
        return disabledBlocks.contains(material);
    }

    /**
     * Cas 1 : bloc cassé par un joueur.
     * On indique à Spigot de ne pas drop les items.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isDisabled(block.getType())) {
            event.setDropItems(false);
        }
    }

    /**
     * Cas 2 : tous les drops "standard" de blocs (minage, explosion, etc.)
     * BlockDropItemEvent est appelé au moment où les items vont apparaître.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDrop(BlockDropItemEvent event) {
        Block block = event.getBlock();
        if (!isDisabled(block.getType())) {
            return;
        }

        // On supprime tous les items qui allaient être droppés
        if (!event.getItems().isEmpty()) {
            event.getItems().clear();
        }
    }

    /**
     * Cas 3 : explosions venant d'entités (TNT, Creeper, Wither…)
     * + wind charges : elles ne casseront plus de blocs.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {

        // Si l'entité qui explose est un wind charge, on empêche la casse de blocs
        if (event.getEntity() != null
                && event.getEntity().getType().name().equalsIgnoreCase("WIND_CHARGE")) {
            // Explosion visuelle / dégâts sur entités, mais aucun bloc cassé
            event.blockList().clear();
            return;
        }

        // Pour le reste (TNT, creeper, etc.), on applique la logique "no drop" sur les blocs listés
        handleExplosionBlockList(event.blockList());
    }

    /**
     * Cas 4 : explosions "de bloc" (bed, respawn anchor, etc.)
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosionBlockList(event.blockList());
    }

    /**
     * Méthode commune pour traiter les listes de blocs affectés par une explosion.
     * Idée :
     *  - pour les blocs désactivés, on les met nous-mêmes à AIR
     *  - on les retire de la blockList pour que Spigot ne génère pas de drops
     */
    private void handleExplosionBlockList(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block block = it.next();
            if (isDisabled(block.getType())) {
                // On détruit le bloc sans drop
                block.setType(Material.AIR, false);
                // On l'enlève de la liste de traitement de l'explosion
                it.remove();
            }
        }
    }
}
