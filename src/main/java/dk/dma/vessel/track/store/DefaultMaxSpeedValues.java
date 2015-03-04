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
package dk.dma.vessel.track.store;

import java.util.HashMap;
import java.util.Map;

/**
 * Returns the default max-speed values for a vessel of the given AIS type.
 */
public class DefaultMaxSpeedValues {

    private static final Map<Integer, Float> MAX_SPEEDS_BY_TYPE = new HashMap<>();
    private static final Map<String, Float> MAX_SPEEDS_BY_NAME = new HashMap<>();

    static {
        // Based on statistics received from ESR@dma.dk
        setMaxSpeedForTypes("WIG", 20, 29, 100f);
        setMaxSpeedForType("Fishing", 30, 11.5f);
        setMaxSpeedForType("Towing", 31, 12.1f);
        setMaxSpeedForType("Towing", 32, 12.1f);
        setMaxSpeedForType("Dredging", 33, 11.3f);
        setMaxSpeedForType("Diving", 34, 18f);
        setMaxSpeedForType("Military", 35, 30f);
        setMaxSpeedForType("Sailing", 36, 6f);
        setMaxSpeedForType("Pleasure", 37, 15f);
        setMaxSpeedForTypes("HSC", 40, 49, 40f);
        setMaxSpeedForType("Pilot", 50, 18.7f);
        setMaxSpeedForType("Sar", 51, 30f);
        setMaxSpeedForType("Tug", 52, 12.1f);
        setMaxSpeedForType("Port tender", 53, 11.9f);
        setMaxSpeedForType("Anti pollution", 54, 18f);
        setMaxSpeedForType("Law enforcement", 55, 20f);
        setMaxSpeedForType("Medical", 58, 15.7f);
        setMaxSpeedForTypes("Passenger", 60, 69, 19.5f);
        setMaxSpeedForTypes("Cargo", 70, 79, 15.1f);
        setMaxSpeedForTypes("Tanker", 80, 89, 13.6f);
    }

    /**
     * Defines the max speed for a vessel of the given AIS type
     *
     * @param name the AIS type name
     * @param type the AIS type
     * @param maxSpeed the max speed
     */
    private static void setMaxSpeedForType(String name, int type, float maxSpeed) {
        MAX_SPEEDS_BY_NAME.put(name, maxSpeed);
        MAX_SPEEDS_BY_TYPE.put(type, maxSpeed);
    }

    /**
     * Defines the max speed for a vessel of the given consecutive AIS type interval.
     *
     * @param name the AIS type name
     * @param typeFrom the AIS type minimum value
     * @param typeTo the AIS type maximum value
     * @param maxSpeed the max speed
     */
    private static void setMaxSpeedForTypes(String name, int typeFrom, int typeTo, float maxSpeed) {
        MAX_SPEEDS_BY_NAME.put(name, maxSpeed);
        for (int type = typeFrom; type <= typeTo; type++) {
            MAX_SPEEDS_BY_TYPE.put(type, maxSpeed);
        }
    }

    /**
     * Returns the max speed for a vessel of the given AIS type
     *
     * @param type the AIS type
     */
    public static float getMaxSpeedForType(int type) {
        Float maxSpeed = MAX_SPEEDS_BY_TYPE.get(type);
        return (maxSpeed == null) ? 0f : maxSpeed;
    }

    /**
     * Returns the max speed for a vessel of the given AIS type name
     *
     * @param typeName the AIS type name
     */
    public static float getMaxSpeedForType(String typeName) {
        Float maxSpeed = MAX_SPEEDS_BY_NAME.get(typeName);
        return (maxSpeed == null) ? 0f : maxSpeed;
    }
}

