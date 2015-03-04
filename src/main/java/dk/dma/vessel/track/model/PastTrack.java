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

import dk.dma.enav.model.geometry.Position;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Represents a past-track for a single MMSI
 */
@SuppressWarnings("unused")
public class PastTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    private TreeSet<PastTrackPos> track = new TreeSet<>();

    public synchronized void add(PastTrackPos pos) {
        track.add(pos);
    }

    public synchronized List<PastTrackPos> asList() {
        return new ArrayList<>(track);
    }

    public synchronized int trim(long ttl) {
        long maxAge = System.currentTimeMillis() - ttl;
        List<PastTrackPos> removeSet = new ArrayList<>();
        for (PastTrackPos pos : track) {
            if (pos.getTime().getTime() < maxAge) {
                removeSet.add(pos);
            } else {
                break;
            }
        }
        for (PastTrackPos old : removeSet) {
            track.remove(old);
        }
        return removeSet.size();
    }

    public synchronized PastTrackPos newestPos() {
        if (track.size() > 0) {
            return track.last();
        }
        return null;
    }

    public synchronized int size() {
        return track.size();
    }

    public static List<PastTrackPos> downSample(List<PastTrackPos> list, int minPastTrackDist, long age) {
        long maxAge = System.currentTimeMillis() - age;
        ArrayList<PastTrackPos> downSampled = new ArrayList<>();
        if (list.size() == 0) {
            return downSampled;
        }
        downSampled.add(list.get(0));
        int i = 0;
        int n;
        while (i < list.size()) {
            PastTrackPos pos = list.get(i);
            n = i + 1;
            while (n < list.size()) {
                PastTrackPos next = list.get(n);
                if (distance(pos, next) > minPastTrackDist) {
                    downSampled.add(next);
                    break;
                }
                n++;
            }
            i = n;
        }
        int start = 0;
        while (start < downSampled.size() && downSampled.get(start).getTime().getTime() < maxAge) {
            start++;
        }
        return downSampled.subList(start, downSampled.size());
    }

    public static double distance(PastTrackPos pos1, PastTrackPos pos2) {
        return Position.create(pos1.getLat(), pos1.getLon()).rhumbLineDistanceTo(Position.create(pos2.getLat(), pos2.getLon()));
    }

}

