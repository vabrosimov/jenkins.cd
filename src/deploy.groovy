@Library('abrosimov.jenkins') _

import ru.abrosimov.jenkins.context.Application
import ru.abrosimov.jenkins.stages.ConfigurePipeline
import ru.abrosimov.jenkins.stages.FindDigest
import ru.abrosimov.jenkins.stages.Deploy
import ru.abrosimov.jenkins.utils.Logger
import ru.abrosimov.jenkins.context.PipelineContext

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
                    Logger.startStage(this)

                    def Defi = load "src/apps/defi/Defi.groovy"

                    pipelineContext = new PipelineContext(this)
                    pipelineContext.applications = [Defi]

                    Logger.endStage(this)
                }
            }
        }

        stage("Configure pipeline") {
            steps {
                script {
                    Logger.startStage(this)

                    ConfigurePipeline configurePipeline = new ConfigurePipeline(this)

                    List<Object> parametersList = []

                    pipelineContext.applications.each { Application application ->
                        parametersList.addAll(configurePipeline.call(application))
                    }

                    properties([parameters(parametersList)])

                    Logger.endStage(this)
                }
            }
        }

        stage("Find digest") {
            steps {
                script {
                    Logger.startStage(this)

                    FindDigest findDigest = new FindDigest(this)

                    pipelineContext.applications.each { Application application ->
                        findDigest.call(application)
                    }

                    Logger.endStage(this)
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    Logger.startStage(this)

                    Deploy deploy = new Deploy(this)

                    pipelineContext.applications.each { Application application ->
                        deploy.call(application)
                    }

                    Logger.endStage(this)
                }
            }
        }
    }
}
