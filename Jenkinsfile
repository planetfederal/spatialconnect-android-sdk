node {
  withCredentials([string(credentialsId: 'boundlessgeoadmin-token', variable: 'GITHUB_TOKEN'), string(credentialsId: 'sonar-jenkins-pipeline-token', variable: 'SONAR_TOKEN')]) {

    currentBuild.result = "SUCCESS"

    try {
      stage('Checkout'){
        checkout scm
        sh """        
          echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
        """
	sh """
	  sh "echo ${env.android-sdk-license} > licenses/android-sdk-license"
	  sh "echo ${env.android-sdk-preview-license} > licenses/android-sdk-preview-license"
	"""
      }

     stage('DockerBuild'){
           docker build -t "alpine:android-sdk" . --no-cache
	}
     stage('Install'){
          docker run -it -v \$(pwd -P):/code \
	     -w /code alpine:android-sdk sh \
	     -c ‘bash -c “./gradlew install”’
	}
    }
    catch (err) {

      currentBuild.result = "FAILURE"
        throw err
    } finally {
      // Success or failure, always send notifications
      echo currentBuild.result
      notifyBuild(currentBuild.result)
    }
  }
}
// Slack Integration

def notifyBuild(String buildStatus = currentBuild.result) {

  // generate a custom url to use the blue ocean endpoint
  def jobName =  "${env.JOB_NAME}".split('/')
  def repo = jobName[0]
  def pipelineUrl = "${env.JENKINS_URL}blue/organizations/jenkins/${repo}/detail/${env.BRANCH_NAME}/${env.BUILD_NUMBER}/pipeline"
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nJenkins: ${pipelineUrl}\n"
  def summary = (env.CHANGE_ID != null) ? "${subject}\nAuthor: ${env.CHANGE_AUTHOR}\n${env.CHANGE_URL}\n" : "${subject}"

  // Override default values based on build status
  if (buildStatus == 'SUCCESS') {
    colorName = 'GREEN'
    colorCode = '#228B22'
  }

  // Send notifications
  slackSend (color: colorCode, message: summary, channel: '#spatialconnect-android-sdk')
}
