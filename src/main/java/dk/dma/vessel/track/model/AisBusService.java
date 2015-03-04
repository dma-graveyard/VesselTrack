package dk.dma.vessel.track.model;

import dk.dma.ais.bus.AisBus;
import dk.dma.ais.bus.consumer.DistributerConsumer;
import dk.dma.ais.configuration.bus.AisBusConfiguration;
import dk.dma.vessel.track.VesselTrackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

/**
 * Encapsulates the AIS bus service
 */
@Service
public class AisBusService {

    static final Logger LOG = LoggerFactory.getLogger(AisBusService.class);

    @Value("${aisbus}")
    String aisbusPath;

    private AisBus aisBus;

    @Autowired
    VesselTrackHandler handler;

    @PostConstruct
    public void init() throws FileNotFoundException, JAXBException {
        LOG.info("Starting AIS bus using config: " + aisbusPath);

        // Load AisBus configuration
        AisBusConfiguration aisBusConf = AisBusConfiguration.load(aisbusPath);
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

    @PreDestroy
    public void destroy() {
        try {
            LOG.info("Shutting down AIS bus");
            aisBus.cancel();
            Thread.sleep(2000);
        } catch (Exception e) {
            LOG.error("Error shutting down AIS bus", e);
        }
    }

}
