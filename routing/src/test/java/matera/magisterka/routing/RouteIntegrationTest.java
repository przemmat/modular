package matera.magisterka.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class RouteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RouteService routeService;

    @Test
    void shouldCreateAndPersistRoute() {
        Route route = routeService.createRoute("Warszawa", "Gliwice", 320.5);

        Route found = routeService.getRoute(route.getId());
        assertThat(found.getDistanceKm()).isEqualTo(320.5);
    }
}