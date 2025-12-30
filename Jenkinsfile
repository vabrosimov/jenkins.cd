def logStartStage() {
    ansiColor('xterm') {
        echo """
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        \u001B[36m▶▶▶ START STAGE: ${STAGE_NAME}\u001B[0m
        \u001B[34m══════════════════════════════════════════════\u001B[0m
        """.stripIndent()
    }
}

def logEndStage() {
    ansiColor('xterm') {
        echo """
        \u001B[32m✔✔✔ END STAGE: ${STAGE_NAME}\u001B[0m
        """.stripIndent()
    }
}

String currentVersion

pipeline {
    agent any

    stages {
        stage("Configure pipeline") {
            steps {
                script {
                    logStartStage()

                    String nexusUrl = "http://nexus:8081"
                    String repo     = "maven-releases"
                    String group    = "ru.abrosimov.defi"
                    String artifact = "defi"

                    GString apiUrl = "${nexusUrl}/service/rest/v1/search?repository=${repo}&group=${group}&name=${artifact}"

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

                            echo "Found versions in Nexus: ${versions}"
                            if (versions.isEmpty()) {
                                error "No release versions found in Nexus"
                            }

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

                    logEndStage()
                }
            }
        }
    }
}
