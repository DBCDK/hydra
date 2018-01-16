pipeline {
    agent { label 'devel8' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20', daysToKeepStr: '20'))
        timestamps()
        timeout(time: 10, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/3 * * * *')
    }

    environment {
        MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dorg.slf4j.simpleLogger.showThreadName=true"
        JAVA_OPTS="-XX:-UseSplitVerifier"
    }

    tools {
        maven 'maven 3.5'
    }

    stages {

        stage('build') {
            steps {
                sh """
                    mvn clean
                    mvn install pmd:pmd findbugs:findbugs javadoc:aggregate -Dmaven.test.failure.ignore=false
                """

                echo '---------------------------------------------------------'
                echo '   ${env.JOB_NAME}'
                echo '---------------------------------------------------------'
            }
        }

        stage('docker build') {
            environment {
                PUSH = "dontpush"
            }
            steps {
                dir('docker') {
                    script {
                        if(env.BRANCH_NAME == "master") PUSH = "--push"
                    }

                    sh """
                        echo 'docker should be building here...'
                    """
                }
            }
        }

    }

    post {
        always {
            archiveArtifacts 'hydra-api/target/*.war'
        }
    }

}
