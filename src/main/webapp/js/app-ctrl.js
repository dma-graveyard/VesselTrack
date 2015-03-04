/**
 * The main VesselTrack controller
 */
angular.module('vesseltrack.app')

    /**
     * The main VesselTrack controller
     */
    .controller('VesselTrackCtrl', ['$scope', '$timeout', '$interval', 'VesselTrackService',
        function ($scope, $timeout, $interval, VesselTrackService) {
            'use strict';

            var proj4326 = new OpenLayers.Projection("EPSG:4326");
            var projmerc = new OpenLayers.Projection("EPSG:900913");

            $scope.activeInfoPanel = undefined;
            $scope.mapSettings = VesselTrackService.mapSettings();
            $scope.bounds = undefined;
            $scope.map = undefined;

            /**
             * Schedules the loading of vessel for the given bounds
             * @param bounds the bounds to load the vessels for
             */
            $scope.reloadVessels = function (bounds) {
                if ($scope.bounds && $scope.bounds.equals(bounds)) {
                    return;
                }
                $scope.bounds = bounds;
                $scope.storeMapSettings();

                // If a back-end loading has already been scheduled, cancel it
                if ($scope.vesselTimer) {
                    $timeout.cancel($scope.vesselTimer);
                    delete $scope.vesselTimer;
                }

                // Schedule the loading of vessels in 500 ms time
                $scope.vesselTimer = $timeout(function () {
                    $scope.fetchVessels(bounds, false);
                    if ($scope.vesselTimer) {
                        delete $scope.vesselTimer;
                    }
                }, 500);
            };

            /**
             * Stores the current map settings
             */
            $scope.storeMapSettings = function() {
                if ($scope.map) {
                    var center = $scope.map.getCenter();
                    var lonlat = new OpenLayers.LonLat(center.lon, center.lat).transform(projmerc, proj4326);
                    $scope.mapSettings.zoom = $scope.map.zoom;
                    $scope.mapSettings.lat = lonlat.lat;
                    $scope.mapSettings.lon = lonlat.lon;
                    VesselTrackService.storeMapSettings($scope.mapSettings);
                }
            };

            $interval(function () {
                // Check for vessels to update every minute unless a full reload is scheduled
                if (!$scope.vesselTimer) {
                    $scope.fetchVessels($scope.bounds, true);
                }
            }, 60000 + (Math.random() * 10.0 - 5.0));

            /*********************************/
            /* Layers                        */
            /*********************************/

            var osmLayer = new OpenLayers.Layer.OSM("OSM", [
                '//a.tile.openstreetmap.org/${z}/${x}/${y}.png',
                '//b.tile.openstreetmap.org/${z}/${x}/${y}.png',
                '//c.tile.openstreetmap.org/${z}/${x}/${y}.png' ], null);

            var vesselContext = {
                graphicWidth: function(feature) { return 20; },
                graphicHeight: function(feature) { return 10; },
                graphicXOffset: function(feature) { return -vesselContext.graphicWidth() / 2; },
                graphicYOffset: function(feature) { return -vesselContext.graphicHeight() / 2; },
                image: function(feature) {
                    return "img/vessel/vessel_blue.png";//feature.data.image;
                }
            };

            var vesselLayer = new OpenLayers.Layer.Vector("Vessels", {
                styleMap : new OpenLayers.StyleMap({
                    "default" : new OpenLayers.Style({
                        externalGraphic : "${image}",
                        graphicWidth : "${width}",
                        graphicHeight : "${height}",
                        graphicYOffset : "${offsetX}",
                        graphicXOffset : "${offsetY}",
                        rotation : "${angle}",
                        graphicOpacity : "${transparency}"
                    })
                }),
                strategies: [
                    new OpenLayers.Strategy.BBOX({
                        resFactor: 1,
                        update: function(options) {
                            if (options && this.getMapBounds()) {
                                var b = this.getMapBounds().transform(projmerc, proj4326);
                                $scope.reloadVessels(b);
                            }
                        }
                    })
                ]

            });

            var selectionLayer = new OpenLayers.Layer.Vector("Selection", {
                styleMap : new OpenLayers.StyleMap({
                    "default" : new OpenLayers.Style({
                        externalGraphic : "/img/vessel/selection.png",
                        graphicWidth : "${width}",
                        graphicHeight : "${height}",
                        graphicYOffset : "${offsetX}",
                        graphicXOffset : "${offsetY}",
                        rotation : "${angle}"
                    }),
                    "select" : new OpenLayers.Style({
                        cursor : "crosshair",
                        externalGraphic : "/img/vessel/selection.png"
                    })
                })
            });

            /*********************************/
            /* Map                           */
            /*********************************/

            $scope.map = new OpenLayers.Map({
                div: 'map',
                theme: null,
                layers: [ osmLayer, vesselLayer, selectionLayer ],
                units: "degrees",
                projection: projmerc,
                center: new OpenLayers.LonLat($scope.mapSettings.lon, $scope.mapSettings.lat).transform(proj4326, projmerc),
                zoom: $scope.mapSettings.zoom
            });


            // Add zoom buttons
            $scope.map.addControl(new OpenLayers.Control.Zoom());

            /*********************************/
            /* Interactive Map Functionality */
            /*********************************/

            /**
             * Formats the tooltip content.
             * @param feature the vessel feature
             * @returns the HTML tooltip contents
             */
            function formatTooltip(feature) {
                var tooltip =
                    '<div class="vessel-tooltip">' +
                    '<div><strong>' + feature.data.vessel[6] + '</strong></div>' +
                    '<div><small>MMSI: ' + feature.data.mmsi + '</small></div>' +
                    '</div>';
                return tooltip;
            }

            var hoverControl = new OpenLayers.Control.SelectFeature(
                vesselLayer, {
                    hover: true,
                    onBeforeSelect: function(feature) {
                        if (feature.popup) {
                            return;
                        }

                        var b = feature.geometry.getBounds();
                        var html = formatTooltip(feature);

                        // add code to create tooltip/popup
                        feature.popup = new OpenLayers.Popup.Anchored(
                            "tooltip",
                            new OpenLayers.LonLat(b.left, b.bottom),
                            new OpenLayers.Size(150, 30 + 18 *  html.occurrences("<br />")),
                            html,
                            {'size': new OpenLayers.Size(0,0), 'offset': new OpenLayers.Pixel(100, 12)},
                            false,
                            null);

                        feature.popup.backgroundColor = '#eeeeee';
                        feature.popup.calculateRelativePosition = function () {
                            return 'bl';
                        };


                        $scope.map.addPopup(feature.popup);
                        return true;
                    },
                    onUnselect: function(feature) {
                        // remove tooltip
                        if (feature.popup) {
                            $scope.map.removePopup(feature.popup);
                            feature.popup.destroy();
                            feature.popup=null;
                        }
                    }
                });

            $scope.map.addControl(hoverControl);
            hoverControl.activate();

            var vesselSelect = new OpenLayers.Handler.Click(
                hoverControl, {
                    click: function (evt) {
                        var feature = this.layer.getFeatureFromEvent(evt);
                        if (feature && feature.attributes && feature.attributes.mmsi) {
                            $scope.onVesselSelect(feature);
                        }
                    }
                }, {
                    single: true,
                    double : false
                });
            vesselSelect.activate();

            /*********************************/
            /* Utility Functions             */
            /*********************************/

            /**
             * Toggle show the given info panel
             * @param panel the panel to show or hide
             */
            $scope.toggleInfoPanel = function (panel) {
                if ($scope.activeInfoPanel == panel) {
                    $scope.activeInfoPanel = undefined;
                } else {
                    $scope.activeInfoPanel = panel;
                }
            };

            $scope.vesselType = function (type) {
                return VesselTrackService.vesselType(type);
            };

            $scope.navStatus = function (status) {
                return VesselTrackService.navStatus(status);
            };

            $scope.formatDate = function (timestamp) {
                return moment(timestamp).format('MMMM Do, HH:mm:ss');
            };

            $scope.marineTraffic = function (mmsi) {
                window.open('http://www.marinetraffic.com/ais/shipdetails.aspx?mmsi=' + mmsi, '_blank');
            };

            $scope.setCenter = function(longitude, latitude) {
                var pos = new OpenLayers.LonLat(longitude, latitude).transform(proj4326, projmerc);

                // set position to find center in pixels
                $scope.map.setCenter(pos, 10);
            };

            // Periodically check if a followed vessel has strayed way from the center
            $interval(function () {
                if ($scope.selVessel && $scope.selVessel.follow) {
                    var center = $scope.map.getPixelFromLonLat($scope.map.center);
                    var pos = $scope.map.getPixelFromLonLat(new OpenLayers.LonLat($scope.selVessel.lon, $scope.selVessel.lat).transform(proj4326, projmerc));
                    var pixelDist = lineDistance(center, pos);
                    if (pixelDist > 10) {
                        $scope.setCenter($scope.selVessel.lon, $scope.selVessel.lat);
                        console.log("Re-center map");
                    }
                }
            }, 60000 + (Math.random() * 10.0 - 5.0));

            // If "follow" is turned on, center the map on the selected vessel
            $scope.$watch(
                function() { return $scope.selVessel != null && $scope.selVessel.follow },
                function(data) {
                    if ($scope.selVessel && $scope.selVessel.follow) {
                        $scope.setCenter($scope.selVessel.lon, $scope.selVessel.lat);
                    }
                 },
                true);


            /*********************************/
            /* Vessel Loading                */
            /*********************************/

            /**
             * Creates an OpenLayer point properly transformed
             * @param lon longitude
             * @param lat latitude
             * @returns the point
             */
            function createPoint(lon, lat) {
                return new OpenLayers.Geometry.Point(lon, lat).transform(proj4326, projmerc);
            }

            /**
             * Fetches the vessels for the given bounds and generates new features
             * or update existing features
             * @param bounds the bounds
             * @param update whether to update existing feature list or generate a new list
             */
            $scope.fetchVessels = function(bounds, update) {
                VesselTrackService.fetchVessels(
                    bounds,
                    function (vessels) {
                        if (update) {
                            $scope.updateVesselFeatures(vessels);
                        } else {
                            $scope.generateVesselFeatures(vessels);
                        }
                    },
                    function () {
                        console.error("Error fetching vessels");
                    }
                )
            };

            /**
             * Creates a vessel feature from a vessel array
             * @param mmsi the mmsi of the vessel
             * @param vessel the vessel data
             * @returns the vessel feature
             */
            $scope.generateVesselFeature = function(mmsi, vessel) {
                var attr = VesselTrackService.vesselGraphics(vessel[3], vessel[4]);
                attr.mmsi = mmsi;
                attr.angle = (vessel[2]) ? vessel[2] - 90 : 0;
                attr.vessel = vessel;
                var geom = createPoint(vessel[1], vessel[0]);
                return new OpenLayers.Feature.Vector(geom, attr);
            };

            /**
             * Generate vessel features for the given list of vessels
             * @param vessels the vessels to generate features for
             */
            $scope.generateVesselFeatures = function (vessels) {
                var features = [];

                $.each(vessels, function (mmsi, vessel) {
                    // Check that the vessel has a valid position
                    if (features.length < 10000 && vessel[0] && vessel[1]) {
                        features.push($scope.generateVesselFeature(mmsi, vessel));
                        $scope.checkUpdateSelectedVessel(mmsi, vessel);
                    }
                });
                vesselLayer.removeAllFeatures();
                vesselLayer.addFeatures(features);
                vesselLayer.redraw();
            };

            /**
             * Update vessel features for the given list of vessels
             * @param vessels the vessels to generate features for
             */
            $scope.updateVesselFeatures = function (vessels) {
                var deleteFeatures = [];

                // Look at existing features and find deleted and changed vessel feature
                $.each(vesselLayer.features, function (index, feature) {
                    if (feature.attributes && feature.attributes.mmsi) {
                       var vessel = vessels[feature.attributes.mmsi];
                       if (!vessel) {
                           deleteFeatures.push(feature);
                       } else {
                           vessel.existing = true;
                           // Check if the vessel has a newer last-report date
                           if (feature.attributes.vessel[5] < vessel[5]) {
                               deleteFeatures.push(feature);
                               vessel.existing = false; // We need to add it again
                           }
                       }
                    }
                });

                // Delete stale features
                vesselLayer.removeFeatures(deleteFeatures);

                // Add new features
                var features = [];
                $.each(vessels, function (mmsi, vessel) {
                    // Check that the vessel has a valid position
                    if (!vessel.existing && features.length < 10000 && vessel[0] && vessel[1]) {
                        features.push($scope.generateVesselFeature(mmsi, vessel));
                        $scope.checkUpdateSelectedVessel(mmsi, vessel);
                    }
                });
                vesselLayer.addFeatures(features);
                vesselLayer.redraw();
            };

            /**
             * Check if the selected vessel needs to be updated
             * @return if the position was updated
             */
            $scope.checkUpdateSelectedVessel = function (mmsi, vessel) {
                if ($scope.selVessel && $scope.selVessel.mmsi == mmsi && $scope.selVessel.lastReport < vessel[5]) {
                    console.log("Updating selected MMSI ");
                    $scope.selVessel.lat = vessel[0];
                    $scope.selVessel.lon = vessel[1];
                    $scope.selVessel.cog = vessel[2];
                    $scope.selVessel.vesselType = vessel[3];
                    $scope.selVessel.navStatus = vessel[4];
                    $scope.selVessel.lastReport = vessel[5];
                    $scope.selVessel.name = vessel[6];
                }
            };

            /**
             * Called when an AIS target is clicked
             * @param feature the MSI feature
             */
            $scope.onVesselSelect = function(feature) {
                hoverControl.unselectAll();
                VesselTrackService.fetchVessel(
                    feature.attributes.mmsi,
                    function (vessel) {
                        $scope.activeInfoPanel = 'details';
                        $scope.selVessel = vessel;
                    },
                    function () {}
                )
            }


        }]);
