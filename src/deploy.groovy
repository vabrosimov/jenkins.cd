@Library('abrosimov.jenkins') _


import ru.abrosimov.jenkins.context.Application
import ru.abrosimov.jenkins.utils.Logger
import ru.abrosimov.jenkins.context.PipelineContext

String currentVersion
Logger logger = new Logger(this)
PipelineContext pipelineContext

pipeline {
    agent any

    environment {
        IMAGE_NAME = "vabrosimov/defi"
        REGISTRY = "http://95.174.94.249:8082/v2/repository/registry"
        REPOSITORY = "http://95.174.94.249:8081"
    }

    stages {
        stage("Init pipeline") {
            steps {
                script {
                    logger.logStartStage()

                    def Defi = load "src/apps/defi/Defi.groovy"

                    pipelineContext = new PipelineContext(this)
                    pipelineContext.applications = [Defi]

                    logger.logEndStage()
                }
            }
        }

        stage("Configure pipeline") {
            steps {
                script {
                    logger.logStartStage()

                    List<Object> choices = pipelineContext.applications.collect { Application application ->
                        String repo = "maven-releases"
                        GString apiUrl = "${REPOSITORY}/service/rest/v1/search?repository=${repo}&group=${application.mavenGroup}&name=${application.mavenArtifact}"

                        withCredentials([
                                usernamePassword(
                                        credentialsId: "NEXUS_CREDENTIALS",
                                        usernameVariable: "NEXUS_USER",
                                        passwordVariable: "NEXUS_PASSWORD"
                                )
                        ]) {
                            withEnv([
                                    "API_URL=${apiUrl}"
                            ]) {
                                List<String> versions = sh(
                                        script: '''
                                curl -s -u "$NEXUS_USER:$NEXUS_PASSWORD" \
                                "$API_URL" \
                                | grep '"version"' \
                                | sed 's/.*"version"[ ]*:[ ]*"//' \
                                | sed 's/".*//' \
                                | grep -v SNAPSHOT \
                                | sort -Vr \
                                | uniq
                                ''',
                                        returnStdout: true
                                ).trim().split("\n")
                                versions.add(0, 'SKIP_INSTALL')

                                if (versions.isEmpty()) {
                                    error "No release versions found in Nexus for ${application.mavenGroup}.${application.mavenArtifact}"
                                }

                                echo "Found versions in Nexus for ${application.mavenGroup}.${application.mavenArtifact}: ${versions}"

                                return choice(
                                        name: application.versionParamName,
                                        choices: versions,
                                        description: 'Version to deploy'
                                )
                            }
                        }
                    }

                    properties([parameters(choices)])

                    logger.logEndStage()
                }
            }
        }

        stage("Find digest") {
            steps {
                script {
                    logger.logStartStage()

                    def manifestsUrl = "${REGISTRY}/${IMAGE_NAME}/manifests/${params.VERSION}"

                    withCredentials([
                            usernamePassword(
                                    credentialsId: "NEXUS_CREDENTIALS",
                                    usernameVariable: "NEXUS_USER",
                                    passwordVariable: "NEXUS_PASSWORD"
                            )
                    ]) {
                        withEnv([
                                "MANIFESTS_URL=${manifestsUrl}"
                        ]) {
                            String digest = sh(
                                    script: '''
                                curl -s -u $NEXUS_USER:$NEXUS_PASSWORD $MANIFESTS_URL |
                                jq -r '.manifests[] | select(.platform.architecture=="amd64") | .digest'
                                ''',
                                    returnStdout: true
                            ).trim()

                            if (digest.isEmpty()) {
                                error "No digest found in registry"
                            }

                            echo "Found digest in registry: ${digest}"
                        }
                    }

                    logger.logEndStage()
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    logger.logStartStage()

                    sshagent(['SSH_KEY_VM']) {
                        sh '''
                            mkdir -p -m 700 ~/.ssh
                            ssh-keyscan -H 176.108.250.97 >> ~/.ssh/known_hosts
                            chmod 600 ~/.ssh/known_hosts

                            ssh -o StrictHostKeyChecking=no vabrosimov@176.108.250.97 \\
                                "sudo mkdir -p -m 755 /opt/defi && sudo chown vabrosimov:vabrosimov /opt/defi && sudo chmod 755 /opt/defi"

                            scp docker-compose.yml vabrosimov@176.108.250.97:/opt/defi/docker-compose.yml

                            ssh -o StrictHostKeyChecking=no vabrosimov@176.108.250.97 \\
                                "cd /opt/defi && sudo docker compose up -d --force-recreate"
                        '''
                    }


                    logger.logEndStage()
                }
            }
        }
    }
}
