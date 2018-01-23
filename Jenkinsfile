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
                step([$class: 'hudson.plugins.pmd.PmdPublisher',
                    pattern: '**/target/pmd.xml',
                    unstableTotalAll: "0",
                    failedTotalAll: "0"])
            }
        }
        stage("docker build") {
            steps {
                script {
                    def repo = "docker-io.dbc.dk"
                    def name = "hydra-service"
                    def version = env.BUILD_NUMBER

                    // Apparently the 'master' git branch is called 'trunk' in a Jenkins pipeline build
                    def isMasterBranch = env.BRANCH_NAME ==~ /master|trunk/

                    if (!isMasterBranch) {
                        def branchSplit = env.BRANCH_NAME.split('/')
                        def gitBranchName = branchSplit[1]
                        version = gitBranchName + '-' + env.BUILD_NUMBER
                    }

                    def image = docker.build("${repo}/${name}:${version}")

                    image.push()

                    if (isMasterBranch) {
                        echo 'Pushing build to latest'
                        image.push('latest')
                    }
                }
            }
        }
    }
}
