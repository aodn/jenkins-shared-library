#!groovy

def getUid() {
    node {
        return sh(script: 'id -u jenkins', returnStdout: true).trim()
    }
}
