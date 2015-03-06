package dk.dma.vessel.track.store;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.vessel.track.model.PastTrack;
import dk.dma.vessel.track.model.PastTrackPos;
import dk.dma.vessel.track.model.VesselTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of a target store
 */
@Service
public class TargetStore {

    static final Logger LOG = LoggerFactory.getLogger(TargetStore.class);

    public static final String PRIME_TARGETS_DB_SQL =
            "select v.mmsi from " + VesselTarget.class.getSimpleName() + " v";

    public static final String LOAD_TARGETS_SQL =
            "SELECT t FROM " + VesselTarget.class.getSimpleName() + " t " +
                    " where t.lastReport > :lastReport";

    public static final String LOAD_TARGETS_INCL_PAST_TRACKS_SQL =
            "SELECT t FROM " + VesselTarget.class.getSimpleName() + " t " +
                    " left join fetch t.lastPastTrackPos p " +
                    " where t.lastReport > :lastReport";

    public static final String DELETE_PAST_TRACKS_SQL =
            "DELETE FROM " + PastTrackPos.class.getSimpleName() + " p " +
                    " where p.time < :time " +
                    " and not exists (" +
                    "    select v from " + VesselTarget.class.getSimpleName() + " v where v.lastPastTrackPos = p" +
                    " )";

    public static final String LOAD_PAST_TRACKS_SQL =
            "SELECT p FROM " + PastTrackPos.class.getSimpleName() + " p " +
                    " where p.time > :time and p.vesselTarget.mmsi = :mmsi " +
                    " order by p.time desc";


    @Resource
    EntityManager em;

    @Value("${targetExpire}")
    String targetExpire;

    @Value("${pastTrackExpire}")
    String pastTrackExpire;

    @Value("${pastTrackTtl}")
    String pastTrackTtl;

    @Value("${pastTrackMinDist}")
    String pastTrackMinDist;

    @Value("${slave:false}")
    boolean slave;

    ConcurrentHashMap<Integer, VesselTarget> cache;

    boolean stopped;
    boolean started;

    /**
     * Called when the store is initialized
     */
    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {

        cache = new ConcurrentHashMap<>();
        LOG.info("Starting up as " + (slave ? "read-only slave instance" : "master instance"));

        // Load data from the DB
        loadFromDB();
        started = true;
    }

    /**
     * Called when the store is destroyed
     */
    @PreDestroy
    public void destroy() {
        try {
            stopped = true;
            LOG.info("Shutting down target store");
        } catch (Exception e) {
            LOG.error("Error shutting down AIS bus", e);
        }
    }

    /**
     * Load and cache the vessel targets from the database
     */
    @Transactional
    private void loadFromDB() {
        long t0 = System.currentTimeMillis();
        long expiry = t0 - Duration.parse(targetExpire).toMillis();

        ConcurrentHashMap<Integer, VesselTarget> newCache = new ConcurrentHashMap<>();

        if (slave) {
            // Load and cache all active vessel targets
            em.createQuery(LOAD_TARGETS_SQL, VesselTarget.class)
                    .setParameter("lastReport", new Date(expiry))
                    .getResultList()
                    .forEach(t -> newCache.put(t.getMmsi(), t));
            em.clear();

            LOG.info("Loaded " + newCache.size() + " targets from DB in " + (System.currentTimeMillis() - t0) + " ms");

        } else {
            // Prime the vessel target table. It increases the speed of the subsequent SQL dramatically
            LOG.debug("Priming DB with " + em.createQuery(PRIME_TARGETS_DB_SQL)
                    .getResultList().stream().count() + " vessels");

            // Load and cache all active vessel targets
            em.createQuery(LOAD_TARGETS_INCL_PAST_TRACKS_SQL, VesselTarget.class)
                    .setParameter("lastReport", new Date(expiry))
                    .getResultList()
                    .forEach(t -> newCache.put(t.getMmsi(), t));
            em.clear();

            // Past track stats
            long pastTrackCnt = newCache.values().stream()
                    .filter(t -> t.getLastPastTrackPos() != null)
                    .count();

            LOG.info("**** Loaded " + newCache.size() + " targets (of which " + pastTrackCnt +
                    " has past-tracks) from DB in " + (System.currentTimeMillis() - t0) + " ms");
        }

        // Update the current cache
        cache = newCache;
    }

    /**
     * Periodically expire vessel targets from the cache
     */
    @Scheduled(cron="10 0 */1 * * *")
    public void periodicallyExpireTargets() {
        long t0 = System.currentTimeMillis();
        long expiry = t0 - Duration.parse(targetExpire).toMillis();
        cache.entrySet().removeIf(t -> t.getValue().getLastReport().getTime() < expiry);
        LOG.info("Clean up expired targets in " + (System.currentTimeMillis() - t0) + " ms");
    }

    /**
     * Only used by master instances:<br>
     * Periodically delete expired past tracks from the database
     */
    @Scheduled(cron="50 17 9 */1 * *")
    @Transactional
    public void periodicallyExpirePastTracks() {
        // Only master instance expires past tracks
        if (slave) {
            return;
        }

        long t0 = System.currentTimeMillis();
        long expiry = t0 - Duration.parse(pastTrackExpire).toMillis();
        try {
            int cnt = em.createQuery(DELETE_PAST_TRACKS_SQL)
                    .setParameter("time", new Date(expiry))
                    .executeUpdate();
            LOG.info("Clean up " + cnt + " expired past-tracks in " + (System.currentTimeMillis() - t0) + " ms");
        } catch (Exception e) {
            LOG.error("Error cleaning up expired past-tracks", e);
        }
    }

    /**
     * Only used by slave instances:<br>
     * Periodically load vessel targets and past tracks from the database
     */
    @Scheduled(cron="40 */1 * * * *")
    @Transactional
    @SuppressWarnings("all")
    public void periodicallyLoadFromDB() {
        // Only slave instances loads data periodically from the DB
        if (slave) {
            loadFromDB();
        }
    }

    /**
     * Only used by master instances:<br>
     * Periodically save changed vessel targets and past tracks to the database
     */
    @Scheduled(cron="20 */1 * * * *")
    @Transactional
    @SuppressWarnings("all")
    public void periodicallySaveToDB() {
        // Only master instances saves data periodically to the DB
        if (slave) {
            return;
        }

        try {
            long t0 = System.currentTimeMillis();
            int cntNewTargets = 0, cntUpdatedTargets = 0, cntNewPastTrack = 0;
            for (VesselTarget t : cache.values()) {
                if (stopped) {
                    break;
                }
                if (t.changed() == VesselTarget.State.NEW || t.changed() == VesselTarget.State.UPDATED) {

                    // Check if there are past track entries to add
                    synchronized (t) {
                        VesselTarget.State state = t.changed();
                        PastTrackPos newPos = t.getNewPastTrackPos();
                        if (newPos != null && t.computePastTrackDist(newPos) > VesselTarget.PAST_TRACK_MIN_DIST) {
                            newPos.setVesselTarget(t);
                            t.setLastPastTrackPos(newPos);
                            newPos = null;
                            cntNewPastTrack++;
                        }

                        // Persist the changes
                        t = em.merge(t);
                        cache.put(t.getMmsi(), t);
                        if (state == VesselTarget.State.NEW) {
                            cntNewTargets++;
                        } else {
                            cntUpdatedTargets++;
                        }
                        if ((cntNewTargets + cntUpdatedTargets) % 1000 == 0) {
                            em.flush();
                        }

                        // newPastTrackPos is transient and must be restored after merge()
                        t.setNewPastTrackPos(newPos);
                        t.flagChanged(VesselTarget.State.NONE);

                    }

                }
            }
            em.clear();
            LOG.info("New targets: " + cntNewTargets +
                    ", updated targets: " + cntUpdatedTargets +
                    ", new past-tracks: " + cntNewPastTrack +
                    ", Time: " + (System.currentTimeMillis() - t0) + " ms");
        } catch (Exception e) {
            LOG.error("Error saving to database", e);
        }
    }

    /**
     * Returns if the store is started and the cache is loaded
     * @return if the store is started and the cache is loaded
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Only used by master instances:<br>
     * Merges the new AIS packet into the vessel target cache
     * @param packet the AIS packet
     * @param message the AIS message
     * @return the merged vessel target
     */
    public VesselTarget merge(AisPacket packet, AisMessage message) {
        if (!slave && started && !stopped) {
            VesselTarget target = cache.computeIfAbsent(message.getUserId(), VesselTarget::new);
            target.merge(packet, message);
            return target;
        }
        return null;
    }

    /**
     * Returns the vessel target with the given MMSI
     * @param mmsi the MMSI
     * @return the vessel target with the given MMSI or null if not found
     */
    public VesselTarget get(int mmsi) {
        return stopped ? null : cache.get(mmsi);
    }

    /**
     * Returns the list of vessel targets currently cached
     * @return the list of vessel targets currently cached
     */
    public Collection<VesselTarget> list() {
        return stopped ? new ArrayList<>() : cache.values();
    }

    /**
     * Returns the number of vessel targets currently cached
     * @return the number of vessel targets currently cached
     */
    public long size() {
        return stopped ? 0 : cache.size();
    }

    /**
     * Returns the past tracks for the vessel target with the given MMSI
     * @param mmsi the MMSI
     * @param minDist the minimum distance in meters between positions
     * @param age the minimum duration of the past track positions
     * @return the past track
     */
    public List<PastTrackPos> getPastTracks(int mmsi, Integer minDist, Duration age) {
        List<PastTrackPos> result = new ArrayList<>();

        // Check that the target is active
        VesselTarget target = get(mmsi);
        if (target == null) {
            return result;
        }

        // Check if minimum distance is defined
        if (minDist == null) {
            minDist = Integer.valueOf(pastTrackMinDist);
        }

        // Check if duration is defined
        if (age == null) {
            age = Duration.parse(pastTrackTtl);
        }
        ZonedDateTime date = ZonedDateTime.now().minus(age);

        // Fetch data from the database
        result.addAll(em.createQuery(LOAD_PAST_TRACKS_SQL, PastTrackPos.class)
                .setParameter("time", Date.from(date.toInstant()))
                .setParameter("mmsi", new Integer(mmsi))
                .getResultList());

        // Add the current position of the vessel target
        PastTrackPos currentPos = new PastTrackPos(target);
        if (result.size() > 0 && !result.get(0).equals(currentPos)) {
            result.add(0, currentPos);
        }

        // Down-sample the past track position list
        return PastTrack.downSample(result, minDist, age.toMillis());
    }

}
