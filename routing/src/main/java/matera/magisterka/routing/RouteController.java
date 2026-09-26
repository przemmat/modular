package matera.magisterka.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public ResponseEntity<Route> createRoute(@RequestParam String origin,
                                             @RequestParam String destination,
                                             @RequestParam Double distanceKm) {
        return ResponseEntity.ok(routeService.createRoute(origin, destination, distanceKm));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Route> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRoute(id));
    }
}