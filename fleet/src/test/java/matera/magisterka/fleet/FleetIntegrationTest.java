package matera.magisterka.fleet;



import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FleetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FleetService fleetService;

    @Autowired
    private FleetRepository fleetRepository;

    @Test
    void shouldRegisterAndRetrieveVehicle() {
        FleetVehicle vehicle = fleetService.registerVehicle("1C9TESTVIN0000001", "PO-12345");

        FleetVehicle found = fleetService.getVehicle(vehicle.getId());
        assertThat(found.getVin()).isEqualTo("1C9TESTVIN0000001");
        assertThat(found.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }
}