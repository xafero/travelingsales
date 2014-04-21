package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.xml.sax.helpers.DefaultHandler;

/**
 * this file is able to read the odr-way-visualization-data.xml file and instantiiate/configure
 * the ODRWayClassifiers that are configured in this file.
 *
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public class ODRVisualizationDataReader extends DefaultHandler {

    /**
     * my logger for debug and error-output.
     */
    protected static final Logger LOG = Logger.getLogger(ODRVisualizationDataReader.class.getName());

    /** the type of the current element. */
    private enum ElementType {
        /** map-definition. */
        MAP_DEFINITION,
        /** way-definition. */
        WAY_DEFINITION,
        /** way-visualization. */
        WAY_VISUALIZATION,
        /** zoom-level-data. */
        ZOOM_LEVEL_DATA,
        /** between elements. */
        UNDEFINED
    }

    /** the current way-visualization subtype. */
    private enum WayVisualizationSubtype {
        /** type definition. */
        TYPE,
        /** min-zoom-level definition. */
        MIN_ZOOM_LEVEL,
        /** max-zoom-level definition. */
        MAX_ZOOM_LEVEL,
        /** stroke color definition. */
        STROKE_COLOR,
        /** outline color definition. */
        OUTLINE_COLOR,
        /** visualization type definition. */
        VISUALIZATION_TYPE,
        /** undefined. */
        UNDEFINED
    }

    /**
     * reads up the data of the configured file.
     *
     * @throws FileNotFoundException file not found...oh oh!
     * @throws XMLStreamException malformed xml....bad boy!
     */
    public void readData() throws FileNotFoundException, XMLStreamException {
        InputStream in = null;
        File odrVisFile = getOdrVisualizationDataFile();
        if (odrVisFile.exists()) {
            in = new FileInputStream(odrVisFile);
        } else {
            in = getClass().getClassLoader()
            .getResourceAsStream(
            "org/openstreetmap/travelingsalesman/painting/odr/"
          + "way-visualization-data.xml");
        }
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader parser = factory.createXMLEventReader(in);

        while (parser.hasNext()) {
          XMLEvent event = parser.nextEvent();
          switch (event.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                processStartElement(event.asStartElement());
                break;

            case XMLStreamConstants.CHARACTERS:
                processCharacters(event.asCharacters());
                break;

            case XMLStreamConstants.END_ELEMENT:
                EndElement endElement = event.asEndElement();
                if (endElement.getName().toString().equals("way-definition")
                        || endElement.getName().toString().equals("way-visualization")
                        || endElement.getName().toString().equals("zoom-level-data")
                        || endElement.getName().toString().equals("map-definition")) {
                    currentElementType = ElementType.UNDEFINED;
                 }

                currentWayVisualizationSubtype = WayVisualizationSubtype.UNDEFINED;
              break;

            default :
              break;
          }
        }
    }

    /**
     * process starting element.
     *
     * @param element element to process
     */
    private void processStartElement(final StartElement element) {
        if (element.getName().toString().equals("map-definition")) {
            currentElementType = ElementType.MAP_DEFINITION;
         } else if (element.getName().toString().equals("way-definition")) {
            readWayDefinition(element);
         } else if (element.getName().toString().equals("way-visualization")) {
            currentElementType = ElementType.WAY_VISUALIZATION;
         } else if (element.getName().toString().equals("zoom-level-data")) {
             currentElementType = ElementType.ZOOM_LEVEL_DATA;
             for (Iterator<?> attributes = element.getAttributes(); attributes.hasNext();) {
                 Attribute attribute = (Attribute) attributes.next();
                 if (attribute.getName().toString().equals("level")) {
                     currentZoomLevel = Integer.parseInt(attribute.getValue());
                 }
             }
         } else {
             switch(currentElementType) {
                 case WAY_VISUALIZATION:
                     readWayVisualizationDetails(element);
                 break;
                 case ZOOM_LEVEL_DATA:
                     getClassifierFromType(element);
                     break;
                 case UNDEFINED:
                     LOG.fine("ODRVisualizationDataReader::processStartElement() unknown element \"" + element.getName().toString() + "\"");
                     break;
                 default:
             }
         }
    }

    /**
     * process a character event.
     *
     * @param characters chars to process
     */
    private void processCharacters(final Characters characters) {
        if (!characters.isWhiteSpace()) {
            switch(currentElementType) {
                case WAY_DEFINITION:
                    buildWayDefinition(characters.getData());
                    break;
                case WAY_VISUALIZATION:
                    assignWayVisualizationDetail(characters.getData());
                    break;
                case ZOOM_LEVEL_DATA:
                    assignWayWidth(characters.getData());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * determines the way type from the type attribute and assigns the currentClassifier
     * accordingly.
     *
     * @param element element to determine way type from
     */
    private void getClassifierFromType(final StartElement element) {
        for (Iterator<?> attributes = element.getAttributes(); attributes.hasNext();) {
            Attribute attribute = (Attribute) attributes.next();
            if (attribute.getName().toString().equals("type")) {
                String wayType = attribute.getValue();
                for (String wayKey : classifiers.keySet()) {
                    if (wayKey.equals(wayType)) {
                        currentClassifier = classifiers.get(wayKey);
                        break;
                    }
                }
            }
        }
    }

    /**
     * adds a new width value for the current zoom level to the current classifier.
     *
     * @param characters width chars
     */
    private void assignWayWidth(final String characters) {
        if (currentClassifier == null) return;

        currentClassifier.addZoomLevelWidth(currentZoomLevel, Float.parseFloat(characters));
        currentClassifier = null;
    }


    /**
     * determines the subtype of a way-visualization detail.
     *
     * @param element element to analyze
     */
    private void readWayVisualizationDetails(final StartElement element) {
        if (element.getName().toString().equals("type")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.TYPE;
        } else if (element.getName().toString().equals("min-zoom-level")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.MIN_ZOOM_LEVEL;
        } else if (element.getName().toString().equals("max-zoom-level")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.MAX_ZOOM_LEVEL;
        } else if (element.getName().toString().equals("stroke-color")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.STROKE_COLOR;
            for (Iterator<?> attributes = element.getAttributes(); attributes.hasNext();) {
                Attribute attribute = (Attribute) attributes.next();
                if (attribute.getName().toString().equals("dashed")) {
                    currentClassifier.setDashed(Boolean.parseBoolean(attribute.getValue()));
                } else if (attribute.getName().toString().equals("dash-array")) {
                    String[] dashArray = attribute.getValue().split(",");
                    float[] floatDashArray = new float[dashArray.length];
                    for (int i = 0; i < dashArray.length; i++) {
                        floatDashArray[i] = Float.parseFloat(dashArray[i].trim());
                    }
                    currentClassifier.setDashArray(floatDashArray);
                }
            }
        } else if (element.getName().toString().equals("outline-color")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.OUTLINE_COLOR;
        } else if (element.getName().toString().equals("visualization-type")) {
            currentWayVisualizationSubtype = WayVisualizationSubtype.VISUALIZATION_TYPE;
        } else {
            LOG.fine("ODRVisualizationDataReader::readWayVisualizationDetails() Unrecognized way-visualization detail \"" + element.getName().toString() + "\"");
        }
    }


    /**
     * reads a key/value pair for a certain way type.
     *
     * @param element way definition element
     */
    private void readWayDefinition(final StartElement element) {
        currentElementType = ElementType.WAY_DEFINITION;

        currentKey = null;
        currentValue = null;

        for (Iterator<?> attributes = element.getAttributes(); attributes.hasNext();) {
            Attribute attribute = (Attribute) attributes.next();
            if (attribute.getName().toString().equals("key")) {
               currentKey = attribute.getValue();
            } else if (attribute.getName().toString().equals("value")) {
                currentValue = attribute.getValue();
            }
        }
    }

    /**
     * after the subtype of the way-visualization was determined in
     * readWayVisualizationDetails the current value is determined and
     * assigned to the current classifier.
     *
     * @param detail detail
     */
    @SuppressWarnings("unchecked")
    private void assignWayVisualizationDetail(final String detail) {
        if (currentWayVisualizationSubtype != WayVisualizationSubtype.TYPE && currentClassifier == null) return;

        switch (currentWayVisualizationSubtype) {
        case TYPE:
            for (String classifierKey : classifiers.keySet()) {
                if (classifierKey.equals(detail)) {
                    currentClassifier = classifiers.get(classifierKey);
                    break;
                }
            }
            break;
        case MIN_ZOOM_LEVEL:
            currentClassifier.setMinZoomLevel(Integer.parseInt(detail));
            break;
        case MAX_ZOOM_LEVEL:
            currentClassifier.setMaxZoomLevel(Integer.parseInt(detail));
            break;
        case STROKE_COLOR:
            currentClassifier.setStrokeColor(Color.decode(detail));
            break;
        case OUTLINE_COLOR:
            currentClassifier.setOutlineColor(Color.decode(detail));
            break;
        case VISUALIZATION_TYPE:
            try {
                Class<ODRWay> odrWayClass = (Class<ODRWay>) Class.forName(detail);
                currentClassifier.setWayImplementation(odrWayClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            break;
        default:
                LOG.warning(" no ODR-detail type defined!");
            break;
        }
    }

    /**
     * builds up a way definition if the way type wasn't there before. otherwise
     * the current key/value pair is added to the existing classifier.
     *
     * @param wayType way type to build / extend
     */
    private void buildWayDefinition(final String wayType) {
        boolean inserted = false;
        for (String key : classifiers.keySet()) {
            if (key.equals(wayType)) {
                classifiers.get(key).addKeyValuePair(currentKey, currentValue);
                inserted = true;
                break;
            }
        }

        if (!inserted) {
            ODRWayClassifier classifier = new ODRWayClassifier();
            classifier.setType(wayType);
            classifier.addKeyValuePair(currentKey, currentValue);
            classifiers.put(wayType, classifier);
        }
    }

    /**
     * @return the odrVisualizationDataFile
     */
    public File getOdrVisualizationDataFile() {
        return odrVisualizationDataFile;
    }

    /**
     * @param anOdrVisualizationDataFile the odrVisualizationDataFile to set
     */
    public void setOdrVisualizationDataFile(final File anOdrVisualizationDataFile) {
        this.odrVisualizationDataFile = anOdrVisualizationDataFile;
    }

    /** the file. */
    private File odrVisualizationDataFile;
    /** the classifiers. */
    private final Hashtable<String, ODRWayClassifier> classifiers = new Hashtable<String, ODRWayClassifier>();
    /** the current classifier. */
    private ODRWayClassifier currentClassifier = null;
    /** current key (from way-definition). */
    private String currentKey;
    /** current value (from way-definition). */
    private String currentValue;
    /** the current element type. */
    private ElementType currentElementType = ElementType.UNDEFINED;
    /** the current way-visualization subtype. */
    private WayVisualizationSubtype currentWayVisualizationSubtype = WayVisualizationSubtype.UNDEFINED;
    /** the current zoom level (from zoom-level-data). */
    private int currentZoomLevel;
}
