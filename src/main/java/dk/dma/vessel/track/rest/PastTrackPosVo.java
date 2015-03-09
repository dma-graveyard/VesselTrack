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
package dk.dma.vessel.track.rest;

import dk.dma.vessel.track.model.PastTrackPos;

import java.util.Date;

/**
 * Defines a value object for the past track position entity
 */
@SuppressWarnings("unused")
public class PastTrackPosVo implements JsonSerializable {

    private static final long serialVersionUID = 1L;

    private float lat;
    private float lon;
    private float cog;
    private float sog;
    private Date time;

    /**
     * No-arg constructor
     */
    public PastTrackPosVo() {
    }

    /**
     * Constructor
     * Copy fields from a post track position
     * @param p the past track position to copy from
     */
    public PastTrackPosVo(PastTrackPos p) {
        super();
        lat = p.getLat();
        lon = p.getLon();
        cog = p.getCog();
        sog = p.getSog();
        time = p.getTime();
    }

    // ****** Getters and setters ******* //

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
