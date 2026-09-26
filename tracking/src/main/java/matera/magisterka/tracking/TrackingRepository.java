package matera.magisterka.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrackingRepository extends JpaRepository<TrackingPing, Long> {
    List<TrackingPing> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId);
}