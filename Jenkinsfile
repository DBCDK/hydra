#!groovy

def workerNode = "devel8"

pipeline {
    agent { label workerNode }
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    triggers {
        pollSCM("H/03 * * * *")
    }
    options {
        timestamps()
    }
    stages {
        stage("clear workspace") {
            steps {
                deleteDir()
                checkout scm
            }
        }
        stage("verify") {
            steps {
                sh "mvn verify pmd:pmd"
                junit "**/target/surefire-reports/TEST-*.xml,**/target/failsafe-reports/TEST-*.xml"
            }
        }
        stage("publish pmd results") {
            steps {
                step([$class          : 'hudson.plugins.pmd.PmdPublisher',
                      pattern         : '**/target/pmd.xml',
                      unstableTotalAll: "0",
                      failedTotalAll  : "0"])
            }
        }
        stage("docker build") {
            steps {
                script {
                    def repo = "docker-io.dbc.dk"
                    def name = "hydra-service"
                    def version = env.BRANCH_NAME + '-' + env.BUILD_NUMBER
                    def image = docker.build("${repo}/${name}:${version}")

                    image.push()

                    if (env.BRANCH_NAME ==~ /master|trunk/) {
                        image.push("DIT-${env.BUILD_NUMBER}")
                    }
                }
            }
        }
    }
}
