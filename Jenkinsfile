pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    IMAGE_NAME = 'agenda-ja-back'
    CONTAINER_NAME = 'agenda-ja-back'
    HOST_PORT = '8091'
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
          chmod +x mvnw
          JENKINS_CONTAINER="$(docker inspect --format '{{.Id}}' "$(hostname)" 2>/dev/null || docker ps -q --filter "volume=/var/jenkins_home" | head -n 1)"

          docker run --rm \
            --network host \
            --volumes-from "${JENKINS_CONTAINER}" \
            -v /var/run/docker.sock:/var/run/docker.sock \
            -w "${WORKSPACE}" \
            -e TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1 \
            -e TESTCONTAINERS_RYUK_DISABLED=true \
            eclipse-temurin:21-jdk \
            sh -c "./mvnw clean package -B -Dmaven.repo.local=/var/jenkins_home/.m2/repository && chown -R $(id -u):$(id -g) target"
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
      sh '''
        mkdir -p "build-artifacts/${BUILD_NUMBER}/surefire-reports"
        mkdir -p "build-artifacts/${BUILD_NUMBER}/jacoco"
        cp -r target/surefire-reports/. "build-artifacts/${BUILD_NUMBER}/surefire-reports/" 2>/dev/null || true
        cp -r target/site/jacoco/. "build-artifacts/${BUILD_NUMBER}/jacoco/" 2>/dev/null || true
        cp target/*.jar "build-artifacts/${BUILD_NUMBER}/" 2>/dev/null || true
      '''
      junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
      archiveArtifacts artifacts: "build-artifacts/${env.BUILD_NUMBER}/**", allowEmptyArchive: true
      recordCoverage tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']]
      publishHTML(target: [
        allowMissing: true,
        alwaysLinkToLastBuild: true,
        keepAll: true,
        reportDir: 'target/site/jacoco',
        reportFiles: 'index.html',
        reportName: 'Cobertura backend'
      ])
      sh 'docker image prune --force --filter "label=com.agenda-ja.managed-by=jenkins" || true'
    }
  }
}
