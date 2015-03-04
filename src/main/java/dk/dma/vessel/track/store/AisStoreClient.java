/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.vessel.track.store;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.vessel.track.model.PastTrack;
import dk.dma.vessel.track.model.PastTrackPos;
import dk.dma.vessel.track.model.VesselTarget;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provides an interface for the back-end AIS-Store
 */
@Service
public class AisStoreClient {

    static final Logger LOG = LoggerFactory.getLogger(AisStoreClient.class);

    @Value("${aisViewUrl}")
    String aisViewUrl;

    @Value("${aisAuthHeader}")
    String aisAuthHeader;

    @Value("${pastTrackExpire}")
    String pastTrackTtl;

    @Value("${pastTrackMinDist}")
    String pastTrackMinDist;

    public List<PastTrackPos> getPastTrack(int mmsi, Integer minDist, Duration age) {

        // Determine URL
        age = age != null ? age : Duration.parse(pastTrackTtl);
        minDist = minDist == null ? Integer.valueOf(pastTrackMinDist) : minDist;
        ZonedDateTime now = ZonedDateTime.now();
        String from = now.format(DateTimeFormatter.ISO_INSTANT);
        ZonedDateTime end = now.minus(age);
        String to = end.format(DateTimeFormatter.ISO_INSTANT);
        String interval = String.format("%s/%s", to, from);
        String url = String.format("%s?mmsi=%d&interval=%s", aisViewUrl, mmsi, interval);


        final List<PastTrackPos> track = new ArrayList<>();
        try {
            long t0 = System.currentTimeMillis();

            // TEST
            url = url + "&filter=" + URLEncoder.encode("(s.country not in (GBR)) & (s.region!=808)", "UTF-8");

            // Set up a few timeouts and fetch the attachment
            URLConnection con = new URL(url).openConnection();
            con.setConnectTimeout(10 * 1000);       // 10 seconds
            con.setReadTimeout(60 * 1000);      // 1 minute

            if (!StringUtils.isEmpty(aisAuthHeader)) {
                con.setRequestProperty ("Authorization", aisAuthHeader);
            }

            try (InputStream in = con.getInputStream();
                BufferedInputStream bin = new BufferedInputStream(in)) {
                AisReader aisReader = AisReaders.createReaderFromInputStream(bin);
                aisReader.registerPacketHandler(new Consumer<AisPacket>() {
                    @Override
                    public void accept(AisPacket p) {
                        AisMessage message = p.tryGetAisMessage();
                        if (message == null || !(message instanceof IVesselPositionMessage)) {
                            return;
                        }
                        VesselTarget target = new VesselTarget();
                        target.merge(p, message);
                        if (!target.checkValidPos()) {
                            return;
                        }
                        track.add(new PastTrackPos(target.getLat(), target.getLon(), target.getCog(), target.getSog(), target
                                .getLastPosReport()));
                    }
                });
                aisReader.start();
                try {
                    aisReader.join();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            LOG.info(String.format("Read %d past track positions in %d ms",
                    track.size(),
                    System.currentTimeMillis() - t0));
        } catch (IOException e) {
            LOG.error("Failed to make REST query: " + url);
            throw new InternalError("REST endpoint failed");
        }
        LOG.info("AisStore returned track with " + track.size() + " points");
        return PastTrack.downSample(track, minDist, age.toMillis());
    }

}
