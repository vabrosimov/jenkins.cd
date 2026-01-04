package apps.defi

import ru.abrosimov.jenkins.context.Application

class Defi implements Application {
    String mavenGroup = "ru.abrosimov.defi"
    String mavenArtifact = "defi"
    String image = "vabrosimov/defi"
    String versionParamName = "VERSION_DEFI"
}

return new Defi()