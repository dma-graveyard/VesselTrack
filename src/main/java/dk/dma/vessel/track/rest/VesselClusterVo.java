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

import dk.dma.enav.model.geometry.Position;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A cluster of vessels described as an area with a number of known vessel positions. A cluster knows its density in
 * vessels per kilometer.
 *
 * Copied from ais-analyzed-common
 */
@SuppressWarnings("unused")
public class VesselClusterVo implements  JsonSerializable {

    Position from;
    Position to;
    private int count;
    double density;
    List<VesselTargetListVo> vessels = new ArrayList<>();

    /**
     * Constructor of Vessel Cluster.
     *
     * @param from Top left corner of area.
     * @param to Bottom right corner of area.
     */
    public VesselClusterVo(Position from, Position to) {
        this.from = requireNonNull(from);
        this.to = requireNonNull(to);
    }

    /**
     * Computes the number of vessels and their density within the cluster
     */
    public void computeVesselCountAndDensity() {
        count = vessels.size();
        Position from = Position.create(getFrom().getLatitude(), getFrom().getLongitude());
        Position to = Position.create(getTo().getLatitude(), getTo().getLongitude());
        Position topRight = Position.create(from.getLatitude(), to.getLongitude());
        Position botLeft = Position.create(to.getLatitude(), from.getLongitude());
        double width = from.geodesicDistanceTo(topRight) / 1000;
        double height = from.geodesicDistanceTo(botLeft) / 1000;
        double areaSize = width * height;
        double density = (double) vessels.size() / areaSize;
        setDensity(density);
    }

    public Position getFrom() {
        return from;
    }

    public void setFrom(Position from) {
        this.from = from;
    }

    public Position getTo() {
        return to;
    }

    public void setTo(Position to) {
        this.to = to;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public List<VesselTargetListVo> getVessels() {
        return vessels;
    }

    public void setVessels(List<VesselTargetListVo> vessels) {
        this.vessels = vessels;
    }
}
