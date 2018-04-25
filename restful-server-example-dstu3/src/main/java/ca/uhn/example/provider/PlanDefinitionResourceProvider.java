package ca.uhn.example.provider;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.StringType;

import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * This is a resource provider which stores PlanDefinition resources in memory using a HashMap. This is obviously not a production-ready solution for many reasons, 
 * but it is useful to help illustrate how to build a fully-functional server.
 */
public class PlanDefinitionResourceProvider implements IResourceProvider {

	/**
	 * This map has a resource ID as a key, and each key maps to a Deque list containing all versions of the resource with that ID.
	 */
	private Map<Long, Deque<PlanDefinition>> myIdToPlanDefinitionVersions = new HashMap<Long, Deque<PlanDefinition>>();

	/**
	 * This is used to generate new IDs
	 */
	private long myNextId = 1;

	/**
	 * Constructor, which pre-populates the provider with one resource instance.
	 */
	public PlanDefinitionResourceProvider() {
		long resourceId = myNextId++;
		
		PlanDefinition plan = new PlanDefinition();
		plan.setId(Long.toString(resourceId));
		plan.addIdentifier();
		plan.getIdentifier().get(0).setValue("00002");
		plan.setName("Test");

		LinkedList<PlanDefinition> list = new LinkedList<PlanDefinition>();
		list.add(plan);
		
		
		myIdToPlanDefinitionVersions.put(resourceId, list);

	}

	
	/**
	 * The "@Search" annotation indicates that this method supports the search operation. You may have many different method annotated with this annotation, to support many different search criteria.
	 * This example searches by family name.
	 * 
	 * @param theFamilyName
	 *            This operation takes one parameter which is the search criteria. It is annotated with the "@Required" annotation. This annotation takes one argument, a string containing the name of
	 *            the search criteria. The datatype here is StringDt, but there are other possible parameter types depending on the specific search criteria.
	 * @return This method returns a list of PlanDefinitions. This list may contain multiple matching resources, or it may also be empty.
	 */
	@Search()
	public List<PlanDefinition> findPlanDefinitionsByName(@RequiredParam(name = PlanDefinition.SP_NAME) StringType name) {
		LinkedList<PlanDefinition> retVal = new LinkedList<PlanDefinition>();

		/*
		 * Look for all PlanDefinitions matching the name
		 */
		for (Deque<PlanDefinition> nextPlanDefinitionList : myIdToPlanDefinitionVersions.values()) {
			PlanDefinition plan = nextPlanDefinitionList.getLast();
			if (name.toString().equals(plan.getName())) {
				retVal.add(plan);
				break;
			}
		}

		return retVal;
	}

	@Search
	public List<PlanDefinition> findPlanDefinitionsUsingArbitraryCtriteria() {
		LinkedList<PlanDefinition> retVal = new LinkedList<PlanDefinition>();

		for (Deque<PlanDefinition> nextPlanDefinitionList : myIdToPlanDefinitionVersions.values()) {
			PlanDefinition nextPlanDefinition = nextPlanDefinitionList.getLast();
			retVal.add(nextPlanDefinition);
		}
	
		return retVal;
	}
	
	/**
	 * The getResourceType method comes from IResourceProvider, and must be overridden to indicate what type of resource this provider supplies.
	 */
	@Override
	public Class<PlanDefinition> getResourceType() {
		return PlanDefinition.class;
	}

	/**
	 * This is the "read" operation. The "@Read" annotation indicates that this method supports the read and/or vread operation.
	 * <p>
	 * Read operations take a single parameter annotated with the {@link IdParam} paramater, and should return a single resource instance.
	 * </p>
	 * 
	 * @param theId
	 *            The read operation takes one parameter, which must be of type IdType and must be annotated with the "@Read.IdParam" annotation.
	 * @return Returns a resource matching this identifier, or null if none exists.
	 */
	@Read(version = true)
	public PlanDefinition readPlanDefinition(@IdParam IdType theId) {
		Deque<PlanDefinition> retVal;
		try {
			retVal = myIdToPlanDefinitionVersions.get(theId.getIdPartAsLong());
		} catch (NumberFormatException e) {
			/*
			 * If we can't parse the ID as a long, it's not valid so this is an unknown resource
			 */
			throw new ResourceNotFoundException(theId);
		}

		if (theId.hasVersionIdPart() == false) {
			return retVal.getLast();
		} else {
			for (PlanDefinition nextVersion : retVal) {
				String nextVersionId = nextVersion.getId();
				if (theId.getVersionIdPart().equals(nextVersionId)) {
					return nextVersion;
				}
			}
			// No matching version
			throw new ResourceNotFoundException("Unknown version: " + theId.getValue());
		}

	}




}
