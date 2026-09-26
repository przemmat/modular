package matera.magisterka.fleet;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FleetRepository extends JpaRepository<FleetVehicle, Long> {
    Optional<FleetVehicle> findByVin(String vin);
}