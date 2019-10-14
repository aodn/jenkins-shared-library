#!groovy

def getHarvesterJobName(String processDirectory) {
    def finder = new FileNameByRegexFinder()
    processFile = finder.getFileNames(processDirectory, '.*_harvester_[0-9]+\\.[0-9]+\\.item')[0]

    if (!processFile) {
        return false
    }

    return processFile.split('/')[-1].replaceAll('_[0-9]+\\.[0-9]+\\.item$','')
}
