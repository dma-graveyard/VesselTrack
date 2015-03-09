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

import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.vessel.track.model.VesselTarget;
import dk.dma.vessel.track.rest.JsonSerializable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Vessel class A and B target
 *
 * This implementation provides backwards compatibility with the AisTrack API
 * and is used by ActicWeb for now
 */
@SuppressWarnings("unused")
public class AWVesselTargetVo implements JsonSerializable {

    private static final long serialVersionUID = 1L;

    AisTargetType targetType;
    int mmsi;
    String country;
    String lastReport;
    String lastPosReport;
    String lastStaticReport;
    Float lat;
    Float lon;
    Float cog;
    Float sog;
    Short heading;
    Short rot;
    Short length;
    Short width;
    String name;
    String callsign;
    Long imoNo;
    String destination;
    Float draught;
    String navStatus;
    Boolean moored;
    String eta;
    String vesselType;
    String vesselCargo;

    public AWVesselTargetVo() {
    }

    /**
     * Copy the fields of the given vessel target
     * @param t the vessel target
     */
    public AWVesselTargetVo(VesselTarget t) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        mmsi = t.getMmsi();
        targetType = t.getTargetType();
        country = t.getCountry();
        lastReport = t.getLastReport() != null ? df.format(t.getLastReport()) : null;
        lastPosReport = t.getLastPosReport() != null ? df.format(t.getLastPosReport()) : null;
        lastStaticReport = t.getLastStaticReport() != null ? df.format(t.getLastStaticReport()) : null;

        // Position data
        sog = t.getSog();
        cog = t.getCog();
        heading = t.getHeading();
        lat = t.getLat();
        lon = t.getLon();
        rot = t.getRot();
        navStatus = t.getNavStatus() != null ? t.getNavStatus().prettyStatus() : null;
        moored = (t.getNavStatus() == NavigationalStatus.AT_ANCHOR || t.getNavStatus() == NavigationalStatus.MOORED);

        // Static data
        name = t.getName();
        callsign = t.getCallsign();
        if (t.getVesselType() != null) {
            ShipTypeCargo shipTypeCargo = new ShipTypeCargo(t.getVesselType());
            vesselType = shipTypeCargo.prettyType();
            vesselCargo = shipTypeCargo.prettyCargo();
        }
        length = t.getLength();
        width = t.getWidth();
        destination = t.getDestination();
        draught = t.getDraught();
        eta = t.getEta() != null ? df.format(t.getEta()) : null;
        imoNo = t.getImoNo();
    }


    @Override
    public int hashCode() {
        return mmsi;
    }

    // ****** Getters and setters ******* //

    public AisTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AisTargetType targetType) {
        this.targetType = targetType;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLastReport() {
        return lastReport;
    }

    public void setLastReport(String lastReport) {
        this.lastReport = lastReport;
    }

    public String getLastPosReport() {
        return lastPosReport;
    }

    public void setLastPosReport(String lastPosReport) {
        this.lastPosReport = lastPosReport;
    }

    public String getLastStaticReport() {
        return lastStaticReport;
    }

    public void setLastStaticReport(String lastStaticReport) {
        this.lastStaticReport = lastStaticReport;
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

    public Float getCog() {
        return cog;
    }

    public void setCog(Float cog) {
        this.cog = cog;
    }

    public Float getSog() {
        return sog;
    }

    public void setSog(Float sog) {
        this.sog = sog;
    }

    public Short getHeading() {
        return heading;
    }

    public void setHeading(Short heading) {
        this.heading = heading;
    }

    public Short getRot() {
        return rot;
    }

    public void setRot(Short rot) {
        this.rot = rot;
    }

    public Short getLength() {
        return length;
    }

    public void setLength(Short length) {
        this.length = length;
    }

    public Short getWidth() {
        return width;
    }

    public void setWidth(Short width) {
        this.width = width;
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

    public Long getImoNo() {
        return imoNo;
    }

    public void setImoNo(Long imoNo) {
        this.imoNo = imoNo;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Float getDraught() {
        return draught;
    }

    public void setDraught(Float draught) {
        this.draught = draught;
    }

    public String getNavStatus() {
        return navStatus;
    }

    public void setNavStatus(String navStatus) {
        this.navStatus = navStatus;
    }

    public Boolean getMoored() {
        return moored;
    }

    public void setMoored(Boolean moored) {
        this.moored = moored;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public String getVesselType() {
        return vesselType;
    }

    public void setVesselType(String vesselType) {
        this.vesselType = vesselType;
    }

    public String getVesselCargo() {
        return vesselCargo;
    }

    public void setVesselCargo(String vesselCargo) {
        this.vesselCargo = vesselCargo;
    }
}
