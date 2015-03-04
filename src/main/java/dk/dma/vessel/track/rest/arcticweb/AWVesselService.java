package dk.dma.vessel.track.rest.arcticweb;

import dk.dma.vessel.track.TargetFilter;
import dk.dma.vessel.track.VesselTrackHandler;
import dk.dma.vessel.track.model.MaxSpeed;
import dk.dma.vessel.track.model.PastTrackPos;
import dk.dma.vessel.track.model.VesselTarget;
import dk.dma.vessel.track.rest.PastTrackPosVo;
import dk.dma.vessel.track.store.AisStoreClient;
import dk.dma.vessel.track.store.DefaultMaxSpeedValues;
import org.apache.commons.lang.StringUtils;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for accessing vessel information
 */
@Controller
@RequestMapping("/target/vessel")
@SuppressWarnings("unused")
public class AWVesselService {

    static final Logger LOG = LoggerFactory.getLogger(AWVesselService.class);

    @Autowired
    VesselTrackHandler handler;

    @Autowired
    AisStoreClient aisStoreClient;

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
    public AWVesselTargetVo getTarget(@PathVariable("mmsi") Integer mmsi, HttpServletResponse response) {
        VesselTarget target = handler.getVessel(mmsi);
        if (target == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        return new AWVesselTargetVo(target);
    }

    /**
     * Returns the filtered list of vessel targets
     * @param ttlLive the time-to-live for LIVE targets
     * @param ttlSat the time-to-live for SAT targets
     * @param mmsi the MMSI of the targets
     * @param geo the geographical extent of the targets
     * @return the filtered list of vessel targets
     */
    @RequestMapping(
            value = "/list",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<AWVesselTargetVo> getTargetList(
            @RequestParam(value="ttlLive", required = false) String ttlLive,
            @RequestParam(value="ttlSat", required = false) String ttlSat,
            @RequestParam(value="mmsi", required = false) String[] mmsi,
            @RequestParam(value="geo", required = false) String[] geo
    ) {
        long t0 = System.currentTimeMillis();
        TargetFilter filter = new TargetFilter();

        if (StringUtils.isNotBlank(ttlLive)) {
            filter.setTtlLive(Duration.parse(ttlLive).getSeconds());
        }
        if (StringUtils.isNotBlank(ttlSat)) {
            filter.setTtlSat(Duration.parse(ttlSat).getSeconds());
        }
        if (mmsi != null && mmsi.length > 0) {
            filter.setMmsis(Arrays.asList(mmsi).stream().collect(Collectors.toSet()));
        }
        if (geo != null && geo.length > 0) {
            filter.setGeos(Arrays.asList(geo).stream().map(TargetFilter::getGeometry).collect(Collectors.toList()));
        }

        List<VesselTarget> result =  handler.getVesselList(filter);
        LOG.info(String.format("/list returned %d targets in %d ms", result.size(), System.currentTimeMillis() - t0));
        return result.stream()
                .map(AWVesselTargetVo::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of active vessel targets
     * @return the number of active vessel targets
     */
    @RequestMapping(
            value = "/count",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String executeQuery() {
        return String.format("{\"count\" : %d}", handler.getVesselStore().size());
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
        List<PastTrackPos> track = handler.getPastTracks(mmsi, minDist, age);
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
     * Returns the past track for the given MMSI
     * @param mmsi the MMSI of the target
     * @param minDist the minimum distance between past track positions
     * @param ageStr the age of the past track positions
     * @param response the servlet response
     * @return the past track for the given MMSI
     */
    @RequestMapping(
            value = "/longtrack/{mmsi}",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<PastTrackPosVo> getLongTrack(
            @PathVariable("mmsi") Integer mmsi,
            @RequestParam(value="minDist", required = false) Integer minDist,
            @RequestParam(value="age", required = false) String ageStr,
            HttpServletResponse response
    ) {
        Duration age = null;
        if (ageStr != null) {
            age = Duration.parse(ageStr);
        }
        List<PastTrackPos> track = aisStoreClient.getPastTrack(mmsi, minDist, age);
        if (track == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
        return track.stream()
                .map(PastTrackPosVo::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the max speed for the given MMSI
     * @param mmsi the MMSI of the target
     * @param response the servlet response
     * @return the max speed for the given MMSI
     */
    @RequestMapping(
            value = "/maxspeed/{mmsi}",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public MaxSpeed getMaxSpeed(
            @PathVariable("mmsi") Integer mmsi,
            HttpServletResponse response
    ) {
        VesselTarget target = handler.getVessel(mmsi);
        if (target == null) {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST);
            return null;
        } else {
            return getMaxSpeed(target);
        }
    }

    /**
     * Returns the max speeds for the active vessel targets
     * @param response the servlet response
     * @return the max speeds for the active vessel targets
     */
    @RequestMapping(
            value = "/maxspeed",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<MaxSpeed> maxSpeedList(
            HttpServletResponse response
    ) {
        return handler.getVesselStore().list().stream()
                .map(this::getMaxSpeed)
                        .collect(Collectors.toList());
    }

    /**
     * Computes the max speed for the given target
     * @param t the vessel target
     * @return the max speed for the given target
     */
    private MaxSpeed getMaxSpeed(VesselTarget t) {
        float sog = t.getSog() == null ? 0f : t.getSog();
        float maxSpeed = t.computeMaxSpeed();
        float speedForType = t.getVesselType() == null ? 0f : DefaultMaxSpeedValues.getMaxSpeedForType(t.getVesselType());

        // Compute the max
        float speed = Math.max(Math.max(sog, maxSpeed), speedForType);

        return new MaxSpeed(t.getMmsi(), speed);
    }
}
