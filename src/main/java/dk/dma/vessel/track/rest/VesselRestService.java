package dk.dma.vessel.track.rest;

import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.ais.message.ShipTypeCargo.ShipType;
import dk.dma.vessel.track.model.PastTrackPos;
import dk.dma.vessel.track.model.VesselTarget;
import dk.dma.vessel.track.store.TargetStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * REST API for accessing vessel information
 */
@Controller
@RequestMapping("/vessels")
@SuppressWarnings("unused")
public class VesselRestService  {

    static final Logger LOG = LoggerFactory.getLogger(VesselRestService.class);

    @Autowired
    TargetStore targetStore;

    /**
     * Returns the number of active vessels
     * @return the number of active vessels
     */
    @RequestMapping(
            value = "/count",
            method = RequestMethod.GET
    )
    @ResponseBody
    public String count() {
        return String.format("{\"count\" : %d}", targetStore.size());
    }

    /**
     * Returns the vessel target with the given MMSI
     * @param mmsi the MMSI
     * @param response the servlet response
     * @return the vessel target with the given MMSI
     */
    @RequestMapping(
            value = "{mmsi}",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public VesselTargetDetailsVo getTarget(@PathVariable("mmsi") Integer mmsi, HttpServletResponse response) {
        VesselTarget target = targetStore.get(mmsi);
        if (target == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        return new VesselTargetDetailsVo(target);
    }

    /**
     * REST call used for returning the vessels within the given OpenLayers bounds
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @param mmsi optionally, a list of MMSI to always include
     * @return the list of vessels within the bounds
     */
    @RequestMapping(
            value = "/list",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<VesselTargetListVo> getVessels(
            @RequestParam(value="top", defaultValue = "90") Float top,
            @RequestParam(value="left", defaultValue = "-180") Float left,
            @RequestParam(value="bottom", defaultValue = "-90") Float bottom,
            @RequestParam(value="right", defaultValue = "180") Float right,
            @RequestParam(value="mmsi", required = false) Integer[] mmsi,
            @RequestParam(value="filter", required = false) String filter,
            @RequestParam(value="maxHits", required = false) Integer maxHits
    ) throws Exception {

        long t0 = System.currentTimeMillis();

        if (maxHits == null) {
            maxHits = Integer.MAX_VALUE;
        }

        List<VesselTargetListVo> result = computeVessels(top, left, bottom, right, mmsi, filter, maxHits);

        LOG.info(String.format("/list returned %d vessels in %d ms", result.size(), System.currentTimeMillis() - t0));

        return result;
    }

    /**
     * REST call used for returning the vessels within the given OpenLayers bounds.
     * The returned data contains a list of cluster entities and vessels.
     *
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @param mmsi optionally, a list of MMSI to always include
     * @return the list of vessels within the bounds
     */
    @RequestMapping(
            value = "/cluster-list",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public VesselClusterResultVo getVesselClusters(
            @RequestParam(value="top", defaultValue = "90") Float top,
            @RequestParam(value="left", defaultValue = "-180") Float left,
            @RequestParam(value="bottom", defaultValue = "-90") Float bottom,
            @RequestParam(value="right", defaultValue = "180") Float right,
            @RequestParam(value="mmsi", required = false) Integer[] mmsi,
            @RequestParam(value="filter", required = false) String filter,
            @RequestParam(value="cellSize", required = false) Float cellSize
    ) throws Exception {

        long t0 = System.currentTimeMillis();

        List<VesselTargetListVo> vessels = computeVessels(top, left, bottom, right, mmsi, filter, Integer.MAX_VALUE);

        cellSize = (cellSize == null) ? (float)0.1 : cellSize;
        VesselClusterResultVo result = VesselClusterResultVo.computeClusterResult(vessels, mmsi, 1, 40, cellSize);

        LOG.info(String.format("/cluster-list returned %d vessels and %d clusters in %d ms",
                result.getVessels().size(),
                result.getClusters().size(),
                System.currentTimeMillis() - t0));

        return result;
    }

    /**
     * Computes the vessels within the given OpenLayers bounds
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @param mmsi optionally, a list of MMSI to always include
     * @return the list of vessels within the bounds
     */
    public List<VesselTargetListVo> computeVessels(Float top, Float left, Float bottom, Float right, Integer[] mmsi, String filter, int maxHits) throws Exception {

        // Construct the filters used for filtering the vessel target list
        Predicate<VesselTarget> mmsiFilter = hasMmsi(mmsi);
        Predicate<VesselTarget> boundsFilter = withinOpenLayersBounds(top, left, bottom, right);
        VesselTargetFilter searchFilter = new VesselTargetFilter(filter);

        return targetStore.list()
                .stream()
                .filter(t -> mmsiFilter.test(t) || (boundsFilter.test(t) && searchFilter.test(t)))
                .limit(maxHits)
                .map(VesselTargetListVo::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the past track for the given MMSI
     * @param mmsi the MMSI of the target
     * @param minDist the minimum distance between past track positions
     * @param ageStr the age of the past track positions
     * @param response the servlet response
     * @return the past track for the given MMSI
     */
    @RequestMapping(
            value = "/track/{mmsi}",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<PastTrackPosVo> getTrack(
            @PathVariable("mmsi") Integer mmsi,
            @RequestParam(value="minDist", required = false) Integer minDist,
            @RequestParam(value="age", required = false) String ageStr,
            HttpServletResponse response
    ) {
        long t0 = System.currentTimeMillis();
        Duration age = null;
        if (ageStr != null) {
            age = Duration.parse(ageStr);
        }
        List<PastTrackPos> track = targetStore.getPastTracks(mmsi, minDist, age);
        if (track == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        LOG.info(String.format("/track returned %d positions in %d ms", track.size(), System.currentTimeMillis() - t0));
        return track.stream()
                .map(PastTrackPosVo::new)
                .collect(Collectors.toList());
    }


    /**
     * Returns the ship type of the given vessel target
     * @param target the vessel target
     * @return the ship type of the given vessel target
     */
    public static ShipType getShipType(VesselTarget target) {
        if (target.getVesselType() != null) {
            ShipTypeCargo shipTypeCargo = new ShipTypeCargo(target.getVesselType());
            return shipTypeCargo.getShipType();
        }
        return ShipType.UNDEFINED;
    }

    /**
     * Returns a predicates that include targets with an mmsi in the list
     * @param mmsi the MMSI's to include
     * @return a predicate filtering the targets on MMSI
     */
    private static Predicate<VesselTarget> hasMmsi(Integer[] mmsi) {
        final Set<Integer> mmsiLookup = new HashSet<>();
        if (mmsi != null && mmsi.length > 0) {
            mmsiLookup.addAll(Arrays.asList(mmsi));
        }
        return t -> mmsiLookup.contains(t.getMmsi());
    }

    /**
     * A predicate that filters for vessels that are withing the given OpenLayers bounds
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @return if the vessel is withing the given bounds
     */
    private static Predicate<VesselTarget> withinOpenLayersBounds(float top, float left, float bottom, float right) {
        return t -> !(t.getLat() == null || t.getLon() == null) &&
                    t.getLat() <= top && t.getLat() >= bottom && withinOpenLayersLongitude(t.getLon(), left, right);
    }

    /**
     * Returns if the longitude is within the given bounds.
     * <p>
     * The way OpenLayers define bounds, the left longitude is ALWAYS smaller than
     * the right longitude. So if the bounds cross the date line, the left longitude may
     * be less than -180 or the right longitude greater than 180 degrees.
     *
     * @param lon the longitude to test
     * @param left the left longitude
     * @param right the right longitude
     * @return if the longitude is within the given bounds
     */
   private static boolean withinOpenLayersLongitude(float lon, float left, float right) {
       if (left < -180) {
           return (lon <= right && lon >= -180) || lon >= left + 360;
       } else if (right > 180) {
           return (lon >= left && lon <= 180) || lon <= right - 360;
       }
       return lon >= left && lon <= right;
   }


    /**
     * Rest call used for returning an auto-complete search filter option list
     * @param key the type of vessel target attribute to filter on
     * @param term the search term
     * @param maxHits the maximum number of values to return
     * @return the search filter option list
     */
    @RequestMapping(
            value = "/search-options",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<VesselTargetFilter.SearchFilterOptionVo> getSearchFilterOptions(
            @RequestParam(value="key") String key,
            @RequestParam(value="term", defaultValue = "") String term,
            @RequestParam(value="maxHits", defaultValue = "20") int maxHits
    ) {
        return VesselTargetFilter.getSearchFilterOptions(targetStore, key, term, maxHits);
    }

}
