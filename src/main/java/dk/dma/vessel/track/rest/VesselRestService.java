package dk.dma.vessel.track.rest;

import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.message.ShipTypeCargo;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    public VesselTargetVo getTarget(@PathVariable("mmsi") Integer mmsi, HttpServletResponse response) {
        VesselTarget target = targetStore.get(mmsi);
        if (target == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        return new VesselTargetVo(target);
    }

    /**
     * Rest cal used for returning the vessels within the given OpenLayers bounds
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @param mmsi optionally, a list of mmsi to include
     * @return the list of vessels within the bounds
     */
    @RequestMapping(
            value = "/list",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object[]> getVessels(
            @RequestParam(value="top", defaultValue = "90") Float top,
            @RequestParam(value="left", defaultValue = "-180") Float left,
            @RequestParam(value="bottom", defaultValue = "-90") Float bottom,
            @RequestParam(value="right", defaultValue = "180") Float right,
            @RequestParam(value="mmsi", required = false) Integer[] mmsi
    ) throws Exception {

        long t0 = System.currentTimeMillis();
        Map<String, Object[]> result = new HashMap<>();

        targetStore.list().stream()
                .filter(withinOpenLayersBoundsOrMmsi(top, left, bottom, right, mmsi))
                .forEach(v -> {
                    Object[] data = new Object[7];
                    int i = 0;
                    data[i++] = v.getLat();
                    data[i++] = v.getLon();
                    data[i++] = v.getCog();
                    data[i++] = getShipType(v).ordinal();
                    data[i++] = v.getNavStatus() != null ? v.getNavStatus().getCode() : NavigationalStatus.UNDEFINED.getCode();
                    data[i++] = v.getLastReport().getTime();
                    data[i++] = v.getName();
                    result.put(String.valueOf(v.getMmsi()), data);
                });

        LOG.info(String.format("/list returned %d targets in %d ms", result.size(), System.currentTimeMillis() - t0));

        return result;
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
    public static ShipTypeCargo.ShipType getShipType(VesselTarget target) {
        if (target.getVesselType() != null) {
            ShipTypeCargo shipTypeCargo = new ShipTypeCargo(target.getVesselType());
            return shipTypeCargo.getShipType();
        }
        return ShipTypeCargo.ShipType.UNDEFINED;
    }

    /**
     * A predicate that filters for vessels that are withing the given OpenLayers bounds or in the list of MMSI
     * @param top the top latitude
     * @param left the left longitude
     * @param bottom the bottom latitude
     * @param right the right longitude
     * @param mmsi optionally, a list of mmsi to include
     * @return if the vessel is withing the given bounds
     */
    private static Predicate<VesselTarget> withinOpenLayersBoundsOrMmsi(float top, float left, float bottom, float right, Integer[] mmsi) {
        final Set<Integer> mmsiLookup = new HashSet<>();
        if (mmsi != null && mmsi.length > 0) {
            mmsiLookup.addAll(Arrays.asList(mmsi));
        }
        return t -> {
            // Must have a valid position
            if (t.getLat() == null && t.getLon() == null) {
                return false;
            }

            // Check if the mmsi param has been specified
            if (mmsiLookup.size() > 0 && mmsiLookup.contains(t.getMmsi())) {
                return true;
            }

            // Check that the vessel is within the bounds
            return  t.getLat() <= top && t.getLat() >= bottom &&
                    withinOpenLayersLongitude(t.getLon(), left, right);
        };
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
}
