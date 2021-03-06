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
package dk.dma.vessel.track;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.packet.AisPacket;
import dk.dma.vessel.track.store.TargetStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * Receives AIS packets from the AIS bus and passes the packets to the underlying store
 */
@Service
public class VesselTrackHandler implements Consumer<AisPacket> {

    static final Logger LOG = LoggerFactory.getLogger(VesselTrackHandler.class);

    @Autowired
    private TargetStore vesselStore;

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(AisPacket packet) {
        // Wait until the store is ready
        if (!vesselStore.isStarted()) {
            LOG.info("Waiting for vessel store to start. Skipping " + packet);
            return;
        }

        // Reject packets without a timestamp
        if (packet.getTimestamp() == null) {
            return;
        }

        // Must have valid AIS message
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        AisTargetType type = message.getTargetType();
        if (type == null) {
            return;
        }

        // Handle different types
        switch (type) {
        case A:
        case B:
            handleVessel(packet);
            break;
        default:
            break;
        }
    }

    /**
     * Merge the AIS packet into the target store
     * @param packet the AIS packet
     */
    private void handleVessel(final AisPacket packet) {
        final AisMessage message = packet.tryGetAisMessage();

        // Reject invalid MMSI numbers
        if (message.getUserId() < 100000000 || message.getUserId() > 999999999) {
            return;
        }

        vesselStore.merge(packet, message);
    }
}
