/**
 * The main VesselTrack controller
 */
angular.module('vesseltrack.app')

    /**
     * The main VesselTrack controller
     */
    .controller('VesselTrackCtrl', ['$scope', '$rootScope', '$timeout', '$interval', 'VesselTrackService',
        function ($scope, $rootScope, $timeout, $interval, VesselTrackService) {
            'use strict';

            var proj4326 = new OpenLayers.Projection("EPSG:4326");
            var projmerc = new OpenLayers.Projection("EPSG:900913");

            $scope.activeInfoPanel = undefined;
            $scope.mapSettings = VesselTrackService.mapSettings();
            $scope.bounds = undefined;
            $scope.map = undefined;
            $scope.vesselScale = 0.7;
            $scope.detaultSelectZoom = 10;

            $('.info-panel').css('z-index','1000');

            /**
             * Schedules the loading of vessels for the given bounds
             * @param bounds the bounds to load the vessels for
             */
            $scope.reloadVessels = function (bounds) {
                if ($scope.bounds && $scope.bounds.equals(bounds)) {
                    return;
                }
                $scope.bounds = bounds;
                $scope.storeMapSettings();

                // Schedule a reload of vessels
                $scope.scheduleReloadVessels();
            };

            /**
             * Schedules the loading of vessels
             */
            $scope.scheduleReloadVessels = function () {

                // If a back-end loading has already been scheduled, cancel it
                if ($scope.vesselTimer) {
                    $timeout.cancel($scope.vesselTimer);
                    delete $scope.vesselTimer;
                }

                // Schedule the loading of vessels in 500 ms time
                $scope.vesselTimer = $timeout(function () {
                    $scope.fetchVessels($scope.bounds, false);
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

            var googleMapsLayer = new OpenLayers.Layer.Google(
                "Google", {
                    type: google.maps.MapTypeId.HYBRID,
                    numZoomLevels: 20,
                    animationEnabled: false
                });

            var vesselLayer = new OpenLayers.Layer.Vector("Vessels", {
                styleMap : new OpenLayers.StyleMap({
                    "default" : new OpenLayers.Style({
                        externalGraphic : "${image}",
                        graphicWidth : "${width}",
                        graphicHeight : "${height}",
                        graphicXOffset : "${offsetX}",
                        graphicYOffset : "${offsetY}",
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

            var clusterLayer = new OpenLayers.Layer.Vector("Vessels Clusters", {
                styleMap: new OpenLayers.StyleMap({
                    "default": new OpenLayers.Style({
                        fillColor: "${fill}",
                        strokeWidth: "1",
                        strokeColor: "${fill}",
                        fontColor: "#fff",
                        fontSize: "10px",
                        fontFamily: "Courier New, monospace",
                        fontWeight: "bold",
                        label: "${label}",
                        labelOutlineWidth: 0
                    }, { context: this.context })
                })
            });
            clusterLayer.setOpacity(0.4);

            var selectionLayer = new OpenLayers.Layer.Vector("Selection", {
                styleMap : new OpenLayers.StyleMap({
                    "default" : new OpenLayers.Style({
                        externalGraphic : "${image}",
                        graphicWidth : "${width}",
                        graphicHeight : "${height}",
                        graphicXOffset : "${offsetX}",
                        graphicYOffset : "${offsetY}",
                        rotation : "${angle}"
                    })
                })
            });

            var trackLayer = new OpenLayers.Layer.Vector("Past Tracks", {
                styleMap : new OpenLayers.StyleMap({
                    'default' : {
                        strokeColor : "#CC2222",
                        strokeWidth : 2
                    }
                })
            });

            var trackLabelLayer = new OpenLayers.Layer.Vector("Past Track Labels", {
                styleMap : new OpenLayers.StyleMap({
                    'default' : {
                        label : "${timeStamp}",
                        fontColor : "black",
                        fontSize : "11px",
                        fontWeight : "bold",
                        labelAlign : "${align}",
                        labelXOffset : "${xOffset}",
                        labelYOffset : "${yOffset}",
                        labelOutlineColor : "#fff",
                        labelOutlineWidth : 2,
                        labelOutline : 1,
                        pointRadius : 2,
                        fill : true,
                        fillColor : "#CC2222",
                        strokeColor : "#CC2222",
                        stroke : true
                    }
                })
            });

            // Toggle the base layer between Google and OSM
            $scope.showGoogleMaps = false;
            $scope.$watch(
                function() { return $scope.showGoogleMaps; },
                function(checked) {
                    $scope.map.setBaseLayer(checked ? googleMapsLayer : osmLayer);
                }
            );


            /*********************************/
            /* Map                           */
            /*********************************/

            $scope.map = new OpenLayers.Map({
                div: 'map',
                theme: null,
                layers: [ osmLayer, googleMapsLayer, clusterLayer, vesselLayer, selectionLayer, trackLayer, trackLabelLayer ],
                units: "degrees",
                projection: projmerc,
                center: new OpenLayers.LonLat($scope.mapSettings.lon, $scope.mapSettings.lat).transform(proj4326, projmerc),
                zoom: $scope.mapSettings.zoom
            });


            // Add zoom buttons
            $scope.map.addControl(new OpenLayers.Control.Zoom());
            var overviewMap = new OpenLayers.Control.OverviewMap({
                div: $('#overviewMap')[0],
                minRatio: 20,
                maxRatio: 20,
                autoPan: true
            });
            $scope.map.addControl(overviewMap);

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
                    '<div><strong>' + feature.data.vessel.name + '</strong></div>' +
                    '<div><small>MMSI: ' + feature.data.mmsi + '</small></div>' +
                    '</div>';
                return tooltip;
            }

            var hoverControl = new OpenLayers.Control.SelectFeature(
                vesselLayer, {
                    hover: true,
                    onBeforeSelect: function(feature) {
                        if ($scope.tooltip) {
                            return;
                        }

                        var b = feature.geometry.getBounds();
                        var html = formatTooltip(feature);

                        // add code to create tooltip/popup
                        $scope.tooltip = new OpenLayers.Popup.Anchored(
                            "tooltip",
                            new OpenLayers.LonLat(b.left, b.bottom),
                            new OpenLayers.Size(150, 30 + 18 *  html.occurrences("<br />")),
                            html,
                            {'size': new OpenLayers.Size(0,0), 'offset': new OpenLayers.Pixel(100, 12)},
                            false,
                            null);

                        $scope.tooltip.backgroundColor = '#eeeeee';
                        $scope.tooltip.calculateRelativePosition = function () {
                            return 'bl';
                        };


                        $scope.map.addPopup($scope.tooltip);
                        return true;
                    },
                    onUnselect: function(feature) {
                        // remove tooltip
                        if ($scope.tooltip) {
                            $scope.map.removePopup($scope.tooltip);
                            $scope.tooltip.destroy();
                            $scope.tooltip=null;
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
                        } else {
                            $scope.onVesselSelect(null);
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

            /** Returns the textual vessel type **/
            $scope.vesselType = function (type) {
                return VesselTrackService.vesselType(type);
            };

            /** Returns the textual nav status **/
            $scope.navStatus = function (status) {
                return VesselTrackService.navStatus(status);
            };

            /** Formats the timestamp as a textual date **/
            $scope.formatDate = function (timestamp) {
                return moment(timestamp).format('MMMM Do, HH:mm:ss');
            };

            /** Opens the given MMSI in marinetraffic.com **/
            $scope.marineTraffic = function (mmsi) {
                window.open('http://www.marinetraffic.com/ais/shipdetails.aspx?mmsi=' + mmsi, '_blank');
            };

            /** Centers the map on the given position **/
            $scope.setCenter = function(longitude, latitude, zoom) {
                var pos = new OpenLayers.LonLat(longitude, latitude).transform(proj4326, projmerc);

                // set position to find center in pixels
                $scope.map.setCenter(pos, zoom ? zoom : $scope.detaultSelectZoom);
            };

            /** Periodically check if a followed vessel has strayed way from the center **/
            $interval(function () {
                if ($scope.selVessel && $scope.selVessel.follow) {
                    var center = $scope.map.getPixelFromLonLat($scope.map.center);
                    var pos = $scope.map.getPixelFromLonLat(new OpenLayers.LonLat($scope.selVessel.lon, $scope.selVessel.lat).transform(proj4326, projmerc));
                    var pixelDist = lineDistance(center, pos);
                    if (pixelDist > 10) {
                        $scope.setCenter($scope.selVessel.lon, $scope.selVessel.lat, $scope.selVessel.followZoom);
                    }
                }
            }, 60000 + (Math.random() * 10.0 - 5.0));

            /** If "follow" is turned on, center the map on the selected vessel **/
            $scope.$watch(
                function() { return $scope.selVessel != null && $scope.selVessel.follow },
                function(data) {
                    if ($scope.selVessel && $scope.selVessel.follow) {
                        $scope.selVessel.followZoom = $scope.mapSettings.zoom;
                        $scope.setCenter($scope.selVessel.lon, $scope.selVessel.lat, $scope.selVessel.followZoom);
                        replaceClass('#follow-btn', 'btn-default', 'btn-danger');
                    } else {
                        replaceClass('#follow-btn', 'btn-danger', 'btn-default');
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
                VesselTrackService.fetchVesselClusters(
                    bounds,
                    $scope.selVessel ? $scope.selVessel.mmsi : undefined,
                    $scope.search.filterVessels ? $scope.search.filter : undefined,
                    $scope.mapSettings.zoom,
                    function (result) {
                        if (update) {
                            $scope.updateVesselFeatures(result);
                        } else {
                            $scope.generateVesselFeatures(result);
                        }
                    },
                    function () {
                        console.error("Error fetching vessels");
                    }
                )
            };

            /**
             * Creates a vessel feature from for the given vessel
             * @param vessel the vessel data
             * @returns the vessel feature
             */
            $scope.generateVesselFeature = function(vessel) {
                var attr = VesselTrackService.vesselGraphics(vessel.vesselType, vessel.navStatus, $scope.vesselScale);
                attr.mmsi = vessel.mmsi;
                attr.angle = (vessel.cog) ? vessel.cog - 90 : 0;
                attr.vessel = vessel;
                var geom = createPoint(vessel.lon, vessel.lat);
                return new OpenLayers.Feature.Vector(geom, attr);
            };

            /**
             * Creates the cluster feature for the given cluster
             * @param cluster
             */
            $scope.generateClusterFeature = function (cluster) {
                var points = [];
                points.push(createPoint(cluster.from.longitude, cluster.from.latitude));
                points.push(createPoint(cluster.to.longitude, cluster.from.latitude));
                points.push(createPoint(cluster.to.longitude, cluster.to.latitude));
                points.push(createPoint(cluster.from.longitude, cluster.to.latitude));

                var rings = [new OpenLayers.Geometry.LinearRing(points)];
                return new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon(rings), {
                    fill: VesselTrackService.getClusterColor(cluster),
                    label: ("" + cluster.count)
                });
            };

            /**
             * Generate vessel features for the given list of vessels
             * @param result the vessels and clusters to generate features for
             */
            $scope.generateVesselFeatures = function (result) {

                // Generate cluster features
                var clusterFeatures = [];
                $.each(result.clusters, function (index, cluster) {
                    clusterFeatures.push($scope.generateClusterFeature(cluster));
                });
                clusterLayer.removeAllFeatures();
                clusterLayer.addFeatures(clusterFeatures);
                clusterLayer.redraw();

                // Generate vessel features
                var vesselFeatures = [];
                var selVesselPosUpdated = false;
                $.each(result.vessels, function (index, vessel) {
                    // Check that the vessel has a valid position
                    if (vessel.lat && vessel.lon) {
                        vesselFeatures.push($scope.generateVesselFeature(vessel));
                        selVesselPosUpdated |= $scope.checkUpdateSelectedVessel(vessel);
                    }
                });
                vesselLayer.removeAllFeatures();
                vesselLayer.addFeatures(vesselFeatures);
                vesselLayer.redraw();

                if (selVesselPosUpdated) {
                    $scope.updateVesselSelectionFeature();
                }
            };

            /**
             * Update vessel features for the given list of vessels
             * @param result the vessels and clusters to generate features for
             */
            $scope.updateVesselFeatures = function (result) {

                // Re-generate all cluster features
                var clusterFeatures = [];
                $.each(result.clusters, function (index, cluster) {
                    clusterFeatures.push($scope.generateClusterFeature(cluster));
                });
                clusterLayer.removeAllFeatures();
                clusterLayer.addFeatures(clusterFeatures);
                clusterLayer.redraw();

                // For vessels, only update changed vessels
                var deleteFeatures = [];

                // Create a mmsi -> vessel lookup map
                var vesselLookup = {};
                $.each(result.vessels, function (index, vessel) {
                    vesselLookup[vessel.mmsi] = vessel;
                });

                // Look at existing features and find deleted and changed vessel feature
                $.each(vesselLayer.features, function (index, feature) {
                    if (feature.attributes && feature.attributes.mmsi) {
                       var vessel = vesselLookup[feature.attributes.mmsi];
                       if (!vessel) {
                           deleteFeatures.push(feature);
                       } else {
                           vessel.existing = true;
                           // Check if the vessel has a newer last-report date
                           if (feature.attributes.vessel.lastReport < vessel.lastReport) {
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
                var selVesselPosUpdated = false;
                $.each(result.vessels, function (index, vessel) {
                    // Check that the vessel has a valid position
                    if (!vessel.existing && features.length < 10000 && vessel.lat && vessel.lon) {
                        features.push($scope.generateVesselFeature(vessel));
                        selVesselPosUpdated |= $scope.checkUpdateSelectedVessel(vessel);
                    }
                });
                vesselLayer.addFeatures(features);
                vesselLayer.redraw();

                if (selVesselPosUpdated) {
                    $scope.updateVesselSelectionFeature();
                }
            };

            /**
             * Check if the selected vessel needs to be updated
             * @return if the position or COG was updated
             */
            $scope.checkUpdateSelectedVessel = function (vessel) {
                var posUpdate = false;
                if ($scope.selVessel && $scope.selVessel.mmsi == vessel.mmsi && $scope.selVessel.lastReport < vessel.lastReport) {
                    posUpdate = $scope.selVessel.lat != vessel.lat || $scope.selVessel.lon != vessel.lon || $scope.selVessel.cog != vessel.cog;
                    $scope.selVessel.lat = vessel.lat;
                    $scope.selVessel.lon = vessel.lon;
                    $scope.selVessel.cog = vessel.cog;
                    $scope.selVessel.vesselType = vessel.vesselType;
                    $scope.selVessel.navStatus = vessel.navStatus;
                    $scope.selVessel.lastReport = vessel.lastReport;
                    $scope.selVessel.name = vessel.name;
                }
                return posUpdate;
            };

            /**
             * Called when an AIS target is clicked
             * @param feature the MSI feature
             */
            $scope.onVesselSelect = function(feature) {
                $scope.selVessel = undefined;
                hoverControl.unselectAll();

                if (feature) {
                    VesselTrackService.fetchVessel(
                        feature.attributes.mmsi,
                        function (vessel) {
                            $scope.activeInfoPanel = 'details';
                            $scope.selVessel = vessel;
                            $scope.updateVesselSelectionFeature();
                        },
                        function () {}
                    )
                } else {
                    // Remove the selection feature
                    $scope.updateVesselSelectionFeature();
                    if ($scope.activeInfoPanel == 'details') {
                        $scope.activeInfoPanel = undefined;
                        if (!$rootScope.$$phase) $rootScope.$apply();
                    }
                }
            };

            /**
             * Updates the vessel selection feature
             */
            $scope.updateVesselSelectionFeature = function () {
                selectionLayer.removeAllFeatures();
                if ($scope.selVessel) {
                    var geom = createPoint($scope.selVessel.lon, $scope.selVessel.lat);
                    var attr = VesselTrackService.vesselSelectGraphics($scope.vesselScale);
                    attr.angle = ($scope.selVessel.cog) ? $scope.selVessel.cog - 90 : 0;
                    selectionLayer.addFeatures([ new OpenLayers.Feature.Vector(geom,attr) ]);
                    selectionLayer.redraw();
                }
            };

            /*********************************/
            /* Past track Loading            */
            /*********************************/

            /**
             * Fetches the past track for the currently selected vessel
             */
            $scope.fetchPastTrack = function() {
                if ($scope.selVessel) {
                    VesselTrackService.fetchTrack(
                        $scope.selVessel.mmsi,
                        $scope.selVessel.trackDuration,
                        function (track) {
                            $scope.generatePastTrackFeature(track);
                        },
                        function () {
                            console.error("Error fetching past track");
                        }
                    )
                }
            };

            /** Generates the feature for the track **/
            $scope.generatePastTrackFeature = function (tracks) {
                trackLayer.removeAllFeatures();
                trackLabelLayer.removeAllFeatures();

                if (!tracks || tracks.length == 0 || !$scope.selVessel || !$scope.selVessel.showTrack) {
                    return;
                }

                // Draw tracks layer
                for ( var i = 1; i < tracks.length; i++) {
                    // Insert line
                    var points = [createPoint(tracks[i - 1].lon, tracks[i - 1].lat), createPoint(tracks[i].lon, tracks[i].lat)];
                    var line = new OpenLayers.Geometry.LineString(points);
                    var lineFeature = new OpenLayers.Feature.Vector(line, {
                        id : $scope.selVessel.mmsi
                    });
                    trackLayer.addFeatures([ lineFeature ]);
                }

                // Draw timestamps layer
                var maxNoTimestampsToDraw = 5;
                var delta = (maxNoTimestampsToDraw - 1) / (tracks[tracks.length - 1].time - tracks[0].time - 1);
                var oldHatCounter = -1;

                for ( var i in tracks) {
                    var track = tracks[i];
                    var hatCounter = Math.floor((track.time - tracks[0].time) * delta);
                    if (oldHatCounter != hatCounter) {
                        oldHatCounter = hatCounter;
                        var timeStampFeature = new OpenLayers.Feature.Vector(createPoint(track.lon, track.lat));
                        timeStampFeature.attributes = {
                            id : $scope.selVessel.mmsi,
                            timeStamp : moment(track.time).format('MMMM Do, HH:mm'),
                            align : "lm",
                            xOffset : 10
                        };

                        trackLabelLayer.addFeatures([ timeStampFeature ]);
                    }
                }
            };

            /** Display past tracks for the given duration **/
            $scope.showTrack = function(duration) {
                if ($scope.selVessel) {
                    // First clear any old track
                    $scope.selVessel.showTrack = false;
                    $timeout(function () {
                        // Show the new track
                        $scope.selVessel.trackDuration = duration;
                        $scope.selVessel.showTrack = true;
                    }, 100);
                }
            };

            /** If "showTrack" is turned on, load the past track **/
            $scope.$watch(
                function() { return $scope.selVessel != null && $scope.selVessel.showTrack },
                function(data) {
                    if ($scope.selVessel && $scope.selVessel.showTrack) {
                        $scope.fetchPastTrack();
                        replaceClass('#track-btn,#track-btn-dropdown', 'btn-default', 'btn-danger');
                    } else {
                        $scope.generatePastTrackFeature(null);
                        replaceClass('#track-btn,#track-btn-dropdown', 'btn-danger', 'btn-default');
                    }
                },
                true);


            /*********************************/
            /* Searching                     */
            /*********************************/

            // Initialize the filter field
            $timeout(function () {
                $scope.searchFilterField = VS.init({
                    container: $('#searchFilter'),
                    query: '',
                    showFacets: true,
                    unquotable: [],
                    callbacks: {
                        search: function (query, searchCollection) {
                            $scope.$apply(function () {
                                $scope.search.filter = $scope.searchFilterField.searchBox.value();
                                if (!$rootScope.$$phase) $rootScope.$apply();
                            });
                        },
                        facetMatches: function (callback) {
                            callback(['mmsi', 'callsign', 'imo', 'name', 'country', 'type', 'status'], {preserveOrder: true});
                        },
                        valueMatches: function (facet, searchTerm, callback) {
                            VesselTrackService.searchFilterOptions(facet, searchTerm, 20,
                                function (result) {
                                    callback(result, {preserveOrder: true});
                                },
                                function () { });
                        }
                    }
                });
            }, 300);

            $scope.search = {
                filter : undefined,
                filterVessels : false,
                vessels : []
            };

            /** If "searchFilter" is changed, refresh the search result **/
            $scope.$watch(
                function() { return $scope.search.filter },
                function(data) {
                    if (!$scope.search.filter) {
                        $scope.search.vessels = [];
                        if ($scope.search.filterVessels) {
                            $scope.scheduleReloadVessels();
                        }
                    } else {
                        VesselTrackService.fetchVessels(
                            undefined,
                            undefined,
                            $scope.search.filter,
                            100,
                            function (vessels) {
                                $scope.search.vessels = vessels;
                                if ($scope.search.filterVessels) {
                                    $scope.scheduleReloadVessels();
                                }
                            },
                            function () {
                                console.error("Error fetching vessels");
                            }
                        )
                    }
                },
                true);

            // Make the "Apply Filter" button really nasty
            $scope.$watch(
                function() { return $scope.search.filterVessels },
                function(filter) {
                    if (filter) {
                        replaceClass('#filter-btn', 'btn-default', 'btn-danger');
                        $scope.scheduleReloadVessels();
                    } else {
                        replaceClass('#filter-btn', 'btn-danger', 'btn-default');
                        $scope.scheduleReloadVessels();
                    }
                }, false);

        }]);
