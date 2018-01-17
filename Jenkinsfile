#!groovy

def workerNode = "devel8"

void deploy(String deployEnvironment) {
    dir("deploy") {
        git(url: "gitlab@git-platform.dbc.dk:metascrum/deploy.git", credentialsId: "gitlab-meta")
    }
    sh """
		virtualenv -p python3 .
		. bin/activate
		pip3 install --upgrade pip
		pip3 install -U -e \"git+https://github.com/DBCDK/mesos-tools.git#egg=mesos-tools\"
		marathon-config-producer deploy/marathon single hydra-${deployEnvironment} --template-keys BUILD_NUMBER=${
        env.BUILD_NUMBER
    } -o hydra-service-${deployEnvironment}.json
		marathon-deployer -a ${MARATHON_TOKEN} -b https://mcp1.dbc.dk:8443 deploy hydra-service-${deployEnvironment}.json
	"""
}

pipeline {
    agent { label workerNode }
    tools {
        // refers to the name set in manage jenkins -> global tool configuration
        maven "Maven 3"
    }
    environment {
        MARATHON_TOKEN = credentials("METASCRUM_MARATHON_TOKEN")
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
                step([$class: 'hudson.plugins.pmd.PmdPublisher', checkstyle: 'target/pmd.xml'])
            }
        }
        stage("docker build") {
            steps {
                script {
                    def repo = "docker-io.dbc.dk"
                    def name = "hydra-service"
                    def version = env.BUILD_NUMBER

                    if (!env.BRANCH_NAME ==~ /master|trunk/ ) {
                        def branchSplit = env.BRANCH_NAME.split('/')
                        def gitBranchName = branchSplit[1]
                        version = gitBranchName + '-' + env.BUILD_NUMBER
                    }

                    def image = docker.build("${repo}/${name}:${version}")

                    image.push()

                    if (env.BRANCH_NAME == 'master') {
                        image.push('latest')
                    }
                }
            }
        }

        stage("deploy staging") {
            when {
                branch "master"
            }
            steps {
                script {
                    echo 'Deploy not yet implemented'
                }
                //deploy("staging")
            }
        }
    }
}