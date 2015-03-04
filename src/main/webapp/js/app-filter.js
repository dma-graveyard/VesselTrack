/**
 * Common angular filters
 */
angular.module('vesseltrack.app')

    /********************************
     * Formats a lon-lat position
     ********************************/
    .filter('lonlat', function() {

        /**
         * Formats the longitude as a string
         * @param longitude the longitude
         * @returns the string representation
         */
        function formatLongitude(longitude) {
            var ns = "E";
            if (longitude < 0) {
                ns = "W";
                longitude *= -1;
            }
            var hours = Math.floor(longitude);
            longitude -= hours;
            longitude *= 60;
            var lonStr = longitude.toFixed(3);
            while (lonStr.indexOf('.') < 2) {
                lonStr = "0" + lonStr;
            }

            return (hours / 1000.0).toFixed(3).substring(2) + " " + lonStr + ns;
        }

        /**
         * Formats the latitude as a string
         * @param latitude the longitude
         * @returns the string representation
         */
        function formatLatitude(latitude) {
            var ns = "N";
            if (latitude < 0) {
                ns = "S";
                latitude *= -1;
            }
            var hours = Math.floor(latitude);
            latitude -= hours;
            latitude *= 60;
            var latStr = latitude.toFixed(3);
            while (latStr.indexOf('.') < 2) {
                latStr = "0" + latStr;
            }

            return (hours / 100.0).toFixed(2).substring(2) + " " + latStr + ns;
        }

        /**
         * Formats the position as a string
         * @param lonlat the position
         * @returns the string representation
         */
        function formatLonLat(lonlat) {
            return formatLatitude(lonlat.lat) + "  " + formatLongitude(lonlat.lon);
        }

        return function(input) {
            input = input || '';
            return formatLonLat(input);
        };
    });


