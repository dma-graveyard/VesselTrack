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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.vessel.track.model.VesselTarget;

import java.io.Serializable;
import java.util.Date;

import static dk.dma.vessel.track.rest.VesselRestService.getShipType;

/**
 * Vessel class A and B target
 */
@SuppressWarnings("unused")
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class VesselTargetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    AisTargetType targetType;
    int mmsi;
    String country;
    Date lastReport;
    Date lastPosReport;
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
    Integer navStatus;
    Boolean moored;
    Date eta;
    int vesselType;

    public VesselTargetVo() {
    }

    /**
     * Copy the fields of the given vessel target
     * @param t the vessel target
     */
    public VesselTargetVo(VesselTarget t) {
        mmsi = t.getMmsi();
        targetType = t.getTargetType();
        country = t.getCountry();
        lastReport = t.getLastReport();

        // Position data
        sog = t.getSog();
        cog = t.getCog();
        heading = t.getHeading();
        lat = t.getLat();
        lon = t.getLon();
        rot = t.getRot();
        navStatus =  t.getNavStatus() != null ? t.getNavStatus().getCode() : NavigationalStatus.UNDEFINED.getCode();
        moored = (t.getNavStatus() == NavigationalStatus.AT_ANCHOR || t.getNavStatus() == NavigationalStatus.MOORED);

        // Static data
        name = t.getName();
        callsign = t.getCallsign();
        vesselType = getShipType(t).ordinal();
        length = t.getLength();
        width = t.getWidth();
        destination = t.getDestination();
        draught = t.getDraught();
        eta = t.getEta();
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

    public Date getLastReport() {
        return lastReport;
    }

    public void setLastReport(Date lastReport) {
        this.lastReport = lastReport;
    }

    public Date getLastPosReport() {
        return lastPosReport;
    }

    public void setLastPosReport(Date lastPosReport) {
        this.lastPosReport = lastPosReport;
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

    public Integer getNavStatus() {
        return navStatus;
    }

    public void setNavStatus(Integer navStatus) {
        this.navStatus = navStatus;
    }

    public Boolean getMoored() {
        return moored;
    }

    public void setMoored(Boolean moored) {
        this.moored = moored;
    }

    public Date getEta() {
        return eta;
    }

    public void setEta(Date eta) {
        this.eta = eta;
    }

    public int getVesselType() {
        return vesselType;
    }

    public void setVesselType(int vesselType) {
        this.vesselType = vesselType;
    }
}
