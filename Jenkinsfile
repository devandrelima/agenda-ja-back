pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    IMAGE_NAME = 'agenda-ja-back'
    CONTAINER_NAME = 'agenda-ja-back'
    HOST_PORT = '8080'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build e testes') {
      steps {
        sh '''
          mkdir -p "build-artifacts/${BUILD_NUMBER}/surefire-reports"
          mkdir -p "build-artifacts/${BUILD_NUMBER}/jacoco"

          JDK21_DIR="${HOME}/.jdk21"
          if [ ! -x "${JDK21_DIR}/bin/javac" ]; then
            mkdir -p "${JDK21_DIR}"
            if ldd --version 2>&1 | grep -qi musl; then
              OS_TYPE="alpine-linux"
            else
              OS_TYPE="linux"
            fi
            curl -fsSL "https://api.adoptium.net/v3/binary/latest/21/ga/${OS_TYPE}/x64/jdk/hotspot/normal/eclipse" \
              | tar -xz -C "${JDK21_DIR}" --strip-components=1
          fi

          export JAVA_HOME="${JDK21_DIR}"
          export PATH="${JAVA_HOME}/bin:${PATH}"
          java -version

          chmod +x mvnw
          ./mvnw clean package -B

          cp -r target/surefire-reports/. "build-artifacts/${BUILD_NUMBER}/surefire-reports/"
          cp -r target/site/jacoco/. "build-artifacts/${BUILD_NUMBER}/jacoco/"
          cp target/*.jar "build-artifacts/${BUILD_NUMBER}/"
        '''
      }
    }

    stage('Imagem de produção') {
      steps {
        sh 'docker build --label "com.agenda-ja.managed-by=jenkins" --tag "${IMAGE_NAME}:${BUILD_NUMBER}" --tag "${IMAGE_NAME}:latest" .'
      }
    }

    stage('Deploy na VPS') {
      steps {
        sh '''
          docker network inspect agendaja-net >/dev/null 2>&1 || docker network create agendaja-net

          if ! docker inspect agendaja-postgres >/dev/null 2>&1; then
            docker run --detach \
              --name agendaja-postgres \
              --restart unless-stopped \
              --network agendaja-net \
              -e POSTGRES_DB=agendaja \
              -e POSTGRES_USER=agendaja \
              -e POSTGRES_PASSWORD=agendaja \
              -v postgres-data:/var/lib/postgresql/data \
              postgres:17-alpine
          else
            docker start agendaja-postgres || true
            docker network connect agendaja-net agendaja-postgres 2>/dev/null || true
          fi

          docker rm --force "${CONTAINER_NAME}" 2>/dev/null || true
          docker run --detach \
            --name "${CONTAINER_NAME}" \
            --restart unless-stopped \
            --network agendaja-net \
            --publish "${HOST_PORT}:8080" \
            -e DB_URL=jdbc:postgresql://agendaja-postgres:5432/agendaja \
            --label "com.agenda-ja.managed-by=jenkins" \
            "${IMAGE_NAME}:${BUILD_NUMBER}"
          docker inspect --format '{{.State.Status}}' "${CONTAINER_NAME}" | grep -x running
        '''
      }
    }
  }

  post {
    always {
      junit testResults: "build-artifacts/${env.BUILD_NUMBER}/surefire-reports/*.xml", allowEmptyResults: true
      archiveArtifacts artifacts: "build-artifacts/${env.BUILD_NUMBER}/**", allowEmptyArchive: true
      recordCoverage tools: [[parser: 'JACOCO', pattern: "build-artifacts/${env.BUILD_NUMBER}/jacoco/jacoco.xml"]]
      publishHTML(target: [
        allowMissing: true,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: "build-artifacts/${env.BUILD_NUMBER}/jacoco",
        reportFiles: 'index.html',
        reportName: 'Cobertura backend'
      ])
      sh 'docker image prune --force --filter "label=com.agenda-ja.managed-by=jenkins" || true'
    }
  }
}
