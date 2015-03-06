package dk.dma.vessel.track.rest;

import dk.dma.ais.message.NavigationalStatus;
import dk.dma.ais.message.ShipTypeCargo;
import dk.dma.enav.model.Country;
import dk.dma.vessel.track.model.VesselTarget;
import dk.dma.vessel.track.store.TargetStore;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Takes a filter, in the format defined by DocumentCloud VisualSearch
 * and use to for filtering.
 * <p>
 * The class also provides functionality for generating VisualSearch auto-complete
 * search filter options.
 */
public class VesselTargetFilter implements Predicate<VesselTarget> {

    final static Pattern FILTER_PART_PATTERN = Pattern.compile("([^:]+): \"([^\"]*)\"[ ]*");

    Set<String> name = new HashSet<>();
    Set<Integer> mmsi = new HashSet<>();
    Set<String> callsign = new HashSet<>();
    Set<Long> imo = new HashSet<>();
    Set<String> country = new HashSet<>();
    Set<ShipTypeCargo.ShipType> type = new HashSet<>();
    Set<Integer> status = new HashSet<>();
    boolean filterDefined = false;

    /**
     * Constructor
     * @param filter the filter
     */
    public VesselTargetFilter(String filter) {
        if (StringUtils.isNotBlank(filter)) {
            parseFilter(filter);
        }
    }

    private void parseFilter(String filter) {
        Matcher m = FILTER_PART_PATTERN.matcher(filter);
        while (m.find()) {
            String key = m.group(1);
            String value = m.group(2);
            if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                switch (key) {
                    case "mmsi":
                        mmsi.add(Integer.valueOf(value));
                        break;
                    case "name":
                        name.add(value.toLowerCase());
                        break;
                    case "callsign":
                        callsign.add(value.toLowerCase());
                        break;
                    case "imo":
                        imo.add(Long.valueOf(value));
                        break;
                    case "country":
                        country.add(value);
                        break;
                    case "type":
                        type.add(ShipTypeCargo.ShipType.valueOf(value));
                        break;
                    case "status":
                        status.add(Integer.valueOf(value));
                        break;
                }
            }
        }
        filterDefined = mmsi.size() + name.size() + callsign.size() +
                imo.size() + country.size() + type.size() + status.size() > 0;
    }

    /**
     * Tests the target against the filter
     * @param t the target
     * @return if the target is included in the filter
     */
    @Override
    public boolean test(VesselTarget t) {
        // If no filter has been defined, return true
        if (!filterDefined) {
            return true;
        }

        boolean included = true;

        if (!mmsi.isEmpty()) {
            included = mmsi.contains(t.getMmsi());
        }
        if (included && !name.isEmpty()) {
            included = t.getName() != null && name.contains(t.getName().toLowerCase());
        }
        if (included && !callsign.isEmpty()) {
            included = t.getCallsign() != null && callsign.contains(t.getCallsign().toLowerCase());
        }
        if (included && !imo.isEmpty()) {
            included = t.getImoNo() != null && imo.contains(t.getImoNo());
        }
        if (included && !country.isEmpty()) {
            included = t.getCountry() != null && country.contains(t.getCountry());
        }
        if (included && !type.isEmpty()) {
            included = t.getVesselType() != null && type.contains(new ShipTypeCargo(t.getVesselType()).getShipType());
        }
        if (included && !status.isEmpty()) {
            included = t.getNavStatus() != null && status.contains(t.getNavStatus().getCode());
        }
        return included;
    }

    /**
     * Rest call used for returning an auto-complete search filter option list
     * @param targetStore the target store
     * @param key the type of vessel target attribute to filter on
     * @param term the search term
     * @param maxHits the maximum number of values to return
     * @return the search filter option list
     */
    public static  List<SearchFilterOptionVo> getSearchFilterOptions(TargetStore targetStore, String key, String term, int maxHits) {

        switch (key) {
            case "mmsi":
                return targetStore.list().stream()
                        .filter(t -> String.valueOf(t.getMmsi()).startsWith(term))
                        .limit(maxHits)
                        .map(t -> new SearchFilterOptionVo(String.valueOf(t.getMmsi())))
                        .collect(Collectors.toList());
            case "callsign":
                return targetStore.list().stream()
                        .filter(t -> StringUtils.isNotBlank(t.getCallsign()) && t.getCallsign().toLowerCase().startsWith(term.toLowerCase()))
                        .limit(maxHits)
                        .map(t -> new SearchFilterOptionVo(t.getCallsign()))
                        .collect(Collectors.toList());
            case "imo":
                return targetStore.list().stream()
                        .filter(t -> t.getImoNo() != null && String.valueOf(t.getImoNo()).startsWith(term))
                        .limit(maxHits)
                        .map(t -> new SearchFilterOptionVo(String.valueOf(t.getImoNo())))
                        .collect(Collectors.toList());
            case "name":
                return targetStore.list().stream()
                        .filter(t -> StringUtils.isNotBlank(t.getName()) && t.getName().toLowerCase().startsWith(term.toLowerCase()))
                        .limit(maxHits)
                        .map(t -> new SearchFilterOptionVo(t.getName()))
                        .collect(Collectors.toList());
            case "country":
                // NB: Inefficient way of getting hold of relevant countries
                //     Change Country to get access to list of countries
                return targetStore.list().stream()
                        .filter(t -> StringUtils.isNotBlank(t.getCountry()))
                        .map(VesselTarget::getCountry)
                        .distinct()
                        .map(Country::getByCode)
                        .filter(c -> c.getName().toLowerCase().startsWith(term.toLowerCase()))
                        .limit(maxHits)
                        .map(c -> new SearchFilterOptionVo(c.getName(), c.getTwoLetter()))
                        .collect(Collectors.toList());
            case "type":
                return Arrays.asList(ShipTypeCargo.ShipType.values()).stream()
                        .filter(t -> getShipTypeName(t).toLowerCase().startsWith(term.toLowerCase()))
                        .limit(maxHits)
                        .map(t -> new SearchFilterOptionVo(getShipTypeName(t), t.toString()))
                        .collect(Collectors.toList());
            case "status":
                return Arrays.asList(NavigationalStatus.values()).stream()
                        .filter(s -> s.prettyStatus().toLowerCase().startsWith(term.toLowerCase()))
                        .limit(maxHits)
                        .map(s -> new SearchFilterOptionVo(s.prettyStatus(), String.valueOf(s.getCode())))
                        .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Formats a ship type in text
     * @param type the ship type
     * @return the type name
     */
    private static String getShipTypeName(ShipTypeCargo.ShipType type) {
        if (type.equals(ShipTypeCargo.ShipType.TOWING_LONG_WIDE)) {
            return  "Towing Long/Wide";
        } else if (type.equals(ShipTypeCargo.ShipType.WIG)) {
            return "WIG";
        } else if (type.equals(ShipTypeCargo.ShipType.HSC)) {
            return  "HSC";
        }
        return StringUtils.capitalize(type.toString().toLowerCase().replace("_", " "));
    }

    /**
     * Encapsulates the options used for auto-completion in search filtering
     */
    public static class SearchFilterOptionVo {
        String label;
        String value;

        /**
         * Constructor
         * @param label the label
         * @param value the value
         */
        public SearchFilterOptionVo(String label, String value) {
            this.label = label;
            this.value = value;
        }

        /**
         * Constructor
         * @param value the value
         */
        public SearchFilterOptionVo(String value) {
            this(value, value);
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

}
