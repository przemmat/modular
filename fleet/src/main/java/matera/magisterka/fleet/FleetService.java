package matera.magisterka.fleet;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final FleetRepository fleetRepository;

    @Transactional
    public FleetVehicle registerVehicle(String vin, String plateNumber) {
        FleetVehicle vehicle = FleetVehicle.builder()
                .vin(vin)
                .plateNumber(plateNumber)
                .status(VehicleStatus.AVAILABLE)
                .build();
        return fleetRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public FleetVehicle getVehicle(Long id) {
        return fleetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + id));
    }
}