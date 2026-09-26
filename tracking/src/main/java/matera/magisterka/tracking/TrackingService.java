package matera.magisterka.tracking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingRepository trackingRepository;

    @Transactional
    public TrackingPing recordLocation(Long vehicleId, Double latitude, Double longitude) {
        TrackingPing ping = TrackingPing.builder()
                .vehicleId(vehicleId)
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(Instant.now())
                .build();
        return trackingRepository.save(ping);
    }

    @Transactional(readOnly = true)
    public List<TrackingPing> getVehicleHistory(Long vehicleId) {
        return trackingRepository.findByVehicleIdOrderByRecordedAtDesc(vehicleId);
    }
}