@Library('abrosimov.jenkins') _

import ru.abrosimov.jenkins.context.Application
import ru.abrosimov.jenkins.stages.ConfigurePipeline
import ru.abrosimov.jenkins.stages.FindDigest
import ru.abrosimov.jenkins.stages.Deploy
import ru.abrosimov.jenkins.utils.Logger
import ru.abrosimov.jenkins.context.PipelineContext

Logger logger = new Logger(this)
PipelineContext pipelineContext

pipeline {
    agent any

    environment {
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

                    ConfigurePipeline configurePipeline = new ConfigurePipeline(this)

                    List<Object> parametersList = []

                    pipelineContext.applications.each { Application application ->
                        parametersList.addAll(configurePipeline.call(application))
                    }

                    properties([parameters(parametersList)])

                    logger.logEndStage()
                }
            }
        }

        stage("Find digest") {
            steps {
                script {
                    logger.logStartStage()

                    FindDigest findDigest = new FindDigest(this)

                    pipelineContext.applications.each { Application application ->
                        findDigest.call(application)
                    }

                    logger.logEndStage()
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    logger.logStartStage()

                    Deploy deploy = new Deploy(this)

                    pipelineContext.applications.each { Application application ->
                        deploy.call(application)
                    }

                    logger.logEndStage()
                }
            }
        }
    }
}
