@Library('PipelineSteps')
import com.lowes.jenkins.shared.SonarQubeHelper
import com.lowes.jenkins.shared.SecurityScanSteps
import com.lowes.jenkins.shared.JenkinsHelper

    JenkinsHelper jenkinsHelper     = new JenkinsHelper(this)
	def securityScan                = new SecurityScanSteps(env,steps)
    def sonarQubeHelper             = new SonarQubeHelper(env, steps);
    def projKey                     = "OMNICAT"
    def projName                    = "backinstock-gcs-processor"
    def sonarName                   = "${projName}"
    def veraAppName                 = "Digital-backinstock-gcs-processor"
    def uploadIncludesPattern       = "target/bisgcsfileprocessor*.jar"
    def envName                     = "${env.BRANCH_NAME}"
    def optionalParams              = " -Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-ut/jacoco.xml" +
                                      " -Dsonar.projectName=${projKey}-${projName}" +
                                      " -Dsonar.language=java" +
                                      " -Dsonar.sources=src/main" +
                                      " -Dsonar.tests=src/test" +
                                      " -Dsonar.exclusions=src/test/java/**/*,target/**/*,src/test/java/**/*,target/**/*,src/test/java/**/*,target/**,**/FileprocessorApplication.java,src/main/java/com/lowes/backinstock/fileprocessor/configuration/**"+
                                      " -Dsonar.coverage.exclusions=src/test/java/**/*,target/**/*,src/test/java/**/*,target/**/*,src/test/java/**/*,target/**,**/FileprocessorApplication.java,src/main/java/com/lowes/backinstock/fileprocessor/configuration/**"+
                                      " -Dsonar.java.libraries=/home/jenkins/.m2/repository" +
                                      " -Dsonar.java.test.libraries=/home/jenkins/.m2/repository"
    def IMAGE_TAG

pipeline {
        agent {
            label 'docker'
        }
    environment {
        GOOGLE_CREDENTIALS = credentials('NPE_GCR_CARBON_CREDENTIALS')
        repository = 'gcr.io/gcp-ushi-carbon-svcs-dev/backinstock-gcs-processor'
        projKey = "OMNICAT"
        projName = "backinstock-gcs-processor"
        apiVersion = "v1"
        mavenReleasesRepo = "http://nexus.d.lowes.com/repository/maven-releases"
        mavenSnapshotsRepo = "http://nexus.d.lowes.com/repository/maven-snapshots"
        MAVEN_OPTS = "-Dmaven.repo.local=/lowes/m2/"
        JDK_IMAGE = "gcr.io/gcp-ushi-carbon-svcs-dev/irs-image-jdk:8u212-alpine3.9"
        
    }
    stages {
        stage('Build and Test Application') {
            agent {
                docker {
                    reuseNode true
                    image '${JDK_IMAGE}'
                    args '-v /home/jenkins/.m2:/lowes/m2/'
                }
            }
            steps {
                sh './mvnw --settings /lowes/m2/settings.xml package'
            }
        }
        stage('Security Scan') {
            steps{
                script{
                    if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop') {
                        securityScan.veracodeScan(veraAppName, projName, uploadIncludesPattern)
                    }
                }
            }
        }
        stage('Docker Build and Push Image') {
          	when {
          	        expression{env.BRANCH_NAME.startsWith('master') || env.BRANCH_NAME.startsWith('develop')}
               }
            steps {
                script {
                    COMMIT_ID = sh(returnStdout: true, script: 'git rev-parse HEAD')
                    IMAGE_TAG = "JENKINS-${env.BUILD_ID}_${BRANCH_NAME}_${COMMIT_ID}"

                    sh 'echo "$GOOGLE_CREDENTIALS" > keyfile.json'
                    sh 'docker login -u _json_key -p "$(cat keyfile.json)" https://gcr.io'
                    sh "docker build . -t ${repository}:${IMAGE_TAG}"
                    sh "docker push ${repository}:${IMAGE_TAG}"
                }
            }
        }
        stage('Trigger Deployment') {
            when {
                anyOf {
                    branch 'master'
                    branch 'develop'// add/remove branches here which auto deploys to spinnaker
                }
            }
            steps {
                script {
                    jenkinsHelper.triggerSpinnakerWebhook(repository, IMAGE_TAG, 'backInStockGCSProcessor')
                }
            }
        }
    }
    post {
        failure {
            script {
                mail (to: 'mpedava@lowes.com',
                        subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) failed.",
                        body: "Please visit ${env.BUILD_URL} for further information."
                );
            }
        }
        cleanup{
            cleanWs();
        }
    }
}