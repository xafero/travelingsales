package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.BasicStroke;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;

/**
 * Project: osmnavigation<br/> ODRPaintVisitor.java<br/> created:
 * 09.12.2007 08:16:55 <br/>
 * <br/> The types of ways we know how to
 * draw.<br/>
 * The enum-values also contain constants on how and
 * at what zoom-levels they are to be painted.<br/>
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 * @see ODRWay
 * @see PathWay
 * @see PolygonWay
 * @see UsedTags#TAG_HIGHWAY
 */
public enum ODR_WAY_TYPE {
        /**
         * A motorway.<br/>
         * This is the widest and fastest kind of road.
         */
        motorway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_8;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.BOLD;
            }
        },
        /** a general road. */
        highway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_10;
            }
        },
        /** some rails. */
        railway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_8;
            }
        },
        /** some rails. */
        railway_tram {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_17;
            }
        },
        /** a track. */
        track {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_6;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a bicycle-way. */
        cycleway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_14;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a footway. */
        footway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_14;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a residential way. */
        residential {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_13;
            }
        },
        /** a service way. */
        service {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_13;
            }
        },
        /** a primary way or link. */
        primary {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_10;
            }
        },
        /** a secondary way. */
        secondary {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_12;
            }
        },
        /** a tertiary way. */
        tertiary {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_14;
            }
        },
        /** a trunk or a trunk link. */
        trunk {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_6;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.BOLD;
            }
        },
        /** a minor highway. */
        minor {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_10;
            }
        },
        /** a road with unknown type. */
        road {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_10;
            }
        },
        /** a path - a way unknown type, may be anything. */
        path {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_17;
            }
        },
        /** a park of some sorts. */
        leisure {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_12;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a forrest or some other form of wood. */
        wood {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_12;
            }
        },
        /** a lake or sth. */
        natural_water {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_10;
            }
        },
        /** a parking ground. */
        parking {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_14;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a house. */
        building {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_14;
            }
            /**
             * Get the {@link Font#getStyle()) to use for drawing
             * the name of the road.
             * @return the style, e.g. Font.BOLD
             */
            public int getFontStyle() {
                return Font.ITALIC;
            }
        },
        /** a pedestrian way. */
        pedestrian {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_16;
            }
        },
        /** a waterway. */
        waterway {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_8;
            }
        },
        /** landuse areas. */
        landuse {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_2;
            }
        },
        /** we ignore this way. */
        ignore {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_0;
            }
        },
        /** we do not know. */
        unknown {
            /**
             * Lowest zoom-level where the name of this
             * type of road is still rendered.
             * @return the zoom-level when to start hiding the name
             */
            @Override
            public int getMinZoomWithName() {
                return ZOOM_2;
            }
        };

        /** Automatically created logger for debug and error-output. */
        static final Logger LOG = Logger.getLogger(ODR_WAY_TYPE.class.getName());

        /**
         * minimum zoom level.
         */
        private static final int ZOOM_0 = 0;
        /**
         * Slightly above minimum zoom level.
         */
        private static final int ZOOM_2 = 2;
        /**
         * Above minimum zoom level.
         */
        private static final int ZOOM_6 = 6;
        /**
         * Normal zoom level.
         */
        private static final int ZOOM_8 = 8;
        /**
         * Slightly above Normal zoom level.
         */
        private static final int ZOOM_10 = 10;
        /**
         * Above normal zoom level.
         */
        private static final int ZOOM_12 = 12;
        /**
         * Above normal zoom level.
         */
        private static final int ZOOM_13 = 13;
        /**
         * Below max zoom level.
         */
        private static final int ZOOM_14 = 14;
        /**
         * Slightly below max zoom level.
         */
        private static final int ZOOM_16 = 16;
        /**
         * Max zoom level.
         */
        private static final int ZOOM_17 = 17;

        /**
         * @param way
         *            the way to analyse.
         * @return the WAYTYPE for the given way.
         */
        public static ODR_WAY_TYPE getWayType(final Way way) {
            boolean isHighway = false;

            for (Tag tag : way.getTags()) {
                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_BUILDING)) {
                    return ODR_WAY_TYPE.building;
                }

                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_LANDUSE)
                        || tag.getKey().equalsIgnoreCase(UsedTags.TAG_SPORT)) {
                    if (tag.getValue().equalsIgnoreCase("forest")
                            || tag.getValue().equalsIgnoreCase("wood")) {
                        return ODR_WAY_TYPE.wood;
                    }
                    return ODR_WAY_TYPE.landuse;
                }

                if (tag.getKey().equalsIgnoreCase("leisure")) {
                    return ODR_WAY_TYPE.leisure;
                }

                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_WATERWAY)) {
                    if (tag.getValue().equalsIgnoreCase(UsedTags.TAG_RIVERBANK)) {
                        return ODR_WAY_TYPE.natural_water;
                    }
                    return ODR_WAY_TYPE.waterway;
                }

                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_NATURAL)) {
                    if (tag.getValue().equalsIgnoreCase("water"))
                        return ODR_WAY_TYPE.natural_water;
                }

                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_HISTORIC)) {
                    if (tag.getValue().equalsIgnoreCase("museum"))
                        return ODR_WAY_TYPE.ignore;
                }

                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_AMENITY)) {
                    if (tag.getValue().equalsIgnoreCase("parking")) return ODR_WAY_TYPE.parking;
                    if (tag.getValue().equalsIgnoreCase("place_of_worship")
                            || tag.getValue().equalsIgnoreCase("school"))
                        return ODR_WAY_TYPE.ignore;
                }

                if (tag.getKey().equalsIgnoreCase(Tags.TAG_HIGHWAY)) {
                    if (tag.getValue().equalsIgnoreCase("motorway")
                            || tag.getValue().equalsIgnoreCase("motorway_link")) {
                        return ODR_WAY_TYPE.motorway;
                    }

                    if (tag.getValue().equalsIgnoreCase("trunk")
                            || tag.getValue().equalsIgnoreCase("trunk_link")) {
                        return ODR_WAY_TYPE.trunk;
                    }

                    if (tag.getValue().equalsIgnoreCase("primary")
                            || tag.getValue().equalsIgnoreCase("primary_link")) {
                        return ODR_WAY_TYPE.primary;
                    }

                    if (tag.getValue().equalsIgnoreCase("secondary")) {
                        return ODR_WAY_TYPE.secondary;
                    }

                    if (tag.getValue().equalsIgnoreCase("tertiary")) {
                        return ODR_WAY_TYPE.tertiary;
                    }

                    if (tag.getValue().equalsIgnoreCase("residential")
                            || tag.getValue().equalsIgnoreCase("minor")) {
                        return ODR_WAY_TYPE.residential;
                    }

                    if (tag.getValue().equalsIgnoreCase("service")) {
                        return ODR_WAY_TYPE.service;
                    }

                    if (tag.getValue().equalsIgnoreCase("footway")
                            || tag.getValue().equalsIgnoreCase("bridleway")) {
                        return ODR_WAY_TYPE.footway;
                    }

                    if (tag.getValue().equalsIgnoreCase("cycleway")) {
                        return ODR_WAY_TYPE.cycleway;
                    }

                    if (tag.getValue().equalsIgnoreCase("track")) {
                        return ODR_WAY_TYPE.track;
                    }

                    if (tag.getValue().equalsIgnoreCase("pedestrian")) {
                        return ODR_WAY_TYPE.pedestrian;
                    }

                    if (tag.getValue().equalsIgnoreCase("steps")) {
                        return ODR_WAY_TYPE.ignore;
                    }
                    if (tag.getValue().equalsIgnoreCase("road")) {
                        return ODR_WAY_TYPE.road;
                    }
                    if (tag.getValue().equalsIgnoreCase("path")) {
                        return ODR_WAY_TYPE.path;
                    }

                    if (LOG.isLoggable(Level.FINEST)
                            && tag.getValue().equalsIgnoreCase("unclassified")) {
                        LOG.log(Level.FINEST, "found unclassified highway (id "
                                + way.getId() + ")");
                    }

//                    if (LOG.isLoggable(Level.FINE)
//                            && !tag.getValue().equalsIgnoreCase("unclassified")) {
                        //                        LOG.log(Level.FINE, "found unknown highway type: "
                        //                                + tag.getValue() + " (id " + way.getId() + ")");
                        //                        for (Tag unknowWayTags : way.getTagList()) {
                        //                            LOG.log(Level.FINE, unknowWayTags.getKey() + " => "
                        //                                    + unknowWayTags.getValue());
                        //                        }
//                    }

                    isHighway = true;
                }

                if (tag.getKey().equals(UsedTags.TAG_RAILWAY)) {
                    if (tag.getValue().equalsIgnoreCase("tram")) {
                        return ODR_WAY_TYPE.railway_tram;
                    } else {
                        return ODR_WAY_TYPE.railway;
                    }
                }
            }

            if (isHighway) {
                return ODR_WAY_TYPE.residential;
            }

            if (LOG.isLoggable(Level.FINE)) {
                StringBuilder msg = new StringBuilder(
                        "found unknown way type (#tags=" + way.getTags().size() + " )");
                for (Tag unknowWayTags : way.getTags()) {
                    msg.append("\n\t" + unknowWayTags.getKey() + " => "
                            + unknowWayTags.getValue());
                }
                //LOG.log(Level.FINE, msg.toString());
            }

            return ODR_WAY_TYPE.unknown;
        }

        /**
         * Lowest zoom-level where the name of this
         * type of road is still rendered.
         * @return the zoom-level when to start hiding the name
         */
        public abstract int getMinZoomWithName();

        /**
         * Get the {@link Font#getStyle()) to use for drawing
         * the name of the road.
         * @return the style, e.g. Font.BOLD
         */
        public int getFontStyle() {
            return 0;
        }
    }