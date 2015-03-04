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

    public static final String LOAD_TARGETS_SQL =
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

    ConcurrentHashMap<Integer, VesselTarget> cache;

    boolean stopped;
    boolean started;

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {

        cache = new ConcurrentHashMap<>();
        LOG.info("Initialized target store with expiry: " + targetExpire);

        // Load data from the DB
        loadFromDB();
        started = true;
    }

    @PreDestroy
    public void destroy() {
        try {
            stopped = true;
            LOG.info("Shutting down target store");
        } catch (Exception e) {
            LOG.error("Error shutting down AIS bus", e);
        }
    }

    @Transactional
    private void loadFromDB() {
        long t0 = System.currentTimeMillis();
        long expiry = t0 - Duration.parse(targetExpire).toMillis();

        // TODO: For some obscure reason, it takes forever to load the vessel targets
        // unless we "prime" the database using something like:
        LOG.debug("Priming DB with " + em.createQuery("select v.mmsi from VesselTarget v").getResultList().stream().count() + " vessels");

        em.createQuery(LOAD_TARGETS_SQL, VesselTarget.class)
                .setParameter("lastReport", new Date(expiry))
                .getResultList()
                .forEach(t -> cache.put(t.getMmsi(), t));
        em.clear();

        long pastTrackCnt = cache.values().stream()
                .filter(t -> t.getLastPastTrackPos() != null)
                .count();

        LOG.info("**** Loaded " + cache.size() + " targets (of which " + pastTrackCnt +
                " has past-tracks) from DB in " + (System.currentTimeMillis() - t0) + " ms");
    }

    @Scheduled(cron="10 0 */1 * * *")
    public void periodicallyExpireTargets() {
        long t0 = System.currentTimeMillis();
        long expiry = t0 - Duration.parse(targetExpire).toMillis();
        cache.entrySet().removeIf(t -> t.getValue().getLastReport().getTime() < expiry);
        LOG.info("Clean up expired targets in " + (System.currentTimeMillis() - t0) + " ms");
    }

    @Scheduled(cron="50 17 9 */1 * *")
    @Transactional
    public void periodicallyExpirePastTracks() {
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

    @Scheduled(cron="20 */1 * * * *")
    @Transactional
    public void periodicallySaveToDB() {
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

    public boolean isStarted() {
        return started;
    }

    public VesselTarget merge(AisPacket packet, AisMessage message) {
        if (started && !stopped) {
            VesselTarget target = cache.computeIfAbsent(message.getUserId(), VesselTarget::new);
            target.merge(packet, message);
            return target;
        }
        return null;
    }

    public VesselTarget get(int mmsi) {
        return stopped ? null : cache.get(mmsi);
    }

    public void put(VesselTarget target) {
        if (!stopped) {
            cache.put(target.getMmsi(), target);
        }
    }

    public Collection<VesselTarget> list() {
        return stopped ? new ArrayList<>() : cache.values();
    }

    public long size() {
        return stopped ? 0 : cache.size();
    }

    public List<PastTrackPos> getPastTracks(int mmsi, Integer minDist, Duration age) {
        List<PastTrackPos> result = new ArrayList<>();

        VesselTarget target = get(mmsi);
        if (target == null) {
            return result;
        }

        if (minDist == null) {
            minDist = Integer.valueOf(pastTrackMinDist);
        }

        if (age == null) {
            age = Duration.parse(pastTrackTtl);
        }
        ZonedDateTime date = ZonedDateTime.now().minus(age);

        result.addAll(em.createQuery(LOAD_PAST_TRACKS_SQL, PastTrackPos.class)
                .setParameter("time", Date.from(date.toInstant()))
                .setParameter("mmsi", new Integer(mmsi))
                .getResultList());

        PastTrackPos currentPos = new PastTrackPos(target);
        if (result.size() > 0 && !result.get(0).equals(currentPos)) {
            result.add(0, currentPos);
        }

        return PastTrack.downSample(result, minDist, age.toMillis());
    }

}
