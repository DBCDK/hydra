#!groovy

def teamSlackNotice = 'team-x-notice'
def teamSlackWarning = 'team-x-warning'
def workerNode = "devel11"

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
                    def repo = "docker-metascrum.artifacts.dbccloud.dk"
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
                    image "docker-dbc.artifacts.dbccloud.dk/build-env:latest"
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
                            set-new-version services/rawrepo/rawrepo-hydra-tmpl.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/dit-gitops-secrets DIT-${env.BUILD_NUMBER} -b master

                            set-new-version rawrepo-hydra-basismig.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-hydra-deploy DIT-${env.BUILD_NUMBER} -b basismig
                            set-new-version rawrepo-hydra-fbstest.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-hydra-deploy DIT-${env.BUILD_NUMBER} -b fbstest
                            set-new-version rawrepo-hydra-metascrum-staging.yml ${env.GITLAB_PRIVATE_TOKEN} metascrum/rawrepo-hydra-deploy DIT-${env.BUILD_NUMBER} -b metascrum-staging
						"""
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                if (BRANCH_NAME == 'main') {
                    def dockerImageName = readFile(file: 'docker.out')
                    slackSend(channel: teamSlackNotice,
                            color: 'good',
                            message: "${JOB_NAME} #${BUILD_NUMBER} completed, and pushed ${dockerImageName} to artifactory.",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        fixed {
            script {
                if ("${env.BRANCH_NAME}" == 'main') {
                    slackSend(channel: teamSlackWarning,
                            color: 'good',
                            message: "${env.JOB_NAME} #${env.BUILD_NUMBER} back to normal: ${env.BUILD_URL}",
                            tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
        failure {
            script {
                if ("${env.BRANCH_NAME}".equals('main')) {
                    slackSend(channel: teamSlackWarning,
                        color: 'warning',
                        message: "${env.JOB_NAME} #${env.BUILD_NUMBER} failed and needs attention: ${env.BUILD_URL}",
                        tokenCredentialId: 'slack-global-integration-token')
                }
            }
        }
    }
}
