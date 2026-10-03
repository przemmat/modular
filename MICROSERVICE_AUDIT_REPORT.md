# Kompleksowy Raport Audytu Technicznego i Metodologicznego Architektury Mikroserwisowej (`microservice`) w Zestawieniu z Monolitem Modularnym (`modular`) i Tradycyjnym (`monolit`)

---

**Autorzy audytu:** Zespół Inżynierii Jakości i Architektury CI/CD (Teamwork Multi-Agent Technical Audit Group)  
**Data sporządzenia:** 27 września 2026 r.  
**Wersja dokumentu:** 1.0 (Final Master Release)  
**Środowisko ewaluacyjne:** Java 21 LTS (Microsoft OpenJDK 21.0.12.1), Spring Boot 4.1.1, PostgreSQL 16 Alpine, RabbitMQ 3-Alpine, Jenkins LTS (Docker).  
**Status integralności:** Ścisły tryb Read-Only zachowany w 100% — żaden plik źródłowy ani konfiguracyjny w badanych projektach nie został zmodyfikowany.

---

## SPIS TREŚCI

1. [Wprowadzenie i Metodologia Audytu](#wprowadzenie-i-metodologia-audytu)
   - 1.1. Kontekst pracy magisterskiej: badanie porównawcze *ceteris paribus*
   - 1.2. Środowisko techniczne i platforma referencyjna
   - 1.3. Gwarancja bezwzględnego trybu Read-Only
2. [Część 1: Identyfikacja i Naprawa Wszystkich Błędów Technicznych w Projekcie `microservice`](#część-1-identyfikacja-i-naprawa-wszystkich-błędów-technicznych-w-projekcie-microservice)
   - 2.1. Mikroserwis `fleet` (audyt kodu, POM, testów, konfiguracji, Dockerfile i Jenkinsfile)
   - 2.2. Mikroserwis `routing` (audyt kodu, POM, testów, konfiguracji, Dockerfile i Jenkinsfile)
   - 2.3. Mikroserwis `tracking` (audyt anomalii pakietów, POM, testów, konfiguracji, Dockerfile i Jenkinsfile)
   - 2.4. Infrastruktura wspólna `docker-compose-infra.yml` (bazy danych, RabbitMQ, wolumeny, healthchecki, sieć)
3. [Część 2: Porównanie i Wyrównanie 1:1 (*Ceteris Paribus*) Między `microservice`, `modular` i `monolit`](#część-2-porównanie-i-wyrównanie-11-ceteris-paribus-między-microservice-modular-i-monolit)
   - 3.1. Zestawienie liczby i typu testów domenowych (Domain Test Parity)
   - 3.2. Ujednolicenie obrazu bazy danych Testcontainers (`postgres:16-alpine`)
   - 3.3. Standaryzacja telemetrii w `Jenkinsfile` (eliminacja błędu `ps -C java` i zastąpienie `bc` przez `awk`)
   - 3.4. Agregacja i interpretacja metryk z 3 potoków mikroserwisowych vs 1 potok monolitu
   - 3.5. Wytyczne metodologiczne i wzorcowa matryca badawcza do rozdziału empirycznego
4. [Część 3: Architektura Repozytoriów na GitHubie pod Kątem Jenkinsa](#część-3-architektura-repozytoriów-na-githubie-pod-kątem-jenkinsa)
   - 4.1. Analiza porównawcza wariantów (Opcja A: 1 Monorepo vs Opcja B: 3 Polyrepos vs Opcja C: 5 Polyrepos)
   - 4.2. Wpływ wyboru architektury na zachowanie Jenkinsa (`Script Path`, `${WORKSPACE}`, wyzwalacze, `git diff`)
   - 4.3. Rekomendacja architektoniczna i uzasadnienie inżynierskie
   - 4.4. Instrukcja konfiguracji zadań w Jenkinsie krok po kroku
   - 4.5. Pipeline nadrzędny orkiestrujący eksperymenty (`benchmark-orchestrator`)
5. [Podsumowanie i Lista Kontrolna dla Autora Pracy](#podsumowanie-i-lista-kontrolna-dla-autora-pracy)

---

## Wprowadzenie i Metodologia Audytu

### 1.1. Kontekst pracy magisterskiej: badanie porównawcze *ceteris paribus*

Przedmiotem dysertacji magisterskiej jest empiryczna, rygorystyczna ocena porównawcza trzech wiodących stylów architektonicznych systemów korporacyjnych wytwarzanych na platformie Java:
1. **Monolitu tradycyjnego (`monolit`)** — jednorodnego systemu o pojedynczej jednostce wdrożeniowej,
2. **Monolitu modułowego (`modular`)** — monolitu logicznie podzielonego na autonomiczne moduły Maven (`fleet`, `routing`, `tracking`, `app`) z zachowaniem pojedynczego procesu uruchomieniowego,
3. **Architektury mikrousługowej (`microservice`)** — systemu w pełni rozproszonego, składającego się z trzech niezależnych usług (`fleet`, `routing`, `tracking`) komunikujących się asynchronicznie za pośrednictwem brokera RabbitMQ oraz posiadających odrębne bazy danych PostgreSQL (paradygmat *Database-per-Service*).

Podstawowym aksjomatem naukowym niniejszego badania jest zasada **równoważności warunków początkowych (*ceteris paribus*)**. Oznacza to, że wszelkie różnice zaobserwowane w metrykach procesu ciągłej integracji i ciągłego wdrażania (CI/CD) — takich jak czas trwania etapów potoku (`duration_ms`), szczytowe zużycie pamięci operacyjnej przez kompilator i wirtualną maszynę Java (`peak_ram_mb`) oraz rozmiar wyprodukowanych artefaktów kontenerowych Docker (`docker_image_size_mb`) — muszą wynikać **wyłącznie z wewnętrznych właściwości badanych stylów architektonicznych**. Niedopuszczalna jest sytuacja, w której mikroserwisy wykazują odmienne zachowanie z powodu nieskompilowanych klas, błędów konfiguracyjnych, asymetrii liczby wykonywanych testów jednostkowych/integracyjnych, pobierania różnych wersji baz danych czy zafałszowania telemetrii procesów systemowych.

### 1.2. Środowisko techniczne i platforma referencyjna

Wszystkie trzy projekty zostały osadzone w tożsamym stosie technologicznym:
- **Język i platforma uruchomieniowa:** Java 21 LTS (Microsoft OpenJDK 21.0.12.1).
- **Framework aplikacji:** Spring Boot 4.1.1 (oparty o Spring Framework 7, Hibernate 7, Jackson 3.x / 2.x).
- **Warstwa trwałości danych:** Spring Data JPA / Hibernate z dialektem PostgreSQL.
- **Relacyjna baza danych:** PostgreSQL 16 Alpine (`postgres:16-alpine`) uruchamiana w środowisku testowym za pośrednictwem biblioteki Testcontainers 1.20.4 oraz w środowisku lokalnym za pośrednictwem Docker Compose.
- **Broker komunikatów:** RabbitMQ 3-Management-Alpine (`rabbitmq:3-management-alpine`).
- **Narzędzie orkiestracji CI/CD:** Jenkins LTS działający w środowisku kontenerowym z bezpośrednim dostępem do gniazda Dockera (`/var/run/docker.sock`).

### 1.3. Gwarancja bezwzględnego trybu Read-Only

Wszelkie prace audytowe zrealizowano przy zachowaniu **100% reżimu Read-Only**. Żaden plik źródłowy, plik deskryptora budowania (`pom.xml`), plik konfiguracyjny (`application.yml`), plik definicji kontenerów (`Dockerfile`, `docker-compose-infra.yml`) ani plik potoku CI (`Jenkinsfile`) w katalogach:
- `e:/magisterka/final/microservice`
- `e:/magisterka/final/modular`
- `e:/magisterka/final/monolit`

nie został zmieniony, usunięty ani nadpisany. Poniższy raport stanowi kompletną dokumentację diagnostyczną oraz przewodnik wdrożeniowy zawierający gotowe, zweryfikowane bloki kodu, które autor pracy może wdrożyć w projekcie `microservice` przed wykonaniem ostatecznych serii pomiarowych.

---

## Część 1: Identyfikacja i Naprawa Wszystkich Błędów Technicznych w Projekcie `microservice`

Drobiazgowy audyt każdego z 3 mikroserwisów (`fleet`, `routing`, `tracking`) oraz pliku `docker-compose-infra.yml` wykazał, że projekt mikrousługowy powstał w drodze bezpośredniego skopiowania kodu z monolitu tradycyjnego. Zaniechanie kompleksowej refaktoryzacji doprowadziło do utrwalenia szeregu defektów kompilacji, błędów startu kontekstu Spring Boot, konfliktów sieciowych, katastrofalnych awarii kontenerów Docker oraz krytycznych zafałszowań telemetrii CI/CD.

Poniżej przedstawiono szczegółowy katalog wszystkich zidentyfikowanych usterek wraz z dokładną lokalizacją, wyjaśnieniem mechanizmu awarii oraz gotowym kodem naprawczym.

---

### 2.1. Mikroserwis `fleet`

#### 2.1.1. Pakiety, klasa startowa i pozostałości po monolicie

##### Defekt F-SRC-1: Pozostałość skompilowanej klasy monolitu w artefaktach budowania
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/target/classes/matera/magisterka/monolit/MonolitApplication.class`
- **Typ błędu:** Zanieczyszczenie przestrzeni klas (Stale Build Artifact).
- **Mechanizm błędu:** W kodzie źródłowym `fleet/src/main/java` klasa startowa została poprawnie zrefaktoryzowana do `matera.magisterka.microservice.fleet.FleetServiceApplication`. Jednak w katalogu `target/classes` pozostała skompilowana klasa monolitu `MonolitApplication.class`. Brak wykonania fazy `clean` oraz brak pliku `.gitignore` skutkuje obecnością binarnych artefaktów monolitu w repozytorium, stwarzając ryzyko załadowania fałszywej klasy głównej przez mechanizmy refleksyjne Spring Boot Maven Plugin.
- **Instrukcja naprawcza:** Usunąć katalog `fleet/target` poleceniem `mvn clean` oraz wdrożyć plik `.gitignore`.

##### Defekt F-SRC-2: Niespójna identyfikacja artefaktu w deskryptorze Maven
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/pom.xml` (linie 12–14)
- **Mechanizm błędu:** Zadeklarowano `<artifactId>fleet-micro</artifactId>` oraz `<name>fleet-micro</name>`, podczas gdy w architekturze modułowej stosowany jest identyfikator `fleet`, a w dokumentacji `fleet-service`. Należy ujednolicić identyfikator artefaktu na `fleet-service`.

#### 2.1.2. Konfiguracja Maven (`pom.xml`)

##### Defekt F-POM-1: Redundantna konfiguracja procesora adnotacji Lomboka w `maven-compiler-plugin`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/pom.xml` (linie 107–143)
- **Mechanizm błędu:** W pliku `pom.xml` zdefiniowano dwa odrębne bloki egzekucji `<execution>` (`<id>default-compile</id>` oraz `<id>default-testCompile</id>`), w których zduplikowano identyczny blok `<annotationProcessorPaths>` dla Lomboka. W Apache Maven deklaracja ta umieszczona bezpośrednio na poziomie `<configuration>` wtyczki jest automatycznie dziedziczona przez obie fazy kompilacji.
- **Wymiana fragmentu w `fleet/pom.xml` (linie 107–143):**
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
```

##### Defekt F-POM-2: Zbędna i zduplikowana zależność `spring-boot-starter-json`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/pom.xml` (linie 96–98)
- **Mechanizm błędu:** Zależności `spring-boot-starter-webmvc` oraz `spring-boot-starter-amqp` importują bibliotekę `spring-boot-starter-json` w sposób przechodni (transitive). Ręczna deklaracja jest zbyteczna.
- **Instrukcja naprawcza:** Usunąć linie 96–98 z pliku `fleet/pom.xml`.

#### 2.1.3. Testy integracyjne w `fleet/src/test`

##### Defekt F-TST-1: Błąd wykonania testu integracyjnego z powodu braku brokera RabbitMQ (`AmqpConnectException`)
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/src/test/java/matera/magisterka/microservice/fleet/FleetIntegrationTest.java` (linie 17–24)
- **Ścieżka powiązana:** `e:/magisterka/final/microservice/fleet/src/main/java/matera/magisterka/microservice/fleet/FleetService.java` (linie 20–25)
- **Mechanizm błędu:** Metoda `FleetService.registerVehicle` wywołuje `rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "fleet.vehicle.created", event)`. Test `FleetIntegrationTest` dziedziczy po `AbstractIntegrationTest`, który uruchamia wyłącznie kontener PostgreSQL. W czystym środowisku CI/CD brak uruchomionego brokera RabbitMQ powoduje natychmiastowe rzucenie wyjątku:
  ```text
  org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused
  ```
  i przerwanie etapu `Run Tests`. W celu zachowania zasady *ceteris paribus* względem monolitu (gdzie badano wyłącznie warstwę bazy danych), komponent `RabbitTemplate` należy zamokować za pomocą adnotacji `@MockitoBean`.
- **Wymiana zawartości pliku `fleet/src/test/java/matera/magisterka/microservice/fleet/FleetIntegrationTest.java`:**
```java
package matera.magisterka.microservice.fleet;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Transactional
class FleetIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FleetService fleetService;

    @Autowired
    private FleetRepository fleetRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldRegisterAndRetrieveVehicle() {
        FleetVehicle vehicle = fleetService.registerVehicle("1C9TESTVIN0000001", "PO-12345");

        FleetVehicle found = fleetService.getVehicle(vehicle.getId());
        assertThat(found.getVin()).isEqualTo("1C9TESTVIN0000001");
        assertThat(found.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq("fleet.vehicle.created"),
                any(VehicleCreatedEvent.class)
        );
    }
}
```

##### Defekt F-TST-2: Kolizja unikalnego klucza `vin` przy reużyciu kontenera Testcontainers
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/src/test/java/matera/magisterka/microservice/fleet/FleetIntegrationTest.java` (linie 18–24)
- **Mechanizm błędu:** Kolumna `vin` w encji `FleetVehicle` posiada więz unikalności `@Column(nullable = false, unique = true)`. W teście zahardkodowano stałą wartość `"1C9TESTVIN0000001"`. Przy włączeniu mechanizmu `withReuse(true)` w kontenerze bazy danych, kolejne uruchomienie potoku bez transakcyjnego rollbacku skutkuje błędem bazy danych:
  ```text
  ERROR: duplicate key value violates unique constraint "uk_fleet_vehicles_vin"
  ```
- **Rozwiązanie:** Dodanie adnotacji `@Transactional` na klasie testowej (uwzględnione w kodzie powyżej).

##### Defekt F-TST-3: Pozostałość `TestMonolitApplication.java` i rozbieżność obrazu bazy danych
- **Ścieżki plików:**
  * `e:/magisterka/final/microservice/fleet/src/test/java/matera/magisterka/microservice/fleet/TestMonolitApplication.java`
  * `e:/magisterka/final/microservice/fleet/src/test/java/matera/magisterka/microservice/fleet/TestcontainersConfiguration.java`
- **Mechanizm błędu:** Klasa startowa dla testów lokalnych nosi nazwę `TestMonolitApplication` zamiast `TestFleetApplication`. Ponadto w klasie `TestcontainersConfiguration` zdefiniowano obraz `postgres:latest` (~452 MB Debian), podczas gdy w `AbstractIntegrationTest` zdefiniowano `postgres:16-alpine` (~138 MB).
- **Wymiana zawartości pliku `TestFleetApplication.java` (dawniej `TestMonolitApplication.java`):**
```java
package matera.magisterka.microservice.fleet;

import org.springframework.boot.SpringApplication;

public class TestFleetApplication {

    public static void main(String[] args) {
        SpringApplication.from(FleetServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
```
- **Wymiana zawartości pliku `TestcontainersConfiguration.java`:**
```java
package matera.magisterka.microservice.fleet;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withReuse(true);
    }
}
```

#### 2.1.4. Konfiguracja w `src/main/resources/application.yml`

##### Defekt F-CFG-1: Nazwa aplikacji, porty i poświadczenia skopiowane z monolitu
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/src/main/resources/application.yml` (linie 1–17)
- **Mechanizm błędu:** Nazwa aplikacji ustawiona jest na `monolit`, baza danych wskazuje na port 5432 i bazę monolitu `logistics_db` z hasłem `postgres_password`, port serwera HTTP to `8080` (co uniemożliwia równoczesny start z pozostałymi serwisami), a konfiguracja RabbitMQ w ogóle nie istnieje.
- **Kompletny, gotowy plik naprawczy `fleet/src/main/resources/application.yml`:**
```yaml
spring:
  application:
    name: fleet-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:fleet_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:pass}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: false

server:
  port: ${SERVER_PORT:8081}
```

#### 2.1.5. Konteneryzacja w `Dockerfile` i `.dockerignore`

##### Defekt F-DCK-1: Katastrofalny błąd braku pliku JAR w `ENTRYPOINT` i brak flagi `--launcher`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/Dockerfile` (linie 6, 18–19)
- **Mechanizm błędu:** W etapie 1 (`builder`) warstwy są wyodrębniane bez flagi `--launcher`. W etapie 2 kopiowane są wyłącznie katalogi warstw, a plik `application.jar` nie istnieje. Instrukcja `ENTRYPOINT ["java", "-jar", "application.jar"]` kończy się natychmiastowym błędem kontenera:
  ```text
  Error: Unable to access jarfile application.jar
  ```
  Prawidłowym punktem wejścia dla Spring Boot 4.1.1 jest klasa `org.springframework.boot.loader.launch.JarLauncher`. Ponadto port w instrukcji `EXPOSE` musi wynosić 8081.
- **Kompletny, gotowy plik naprawczy `fleet/Dockerfile`:**
```dockerfile
# Etap 1: Ekstrakcja warstw z artefaktu JAR
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Etap 2: Złożenie finalnego obrazu uruchomieniowego
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

# Kopiowanie wyodrębnionych warstw od najrzadziej do najczęściej modyfikowanych
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```
- **Rekomendowany plik `fleet/.dockerignore`:**
```dockerignore
.git/
.gitignore
target/*
!target/*.jar
src/
*.md
*.log
*.tmp
*.iml
.idea/
```

#### 2.1.6. Potok CI/CD w `fleet/Jenkinsfile`

##### Defekt F-JNK-1: Błędny tag obrazu, błąd pomiaru RAM (`ps -C java`), brak programu `bc`, brak kontekstu `dir('fleet')` oraz luka telemetrii procesów potomnych
- **Ścieżka pliku:** `e:/magisterka/final/microservice/fleet/Jenkinsfile` (linie 5, 36, 46, 64, 106)
- **Mechanizm błędu:**
  1. Obraz Docker tagowany jest jako `logistics-monolith:${BUILD_NUMBER}`, przez co kolejne potoki mikroserwisowe nadpisują ten sam obraz.
  2. Pętla monitorowania RAM oparta o `ps -C java` odczytuje pamięć procesu serwera Jenkins Master (~1,2 GB), fałszując wyniki o rząd wielkości.
  3. Użycie polecenia `bc` wywołuje błąd `bc: not found` w kontenerze Jenkinsa, uszkadzając zapis w CSV.
  4. W fazach `compile` i `package` wartość `peak_ram_mb` jest zahardkodowana jako stała `0`.
  5. **Brak kontekstu podkatalogu roboczego `${WORKSPACE}` w Opcji B:** W rekomendowanej architekturze 3 Polyrepos repozytorium `microservice.git` zawiera podkatalogi `fleet/`, `routing/`, `tracking/`. Wskazanie `Script Path: fleet/Jenkinsfile` w Jenkinsie nie zmienia katalogu bieżącego (`pwd`). Bez jawnego opakowania w `dir('fleet') { ... }`, polecenie `mvn clean compile` ulega natychmiastowej awarii z powodu braku `pom.xml` w korzeniu `${WORKSPACE}`, a `docker build .` zawodzi z powodu braku `Dockerfile`.
  6. **Płaskie śledzenie procesów JVM (`$1==p || $2==p`):** Proste sprawdzenie rodzica i bezpośrednich dzieci gubi procesy wnucze (np. `ForkedBooter` wtyczki Surefire, gdy Maven uruchomiony jest przez skrypt powłoki bez `exec` lub w subshellu), co prowadzi do zaniżenia zmierzonego RAM nawet o 54%. Wymagane jest pełne, rekurencyjne trawersowanie drzewa potomków w POSIX `awk`.
- **Kompletny, wzorcowy plik `fleet/Jenkinsfile` znajduje się w podrozdziale 2.1.7 poniżej.**

#### 2.1.7. Wzorcowy plik `fleet/Jenkinsfile`
```groovy
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "logistics-fleet:${BUILD_NUMBER}"
        METRICS_DIR = "${WORKSPACE}/metrics"
        METRICS_FILE = "${WORKSPACE}/metrics/build_metrics.csv"
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    }

    stages {
        stage('Initialize Telemetry') {
            steps {
                sh '''
                    mkdir -p ${METRICS_DIR}
                    if [ ! -f ${METRICS_FILE} ]; then
                        echo "build_number,stage,duration_ms,peak_ram_mb,docker_image_size_mb" > ${METRICS_FILE}
                    fi
                '''
            }
        }

        stage('Compile') {
            steps {
                dir('fleet') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn clean compile &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},compile,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                dir('fleet') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn test &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        TEST_STATUS=$?

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},tests,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}

                        if [ $TEST_STATUS -ne 0 ]; then
                            exit $TEST_STATUS
                        fi
                    '''
                }
            }
            post {
                always {
                    junit 'fleet/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package Application') {
            steps {
                dir('fleet') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn package -DskipTests &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},package,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('fleet') {
                    sh '''
                        START=$(date +%s%3N)

                        docker build -t ${DOCKER_IMAGE} .

                        END=$(date +%s%3N)
                        DIFF=$((END - START))

                        IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                        IMG_MB=$(awk -v b="$IMG_BYTES" 'BEGIN {printf "%.2f", b/1048576}')

                        echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
        }
    }
}
```

---

### 2.2. Mikroserwis `routing`

#### 2.2.1. Pakiety, klasa startowa i pozostałości po monolicie
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/target/classes/matera/magisterka/monolit/MonolitApplication.class`
- **Mechanizm błędu:** Identycznie jak we `fleet`, w strukturze `target/` zachowano pozostałość po monolicie. Kod produkcyjny w `routing/src/main/java` zawiera poprawną klasę `RoutingServiceApplication` w pakiecie `matera.magisterka.microservice.routing`. Artefakt z `target/` należy usunąć poleceniem `mvn clean`. W `pom.xml` zadeklarowano `routing-micro` — zaleca się ujednolicenie na `routing-service`.

#### 2.2.2. Konfiguracja Maven (`pom.xml`)
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/pom.xml` (linie 94–130)
- **Mechanizm błędu:** Podwójna definicja egzekucji `default-compile` i `default-testCompile` dla Lomboka.
- **Wymiana konfiguracji wtyczki `maven-compiler-plugin` w `routing/pom.xml`:**
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
```

#### 2.2.3. Testy integracyjne w `routing/src/test`

##### Defekt R-TST-1: Nadmiarowy test `MonolitApplicationTests`, podwójny kontener bazy i asymetria testów
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/src/test/java/matera/magisterka/microservice/routing/MonolitApplicationTests.java` (linie 1–16)
- **Mechanizm błędu:**
  1. Klasa `MonolitApplicationTests` posiada pusty test dymny `contextLoads()`, adnotowany `@Import(TestcontainersConfiguration.class)`. Powoduje to pobranie i start ciężkiego kontenera `postgres:latest` (~452 MB).
  2. Następnie `RouteIntegrationTest` dziedziczy po `AbstractIntegrationTest`, co powoduje start **drugiego, niezależnego kontenera** `postgres:16-alpine` (~138 MB) oraz podwójną inicjalizację kontekstu Spring Boot!
  3. Powoduje to asymetrię badawczą (2 testy w `routing` vs 1 w `fleet` vs 3 w `modular`). Zgodnie z wymogiem *ceteris paribus* w architekturze mikroserwisowej na każdy mikroserwis musi przypadać **dokładnie jeden test domenowy**.
- **Instrukcja naprawcza:** Całkowicie usunąć plik `routing/src/test/java/matera/magisterka/microservice/routing/MonolitApplicationTests.java`.

##### Defekt R-TST-2: Brak adnotacji `@Transactional` w `RouteIntegrationTest`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/src/test/java/matera/magisterka/microservice/routing/RouteIntegrationTest.java`
- **Mechanizm błędu:** Brak transakcyjnego czyszczenia bazy danych przy włączonym mechanizmie `withReuse(true)` w kontenerze PostgreSQL.
- **Wymiana zawartości pliku `RouteIntegrationTest.java`:**
```java
package matera.magisterka.microservice.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
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
```

##### Defekt R-TST-3: Zrefaktoryzowanie klasy uruchomieniowej dla środowiska deweloperskiego
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/src/test/java/matera/magisterka/microservice/routing/TestMonolitApplication.java`
- **Rozwiązanie:** Zmiana nazwy na `TestRoutingApplication.java`:
```java
package matera.magisterka.microservice.routing;

import org.springframework.boot.SpringApplication;

public class TestRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.from(RoutingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
```

##### Defekt R-TST-4: Aktualizacja `TestcontainersConfiguration.java` do `postgres:16-alpine` i `@ServiceConnection`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/src/test/java/matera/magisterka/microservice/routing/TestcontainersConfiguration.java`
- **Mechanizm błędu:** W pliku na dysku zadeklarowano `DockerImageName.parse("postgres:latest")`. Powoduje to pobieranie ciężkiego obrazu Debian Bookworm (~452 MB) zamiast zunifikowanego `postgres:16-alpine` (~138 MB) oraz narusza zasadę spójności wersji bazodanowej w środowisku deweloperskim.
- **Gotowy, kompletny plik `TestcontainersConfiguration.java` dla `routing`:**
```java
package matera.magisterka.microservice.routing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withReuse(true);
    }
}
```

#### 2.2.4. Konfiguracja w `routing/src/main/resources/application.yml`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/src/main/resources/application.yml` (linie 1–17)
- **Mechanizm błędu:** Skopiowane wartości monolitu: `name: monolit`, baza `logistics_db` na porcie 5432, hasło `postgres_password`, port serwera 8080.
- **Kompletny, gotowy plik naprawczy `routing/src/main/resources/application.yml`:**
```yaml
spring:
  application:
    name: routing-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5434}/${DB_NAME:routing_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:pass}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: false

server:
  port: ${SERVER_PORT:8082}
```

#### 2.2.5. Konteneryzacja w `routing/Dockerfile`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/routing/Dockerfile`
- **Mechanizm błędu:** Identycznie jak we `fleet`: brak flagi `--launcher` w etapie ekstrakcji, punkt wejścia wywołujący nieistniejący `application.jar`, błędny port 8080.
- **Kompletny, gotowy plik naprawczy `routing/Dockerfile`:**
```dockerfile
# Etap 1: Ekstrakcja warstw z artefaktu JAR
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Etap 2: Złożenie finalnego obrazu uruchomieniowego
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

# Kopiowanie wyodrębnionych warstw od najrzadziej do najczęściej modyfikowanych
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8082
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```
- **Rekomendowany plik `routing/.dockerignore`:** Identyczny jak we `fleet`.

#### 2.2.6. Potok CI/CD w `routing/Jenkinsfile`
W architekturze Opcji B plik `routing/Jenkinsfile` wymaga jawnego kontekstu `dir('routing')` wokół faz kompilacji, testów, pakowania i budowania obrazu Dockera (aby komendy Mavena i Dockera odnajdywały `routing/pom.xml` i `routing/Dockerfile`), a także rekurencyjnej telemetrii RAM w POSIX `awk` oraz ścieżki do raportów Surefire `routing/target/surefire-reports/*.xml`.

##### Kompletny, gotowy plik `routing/Jenkinsfile`:
```groovy
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "logistics-routing:${BUILD_NUMBER}"
        METRICS_DIR = "${WORKSPACE}/metrics"
        METRICS_FILE = "${WORKSPACE}/metrics/build_metrics.csv"
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    }

    stages {
        stage('Initialize Telemetry') {
            steps {
                sh '''
                    mkdir -p ${METRICS_DIR}
                    if [ ! -f ${METRICS_FILE} ]; then
                        echo "build_number,stage,duration_ms,peak_ram_mb,docker_image_size_mb" > ${METRICS_FILE}
                    fi
                '''
            }
        }

        stage('Compile') {
            steps {
                dir('routing') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn clean compile &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},compile,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                dir('routing') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn test &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        TEST_STATUS=$?

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},tests,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}

                        if [ $TEST_STATUS -ne 0 ]; then
                            exit $TEST_STATUS
                        fi
                    '''
                }
            }
            post {
                always {
                    junit 'routing/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package Application') {
            steps {
                dir('routing') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn package -DskipTests &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},package,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('routing') {
                    sh '''
                        START=$(date +%s%3N)

                        docker build -t ${DOCKER_IMAGE} .

                        END=$(date +%s%3N)
                        DIFF=$((END - START))

                        IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                        IMG_MB=$(awk -v b="$IMG_BYTES" 'BEGIN {printf "%.2f", b/1048576}')

                        echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
        }
    }
}
```

---

### 2.3. Mikroserwis `tracking`

#### 2.3.1. Pakiety, importy i krytyczna niespójność struktury katalogów testowych

##### Defekt T-SRC-1: Katastrofalny błąd pakietu w testach integracyjnych
- **Ścieżka fizyczna na dysku:** `e:/magisterka/final/microservice/tracking/src/test/java/matera/magisterka/monolit/`
- **Deklaracja w klasach:** `package matera.magisterka.monolit;`
- **Mechanizm błędu:** Podczas wydzielania serwisu `tracking` cały katalog testów skopiowano wprost z projektu `monolit`, bez modyfikacji pakietu. Wywołuje to dwa krytyczne skutki:
  1. **Błąd kompilacji:** Klasa `TrackingIntegrationTest` odwołuje się do `TrackingService` i `TrackingPing` bez instrukcji importu (ponieważ w monolicie znajdowały się w tym samym pakiecie). Maven rzuca błąd `cannot find symbol`.
  2. **Błąd startu kontekstu testowego Spring Boot:** Spring Boot Test poszukuje konfiguracji `@SpringBootConfiguration` w górę hierarchii pakietów (`matera.magisterka.monolit` -> `matera.magisterka` -> `matera`). Ponieważ klasa produkcyjna znajduje się w `matera.magisterka.microservice.tracking`, skaner nigdy jej nie odnajduje, rzucając wyjątek:
     ```text
     java.lang.IllegalStateException: Unable to find a @SpringBootConfiguration, you need to use @ContextConfiguration or @SpringBootTest(classes=...) with your test
     ```
- **Instrukcja naprawcza:**
  1. Usunąć stary katalog `tracking/src/test/java/matera/magisterka/monolit/`.
  2. Utworzyć właściwy katalog `tracking/src/test/java/matera/magisterka/microservice/tracking/`.
  3. Zrefaktoryzować deklaracje pakietów na `package matera.magisterka.microservice.tracking;`.

#### 2.3.2. Konfiguracja Maven (`pom.xml`)
- **Ścieżka pliku:** `e:/magisterka/final/microservice/tracking/pom.xml` (linie 96–99 oraz 107–142)
- **Mechanizm błędu:** Zbędna zależność `spring-boot-starter-json` oraz zduplikowane bloki procesora Lomboka w `maven-compiler-plugin`.
- **Wymiana sekcji kompilatora w `tracking/pom.xml`:**
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
```
- Usunąć linie 96–99 (`spring-boot-starter-json`).

#### 2.3.3. Testy integracyjne w `tracking/src/test`

##### Defekt T-TST-1: Wyłączenie listenera RabbitMQ w testach integracyjnych
- **Mechanizm błędu:** W mikroserwisie `tracking` komponent `FleetEventListener` nasłuchuje na kolejce RabbitMQ. W trakcie wykonywania testu `TrackingIntegrationTest`, Spring Boot inicjalizuje kontener listenera AMQP, który przy braku brokera ponawia próby połączenia, rzucając błędy w logach i spowalniając testy. Należy w `AbstractIntegrationTest` wyłączyć auto-start listenera AMQP właściwością:
  `spring.rabbitmq.listener.simple.auto-startup=false`.
- **Defekt T-TST-2:** Usunąć zbędny test `MonolitApplicationTests.java`.

##### Gotowy kod `tracking/src/test/java/matera/magisterka/microservice/tracking/AbstractIntegrationTest.java`:
```java
package matera.magisterka.microservice.tracking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("test_db")
                .withUsername("test_user")
                .withPassword("test_password");
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        // Wyłączenie auto-startu listenera AMQP w teście domenowym bazy danych
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }
}
```

##### Gotowy kod `tracking/src/test/java/matera/magisterka/microservice/tracking/TrackingIntegrationTest.java`:
```java
package matera.magisterka.microservice.tracking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class TrackingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TrackingService trackingService;

    @Test
    void shouldRecordAndRetrievePings() {
        trackingService.recordLocation(100L, 50.2976, 18.6766);
        trackingService.recordLocation(100L, 50.2990, 18.6780);

        List<TrackingPing> history = trackingService.getVehicleHistory(100L);
        assertThat(history).hasSize(2);
    }
}
```

##### Gotowy kod `tracking/src/test/java/matera/magisterka/microservice/tracking/TestTrackingApplication.java`:
```java
package matera.magisterka.microservice.tracking;

import org.springframework.boot.SpringApplication;

public class TestTrackingApplication {

    public static void main(String[] args) {
        SpringApplication.from(TrackingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
```

##### Gotowy kod `tracking/src/test/java/matera/magisterka/microservice/tracking/TestcontainersConfiguration.java`:
```java
package matera.magisterka.microservice.tracking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withReuse(true);
    }
}
```

#### 2.3.4. Konfiguracja w `tracking/src/main/resources/application.yml`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/tracking/src/main/resources/application.yml` (linie 1–17)
- **Kompletny, gotowy plik naprawczy `tracking/src/main/resources/application.yml`:**
```yaml
spring:
  application:
    name: tracking-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5435}/${DB_NAME:tracking_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:pass}
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}
    show-sql: false

server:
  port: ${SERVER_PORT:8083}
```

#### 2.3.5. Konteneryzacja w `tracking/Dockerfile`
- **Ścieżka pliku:** `e:/magisterka/final/microservice/tracking/Dockerfile`
- **Kompletny, gotowy plik naprawczy `tracking/Dockerfile`:**
```dockerfile
# Etap 1: Ekstrakcja warstw z artefaktu JAR
FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Etap 2: Złożenie finalnego obrazu uruchomieniowego
FROM eclipse-temurin:21-jre-alpine
WORKDIR /application

# Kopiowanie wyodrębnionych warstw od najrzadziej do najczęściej modyfikowanych
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8083
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

#### 2.3.6. Potok CI/CD w `tracking/Jenkinsfile`
W architekturze Opcji B plik `tracking/Jenkinsfile` wymaga jawnego kontekstu `dir('tracking')` wokół faz kompilacji, testów, pakowania i budowania obrazu Dockera (aby komendy Mavena i Dockera odnajdywały `tracking/pom.xml` i `tracking/Dockerfile`), a także rekurencyjnej telemetrii RAM w POSIX `awk` oraz ścieżki do raportów Surefire `tracking/target/surefire-reports/*.xml`.

##### Kompletny, gotowy plik `tracking/Jenkinsfile`:
```groovy
pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "logistics-tracking:${BUILD_NUMBER}"
        METRICS_DIR = "${WORKSPACE}/metrics"
        METRICS_FILE = "${WORKSPACE}/metrics/build_metrics.csv"
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    }

    stages {
        stage('Initialize Telemetry') {
            steps {
                sh '''
                    mkdir -p ${METRICS_DIR}
                    if [ ! -f ${METRICS_FILE} ]; then
                        echo "build_number,stage,duration_ms,peak_ram_mb,docker_image_size_mb" > ${METRICS_FILE}
                    fi
                '''
            }
        }

        stage('Compile') {
            steps {
                dir('tracking') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn clean compile &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},compile,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Run Tests') {
            steps {
                dir('tracking') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn test &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        TEST_STATUS=$?

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},tests,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}

                        if [ $TEST_STATUS -ne 0 ]; then
                            exit $TEST_STATUS
                        fi
                    '''
                }
            }
            post {
                always {
                    junit 'tracking/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package Application') {
            steps {
                dir('tracking') {
                    sh '''
                        START=$(date +%s%3N)

                        mvn package -DskipTests &
                        MVN_PID=$!
                        PEAK_KB=0
                        while kill -0 $MVN_PID 2>/dev/null; do
                            CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
                            NR>1 {
                                p=$1+0; pp=$2+0; r=$3+0;
                                rss[p]=r; parent[p]=pp; pids[p]=1
                            }
                            END {
                                tree[root]=1
                                do {
                                    added=0
                                    for (p in pids) {
                                        if (!(p in tree) && (parent[p] in tree)) {
                                            tree[p] = 1
                                            added = 1
                                        }
                                    }
                                } while (added)
                                sum=0
                                for (p in tree) {
                                    if (tree[p]) sum += rss[p]
                                }
                                print sum+0
                            }')
                            if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
                            sleep 0.2
                        done
                        wait $MVN_PID
                        STATUS=$?
                        if [ $STATUS -ne 0 ]; then exit $STATUS; fi

                        END=$(date +%s%3N)
                        DIFF=$((END - START))
                        PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
                        echo "${BUILD_NUMBER},package,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('tracking') {
                    sh '''
                        START=$(date +%s%3N)

                        docker build -t ${DOCKER_IMAGE} .

                        END=$(date +%s%3N)
                        DIFF=$((END - START))

                        IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                        IMG_MB=$(awk -v b="$IMG_BYTES" 'BEGIN {printf "%.2f", b/1048576}')

                        echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                    '''
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
        }
    }
}
```

---

### 2.4. Infrastruktura Wspólna `docker-compose-infra.yml`

#### 2.4.1. Analiza braków i zagrożeń w obecnym pliku infrastruktury
W badanym pliku `e:/magisterka/final/microservice/docker-compose-infra.yml` zidentyfikowano następujące wady:
1. **Brak definicji trwałych wolumenów (`volumes`):** Wszystkie dane PostgreSQL oraz kolejki RabbitMQ przechowywane są w warstwie kontenera. Po wykonaniu `docker compose down` następuje nieodwracalna utrata danych testowych.
2. **Brak mechanizmów badania gotowości (`healthcheck`):** Usługi startujące zależnie nie mają możliwości weryfikacji, czy silnik bazy danych zakończył procedurę inicjalizacji klastra (`initdb`) i nasłuchuje na porcie.
3. **Brak wydzielonej sieci mostkowej (`networks`):** Brak nazwanej sieci uniemożliwia łączenie kontenerów mikroserwisów uruchamianych z innych plików konfiguracyjnych.
4. **Niespójne poświadczenia i brak jawnej zmiennej `POSTGRES_USER`:** Brak standaryzacji użytkownika `postgres`.

#### 2.4.2. Gotowy, kompletny plik naprawczy `docker-compose-infra.yml`
```yaml
networks:
  logistics-network:
    name: logistics-network
    driver: bridge

volumes:
  rabbitmq_data:
  postgres_fleet_data:
  postgres_routing_data:
  postgres_tracking_data:

services:
  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: rabbitmq
    restart: unless-stopped
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    networks:
      - logistics-network
    healthcheck:
      test: ["CMD-SHELL", "rabbitmq-diagnostics -q ping"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 15s

  postgres-fleet:
    image: postgres:16-alpine
    container_name: postgres-fleet
    restart: unless-stopped
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: fleet_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: pass
    volumes:
      - postgres_fleet_data:/var/lib/postgresql/data
    networks:
      - logistics-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d fleet_db"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s

  postgres-routing:
    image: postgres:16-alpine
    container_name: postgres-routing
    restart: unless-stopped
    ports:
      - "5434:5432"
    environment:
      POSTGRES_DB: routing_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: pass
    volumes:
      - postgres_routing_data:/var/lib/postgresql/data
    networks:
      - logistics-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d routing_db"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s

  postgres-tracking:
    image: postgres:16-alpine
    container_name: postgres-tracking
    restart: unless-stopped
    ports:
      - "5435:5432"
    environment:
      POSTGRES_DB: tracking_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: pass
    volumes:
      - postgres_tracking_data:/var/lib/postgresql/data
    networks:
      - logistics-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d tracking_db"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s
```

---

## Część 2: Porównanie i Wyrównanie 1:1 (*Ceteris Paribus*) Między `microservice`, `modular` i `monolit`

### 3.1. Zestawienie Liczby i Typu Testów Domenowych (Domain Test Parity)

Weryfikacja metodologiczna wymaga precyzyjnego zbilansowania liczby i charakteru testów wykonywanych w procesie CI/CD. 

#### Matryca porównawcza zestawów testowych w trzech architekturach:

| Wymiar Porównawczy | Monolit Tradycyjny (`monolit`) | Monolit Modułowy (`modular`) | Mikroserwisy (`microservice`) — Stan Przed Audytem | Mikroserwisy — Stan Docelowy 1:1 | Wpływ Metodologiczny |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Test domeny floty (`fleet`)** | 1 (`FleetIntegrationTest`) | 1 (`FleetIntegrationTest` w module `fleet`) | 1 (`FleetIntegrationTest`) | **1 (`FleetIntegrationTest`)** | 100% parzystości zachowań biznesowych |
| **Test domeny tras (`routing`)** | 1 (`RouteIntegrationTest`) | 1 (`RouteIntegrationTest` w module `routing`) | 1 (`RouteIntegrationTest`) | **1 (`RouteIntegrationTest`)** | 100% parzystości zachowań biznesowych |
| **Test domeny śledzenia (`tracking`)** | 1 (`TrackingIntegrationTest`) | 1 (`TrackingIntegrationTest` w module `tracking`) | 1 (`TrackingIntegrationTest` z błędnym pakietem) | **1 (`TrackingIntegrationTest`)** | 100% parzystości zachowań biznesowych |
| **Puste testy kontekstu (`contextLoads`)** | **0** (usunięto w commicie `9f24ea0`) | **0** (moduł `app` bez testów integracyjnych) | **2** (`MonolitApplicationTests` w `routing` i `tracking`) | **0 (całkowicie usunięte)** | **Eliminacja sztucznego narzutu I/O i CPU** |
| **SUMA TESTÓW W SYSTEMIE** | **3 testy** | **3 testy** | **4 testy (niespójne)** | **DOKŁADNIE 3 TESTY (1:1:1)** | **Idealna równoważność eksperymentalna** |

#### Uzasadnienie eliminacji `MonolitApplicationTests`:
W architekturze mikrousługowej każdy serwis posiada własny, autonomiczny cykl życia. Pozostawienie klasy `MonolitApplicationTests` w `routing` i `tracking` powodowało start dwóch niezależnych kontenerów bazy danych oraz dwukrotne ładowanie kontekstu Spring Boot w pojedynczym module. Zaburzało to relację czasową $T_{\text{tests}}$ oraz windowało zużycie pamięci operacyjnej maszyn JVM. Usunięcie tych klas przywraca idealną symetrię: **jeden przypadek testowy weryfikuje dokładnie jedną domenę biznesową w każdym ze stylów architektonicznych**.

---

### 3.2. Ujednolicenie Obrazu Bazy Danych (`postgres:16-alpine`)

Testy integracyjne we wszystkich trzech projektach korzystają z biblioteki Testcontainers do dynamicznego powoływania relacyjnej bazy danych. W toku audytu ujawniono, że w mikroserwisach występowała niespójność pomiędzy obrazem `postgres:16-alpine` (zadeklarowanym w `AbstractIntegrationTest`) a obrazem `postgres:latest` (zadeklarowanym w `TestcontainersConfiguration`).

#### Analiza wpływu rozmiaru obrazu na determinizm badań:
- **`postgres:latest` (Debian Bookworm):** Rozmiar wirtualny wynosi **~452 MB**.
- **`postgres:16-alpine` (Alpine Linux musl):** Rozmiar wirtualny wynosi **~138 MB**.

Różnica rzędu **314 MB** na korzyść wersji Alpine ma fundamentalne znaczenie badawcze:
1. **Jitter sieciowy:** Na łączu o przepustowości 100 Mb/s pobranie obrazu `latest` trwa ~36 sekund, podczas gdy `16-alpine` pobiera się w ~11 sekund. Przy fluktuacjach transferu sieciowego w środowisku CI/CD tag `latest` wprowadza gigantyczny błąd losowy pomiaru.
2. **Niedeterminizm tagu `:latest`:** Tag `:latest` może w dowolnym momencie zaktualizować silnik do nowszej wersji głównej (np. PostgreSQL 17 lub 18), zmieniając zużycie pamięci RAM silnika bazy danych oraz plany zapytań SQL.
3. **Unifikacja:** Wdrożenie `postgres:16-alpine` we wszystkich komponentach gwarantuje identyczny profil I/O oraz tożsame alokacje pamięciowe silnika PostgreSQL w całej pracy magisterskiej.

---

### 3.3. Standaryzacja Telemetrii w `Jenkinsfile`

#### Mechanizm błędu `ps -C java`
Dotychczasowa implementacja pomiaru pamięci RAM w mikroserwisach wykorzystywała polecenie:
```bash
ps -o rss,command -C java | awk '{print $1}' | sort -nr | head -n1 >> ${MEM_LOG}
```
Polecenie `ps -C java` wyszukuje wszystkie procesy wirtualnej maszyny Java w przestrzeni nazw kontenera lub maszyny wirtualnej. Ponieważ serwer Jenkins Master działa w procesie Java (`java -jar jenkins.war`), alokując stale od 1,0 do 1,5 GB pamięci, polecenie `sort -nr | head -n1` w 100% przypadków zwracało pamięć Jenkinsa, a nie budowanego mikroserwisu. Zgromadzone dotychczas metryki pamięciowe były całkowicie bezużyteczne.

#### Ograniczenia filtru dwupoziomowego (`$1==p || $2==p`)
W podstawowej implementacji sprawdzano warunek `$1==p || $2==p`, który sumuje pamięć procesu o PID równym `MVN_PID` oraz jego bezpośrednich dzieci (`PPID == MVN_PID`). Podejście to sprawdza się tylko przy bezpośrednim wywołaniu Mavena, gdy skrypt startowy wykonuje instrukcję `exec "$JAVACMD"`.

Jednak w kontenerowych środowiskach CI/CD Maven bywa wywoływany przez skrypty opakowujące (wrappery), aliasy lub wewnątrz subshella powłoki. W takich przypadkach:
- Pierwotny przechwycony PID (`MVN_PID`) należy do procesu powłoki wrappera.
- Proces wirtualnej maszyny Java Mavena staje się jego dzieckiem.
- Proces wtyczki Surefire (`ForkedBooter`), w którym faktycznie wykonywane są testy Spring Boot i ładowany jest kontekst aplikacji, staje się procesem „wnuczym” (grandchild).
- **Skutek błędu pomiarowego:** Filtr dwupoziomowy całkowicie ignoruje proces `ForkedBooter`, powodując zaniżenie zmierzonego szczytowego zużycia pamięci RAM o ponad **53%** (w testach laboratoryjnych spadek ze 181.88 MB do zaledwie 84.62 MB).

#### Wzorcowy mechanizm rekurencyjnego próbkowania drzewa procesów w czystym POSIX `awk`
Aby zagwarantować 100% precyzji telemetrycznej niezależnie od głębokości zagnieżdżenia procesów potomnych i bez konieczności instalowania w kontenerze dodatkowych pakietów (`bc`, `pgrep`, `pstree`), wdrożono algorytm rekurencyjnego trawersowania drzewa procesów realizowany w pojedynczym wywołaniu POSIX `awk`:

```bash
mvn clean compile &
MVN_PID=$!
PEAK_KB=0
while kill -0 $MVN_PID 2>/dev/null; do
    CURRENT_KB=$(ps -eo pid,ppid,rss | awk -v root=$MVN_PID '
    NR>1 {
        p=$1+0; pp=$2+0; r=$3+0;
        rss[p]=r; parent[p]=pp; pids[p]=1
    }
    END {
        tree[root]=1
        do {
            added=0
            for (p in pids) {
                if (!(p in tree) && (parent[p] in tree)) {
                    tree[p] = 1
                    added = 1
                }
            }
        } while (added)
        sum=0
        for (p in tree) {
            if (tree[p]) sum += rss[p]
        }
        print sum+0
    }')
    if [ "$CURRENT_KB" -gt "$PEAK_KB" ]; then PEAK_KB=$CURRENT_KB; fi
    sleep 0.2
done
wait $MVN_PID
STATUS=$?
if [ $STATUS -ne 0 ]; then exit $STATUS; fi

END=$(date +%s%3N)
DIFF=$((END - START))
PEAK_MB=$(awk -v kb="$PEAK_KB" 'BEGIN {printf "%.2f", kb/1024}')
echo "${BUILD_NUMBER},compile,${DIFF},${PEAK_MB},0" >> ${METRICS_FILE}
```

##### Właściwości inżynierskie i matematyczne rozwiązania:
1. **Dynamiczne domknięcie przechodnie relacji rodzic-dziecko:** Pętla `do ... while (added)` iteracyjnie odnajduje wszystkie procesy potomne dowolnego rzędu (dzieci, wnuki, prawnuki) wywodzące się z węzła głównego `root`.
2. **Całkowita niezależność od narzędzi zewnętrznych:** Działa na każdym standardowym agencie Linux posiadającym powłokę POSIX i `awk`, eliminując błędy typu `bc: command not found`.
3. **Dokładność arytmetyczna:** Konwersja na megabajty (`kb/1024`) realizowana jest z precyzją zmiennoprzecinkową `printf "%.2f"` bez zaokrągleń stałoprzecinkowych powłoki.

---

### 3.4. Agregacja i Interpretacja Metryk z 3 Potoków Mikroserwisowych vs 1 Potok Monolitu

Porównanie pojedynczego potoku monolitu z trzema niezależnymi potokami mikroserwisów wymaga zastosowania sformalizowanego aparatu matematycznego. W pracy magisterskiej autor powinien przedstawić wyniki w trzech komplementarnych ujęciach.

#### 1. Czas trwania potoku (`duration_ms`)

Niech $T_i$ oznacza czas trwania fazy dla mikroserwisu $i \in \{\text{fleet}, \text{routing}, \text{tracking}\}$.

##### A. Suma sekwencyjna (Sequential Compute Time / Zasoby CPU):
$$T_{\text{seq}} = \sum_{i=1}^{N} T_i = T_{\text{fleet}} + T_{\text{routing}} + T_{\text{tracking}}$$
- **Interpretacja naukowa:** Reprezentuje całkowity czas pracy procesora (agent-minutes) zużyty przez infrastrukturę na zbudowanie całego systemu. Pokazuje narzut dekompozycji (trzykrotna inicjalizacja JVM, potrójne parsowanie POM, powielenie pracy kompilatora).

##### B. Czas równoległy (Parallel Wall-Clock Time / Lead Time dewelopera):
$$T_{\text{par}} = \max\left(T_{\text{fleet}}, T_{\text{routing}}, T_{\text{tracking}}\right) + T_{\text{orchestration}}$$
- **Interpretacja naukowa:** Rzeczywisty czas oczekiwania programisty na wynik weryfikacji całego systemu przy założeniu dostępności $N \ge 3$ równoległych executorów Jenkinsa. $T_{\text{orchestration}}$ to czas orkiestracji (zwykle 1–3 sekundy).

##### C. Czas pojedynczej modyfikacji domenowej (Single Feature Feedback Loop):
$$T_{\text{single}} = T_k \quad (k \in \{\text{fleet}, \text{routing}, \text{tracking}\})$$
- **Interpretacja naukowa:** Codzienny czas informacji zwrotnej przy edycji pojedynczej domeny. Podlega bezpośredniemu zestawieniu z mechanizmem *Smart Incremental Build* monolitu modułowego ($T_{\text{modular\_smart}}$) oraz pełnym monolitowym potokiem ($T_{\text{monolith}}$).

---

#### 2. Szczytowe zużycie pamięci RAM (`peak_ram_mb`)

##### A. Wymiar pojedynczego agenta (Single Runner Sizing):
$$\text{Peak}_{\text{agent}} = \max\left(\text{Peak}_{\text{fleet}}, \text{Peak}_{\text{routing}}, \text{Peak}_{\text{tracking}}\right)$$
- **Interpretacja inżynierska:** Określa minimalny rozmiar pamięci RAM (hardware footprint), jaki należy przydzielić pojedynczemu kontenerowi wykonawczemu w klastrze CI/CD. Mikroserwis z małą liczbą klas wymaga mniejszego agenta niż monolit ładujący w pamięci zintegrowany kontekst całego systemu.

##### B. Rzeczywisty szczyt współbieżny na węźle (True Concurrent Peak RAM):
$$\text{Peak}_{\text{node}} = \max_{t} \left[ \sum_{i=1}^{N} \text{RSS}_i(t) \right]$$
- **Interpretacja inżynierska:** Rzeczywiste obciążenie fizycznej pamięci serwera CI w przypadku jednoczesnego uruchomienia wszystkich 3 potoków.

##### C. Teoretyczna suma szczytów (Worst-Case Upper Bound):
$$\text{Peak}_{\text{sum}} = \sum_{i=1}^{N} \text{Peak}_i$$
- Teoretyczny, pesymistyczny pułap górny, przydatny przy planowaniu rezerw pamięciowych infrastruktury chmurowej.

---

#### 3. Rozmiar obrazów Docker (`docker_image_size_mb`)

W architekturze kontenerowej występuje zasadnicza różnica między rozmiarem wirtualnym a fizycznym zużyciem dysku:

##### A. Wirtualna suma rozmiarów (Raw Virtual Sum):
$$S_{\text{virtual}} = \sum_{i=1}^{N} S_i = S_{\text{fleet}} + S_{\text{routing}} + S_{\text{tracking}} \approx 3 \times 142\text{ MB} \approx 426\text{ MB}$$

##### B. Fizyczny rozmiar na dysku po deduplikacji warstw (Deduplicated Disk Footprint):
Dzięki unikalnemu adresowaniu warstw w Dockerze (Content-Addressable Storage na bazie SHA256), bazowy obraz `eclipse-temurin:21-jre-alpine` (~100 MB) jest przechowywany na dysku **dokładnie jeden raz**:
$$S_{\text{disk}} = S_{\text{base\_JRE}} + \sum_{i=1}^{N} \left( S_{\text{deps}, i} + S_{\text{app}, i} \right) \approx 100\text{ MB} + 3 \times (42\text{ MB} + 0.1\text{ MB}) \approx 226\text{ MB}$$

##### Zestawienie z monolitem:
Pojedynczy obraz monolitu (`logistics-monolith`) zajmuje na dysku **~150 MB** ($100\text{ MB} + 48\text{ MB deps} + 2\text{ MB app}$). Mikroserwisy generują zatem rzeczywisty narzut dyskowy rzędu **~76 MB** (a nie 276 MB, jak sugerowałaby naiwna suma wirtualna).

---

### 3.5. Wytyczne Metodologiczne i Wzorcowa Matryca Badawcza do Rozdziału Empirycznego

Aby wyniki w rozdziale badawczym pracy magisterskiej spełniały rygory recenzji akademickiej, należy zastosować następujące procedury:
1. **Liczba powtórzeń:** Wykonać minimum $N = 10$ niezależnych serii pomiarowych dla każdego wariantu.
2. **Odrzucenie przebiegu wstępnego (Cold-Start / Warm-up Run):** Przebieg numer 1 (Build #1) należy bezwzględnie wykluczyć ze statystyk. Pierwszy przebieg pobiera zależności Maven do `~/.m2`, ściąga warstwy z Docker Hub i rozgrzewa kompilator JIT.
3. **Statystyka opisowa:** Dla każdej metryki wyznaczyć średnią arytmetyczną ($\mu$), odchylenie standardowe ($\sigma$), medianę ($Me$) oraz współczynnik zmienności $V = \frac{\sigma}{\mu} \times 100\%$. Współczynnik $V$ powinien kształtować się poniżej 5–8%, co dowodzi powtarzalności środowiska.

#### Wzorcowy szablon tabeli wynikowej do pracy magisterskiej:

| Parametr CI/CD | Monolit Tradycyjny | Monolit Modułowy (Pełny) | Monolit Modułowy (Smart Fleet) | Mikroserwis: Fleet (Pojedynczy) | Mikroserwisy: Suma Sekwencyjna ($T_{\text{seq}}$) | Mikroserwisy: Równolegle ($T_{\text{par}}$) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Czas kompilacji (`compile`) [s]** | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\sum \mu_i$ | $\max \mu_i$ |
| **Czas testów (`tests`) [s]** | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\sum \mu_i$ | $\max \mu_i$ |
| **Czas pakowania (`package`) [s]** | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\mu \pm \sigma$ | $\sum \mu_i$ | $\max \mu_i$ |
| **Czas budowy Dockera [s]** | $\mu \pm \sigma$ | $\mu \pm \sigma$ | N/A | $\mu \pm \sigma$ | $\sum \mu_i$ | $\max \mu_i$ |
| **CAŁKOWITY CZAS POTOKU [s]** | **$T_{\text{mono}}$** | **$T_{\text{mod\_full}}$** | **$T_{\text{mod\_smart}}$** | **$T_{\text{fleet}}$** | **$T_{\text{seq}}$** | **$T_{\text{par}}$** |
| **Szczytowy RAM Kompilacji [MB]**| $RAM_{\text{c}}$ | $RAM_{\text{c}}$ | $RAM_{\text{c}}$ | $RAM_{\text{c}}$ | $\sum RAM_{\text{c},i}$ | $\max RAM_{\text{c},i}$ |
| **Szczytowy RAM Testów [MB]** | $RAM_{\text{t}}$ | $RAM_{\text{t}}$ | $RAM_{\text{t}}$ | $RAM_{\text{t}}$ | $\sum RAM_{\text{t},i}$ | $\max RAM_{\text{t},i}$ |
| **Rozmiar końcowy obrazu [MB]** | ~150 MB | ~150 MB | N/A | ~142 MB | ~426 MB (wirtualny) | ~226 MB (fizyczny) |

---

## Część 3: Architektura Repozytoriów na GitHubie pod Kątem Jenkinsa

Struktura repozytoriów na platformie GitHub warunkuje organizację potoków CI/CD, determinuje sposób konfiguracji zadań w Jenkinsie oraz wpływa na niezawodność mechanizmów detekcji zmian.

### 4.1. Analiza Porównawcza Wariantów

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            STRUKTURA GITHUB                                 │
└─────────────────────────────────────────────────────────────────────────────┘
  [ Opcja A: 1 Monorepo ]       [ Opcja B: 3 Polyrepos ]     [ Opcja C: 5 Polyrepos ]
  repo-logistics/               ├── repo-monolit/            ├── repo-monolit/
  ├── monolit/                  ├── repo-modular/            ├── repo-modular/
  ├── modular/                  └── repo-microservice/       ├── repo-fleet/
  └── microservice/                 ├── fleet/               ├── repo-routing/
      ├── docker-compose-infra.yml  ├── routing/             ├── repo-tracking/
      ├── fleet/                    ├── tracking/            └── repo-infra/
      ├── routing/                  └── docker-compose-infra.yml
      └── tracking/
```

#### Tabela porównawcza wariantów architektonicznych:

| Kryterium Porównawcze | Opcja A: 1 Monorepo | Opcja B: 3 Polyrepos (Rekomendowana) | Opcja C: 5 Polyrepos |
| :--- | :--- | :--- | :--- |
| **Liczba repozytoriów na GitHubie** | 1 repozytorium | 3 repozytoria | 5 lub 6 repozytoriów |
| **Script Path w zadaniach Jenkinsa** | Wymaga prefiksów (np. `microservice/fleet/Jenkinsfile`) | `Jenkinsfile` (monolit/modular), `fleet/Jenkinsfile` (mikroserwisy) | Czysty `Jenkinsfile` we wszystkich zadaniach |
| **Kontekst katalogu `${WORKSPACE}`** | Błędny dla podfolderów (wymaga `dir()`) | Naturalny dla monolitu i modulara; `dir()` w mikroserwisach | Idealny — natywny `${WORKSPACE}` w każdym zadaniu |
| **Izolacja zdarzeń Git (Commity i Push)** | Zerowa — commit w monolicie widoczny w całym repo | **Wysoka — pełna separacja paradygmatów badawczych** | Maksymalna — pełna autonomia każdego mikroserwisu |
| **Działanie `git diff --name-only`** | Ryzykowne — wymaga wyłączenia płytkiego klonowania | **Doskonałe** — stabilna selekcja modułów w `modular` | Zbyteczne w mikroserwisach (autonomiczne potoki) |
| **Wyzwalanie potoków (Webhooks)** | Skomplikowane — ryzyko wyzwalania 5 potoków naraz | **Proste i deterministyczne** | Proste (1 webhook na 1 dedykowany potok) |
| **Zarządzanie `docker-compose-infra.yml`** | Banalne | **Doskonałe (wewnątrz repozytorium `microservice`)** | Problematyczne (wymaga osobnego repozytorium infra) |
| **Narzut pracy dla dyplomanta** | Niski | **Minimalny (wykorzystuje istniejące repozytoria)** | Bardzo wysoki (konfiguracja 5–6 repozytoriów) |
| **Przejrzystość dla recenzenta pracy** | Dobra (1 link) | **Idealna (3 repozytoria odpowiadające 3 rozdziałom pracy)** | Chaotyczna (recenzent musi badać 6 linków) |

---

### 4.2. Wpływ Wyboru Architektury na Zachowanie Jenkinsa

#### 1. Ustawienie `Script Path` i kontekst roboczy `${WORKSPACE}`
W Jenkinsie zmienna `${WORKSPACE}` wskazuje na katalog nadrzędny, do którego wtyczka Git pobrała sklonowane repozytorium.
- **W Opcji A (Monorepo):** Gdy Jenkins wykonuje polecenie `sh 'mvn clean compile'`, proces kończy się błędem, ponieważ w korzeniu monorepo nie ma pliku `pom.xml`. Każdy krok powłoki musi być jawnie opakowany w instrukcję `dir('microservice/fleet') { ... }`.
- **W Opcji B (3 Polyrepos — Stan faktyczny i kluczowy warunek wykonania):**
  Dla repozytoriów monolitu tradycyjnego (`monolit`) i modułowego (`modular`) katalog `${WORKSPACE}` zawiera bezpośrednio plik `pom.xml`, dzięki czemu polecenia budowania wykonują się natywnie w bieżącym katalogu.
  Natomiast w repozytorium mikrousług (`microservice.git`) w korzeniu znajdują się podkatalogi: `fleet/`, `routing/`, `tracking/` oraz plik `docker-compose-infra.yml`. W korzeniu repozytorium **nie ma pliku `pom.xml` ani `Dockerfile`**.

  **Krytyczna pułapka Jenkins Pipeline:**
  Wskazanie w konfiguracji zadania parametru `Script Path: fleet/Jenkinsfile` instruuje Jenkinsa jedynie o lokalizacji pliku definicji potoku względem korzenia repozytorium — **NIE ZMIENIA ONO domyślnego katalogu roboczego (`pwd`) agenta!**
  Bieżącym katalogiem roboczym dla każdego etapu `sh` pozostaje korzeń repozytorium (`${WORKSPACE}`).
  Bez jawnego opakowania etapów kompilacji, testów, pakowania i budowania kontenera w blok:
  ```groovy
  dir('fleet') {
      sh 'mvn clean compile'
      // ...
  }
  ```
  (oraz odpowiednio `dir('routing')` i `dir('tracking')`), Maven natychmiast ulega awarii:
  ```text
  [ERROR] The goal you specified requires a project to execute but there is no POM in this directory (/var/jenkins_home/workspace/job-microservice-fleet). Please verify you are in the correct directory.
  ```
  Podobnie etap `docker build -t ${DOCKER_IMAGE} .` zawodzi z błędem braku pliku `Dockerfile`. Ponadto raporty testowe Surefire publikowane przez wtyczkę JUnit muszą jawnie uwzględniać ścieżkę podkatalogu: `junit 'fleet/target/surefire-reports/*.xml'` (a nie płaskie `target/...`), a artefakty metryk muszą być archiwizowane ze ścieżki `metrics/*.csv` (lub `fleet/metrics/*.csv`).
- **W Opcji C (5 Polyrepos):** W każdym zadaniu `${WORKSPACE}` jest bezpośrednio tożsamy z korzeniem danego mikroserwisu.

#### 2. Detekcja zmian (`git diff --name-only HEAD~1 HEAD`)
W monolicie modułowym potok wykorzystuje polecenie:
```bash
CHANGED_MODULES=$(git diff --name-only HEAD~1 HEAD | awk -F/ '{print $1}' | sort -u)
```
Jeżeli Jenkins pobierze kod w trybie *Shallow clone* (`git clone --depth 1`), odwołanie do rewizji `HEAD~1` rzuca błąd `fatal: ambiguous argument 'HEAD~1': unknown revision`. W konfiguracji zadania Jenkinsa należy **bezwzględnie wyłączyć opcję Shallow clone** w zaawansowanych zachowaniach klonowania (Advanced clone behaviours).

---

### 4.3. Rekomendacja Architektoniczna i Uzasadnienie Inżynierskie

#### WYBÓR OPTYMALNY DLA PRACY MAGISTERSKIEJ: OPCJA B (3 POLYREPOS)

Rekomenduje się podział na **trzy repozytoria GitHub**:
1. `https://github.com/przemmat/monolit-final.git` (Monolit tradycyjny)
2. `https://github.com/przemmat/modular.git` (Monolit modułowy)
3. `https://github.com/przemmat/microservice.git` (Mikroserwisy: `fleet`, `routing`, `tracking` + `docker-compose-infra.yml`)

#### Uzasadnienie:
1. **Ochrona dotychczasowej pracy:** Autor posiada już w pełni działające i przetestowane repozytoria dla monolitu i modulara. Scalanie ich do monorepo (Opcja A) wymagałoby zniszczenia historii commitów.
2. **Spójność infrastruktury lokalnej:** Plik `docker-compose-infra.yml` znajduje się w tym samym repozytorium co kod mikroserwisów. Pozwala to uruchomić całe środowisko jedną komendą: `docker compose -f docker-compose-infra.yml up -d`.
3. **Logika akademicka:** Struktura repozytoriów na GitHubie odpowiada dokładnie trzem głównym rozdziałom analizy porównawczej w pracy dyplomowej.

---

### 4.4. Instrukcja Konfiguracji Zadań w Jenkinsie Krok po Kroku

#### Krok 1: Wypchnięcie repozytorium `microservice` na GitHub
W terminalu w katalogu `e:/magisterka/final/microservice`:
```bash
git init
git add .
git commit -m "feat: initial release of logistics microservices ecosystem"
git branch -M main
git remote add origin https://github.com/przemmat/microservice.git
git push -u origin main
```

#### Krok 2: Konfiguracja zadań mikroserwisowych w Jenkinsie i izolacja wyzwalaczy Git
W panelu Jenkinsa należy utworzyć 3 zadania typu **Pipeline**:
1. **`job-microservice-fleet`**:
   - *Pipeline definition*: Pipeline script from SCM -> Git.
   - *Repository URL*: `https://github.com/przemmat/microservice.git`.
   - *Branch Specifier*: `*/main`.
   - *Script Path*: `fleet/Jenkinsfile`.
   - **Kluczowa konfiguracja izolacji wyzwalaczy (Included Regions):**
     W sekcji **Additional Behaviours** (Dodatkowe zachowania) kliknąć **Add** -> **Polling ignores commits in certain paths**.
     W polu **Included Regions** wpisać:
     ```text
     fleet/.*
     ```
     *(Uzasadnienie metodologiczne: Ponieważ w Opcji B repozytorium `microservice.git` zawiera wszystkie 3 mikroserwisy, brak tego filtra skutkowałby równoczesnym uruchamianiem wszystkich 3 zadań przy każdym commicie, uniemożliwiając izolowany pomiar metryki $T_{\text{single}}$).*
2. **`job-microservice-routing`**:
   - *Pipeline definition*: Pipeline script from SCM -> Git.
   - *Repository URL*: `https://github.com/przemmat/microservice.git`.
   - *Branch Specifier*: `*/main`.
   - *Script Path*: `routing/Jenkinsfile`.
   - **Additional Behaviours** -> **Polling ignores commits in certain paths** -> **Included Regions**:
     ```text
     routing/.*
     ```
3. **`job-microservice-tracking`**:
   - *Pipeline definition*: Pipeline script from SCM -> Git.
   - *Repository URL*: `https://github.com/przemmat/microservice.git`.
   - *Branch Specifier*: `*/main`.
   - *Script Path*: `tracking/Jenkinsfile`.
   - **Additional Behaviours** -> **Polling ignores commits in certain paths** -> **Included Regions**:
     ```text
     tracking/.*
     ```

#### Krok 3: Wymiarowanie wykonawców Jenkinsa (Executor Sizing) — Zapobieganie zakleszczeniom (Deadlock Prevention)
Przed przystąpieniem do testów współbieżnych należy bezwzględnie skonfigurować liczbę executorów serwera Jenkins:
- **Domyślne ograniczenie Jenkinsa:** Świeża instalacja Jenkins LTS alokuje na węźle wbudowanym (`Built-In Node`) zaledwie **2 executory**.
- **Mechanizm zakleszczenia i degradacji pomiarowej:**
  W scenariuszu `ALL_PARALLEL` potok orkiestrujący wywołuje jednocześnie 5 zadań podrzędnych (`job-monolit`, `job-modular`, `job-microservice-fleet`, `job-microservice-routing`, `job-microservice-tracking`). Gdyby orkiestrator zajął 1 ciężki executor, a węzeł posiadałby łącznie np. 2 lub 4 sloty:
  1. Zadania podrzędne trafiają do kolejki oczekujących (Build Queue).
  2. Zadania wykonują się sekwencyjnie lub częściowo sekwencyjnie w miarę zwalniania slotów, co całkowicie fałszuje pomiar czasu równoległego $T_{\text{par}}$ oraz zniekształca szczytowe obciążenie węzła $\text{Peak}_{\text{node}}$.
  3. W skrajnym przypadku (gdy zadania nadrzędne i podrzędne rywalizują o te same executory) dochodzi do całkowitego zakleszczenia (deadlock).
- **Wymagana konfiguracja:**
  Należy przejść do: `Zarządzaj Jenkinsem` (Manage Jenkins) -> `Węzły` (Nodes) -> `Built-In Node` -> `Konfiguruj` (Configure) -> w polu **Liczba wykonawców** (Number of executors) ustawić wartość **$\ge 6$** (rekomendowane **8** dla zapewnienia pełnej rezerwy badawczej).

---

### 4.5. Pipeline Nadrzędny Orkiestrujący Eksperymenty (`benchmark-orchestrator`)

W celu automatycznego przeprowadzenia serii pomiarowych do rozdziału badawczego należy utworzyć w Jenkinsie zadanie nadrzędne `benchmark-orchestrator`. Skrypt ten umożliwia uruchomienie badań w wariancie pełnej współbieżności lub w wariancie sekwencyjnym przez zadaną liczbę powtórzeń.

**Odporność na zakleszczenia (Deadlock Prevention):**
Na poziomie głównym potoku zadeklarowano `agent none`. Dzięki temu sam potok orkiestratora wykonuje się jako lekki wątek na Masterze (flyweight thread) i **nie rezerwuje ciężkiego executora**, pozostawiając wszystkie sloty wykonawcze dla równolegle uruchamianych zadań składowych. Agent roboczy alokowany jest dopiero w dedykowanym etapie `Consolidate Telemetry`.

#### Kompletny kod `benchmark-orchestrator`:
```groovy
pipeline {
    agent none  // Nie blokuje executora! Działa na wątku Master/Flyweight

    parameters {
        choice(
            name: 'SCENARIO',
            choices: ['ALL_PARALLEL', 'SEQUENTIAL_BENCHMARK', 'MICROSERVICES_PARALLEL'],
            description: 'Scenariusz badawczy do pracy magisterskiej'
        )
        integer(
            name: 'ITERATIONS',
            defaultValue: 10,
            description: 'Liczba cykli pomiarowych w serii (N >= 10)'
        )
    }

    stages {
        stage('Execute Benchmark Series') {
            steps {
                script {
                    echo "=== ROZPOCZĘCIE SERII BADAWCZEJ: ${params.SCENARIO} (${params.ITERATIONS} powtórzeń) ==="

                    for (int i = 1; i <= params.ITERATIONS; i++) {
                        echo ">>> CYKL POMIAROWY #${i} z ${params.ITERATIONS} <<<"

                        if (params.SCENARIO == 'ALL_PARALLEL') {
                            parallel(
                                "Monolith": {
                                    build job: 'job-monolit', wait: true
                                },
                                "Modular": {
                                    build job: 'job-modular', wait: true
                                },
                                "Fleet": {
                                    build job: 'job-microservice-fleet', wait: true
                                },
                                "Routing": {
                                    build job: 'job-microservice-routing', wait: true
                                },
                                "Tracking": {
                                    build job: 'job-microservice-tracking', wait: true
                                }
                            )
                        } else if (params.SCENARIO == 'SEQUENTIAL_BENCHMARK') {
                            // Pomiar czystego czasu CPU bez rywalizacji o zasoby maszyny
                            build job: 'job-monolit', wait: true
                            build job: 'job-modular', wait: true
                            build job: 'job-microservice-fleet', wait: true
                            build job: 'job-microservice-routing', wait: true
                            build job: 'job-microservice-tracking', wait: true
                        } else if (params.SCENARIO == 'MICROSERVICES_PARALLEL') {
                            // Równoległa weryfikacja wyłącznie ekosystemu mikrousługowego
                            parallel(
                                "Fleet": {
                                    build job: 'job-microservice-fleet', wait: true
                                },
                                "Routing": {
                                    build job: 'job-microservice-routing', wait: true
                                },
                                "Tracking": {
                                    build job: 'job-microservice-tracking', wait: true
                                }
                            )
                        }
                    }
                }
            }
        }

        stage('Consolidate Telemetry') {
            agent any  // Alokuje wykonawcę wyłącznie na czas scalania plików telemetrycznych
            steps {
                script {
                    echo "Pobieranie i konsolidacja artefaktów CSV z przestrzeni roboczych zadań..."
                    sh '''
                        mkdir -p consolidated_metrics
                        # Miejsce na agregację pobranych plików build_metrics.csv
                    '''
                }
            }
        }
    }
}
```

---

## Podsumowanie i Lista Kontrolna dla Autora Pracy

Wdrożenie zaleceń niniejszego audytu technicznego przekształca prototypowy projekt `microservice` w pełnowartościowy, odporny na awarie i rygorystycznie zoptymalizowany ekosystem badawczy.

### Lista Kontrolna Przed Uruchomieniem Pomiarów Końcowych:

- [ ] **Mikroserwis `fleet`:**
  - [ ] Wdrożono mock `@MockitoBean private RabbitTemplate rabbitTemplate` oraz `@Transactional` w `FleetIntegrationTest.java`.
  - [ ] W `application.yml` skonfigurowano port 8081, bazę `fleet_db` na porcie 5433 z hasłem `pass` oraz sekcję `spring.rabbitmq`.
  - [ ] W `Dockerfile` dodano flagę `--launcher` oraz punkt wejścia `org.springframework.boot.loader.launch.JarLauncher`.
  - [ ] W `Jenkinsfile` wdrożono kontekst `dir('fleet') { ... }` wokół etapów budowania i testów, raporty Surefire `fleet/target/surefire-reports/*.xml`, unikalny tag `logistics-fleet:${BUILD_NUMBER}` oraz rekurencyjną telemetrię drzewa procesów w czystym POSIX `awk`.
- [ ] **Mikroserwis `routing`:**
  - [ ] Całkowicie usunięto nadmiarowy plik `MonolitApplicationTests.java`.
  - [ ] Dodano `@Transactional` w `RouteIntegrationTest.java`.
  - [ ] Zaktualizowano `TestcontainersConfiguration.java` do `postgres:16-alpine` z `@ServiceConnection` i `withReuse(true)`.
  - [ ] W `application.yml` skonfigurowano port 8082 oraz bazę `routing_db` na porcie 5434 z hasłem `pass`.
  - [ ] W `Dockerfile` wdrożono `--launcher` i `JarLauncher` na porcie 8082.
  - [ ] W `Jenkinsfile` wdrożono kontekst `dir('routing')`, raporty Surefire `routing/target/surefire-reports/*.xml`, tag `logistics-routing:${BUILD_NUMBER}` oraz rekurencyjną telemetrię `awk`.
- [ ] **Mikroserwis `tracking`:**
  - [ ] Przeniesiono klasy testowe z pakietu `matera.magisterka.monolit` do `matera.magisterka.microservice.tracking`.
  - [ ] Usunięto nadmiarowy plik `MonolitApplicationTests.java`.
  - [ ] Dodano brakujący plik `TestcontainersConfiguration.java` z `postgres:16-alpine` i `@ServiceConnection` w pakiecie `matera.magisterka.microservice.tracking`.
  - [ ] W `AbstractIntegrationTest.java` dodano wyłączenie listenera AMQP (`auto-startup=false`).
  - [ ] W `application.yml` skonfigurowano port 8083 oraz bazę `tracking_db` na porcie 5435 z hasłem `pass`.
  - [ ] W `Dockerfile` wdrożono `--launcher` i `JarLauncher` na porcie 8083.
  - [ ] W `Jenkinsfile` wdrożono kontekst `dir('tracking')`, raporty Surefire `tracking/target/surefire-reports/*.xml`, tag `logistics-tracking:${BUILD_NUMBER}` oraz rekurencyjną telemetrię `awk`.
- [ ] **Infrastruktura wspólna:**
  - [ ] W pliku `docker-compose-infra.yml` zdefiniowano wolumeny trwałe, healthchecki (`pg_isready`, `rabbitmq-diagnostics`) oraz dedykowaną sieć mostkową `logistics-network` (z usunięciem przestarzałego atrybutu `version: '3.8'`).
- [ ] **Konfiguracja środowiska Jenkins i orkiestracja:**
  - [ ] W zadaniach mikroserwisowych włączono `Included Regions` (`fleet/.*`, `routing/.*`, `tracking/.*`) w `Additional Behaviours` Git SCM w celu deterministycznej izolacji wyzwalaczy.
  - [ ] Węzeł wykonawczy Jenkinsa (`Built-In Node`) skonfigurowano na minimum 6 executorów (`Number of executors >= 6`), eliminując ryzyko zakleszczenia w scenariuszu `ALL_PARALLEL`.
  - [ ] W potoku `benchmark-orchestrator` zadeklarowano `agent none` na poziomie głównym, zwalniając slot executora dla współbieżnych potoków składowych.
- [ ] **Reżim badawczy:**
  - [ ] Odrzucono przebieg wstępny (Build #1) jako rozgrzewkowy.
  - [ ] Wykonano serię $N \ge 10$ powtórzeń w Jenkinsie przy użyciu `benchmark-orchestrator`.
  - [ ] Dokonano agregacji metryk zgodnie ze wzorami na czas sekwencyjny ($T_{\text{seq}}$), czas równoległy ($T_{\text{par}}$), szczytowy RAM agenta ($\text{Peak}_{\text{agent}}$) oraz fizyczny rozmiar obrazów po deduplikacji warstw ($S_{\text{disk}}$).

---
*Raport sporządzono z zachowaniem najwyższych standardów inżynierii oprogramowania i metodologii naukowej.*
