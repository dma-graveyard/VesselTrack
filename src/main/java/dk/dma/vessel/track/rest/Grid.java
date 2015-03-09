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

import java.util.HashSet;
import java.util.Set;

/**
 * Copied from ais-analyzed-common
 */
public class Grid {
    private final double GEO_CELL_SIZE_IN_DEGREES;
    private final double MULTIPLIER;

    protected Grid(double _cellSizeInDegrees) {
        GEO_CELL_SIZE_IN_DEGREES = _cellSizeInDegrees;
        MULTIPLIER = 360.0 / GEO_CELL_SIZE_IN_DEGREES;
    }

    public double getCellSizeInDegrees() {
        return GEO_CELL_SIZE_IN_DEGREES;
    }

    public long getCellId(double _lat, double _lon) {
        // We use floor(), not truncation (cast) to handle negative latitudes correctly.
        // Negative longitudes are handled by adding 360 (for backwards compatibility).
        //
        // floor(_lat / GEO_CELL_SIZE) Range -1800..1800, span 3600
        // static_cast<long>((360.0 + _lon)/GEO_CELL_SIZE) Range 3600..10800, span 7200
        // static_cast<long>(360.0 / GEO_CELL_SIZE) Constant = 7200
        // Result of last two lines Range -3600..3600, span 7200

        return (long) (Math.floor(_lat / GEO_CELL_SIZE_IN_DEGREES) * MULTIPLIER)
                + (long) ((360.0 + _lon) / GEO_CELL_SIZE_IN_DEGREES) - (long) (360.0 / GEO_CELL_SIZE_IN_DEGREES);
    }

    public Position getGeoPosOfCellId(long _id) {
        // Make lonPart range be 0..7200
        _id += (long) ((360 / GEO_CELL_SIZE_IN_DEGREES) / 2);
        // Cut off lonPart
        long latPart = (long) (Math.floor(_id / MULTIPLIER));
        // Move lonPart range back again
        _id -= (long) ((360 / GEO_CELL_SIZE_IN_DEGREES) / 2);
        long lonPart = (long) (_id - latPart * MULTIPLIER);

        double _lat = GEO_CELL_SIZE_IN_DEGREES * latPart;
        double _lon = GEO_CELL_SIZE_IN_DEGREES * lonPart;

        return Position.create(_lat, _lon);
    }

    public Long getCellIdNorthOf(Long id) {
        return (long) (id + MULTIPLIER);
    }

    public Long getCellIdSouthOf(Long id) {
        return (long) (id - MULTIPLIER);
    }

    public Long getCellIdWestOf(Long id) {
        if (id % MULTIPLIER == 1) {
            return (long) (id + MULTIPLIER - 1);
        }

        return id - 1;
    }

    public Long getCellIdEastOf(Long id) {
        if (id % MULTIPLIER == 0) {
            return (long) (id - MULTIPLIER + 1);
        }

        return id + 1;
    }

    public Set<Long> getNearbyCellIds(Position position, double radius) {
        Set<Long> cellIds = new HashSet<Long>();

        double latN = position.getLatitude() + radius;
        double latS = position.getLatitude() - radius;
        double lonW = position.getLongitude() - radius;
        double lonE = position.getLongitude() + radius;

        Long cellIdNW = getCellId(latN, lonW);
        Long cellIdNE = getCellId(latN, lonE);
        Long cellIdSE = getCellId(latS, lonE);

        Long firstCellId = cellIdNW;
        Long iteratorCellId = cellIdNW;
        Long lastCellId = cellIdNE;

        while (iteratorCellId <= lastCellId) {
            iteratorCellId = firstCellId;

            cellIds.add(firstCellId);

            while (iteratorCellId <= lastCellId) {
                iteratorCellId = getCellIdEastOf(iteratorCellId);

                cellIds.add(iteratorCellId);
            }

            firstCellId = getCellIdSouthOf(firstCellId);
            lastCellId = getCellIdSouthOf(lastCellId);
        }

        if (cellIdNW == cellIdSE) {
            cellIds.add(cellIdNW);
        }

        return cellIds;
    }
}
