@Library('abrosimov.jenkins') _

import utils.Logger
import context.PipelineContext

String currentVersion
Logger logger = new Logger(this)

pipeline {
    agent any

    environment {
        IMAGE_NAME = "vabrosimov/defi"
        REGISTRY = "http://95.174.94.249:8082/v2/repository/registry"
        REPOSITORY = "http://95.174.94.249:8081"
    }

    stages {
        stage("Init pipeline") {

        }

        stage("Configure pipeline") {
            steps {
                script {
                    logger.logStartStage()

                    String repo     = "maven-releases"
                    String group    = "ru.abrosimov.defi"
                    String artifact = "defi"

                    GString apiUrl = "${REPOSITORY}/service/rest/v1/search?repository=${repo}&group=${group}&name=${artifact}"

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
                                error "No release versions found in Nexus"
                            }

                            echo "Found versions in Nexus: ${versions}"

                            properties([
                                parameters([
                                    choice(
                                        name: 'VERSION',
                                        choices: versions,
                                        description: 'Version to deploy'
                                    )
                                ])
                            ])
                        }
                    }

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
