package apps.defi

import ru.abrosimov.jenkins.context.Application

class Defi implements Application {
    String mavenGroup = "ru.abrosimov.defi"
    String mavenArtifact = "defi"
    String image = "vabrosimov/defi"
    String versionParamName = "VERSION_DEFI"
    String vmAddress = "176.108.250.97"
    String vmUser = "vabrosimov"
    String digest = ""

    @Override
    void setDigest(String digest) {
        this.digest = digest
    }
}

return new Defi()