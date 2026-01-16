import ru.abrosimov.jenkins.cd.context.Application
import ru.abrosimov.jenkins.cd.context.PipelineContext

class PipelineContextImpl extends PipelineContext {

    PipelineContextImpl(Object jenkins) {
        super(jenkins)
    }

    String registry = "95.174.94.249:8082/repository/registry/"
    List<Application> applications = null
}

return new PipelineContextImpl(this)