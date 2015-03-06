package dk.dma.vessel.track;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.bus.consumer.DistributerConsumer;
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.ais.configuration.filter.ExpressionFilterConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Encapsulates the AIS bus service
 */
@Service
@SuppressWarnings("unused")
public class AisBusService {

    static final Logger LOG = LoggerFactory.getLogger(AisBusService.class);

    @Value("${aisbus:}")
    String aisbusPath;

    @Value("${aisbusFilter:}")
    String aisbusFilter;

    @Value("${slave:false}")
    boolean slave;

    private AisBus aisBus;

    @Autowired
    VesselTrackHandler handler;

    @PostConstruct
    public void init() throws FileNotFoundException, JAXBException {
        // Only master instances listens to the AIS bus
        if (slave) {
            LOG.info("AIS bus not used in slave instances");

        } else {
            LOG.info("Starting AIS bus using config: " + aisbusPath);

            // Load AisBus configuration
            AisBusConfiguration aisBusConf = loadAisBusConfiguraion(aisbusPath);

            // Check if we need to update an expression filter
            if (StringUtils.isNotBlank(aisbusFilter)) {
                LOG.info("**** Setting AIS bus filter: " + aisbusFilter);
                createExpressionFilter(aisBusConf, aisbusFilter);
            }

            aisBus = aisBusConf.getInstance();

            // Create distributor consumer and add to aisBus
            DistributerConsumer distributer = new DistributerConsumer();
            distributer.getConsumers().add(handler);
            distributer.init();
            aisBus.registerConsumer(distributer);
            aisBus.start();
            aisBus.startConsumers();
            aisBus.startProviders();
        }
    }

    /**
     * Loads the AIS bus configuration
     * @param aisbusPath the path to the aisbus.xml configuration file
     * @return the AIS bus configuraiton
     */
    private AisBusConfiguration loadAisBusConfiguraion(String aisbusPath) throws FileNotFoundException, JAXBException {

        if (StringUtils.isNotBlank(aisbusPath) && Files.exists(Paths.get(aisbusPath))) {
            LOG.info("Loading AIS bus configuration from file " + aisbusPath);
            return AisBusConfiguration.load(aisbusPath);
        }

        LOG.info("Loading default AIS bus configuration");
        return AisBusConfiguration.load(getClass().getResourceAsStream("/aisbus.xml"));
    }

    /**
     * Updates the AIS bus with an expression filter
     *
     * @param aisBusConf the AIS bus configuration
     * @param aisbusFilter the expression filter
     * @return the updated expression filter
     */
    private ExpressionFilterConfiguration createExpressionFilter(AisBusConfiguration aisBusConf, String aisbusFilter) {
        // Check if the filter is already defined
        ExpressionFilterConfiguration expressionFilter
                = (ExpressionFilterConfiguration)aisBusConf.getFilters().stream()
                .filter(f -> f instanceof ExpressionFilterConfiguration)
                .findFirst()
                .orElse(null);

        if (expressionFilter == null) {
            expressionFilter = new ExpressionFilterConfiguration();
            aisBusConf.getFilters().add(expressionFilter);
        }
        expressionFilter.setExpression(aisbusFilter);
        return expressionFilter;
    }

    @PreDestroy
    public void destroy() {
        try {
            if (aisBus != null) {
                LOG.info("Shutting down AIS bus");
                aisBus.cancel();
                aisBus = null;
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            LOG.error("Error shutting down AIS bus", e);
        }
    }

}
