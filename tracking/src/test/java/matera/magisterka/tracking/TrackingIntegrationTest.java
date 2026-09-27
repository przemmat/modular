package matera.magisterka.tracking;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TrackingService trackingService;

    @Test
    void shouldRecordAndRetrievePings() {
        trackingService.recordLocation(100L, 50.2976, 18.6766);
        trackingService.recordLocation(100L, 50.2990, 18.6780);

        List<TrackingPing> history = trackingService.getVehicleHistory(100L);
        assertThat(history).hasSize(2);
    }
}