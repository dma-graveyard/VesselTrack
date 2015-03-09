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

import dk.dma.ais.message.NavigationalStatus;
import dk.dma.vessel.track.model.VesselTarget;

import static dk.dma.vessel.track.rest.VesselRestService.getShipType;

/**
 * Vessel target value object used for compact vessel lists
 */
@SuppressWarnings("unused")
public class VesselTargetListVo implements JsonSerializable {

    private static final long serialVersionUID = 1L;

    int mmsi;
    Long lastReport;
    Float lat;
    Float lon;
    Float cog;
    Float sog;
    String name;
    String callsign;
    int navStatus;
    int vesselType;

    public VesselTargetListVo() {
    }

    /**
     * Copy the fields of the given vessel target
     * @param t the vessel target
     */
    public VesselTargetListVo(VesselTarget t) {
        mmsi = t.getMmsi();
        lastReport = t.getLastReport() != null ? t.getLastReport().getTime() : null;

        // Position data
        sog = t.getSog();
        cog = t.getCog();
        lat = t.getLat();
        lon = t.getLon();
        navStatus =  t.getNavStatus() != null ? t.getNavStatus().getCode() : NavigationalStatus.UNDEFINED.getCode();

        // Static data
        name = t.getName();
        callsign = t.getCallsign();
        vesselType = getShipType(t).ordinal();
    }

    @Override
    public int hashCode() {
        return mmsi;
    }

    // ****** Getters and setters ******* //


    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }

    public Long getLastReport() {
        return lastReport;
    }

    public void setLastReport(Long lastReport) {
        this.lastReport = lastReport;
    }

    public Float getLat() {
        return lat;
    }

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLon() {
        return lon;
    }

    public void setLon(Float lon) {
        this.lon = lon;
    }

    public Float getSog() {
        return sog;
    }

    public void setSog(Float sog) {
        this.sog = sog;
    }

    public Float getCog() {
        return cog;
    }

    public void setCog(Float cog) {
        this.cog = cog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public int getNavStatus() {
        return navStatus;
    }

    public void setNavStatus(int navStatus) {
        this.navStatus = navStatus;
    }

    public int getVesselType() {
        return vesselType;
    }

    public void setVesselType(int vesselType) {
        this.vesselType = vesselType;
    }
}
