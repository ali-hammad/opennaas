package org.opennaas.core.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.opennaas.core.resources.descriptor.ResourceDescriptor;

/**
 * This interface must be implemented by all the classes that want to manage resources. They must listen to all the IResourceRepositories, and react
 * to their events creating, modifying and removing resources
 * 
 * @author Eduard Grasa
 * @author Roc Vallès <roc.valles@i2cat.net>
 * 
 */
@Path("/resources")
public interface IResourceManager {

	public static final String	NOTIFICATIONS_TOPIC	= "com/iaasframework/resources/core/ResourceManager";
	public static final String	NOTIFICATION_CODE	= "code";
	public static final String	RESOURCE_IDENTIFIER	= "resourceId";
	public static final String	RESOURCE_CREATED	= "resourceCreated";
	public static final String	RESOURCE_MODIFIED	= "resourceModified";
	public static final String	RESOURCE_REMOVED	= "resourceRemoved";
	public static final String	RESOURCE_STARTED	= "resourceStarted";
	public static final String	RESOURCE_STOPED		= "resourceStoped";

	/**
	 * Create a new resource with a given resourceDescriptor, and returns its id.
	 * 
	 * @param resourceDescriptor
	 * @returns the id of the new resource
	 * @throws ResourceException
	 */
	@Path("/create")
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String createResourceWS(ResourceDescriptor resourceDescriptor) throws ResourceException;

	/**
	 * Create a new resource with a given resourceDescriptor
	 * 
	 * @param resourceDescriptor
	 * @returns the new resource
	 * @throws ResourceException
	 */
	public IResource createResource(ResourceDescriptor resourceDescriptor) throws ResourceException;

	@Path("/{resourceId}/modify")
	@PUT
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public String modifyResource(@PathParam("resourceId") String resourceId, ResourceDescriptor resourceDescriptor) throws ResourceException;

	/**
	 * Modify the existing resource that matches the id (inside resourceDescriptor)
	 * 
	 * @param resourceIdentifier
	 * @param resourceDescriptor
	 * @return the modified resource
	 * @throws ResourceException
	 *             if failed to modify
	 */
	public IResource modifyResource(IResourceIdentifier resourceIdentifier, ResourceDescriptor resourceDescriptor) throws ResourceException;

	@Path("/{resourceId}/remove")
	@DELETE
	public void removeResource(@PathParam("resourceId") String resourceId) throws ResourceException;

	/**
	 * Remove the existing resource that matches the id
	 * 
	 * @param resourceIdentifier
	 * @throws ResourceException
	 */
	public void removeResource(IResourceIdentifier resourceIdentifier) throws ResourceException;

	/**
	 * List all the existing resources of a given type. If type is null, list all resources whatever its type is.
	 * 
	 * @return The list of the resources contained on the given type repository. Is the type is not a valid type of repository it will return null
	 *         value.
	 */
	@GET
	@Path("/getResourcesByType/{type}")
	@Produces(MediaType.APPLICATION_XML)
	public List<IResource> listResourcesByType(@PathParam("type") String type);

	/**
	 * List all resources in container.
	 * 
	 * @return
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_XML)
	public List<IResource> listResources();

	/**
	 * Returns a list of available resource types.
	 */
	@GET
	@Path("/getResourceTypes")
	@Produces(MediaType.APPLICATION_XML)
	public List<String> getResourceTypes();

	/**
	 * Get an existing resource
	 * 
	 * @param resourceIdentifier
	 *            the id of the resource to get
	 * @return the resource
	 * @throws ResourceException
	 *             if resource is not found
	 */
	public IResource getResource(IResourceIdentifier resourceIdentifier) throws ResourceException;

	/**
	 * Get an existing resource
	 * 
	 * @param resourceId
	 *            resource's resourceId
	 * @return the resource
	 * @throws ResourceException
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public IResource getResourceById(@PathParam("id") String resourceId) throws ResourceException;

	@POST
	@Path("{resourceId}/start")
	public void startResource(@PathParam("resourceId") String resourceId) throws ResourceException;

	/**
	 * Start an existing resource
	 * 
	 * @param resourceIdentifier
	 * @throws ResourceException
	 */
	public void startResource(IResourceIdentifier resourceIdentifier) throws ResourceException;

	@POST
	@Path("{resourceId}/stop")
	public void stopResource(@PathParam("resourceId") String resourceId) throws ResourceException;

	/**
	 * Stop an existing resource
	 * 
	 * @param resourceIdentifier
	 * @throws ResourceException
	 */
	public void stopResource(IResourceIdentifier resourceIdentifier) throws ResourceException;

	/**
	 * Export a resource descriptor to an XML file
	 * 
	 * @param resourceIdentifier
	 * @param fileName
	 * @throws ResourceException
	 */
	// FIXME remove or convert to ResourceDescriptor getResourceDescriptor(IResourceIdentifier resourceIdentifier).
	// export to file is a view functionality, not related with business logic.
	public void exportResourceDescriptor(IResourceIdentifier resourceIdentifier, String fileName) throws ResourceException;

	/**
	 * 
	 * @param resourceType
	 * @param resourceName
	 * @return
	 * @throws ResourceException
	 */
	@GET
	@Path("/getIdentifierFromResourceName")
	@Produces(MediaType.APPLICATION_XML)
	public IResourceIdentifier getIdentifierFromResourceName(@QueryParam("resourceType") String resourceType,
			@QueryParam("resourceName") String resourceName)
			throws ResourceException;

	/**
	 * 
	 * @param ID
	 * @return
	 * @throws ResourceException
	 */
	@GET
	@Path("/{id}/name")
	public String getNameFromResourceID(@PathParam("id") String ID) throws ResourceException;

	/**
	 * 
	 * @param resourceIdentifier
	 * @throws ResourceException
	 */
	public void forceStopResource(IResourceIdentifier resourceIdentifier) throws ResourceException;

	/**
	 * 
	 * @param resourceIdentifier
	 * @throws ResourceException
	 */
	public void destroyAllResources() throws ResourceException;

}
