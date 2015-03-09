package dk.dma.vessel.track.rest;

import dk.dma.enav.model.geometry.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Used for returning a vessel target search result that consists of a list
 * of cluster entities and vessels.
 */
@SuppressWarnings("unused")
public class VesselClusterResultVo implements JsonSerializable {

    private static final long serialVersionUID = 1L;

    List<VesselTargetListVo> vessels = new ArrayList<>();
    List<VesselClusterVo> clusters = new ArrayList<>();

    /**
     * Returns a list of vessel clusters based on a filtering. The returned list does only contain clusters with vessels.
     *
     * @param vessels the total list vessels
     * @param mmsi optionally, a list of MMSI to always include
     * @param totalClusterThreshold if the total number of vessels is less than this threshold, no clustering is used
     * @param cellClusterThreshold if the number of vessels in a cluster cell is less than this threshold, no clustering is used for this cell
     * @param cellSizeDegrees the size of the cluster cells in degrees
     * @return the list of cluster entities and un-clustered vessel targets
     */
    public static VesselClusterResultVo computeClusterResult(
            List<VesselTargetListVo> vessels,
            Integer[] mmsi,
            int totalClusterThreshold,
            int cellClusterThreshold,
            float cellSizeDegrees) {

        VesselClusterResultVo result = new VesselClusterResultVo();

        // Define a set of the MMSI should always be included in the vessel list
        final Set<Integer> mmsiLookup = new HashSet<>();
        if (mmsi != null && mmsi.length > 0) {
            mmsiLookup.addAll(Arrays.asList(mmsi));
        }

        // If the number of vessels is below the given threshold, return all
        if (vessels.size() <= totalClusterThreshold) {
            result.setVessels(vessels);
            return result;
        }

        Grid grid = new Grid(cellSizeDegrees);

        // Maps cell ids to vessel clusters
        HashMap<Long, VesselClusterVo> map = new HashMap<>();

        // Iterate over targets
        vessels.forEach(vessel -> {
            // Compute the grid cell id
            long cellId = grid.getCellId(vessel.getLat(), vessel.getLon());
            VesselClusterVo cell = map.get(cellId);

            // Check if this is a new cell
            if (cell == null) {
                Position from = grid.getGeoPosOfCellId(cellId);

                double toLon = from.getLongitude() + grid.getCellSizeInDegrees();
                double toLat = from.getLatitude() + grid.getCellSizeInDegrees();
                Position to = Position.create(toLat, toLon);

                cell = new VesselClusterVo(from, to);
                map.put(cellId, cell);
            }

            cell.getVessels().add(vessel);
        });

        // Fill out the result.
        // Depending on the number of vessels in a cell, either return
        // the vessels or the cluster entity
        map.values().forEach(cell -> {
            if (cell.getVessels().size() <= cellClusterThreshold) {
                // Return the vessels of the cluster cell
                result.getVessels().addAll(cell.getVessels());

            } else {
                // Return the cluster cell, not the vessels.
                // However, first check if the cluster contains vessels that must always be included
                cell.getVessels().stream()
                        .filter(v -> mmsiLookup.contains(v.getMmsi()))
                        .forEach(v -> result.getVessels().add(v));

                // Compute the cluster metrics and nuke the vessel list
                cell.computeVesselCountAndDensity();
                cell.setVessels(null);
                result.getClusters().add(cell);
            }
        });

        return result;
    }

    public List<VesselTargetListVo> getVessels() {
        return vessels;
    }

    public void setVessels(List<VesselTargetListVo> vessels) {
        this.vessels = vessels;
    }

    public List<VesselClusterVo> getClusters() {
        return clusters;
    }

    public void setClusters(List<VesselClusterVo> clusters) {
        this.clusters = clusters;
    }
}
