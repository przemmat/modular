package matera.magisterka.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional
    public Route createRoute(String origin, String destination, Double distanceKm) {
        Route route = Route.builder()
                .origin(origin)
                .destination(destination)
                .distanceKm(distanceKm)
                .build();
        return routeRepository.save(route);
    }

    @Transactional(readOnly = true)
    public Route getRoute(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
    }
}