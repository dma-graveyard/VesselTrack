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

import dk.dma.ais.data.AisTargetDimensions;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketTags;
import dk.dma.ais.packet.AisPacketTags.SourceType;
import dk.dma.enav.model.Country;
import dk.dma.enav.model.geometry.Position;
import org.apache.commons.lang.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;

/**
 * Vessel target entity
 */
@Entity
@SuppressWarnings("unused")
public class VesselTarget implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The number of days (plus one, actually) to track max-speed for.
     */
    private static final int MAX_SPEED_DAYS = 30;

    /**
     * The minimum distance between two past track positions.
     */
    public static final int PAST_TRACK_MIN_DIST = 100; // Meters

    public enum State { NONE, NEW, UPDATED }

    @Transient
    State changed = State.NONE;

    @Id
    int mmsi;

    @NotNull
    AisTargetType targetType;

    @NotNull
    SourceType sourceType;

    @Column(length = 2)
    String country;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    Date lastReport;

    // **** Position Data - Instantiate with invalid data
    @Temporal(TemporalType.TIMESTAMP)
    Date lastPosReport;
    Float lat;
    Float lon;
    Float cog;
    Float sog;
    Short heading;
    Short rot;
    NavigationalStatus navStatus;

    // **** Static Data
    @Temporal(TemporalType.TIMESTAMP)
    Date lastStaticReport;
    Short length;
    Short width;
    String name;
    String callsign;
    Long imoNo;
    String destination;
    Float draught;
    Date eta;
    Integer vesselType;

    // *** Max-speed data
    @Column(columnDefinition="BINARY(" + (MAX_SPEED_DAYS * 2) + ")")
    byte[] maxSpeed;

    // *** Past track reference
    @OneToOne(cascade = CascadeType.ALL)
    PastTrackPos lastPastTrackPos;

    @Transient
    PastTrackPos newPastTrackPos;


    /**
     * Constructor
     */
    public VesselTarget() {
    }

    /**
     * Constructor
     * Used when creating new vessel targets
     */
    public VesselTarget(int mmsi) {
        this.mmsi = mmsi;
        changed = State.NEW;
    }

    /**
     * Merges the information of the AIS packet wiht this vessel target
     * @param packet the AIS packet
     * @param message the AIS message
     */
    public synchronized void merge(AisPacket packet, AisMessage message) {

        Objects.requireNonNull(packet);
        Objects.requireNonNull(message);

        boolean updated = false;

        // Sanity check
        if (message.getUserId() != mmsi) {
            throw new IllegalArgumentException("Cannot merge with MMSI " + message.getUserId());
        }

        // Update target type
        if (message.getTargetType() != targetType) {
            targetType = message.getTargetType();
            updated = true;
        }

        // Update country
        Country c = Country.getCountryForMmsi(message.getUserId());
        if (c != null && StringUtils.isNotBlank(c.getTwoLetter()) && !c.getTwoLetter().equals(country)) {
            country = c.getTwoLetter();
            updated = true;
        }

        // Position Data
        if (message instanceof IVesselPositionMessage) {
            updated |= updateVesselPositionMessage((IVesselPositionMessage) message, packet.getTimestamp());
        }

        // Static Data
        if (message instanceof AisStaticCommon) {
            updated |= updateVesselStaticMessage((AisStaticCommon) message, packet.getTimestamp());
        }

        // Only update the lastReport time stamp if any fields have been updated
        if (updated) {
            lastReport = packet.getTimestamp();
            AisPacketTags tags = packet.getTags();
            sourceType = (tags.getSourceType() == null) ? SourceType.TERRESTRIAL : tags.getSourceType();
            if (changed != State.NEW) {
                changed = State.UPDATED;
            }
        }

    }

    /**
     * Update the positional fields from the AIS message
     * @param posMessage the AIS message
     * @param date the date of the message
     * @return if any fields were updated
     */
    private boolean updateVesselPositionMessage(IVesselPositionMessage posMessage, Date date) {

        // Check that this is a newer position update
        if (lastPosReport != null && lastPosReport.getTime() >= date.getTime()) {
            return false;
        }

        boolean updated = false;

        // Update sog
        Float sog = posMessage.getSog() / 10.0f;
        if (posMessage.isSogValid() && !compare(sog, this.sog)) {
            this.sog = sog;
            updateMaxSpeedToday((short)Math.round(sog));
            updated = true;
        }

        // Update cog
        Float cog = posMessage.getCog() / 10.0f;
        if (posMessage.isCogValid() && !compare(cog, this.cog)) {
            this.cog = cog;
            updated = true;
        }

        // Update heading
        Short heading = (short)posMessage.getTrueHeading();
        if (posMessage.isHeadingValid() && !compare(heading, this.heading)) {
            this.heading = heading;
            updated = true;
        }

        if (posMessage.isPositionValid()) {
            Position pos = posMessage.getPos().getGeoLocation();

            // Update latitude
            if (!compare(pos.getLatitude(), this.lat)) {
                this.lat = (float)pos.getLatitude();
                updated = true;
            }

            // Update longitude
            if (!compare(pos.getLongitude(), this.lon)) {
                this.lon = (float)pos.getLongitude();
                updated = true;
            }
        }

        if (posMessage instanceof AisPositionMessage) {
            AisPositionMessage classAposMessage = (AisPositionMessage) posMessage;

            // Update rot
            Short rot = (short)classAposMessage.getRot();
            if (classAposMessage.isRotValid() && !compare(rot, this.rot)) {
                this.rot = rot;
                updated = true;
            }

            // Update nav status
            NavigationalStatus navStatus = NavigationalStatus.get(classAposMessage.getNavStatus());
            if (navStatus != this.navStatus) {
                this.navStatus = navStatus;
                updated = true;
            }
        }

        // Only update lasPosReport if any positional field has been updated
        if (updated) {
            lastPosReport = date;
        }

        // Check if we need to update past track
        if (updated && checkValidPos()) {
            updatePastTrack();
        }

        return updated;
    }

    /**
     * Update the static information fields from the AIS message
     * @param message the AIS message
     * @param date the date of the message
     * @return if any fields were updated
     */
    private boolean updateVesselStaticMessage(AisStaticCommon message, Date date) {

        // Check that this is a newer static update
        if (lastStaticReport != null && lastStaticReport.getTime() >= date.getTime()) {
            return false;
        }

        boolean updated = false;

        // Update the name
        String name = AisMessage.trimText(message.getName());
        if (StringUtils.isNotBlank(name) && !name.equals(this.name)) {
            this.name = name;
            updated = true;
        }

        // Update the call-sign
        String callsign = AisMessage.trimText(message.getCallsign());
        if (StringUtils.isNotBlank(callsign) && !callsign.equals(this.callsign)) {
            this.callsign = callsign;
            updated = true;
        }

        // Update the vessel type
        Integer vesselType = message.getShipType();
        if (!vesselType.equals(this.vesselType)) {
            this.vesselType = vesselType;
            updated = true;
        }

        if (message instanceof AisMessage5) {
            AisMessage5 msg5 = (AisMessage5) message;
            AisTargetDimensions dim = new AisTargetDimensions(msg5);

            // Update length
            Short length = (short)(dim.getDimBow() + dim.getDimStern());
            if (!length.equals(this.length)) {
                this.length = length;
                updated = true;
            }

            // Update width
            Short width = (short)(dim.getDimPort() + dim.getDimStarboard());
            if (!width.equals(this.width)) {
                this.width = width;
                updated = true;
            }

            // Update destination
            String destination = StringUtils.defaultIfBlank(AisMessage.trimText(msg5.getDest()), null);
            if (destination != null && !destination.equals(this.destination)) {
                this.destination = destination;
                updated = true;
            }

            // Update draught
            Float draught = msg5.getDraught() / 10.0f;
            if (msg5.getDraught() > 0 && !compare(draught, this.draught)) {
                this.draught = draught;
                updated = true;
            }

            // Update ETA
            Date eta = msg5.getEtaDate();
            if (eta != null && !eta.equals(this.eta)) {
                this.eta = eta;
                updated = true;
            }

            // Update IMO
            Long imo = msg5.getImo();
            if (msg5.getImo() > 0 && !imo.equals(this.imoNo)) {
                this.imoNo = imo;
                updated = true;
            }
        }

        // Only update lastStaticReport if any static field has been updated
        if (updated) {
            lastStaticReport = date;
        }

        return updated;
    }

    /**
     * Check if we need to insert a new past track entity.
     * This happens if the distance to the last past track position
     * exceeds a threshold.
     * <p>
     * Please also note that there will be at most 1 new past track per minute
     * which is the time between the persistence process.
     */
    private boolean updatePastTrack() {
        if (lastPastTrackPos != null && newPastTrackPos == null && computePastTrackDist(lastPastTrackPos) > PAST_TRACK_MIN_DIST) {
            // The distance to the last registered past track position exceeds the threshold
            newPastTrackPos = new PastTrackPos(this);
            return true;

        } else if (lastPastTrackPos == null && newPastTrackPos == null) {
            // We need to keep track of the first registered position
            newPastTrackPos = new PastTrackPos(this);
            return true;
        }
        return false;
    }

    /**
     * Computes the distance between the current vessel position and the given past track position
     * @param pos the past track position
     * @return the distance between the current vessel position and the given past track position
     */
    public double computePastTrackDist(PastTrackPos pos) {
        return Position.create(lat, lon).rhumbLineDistanceTo(Position.create(pos.getLat(), pos.getLon()));
    }

    /**
     * Utility method that compares two numbers
     * @param f1 the first number
     * @param f2 the first number
     * @return if the numbers are (almost) identical
     */
    private boolean compare(Number f1, Number f2) {
        if (f1 == null && f2 == null) {
            return true;
        } else if (f1 == null || f2 == null) {
            return false;
        }
        return Math.abs(f1.doubleValue() - f2.doubleValue()) < 0.001;
    }

    public synchronized void flagChanged(State changed) {
        this.changed = changed;
    }

    /**
     * Returns the changed state of the entity
     * @return the changed state of the entity
     */
    public State changed() {
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return mmsi;
    }

    /**
     * Checks if the target defines a valid position
     * @return if the target defines a valid position
     */
    public boolean checkValidPos() {
        return lat != null && lon != null && cog != null && sog != null && lastPosReport != null;
    }

    // ****** Max Speed functions ******* //

    /**
     * Reads the max-speed for the given day index
     * @param day the day index
     * @return the max-speed for the given day index
     */
    public short readMaxSpeed(long day) {
        if (maxSpeed == null) {
            return 0;
        }
        int index = (int)(day % MAX_SPEED_DAYS) * 2;
        return (short)((maxSpeed[index] << 8) | maxSpeed[index + 1]);
    }

    /**
     * Writes the max-speed for the given day index
     * @param day the day index
     * @param speed the speed to write
     */
    public void writeMaxSpeed(long day, short speed) {
        if (speed == 0) {
            return;
        }
        if (maxSpeed == null) {
            maxSpeed = new byte[MAX_SPEED_DAYS * 2];
        }
        int index = (int)(day % MAX_SPEED_DAYS) * 2;
        maxSpeed[index] = (byte)((speed >> 8) & 0xff);
        maxSpeed[index + 1] = (byte)(speed & 0xff);
    }

    /**
     * Updates the max-speed for today.
     * This should be used with care, as it involves two operations:
     * <ul>
     *     <li>Update the speed for today if it is larger that the existing value</li>
     *     <li>Reset the max-speed of tomorrow</li>
     * </ul>
     *
     * @param speed the speed to write
     */
    public void updateMaxSpeedToday(short speed) {
        long day = LocalDate.now().toEpochDay();
        short oldSpeed = readMaxSpeed(day);
        if (speed > oldSpeed) {
            writeMaxSpeed(day, speed);
        }
        writeMaxSpeed(day + 1, (short)0);
    }

    /**
     * Computes the max speed over the recorded period
     * @return the max speed over the recorded period
     */
    public short computeMaxSpeed() {
        if (maxSpeed == null) {
            return 0;
        }

        int speed = 0;
        for (int index = 0; index < MAX_SPEED_DAYS; index++) {
            speed = Math.max(speed, (maxSpeed[index * 2] << 8) | maxSpeed[index * 2 + 1]);
        }
        return (short)speed;
    }

    @Override
    public String toString() {
        return "VesselTarget{" +
                "changed=" + changed +
                ", mmsi=" + mmsi +
                ", lastReport=" + lastReport +
                ", lat=" + lat +
                ", lon=" + lon +
                ", lastPastTrackPos=" + lastPastTrackPos +
                ", newPastTrackPos=" + newPastTrackPos +
                '}';
    }

    // ****** Getters and setters ******* //

    public int getMmsi() {
        return mmsi;
    }

    public AisTargetType getTargetType() {
        return targetType;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getCountry() {
        return country;
    }

    public Date getLastReport() {
        return lastReport;
    }

    public Date getLastPosReport() {
        return lastPosReport;
    }

    public Float getLat() {
        return lat;
    }

    public Float getLon() {
        return lon;
    }

    public Float getCog() {
        return cog;
    }

    public Float getSog() {
        return sog;
    }

    public Short getHeading() {
        return heading;
    }

    public Short getRot() {
        return rot;
    }

    public NavigationalStatus getNavStatus() {
        return navStatus;
    }

    public Date getLastStaticReport() {
        return lastStaticReport;
    }

    public Short getLength() {
        return length;
    }

    public Short getWidth() {
        return width;
    }

    public String getName() {
        return name;
    }

    public String getCallsign() {
        return callsign;
    }

    public Long getImoNo() {
        return imoNo;
    }

    public String getDestination() {
        return destination;
    }

    public Float getDraught() {
        return draught;
    }

    public Date getEta() {
        return eta;
    }

    public Integer getVesselType() {
        return vesselType;
    }

    public byte[] getMaxSpeed() {
        return maxSpeed;
    }

    public PastTrackPos getLastPastTrackPos() {
        return lastPastTrackPos;
    }

    public void setLastPastTrackPos(PastTrackPos lastPastTrackPos) {
        this.lastPastTrackPos = lastPastTrackPos;
    }

    public PastTrackPos getNewPastTrackPos() {
        return newPastTrackPos;
    }

    public void setNewPastTrackPos(PastTrackPos newPastTrackPos) {
        this.newPastTrackPos = newPastTrackPos;
    }
}
