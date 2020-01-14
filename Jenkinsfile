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
        upstream(upstreamProjects: "Docker-payara5-bump-trigger",
                threshold: hudson.model.Result.SUCCESS)
    }
    options {
        timestamps()
    }
    environment {
        GITLAB_PRIVATE_TOKEN = credentials("metascrum-gitlab-api-token")
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
        stage("Update DIT") {
            agent {
                docker {
                    label workerNode
                    image "docker.dbc.dk/build-env:latest"
                    alwaysPull true
                }
            }
            when {
                expression {
                    (currentBuild.result == null || currentBuild.result == 'SUCCESS') && env.BRANCH_NAME == 'master'
                }
            }
            steps {
                script {
                    dir("deploy") {
                        sh """
                            set-new-version services/rawrepo-hydra.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets DIT-${env.BUILD_NUMBER} -b master
						"""
                    }
                }
            }
        }
    }
}
