/**
 * Services that provides access to the VesselTrack backend
 */
angular.module('vesseltrack.app')


    .factory('VesselTrackService', [ '$http', '$window',
        function($http, $window) {
        'use strict';

        var storage = $window.sessionStorage;

        return {

            /**
             * Returns the persisted map settings
             */
            mapSettings: function() {
                try { return JSON.parse(storage.mapSettings); } catch (e) {}
                return {
                        zoom : 7,
                        lon : 12.568,
                        lat : 55.676
                    };
            },

            /**
             * Stores the given map settings
             */
            storeMapSettings: function(settings) {
                try { storage.mapSettings = JSON.stringify(settings); } catch (e) {}
            },

            /**
             * fetches the given vessel
             */
            fetchVessel: function(mmsi, success, error) {
                $http.get('/vessels/' + mmsi)
                    .success(success)
                    .error(error);
            },

            /**
             * fetches all vessels within the given bounds
             */
            fetchVessels: function(bounds, mmsi, success, error) {
                $http.get('/vessels/list'
                    + '?top=' + bounds.top
                    + '&left=' + bounds.left
                    + '&bottom=' + bounds.bottom
                    + '&right=' + bounds.right
                    + (mmsi ? '&mmsi=' + mmsi : ''))
                    .success(success)
                    .error(error);
            },

            /**
             * Fetches past track positions for the given vessel
             */
            fetchTrack: function(mmsi, duration, success, error) {
                $http.get('/vessels/track/' + mmsi + (duration ? '?age=' + duration : ''))
                    .success(success)
                    .error(error);
            },

            /** Create feature attributes for the vessel graphics **/
            vesselGraphics: function(type, status, vesselScale) {
                var col;
                switch (type) {
                    case 18: // Passenger
                        col = "blue";
                        break;
                    case 19: // Cargo
                        col = "green";
                        break;
                    case 20: // Tanker
                        col = "red";
                        break;
                    case 17: // HSC
                    case 1:  // WIG
                        col = "yellow";
                        break;
                    case 0: // Undefined
                    case 22: // Unknown
                        col = "gray";
                        break;
                    case 9: // Fishing
                        col = "orange";
                        break;
                    case 15: // Sailing
                    case 16: // Pleasure
                        col = "purple";
                        break;
                    default:
                        col = "gray";
                }
                if (status == 1 || status == 5) {
                    return {
                        image: "img/vessel/vessel_" + col + "_moored.png",
                        width: 12 * vesselScale,
                        height: 12 * vesselScale,
                        offsetX: -6 * vesselScale,
                        offsetY: -6 * vesselScale
                    }
                } else {
                    return {
                        image: "img/vessel/vessel_" + col + ".png",
                        width: 20 * vesselScale,
                        height: 10 * vesselScale,
                        offsetX: -10 * vesselScale,
                        offsetY: -5 * vesselScale
                    }
                }
            },

            /** Create feature attributes for the vessel selection graphics **/
            vesselSelectGraphics: function (vesselScale) {
                return {
                    image: "img/vessel/selection.png",
                    width : function() { return 32 * vesselScale; },
                    height : function() { return 32 * vesselScale; },
                    offsetY : function() { return -16 * vesselScale; },
                    offsetX : function() { return -16 * vesselScale; }
                }
            },

            /** Returns if the vessel status indicates a moored vessel **/
            moored: function(status) {
                return status == 1 || status == 5;
            },


            vesselType: function (type) {
                switch (type) {
                    case  0: return "Undefined";
                    case  1: return "WIG";
                    case  2: return "Pilot";
                    case  3: return "SAR";
                    case  4: return "TUG";
                    case  5: return "Port tender";
                    case  6: return "Anti-pollution";
                    case  7: return "Law enforcement";
                    case  8: return "Medical";
                    case  9: return "Fishing";
                    case 10: return "Towing";
                    case 11: return "Towing long/Wide";
                    case 12: return "Dredging";
                    case 13: return "Diving";
                    case 14: return "Military";
                    case 15: return "Sailing";
                    case 16: return "Pleasure";
                    case 17: return "HSC";
                    case 18: return "Passenger";
                    case 19: return "Cargo";
                    case 20: return "Tanker";
                    case 21: return "Ship according to RR";
                    case 22: return "Unknown";
                    default : return "Unknown";
                }
            },

            navStatus: function(status) {
                switch (status) {
                    case 0: return "Under way using engine";
                    case 1: return "At anchor";
                    case 2: return "Not under command";
                    case 3: return "Restricted manoeuvrability";
                    case 4: return "Constrained by her draught";
                    case 5: return "Moored";
                    case 6: return "Aground";
                    case 7: return "Engaged in fishing";
                    case 8: return "Under way sailing";
                    case 14: return "AIS-SART";
                    default: return "Undefined"
                }
            }

        };
    }]);

