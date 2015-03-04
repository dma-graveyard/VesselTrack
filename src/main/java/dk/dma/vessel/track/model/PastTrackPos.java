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
package dk.dma.vessel.track.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@SuppressWarnings("unused")
public class PastTrackPos implements Serializable, Comparable<PastTrackPos> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    Long id;

    @NotNull
    @ManyToOne(cascade = CascadeType.ALL)
    VesselTarget vesselTarget;

    float lat;
    float lon;
    float cog;
    float sog;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    Date time;

    public PastTrackPos() {
    }

    public PastTrackPos(VesselTarget target) {
        this.lat = target.getLat();
        this.lon = target.getLon();
        this.cog = target.getCog();
        this.sog = target.getSog();
        this.time = target.getLastPosReport();
    }

    public PastTrackPos(float lat, float lon, float cog, float sog, Date time) {
        super();
        this.lat = lat;
        this.lon = lon;
        this.cog = cog;
        this.sog = sog;
        this.time = time;
    }

    public boolean isDead(int ttl) {
        int elapsed = (int) ((System.currentTimeMillis() - time.getTime()) / 1000);
        return elapsed > ttl;
    }

    @Override
    public int compareTo(PastTrackPos p2) {
        return time.compareTo(p2.getTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PastTrackPos that = (PastTrackPos) o;

        return time.equals(that.time);
    }

    @Override
    public String toString() {
        return "PastTrackPos{" +
                "id=" + id +
                ", lat=" + lat +
                ", lon=" + lon +
                ", time=" + time +
                '}';
    }

    @Override
    public int hashCode() {
        return time.hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VesselTarget getVesselTarget() {
        return vesselTarget;
    }

    public void setVesselTarget(VesselTarget vesselTarget) {
        this.vesselTarget = vesselTarget;
    }

    public float getLat() {
        return lat;
    }

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getCog() {
        return cog;
    }

    public void setCog(float cog) {
        this.cog = cog;
    }

    public float getSog() {
        return sog;
    }

    public void setSog(float sog) {
        this.sog = sog;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
