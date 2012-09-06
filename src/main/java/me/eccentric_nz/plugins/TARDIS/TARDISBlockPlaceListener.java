package me.eccentric_nz.plugins.TARDIS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class TARDISBlockPlaceListener implements Listener {

    private TARDIS plugin;
    TARDISdatabase service = TARDISdatabase.getInstance();

    public TARDISBlockPlaceListener(TARDIS plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {

        Block block = event.getBlockPlaced();
        // only listen for redstone torches
        if (block.getType() == Material.REDSTONE_TORCH_ON) {
            Block blockBelow = block.getRelative(BlockFace.DOWN);
            Block blockBottom = blockBelow.getRelative(BlockFace.DOWN);
            // only continue if the redstone torch is placed on top of a LAPIS_BLOCK on top of an IRON_BLOCK
            if (blockBelow.getType() == Material.LAPIS_BLOCK && blockBottom.getType() == Material.IRON_BLOCK) {
                Player player = event.getPlayer();
                String playerNameStr = player.getName();
                // check to see if they already have a TARDIS
                try {
                    Connection connection = service.getConnection();
                    Statement statement = connection.createStatement();
                    // check if the chunk already contains a TARDIS
                    String queryTardis = "SELECT * FROM tardis WHERE owner = ?";
                    PreparedStatement pstatement = connection.prepareStatement(queryTardis);
                    pstatement.setString(1, playerNameStr);
                    ResultSet rs = pstatement.executeQuery();
                    if (!rs.next()) {
                        // get this chunk co-ords
                        Chunk chunk = blockBottom.getChunk();
                        String cw;
                        World chunkworld;
                        // check config to see whether we are using a default world to store TARDII
                        if (plugin.config.getBoolean("default_world") == Boolean.valueOf("true")) {
                            cw = plugin.config.getString("default_world_name");
                            chunkworld = plugin.getServer().getWorld(cw);
                        } else {
                            chunkworld = chunk.getWorld();
                            cw = chunkworld.getName();
                        }
                        int cx = chunk.getX();
                        int cz = chunk.getZ();
                        TARDISBuilder builder = new TARDISBuilder(plugin);
                        TARDISUtils utils = new TARDISUtils(plugin);
                        if (!utils.checkChunk(cw, cx, cz)) {
                            statement.executeUpdate("INSERT INTO chunks (world,x,z) VALUES ('" + cw + "'," + cx + "," + cz + ")");
                            float pyaw = player.getLocation().getYaw();
                            if (pyaw >= 0) {
                                pyaw = (pyaw % 360);
                            } else {
                                pyaw = (360 + (pyaw % 360));
                            }
                            Location block_loc = blockBottom.getLocation();
                            // determine direction player is facing
                            String d = "";
                            if (pyaw >= 315 || pyaw < 45) {
                                d = "SOUTH";
                            }
                            if (pyaw >= 225 && pyaw < 315) {
                                d = "EAST";
                            }
                            if (pyaw >= 135 && pyaw < 225) {
                                d = "NORTH";
                            }
                            if (pyaw >= 45 && pyaw < 135) {
                                d = "WEST";
                            }
                            // save data to database (tardis table)
                            int lastInsertId = 0;
                                String chun = cw + ":" + cx + ":" + cz;
                                String home = block_loc.getWorld().getName() + ":" + block_loc.getBlockX() + ":" + block_loc.getBlockY() + ":" + block_loc.getBlockZ();
                                String save = block_loc.getWorld().getName() + ":" + block_loc.getBlockX() + ":" + block_loc.getBlockY() + ":" + block_loc.getBlockZ();
                                String queryInsert = "INSERT INTO tardis (owner,chunk,direction,home,save) VALUES ('" + playerNameStr + "','" + chun + "','" + d + "','" + home + "','" + save + "')";
                                statement.executeUpdate(queryInsert);
                                ResultSet idRS = statement.getGeneratedKeys();
                                if (idRS != null && idRS.next()) {
                                    lastInsertId = idRS.getInt(1);
                                }
                                statement.close();
                            // turn the block stack into a TARDIS
                            builder.buildOuterTARDIS(lastInsertId, block_loc, Constants.COMPASS.valueOf(d));
                            builder.buildInnerTARDIS(plugin.schematic, chunkworld, Constants.COMPASS.valueOf(d), lastInsertId);
                        } else {
                            player.sendMessage("A TARDIS already exists at this location, please try another chunk!");
                        }
                    } else {
                        //ResultSet rsLeft = statement.executeQuery("SELECT save FROM tardis WHERE owner ='" + player + "'");
                        String leftLoc = rs.getString("save");
                        String[] leftData = leftLoc.split(":");
                        player.sendMessage("You already have a TARDIS, you left it in " + leftData[0] + " at x:" + leftData[1] + " y:" + leftData[2] + " z:" + leftData[3]);
                    }
                    rs.close();
                    statement.close();
                } catch (SQLException e) {
                    player.sendMessage(ChatColor.GRAY + Constants.MY_PLUGIN_NAME + ChatColor.RED + "Block Place Listener Error: " + ChatColor.WHITE + e);
                }
            }
        }
    }
}