package in.org.projecteka.hiu.dataprocessor;

import in.org.projecteka.hiu.dataprocessor.model.DataContext;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;


public interface HITypeResourceProcessor {
    public boolean supports(ResourceType type);

    void process(Resource resource, DataContext context);
}