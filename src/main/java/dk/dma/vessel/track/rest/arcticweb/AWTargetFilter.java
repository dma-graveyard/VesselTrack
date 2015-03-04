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
package dk.dma.vessel.track.rest.arcticweb;

import dk.dma.ais.packet.AisPacketTags;
import dk.dma.enav.model.geometry.Area;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.Circle;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.vessel.track.model.VesselTarget;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Supports target filtering
 *
 * This implementation provides backwards compatibility with the AisTrack API
 * and is used by ActicWeb for now
 */
@SuppressWarnings("unused")
public class AWTargetFilter {

    Long ttlLive;
    Long ttlSat;
    Set<String> mmsis;
    List<Area> geos;

    public AWTargetFilter() {

    }

    public boolean test(VesselTarget target) {
        Long ttl = target.getSourceType() != null && target.getSourceType() == AisPacketTags.SourceType.SATELLITE ? ttlSat : ttlLive;
        if  (ttl != null) {
            long age = (System.currentTimeMillis() - target.getLastReport().getTime()) / 1000;
            if (age > ttl) {
                return false;
            }
        }
        if (mmsis != null) {
            if (!mmsis.contains(Integer.toString(target.getMmsi()))) {
                return false;
            }
        }
        if (geos != null) {
            if (target.getLat() == null || target.getLon() == null || !Position.isValid(target.getLat(), target.getLon())) {
                return false;
            }
            Position pos = Position.create(target.getLat(), target.getLon());
            for (Area area : geos) {
                if (area.contains(pos)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public Long getTtlLive() {
        return ttlLive;
    }

    public void setTtlLive(Long ttlLive) {
        this.ttlLive = ttlLive;
    }

    public Long getTtlSat() {
        return ttlSat;
    }

    public void setTtlSat(Long ttlSat) {
        this.ttlSat = ttlSat;
    }

    public Set<String> getMmsis() {
        return mmsis;
    }

    public void setMmsis(Set<String> mmsis) {
        this.mmsis = mmsis;
    }

    public List<Area> getGeos() {
        return geos;
    }

    public void setGeos(List<Area> geos) {
        this.geos = geos;
    }

    public static Integer getInt(String str) {
        if (str == null) {
            return null;
        }
        return Integer.parseInt(str);
    }
    
    public static Area getGeometry(String geometry) {
        String[] elems = StringUtils.split(geometry, ',');
        double[] numbers = new double[elems.length - 1];
        for (int i = 1; i < elems.length; i++) {
            numbers[i - 1] = Double.parseDouble(elems[i]);
        }
        if (elems[0].equalsIgnoreCase("circle")) {
            return new Circle(numbers[0], numbers[1], numbers[2], CoordinateSystem.GEODETIC);
        }
        if (elems[0].equalsIgnoreCase("bb")) {
            Position pos1 = Position.create(numbers[0], numbers[1]);
            Position pos2 = Position.create(numbers[2], numbers[3]);
            return BoundingBox.create(pos1, pos2, CoordinateSystem.GEODETIC);
        }
        return null;
    }

}
