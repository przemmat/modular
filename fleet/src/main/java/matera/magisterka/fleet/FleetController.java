package matera.magisterka.fleet;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @PostMapping
    public ResponseEntity<FleetVehicle> registerVehicle(@RequestParam String vin, @RequestParam String plateNumber) {
        return ResponseEntity.ok(fleetService.registerVehicle(vin, plateNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FleetVehicle> getVehicle(@PathVariable Long id) {
        return ResponseEntity.ok(fleetService.getVehicle(id));
    }
}