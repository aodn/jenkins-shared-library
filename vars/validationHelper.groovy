#!groovy

import com.cloudbees.groovy.cps.NonCPS
import java.util.regex.Pattern

/**
 * Validate a string against a regex
 *
 * @param stringToValidate string
 * @param regex java.util.regex.Pattern
 * @return the project name or null
 */
@NonCPS
boolean validateString(String stringToValidate, Pattern regexPattern) {
    def matcher = stringToValidate =~ regexPattern
    return matcher.find()
}
