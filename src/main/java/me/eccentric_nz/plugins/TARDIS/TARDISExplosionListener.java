package me.eccentric_nz.plugins.TARDIS;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class TARDISExplosionListener implements Listener {

    private TARDIS plugin;
    TARDISdatabase service = TARDISdatabase.getInstance();
    TARDISUtils utils = new TARDISUtils(plugin);

    public TARDISExplosionListener(TARDIS plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent e) {
        List<Block> blocks = e.blockList();
        int idchk = 0;
        // get list of police box blocks from DB
        try {
            Connection connection = service.getConnection();
            Statement statement = connection.createStatement();
            String queryBlocks = "SELECT blocks.*, doors.door_location, doors.door_direction FROM blocks, doors WHERE blocks.tardis_id = doors.tardis_id and doors.door_type = 0";
            ResultSet rsBlocks = statement.executeQuery(queryBlocks);
            if (rsBlocks.isBeforeFirst()) {
                while (rsBlocks.next()) {
                    String location = rsBlocks.getString("location");
                    int id = rsBlocks.getInt("tardis_id");
                    String[] loc_tmp = location.split(",");
                    String[] wStr = loc_tmp[0].split("=");
                    String world = wStr[2].substring(0, wStr[2].length() - 1);
                    World w = plugin.getServer().getWorld(world);
                    String[] xStr = loc_tmp[1].split("=");
                    String[] yStr = loc_tmp[2].split("=");
                    String[] zStr = loc_tmp[3].split("=");
                    int x = utils.parseNum(xStr[1].substring(0, (xStr[1].length() - 2)));
                    int y = utils.parseNum(yStr[1].substring(0, (yStr[1].length() - 2)));
                    int z = utils.parseNum(zStr[1].substring(0, (zStr[1].length() - 2)));
                    Block block = w.getBlockAt(x, y, z);
                    // if the block is a TARDIS block then remove it
                    if (e.blockList().contains(block)) {
                        e.blockList().remove(block);
                    }
                    if (id != idchk) {
                        String doorLoc[] = rsBlocks.getString("door_location").split(":");
                        Constants.COMPASS d = Constants.COMPASS.valueOf(rsBlocks.getString("door_direction"));
                        int dx = utils.parseNum(doorLoc[1]);
                        int dy = utils.parseNum(doorLoc[2]);
                        int dz = utils.parseNum(doorLoc[3]);
                        Block door_bottom = w.getBlockAt(dx, dy, dz);
                        Block door_under = door_bottom.getRelative(BlockFace.DOWN);
                        Block door_top = door_bottom.getRelative(BlockFace.UP);
                        BlockFace bf;
                        switch (d) {
                            case NORTH:
                                bf = BlockFace.WEST;
                                break;
                            case WEST:
                                bf = BlockFace.SOUTH;
                                break;
                            case SOUTH:
                                bf = BlockFace.EAST;
                                break;
                            default:
                                bf = BlockFace.NORTH;
                                break;
                        }
                        Block sign = door_top.getRelative(BlockFace.UP).getRelative(bf);
                        if (e.blockList().contains(sign)) {
                            e.blockList().remove(sign);
                        }
                        if (e.blockList().contains(door_under)) {
                            e.blockList().remove(door_under);
                        }
                        if (e.blockList().contains(door_bottom)) {
                            e.blockList().remove(door_bottom);
                        }
                        if (e.blockList().contains(door_top)) {
                            e.blockList().remove(door_top);
                        }
                    }
                    idchk = id;
                }
            }
            rsBlocks.close();
            statement.close();
        } catch (SQLException err) {
            System.err.println(Constants.MY_PLUGIN_NAME + " Explosion Listener error: " + err);
        }
    }
}