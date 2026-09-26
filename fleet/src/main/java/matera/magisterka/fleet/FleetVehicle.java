package matera.magisterka.fleet;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fleet_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FleetVehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = false)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;
}
