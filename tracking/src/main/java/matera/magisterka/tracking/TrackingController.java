package matera.magisterka.tracking;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping
    public ResponseEntity<TrackingPing> recordPing(@RequestParam Long vehicleId,
                                                   @RequestParam Double lat,
                                                   @RequestParam Double lon) {
        return ResponseEntity.ok(trackingService.recordLocation(vehicleId, lat, lon));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<TrackingPing>> getHistory(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(trackingService.getVehicleHistory(vehicleId));
    }
}