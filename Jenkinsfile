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
          chmod +x mvnw
          if command -v java >/dev/null 2>&1; then
            ./mvnw clean package -B
          else
            docker run --rm \
              --network host \
              -v /var/run/docker.sock:/var/run/docker.sock \
              -v "${WORKSPACE}:/app" \
              -v "${HOME}/.m2:/root/.m2" \
              -w /app \
              eclipse-temurin:21-jdk-alpine \
              ./mvnw clean package -B
          fi
          mkdir -p "build-artifacts/${BUILD_NUMBER}"
          cp -r target/surefire-reports "build-artifacts/${BUILD_NUMBER}/surefire-reports"
          cp -r target/site/jacoco "build-artifacts/${BUILD_NUMBER}/jacoco"
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
          docker compose up -d postgres
          docker rm --force "${CONTAINER_NAME}" 2>/dev/null || true
          docker run --detach \
            --name "${CONTAINER_NAME}" \
            --restart unless-stopped \
            --network host \
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
        reportDir: "build-artifacts/${env.BUILD_NUMBER}/jacoco",
        reportFiles: 'index.html',
        reportName: 'Cobertura backend'
      ])
      sh 'docker image prune --force --filter "label=com.agenda-ja.managed-by=jenkins" || true'
    }
  }
}
