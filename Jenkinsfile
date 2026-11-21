pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    IMAGE_NAME = 'agenda-ja-back'
    CONTAINER_NAME = 'agenda-ja-back'
    MAILPIT_CONTAINER = 'agendaja-mailpit'
    HOST_PORT = '8093'
    MAILPIT_HOST_PORT = '8025'
    FRONTEND_RESET_PASSWORD_URL = 'http://84.247.135.249:8092/redefinir-senha'
    WAHA_SECRET_FILE = '/opt/agendaja/secrets/waha-api-key'
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

    stage('Análise estática') {
      steps {
        writeFile file: 'jenkins-pmd-rules.xml', text: '''<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="Qualidade Agenda Já" xmlns="http://pmd.sourceforge.net/ruleset/2.0.0">
  <description>Regras de acoplamento, tamanho e complexidade para visualização no Jenkins.</description>
  <rule ref="category/java/design.xml/CouplingBetweenObjects">
    <properties><property name="threshold" value="10"/></properties>
  </rule>
  <rule ref="category/java/design.xml/GodClass"/>
  <rule ref="category/java/design.xml/CognitiveComplexity"/>
</ruleset>'''
        sh '''
          JENKINS_CONTAINER="$(docker inspect --format '{{.Id}}' "$(hostname)" 2>/dev/null || docker ps -q --filter "volume=/var/jenkins_home" | head -n 1)"
          docker run --rm --network host --volumes-from "${JENKINS_CONTAINER}" \
            -w "${WORKSPACE}" eclipse-temurin:21-jdk \
            sh -c "./mvnw checkstyle:checkstyle -B -Dmaven.repo.local=/var/jenkins_home/.m2/repository"
          docker run --rm --volumes-from "${JENKINS_CONTAINER}" \
            -w "${WORKSPACE}" pmdcode/pmd:7.17.0 \
            check -d src/main/java -R jenkins-pmd-rules.xml -f xml -r target/pmd-design.xml --no-fail-on-violation
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

          if ! docker inspect "${MAILPIT_CONTAINER}" >/dev/null 2>&1; then
            docker run --detach \
              --name "${MAILPIT_CONTAINER}" \
              --restart unless-stopped \
              --network agendaja-net \
              --publish "${MAILPIT_HOST_PORT}:8025" \
              axllent/mailpit:latest
          else
            docker start "${MAILPIT_CONTAINER}" || true
            docker network connect agendaja-net "${MAILPIT_CONTAINER}" 2>/dev/null || true
          fi

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

          DEPLOY_PORT="${HOST_PORT}"
          while docker ps --format '{{.Ports}}' | grep -q ":${DEPLOY_PORT}->"; do
            DEPLOY_PORT=$((DEPLOY_PORT + 1))
          done
          echo "Subindo ${CONTAINER_NAME} na porta ${DEPLOY_PORT}..."

          docker run --rm \
            --mount "type=bind,source=${WAHA_SECRET_FILE},target=/run/secrets/waha-api-key,readonly" \
            --entrypoint sh \
            "${IMAGE_NAME}:${BUILD_NUMBER}" \
            -c 'test -s /run/secrets/waha-api-key'

          docker run --detach \
            --name "${CONTAINER_NAME}" \
            --restart unless-stopped \
            --network agendaja-net \
            --publish "${DEPLOY_PORT}:8080" \
            -e DB_URL=jdbc:postgresql://agendaja-postgres:5432/agendaja \
            -e MAIL_HOST="${MAILPIT_CONTAINER}" \
            -e MAIL_PORT=1025 \
            -e MAIL_FROM=no-reply@agendaja.local \
            -e FRONTEND_RESET_PASSWORD_URL="${FRONTEND_RESET_PASSWORD_URL}" \
            -e WAHA_ENABLED=true \
            -e WAHA_BASE_URL=http://agenda-ja-waha:3000 \
            -e WAHA_SESSION=agendaja \
            -e WAHA_CONNECT_TIMEOUT_MS=5000 \
            -e WAHA_READ_TIMEOUT_MS=10000 \
            --mount "type=bind,source=${WAHA_SECRET_FILE},target=/run/secrets/waha-api-key,readonly" \
            --entrypoint sh \
            --label "com.agenda-ja.managed-by=jenkins" \
            "${IMAGE_NAME}:${BUILD_NUMBER}" \
            -c 'export WAHA_API_KEY="$(tr -d "\\r\\n" < /run/secrets/waha-api-key)"; exec java -jar /app/app.jar'
          docker inspect --format '{{.State.Status}}' "${CONTAINER_NAME}" | grep -x running

          for tentativa in $(seq 1 18); do
            if docker exec "${CONTAINER_NAME}" wget -q -O /dev/null http://127.0.0.1:8080/api/auth/csrf; then
              echo "Backend respondeu à verificação de saúde."
              exit 0
            fi
            sleep 5
          done

          docker logs --tail 200 "${CONTAINER_NAME}"
          echo "Backend não respondeu após 90 segundos."
          exit 1
        '''
      }
    }
  }

  post {
    always {
      sh '''
        docker rm --force $(docker ps -aq --filter "label=org.testcontainers=true") 2>/dev/null || true
        mkdir -p "build-artifacts/${BUILD_NUMBER}/surefire-reports"
        mkdir -p "build-artifacts/${BUILD_NUMBER}/jacoco"
        cp -r target/surefire-reports/. "build-artifacts/${BUILD_NUMBER}/surefire-reports/" 2>/dev/null || true
        cp -r target/site/jacoco/. "build-artifacts/${BUILD_NUMBER}/jacoco/" 2>/dev/null || true
        cp target/*.jar "build-artifacts/${BUILD_NUMBER}/" 2>/dev/null || true
      '''
      junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
      recordIssues enabledForFailure: true, skipPublishingChecks: true,
        tool: checkStyle(pattern: 'target/checkstyle-result.xml'),
        sourceCodeEncoding: 'UTF-8'
      recordIssues enabledForFailure: true, skipPublishingChecks: true,
        tool: pmdParser(pattern: 'target/pmd-design.xml'),
        sourceCodeEncoding: 'UTF-8'
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
