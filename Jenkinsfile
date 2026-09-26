pipeline {
	agent any

	environment {
		DOCKER_IMAGE = "logistics-monolith:${BUILD_NUMBER}"
		METRICS_DIR = "${WORKSPACE}/metrics"
		METRICS_FILE = "${WORKSPACE}/metrics/build_metrics.csv"
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
				sh '''
                    START=$(date +%s%3N)

                    mvn clean compile

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    echo "${BUILD_NUMBER},compile,${DIFF},0,0" >> ${METRICS_FILE}
                '''
			}
		}

		stage('Smart Incremental Test & Build') {
			steps {
				sh '''
            START=$(date +%s%3N)

            # 1. Wykrycie zmienionych modułów względem poprzedniego commita
            CHANGED_MODULES=""
            if git diff --name-only HEAD~1 HEAD | grep -q "^fleet/"; then
                CHANGED_MODULES="${CHANGED_MODULES}:fleet,"
            fi
            if git diff --name-only HEAD~1 HEAD | grep -q "^routing/"; then
                CHANGED_MODULES="${CHANGED_MODULES}:routing,"
            fi
            if git diff --name-only HEAD~1 HEAD | grep -q "^tracking/"; then
                CHANGED_MODULES="${CHANGED_MODULES}:tracking,"
            fi

            # Usunięcie końcowego przecinka
            CHANGED_MODULES=$(echo ${CHANGED_MODULES} | sed 's/,$//')

            if [ -z "$CHANGED_MODULES" ]; then
                echo "Zmiany poza modułami domenowymi (np. app lub root). Budowanie całości."
                mvn clean test
            else
                echo "Zmiany wykryte w modułach: ${CHANGED_MODULES}. Uruchamianie smart buildu."
                # -pl (projects list), -amd (also make dependents - dobudowuje moduł :app)
                mvn test -pl ${CHANGED_MODULES} -amd
            fi

            END=$(date +%s%3N)
            DIFF=$((END - START))
            echo "${BUILD_NUMBER},smart_tests,${DIFF},0,0" >> ${METRICS_FILE}
        '''
			}
		}

		stage('Package Application') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    mvn package -DskipTests

                    END=$(date +%s%3N)
                    DIFF=$((END - START))
                    echo "${BUILD_NUMBER},package,${DIFF},0,0" >> ${METRICS_FILE}
                '''
			}
		}

		stage('Build Docker Image') {
			steps {
				sh '''
                    START=$(date +%s%3N)

                    docker build -t ${DOCKER_IMAGE} .

                    END=$(date +%s%3N)
                    DIFF=$((END - START))

                    # Rozmiar obrazu w MB
                    IMG_BYTES=$(docker inspect -f "{{ .Size }}" ${DOCKER_IMAGE})
                    IMG_MB=$(echo "scale=2; ${IMG_BYTES} / 1048576" | bc)

                    echo "${BUILD_NUMBER},docker_build,${DIFF},0,${IMG_MB}" >> ${METRICS_FILE}
                '''
			}
		}
	}

	post {
		always {
			// Zachowanie pliku metryk jako artefaktu Jenkinsa
			archiveArtifacts artifacts: 'metrics/*.csv', allowEmptyArchive: true
		}
	}
}