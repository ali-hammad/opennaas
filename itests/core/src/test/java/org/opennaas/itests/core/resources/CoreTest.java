package org.opennaas.itests.core.resources;

import static org.junit.Assert.*;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.*;
import static org.opennaas.extensions.itests.helpers.OpennaasExamOptions.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennaas.core.resources.ILifecycle.State;
import org.opennaas.core.resources.IResource;
import org.opennaas.core.resources.IResourceManager;
import org.opennaas.core.resources.ResourceException;
import org.opennaas.core.resources.action.ActionSet;
import org.opennaas.core.resources.action.IActionSet;
import org.opennaas.core.resources.descriptor.ResourceDescriptor;
import org.opennaas.core.resources.helpers.ResourceDescriptorFactory;
import org.opennaas.core.resources.mock.MockAction;
import org.opennaas.core.resources.mock.MockProfileFactory;
import org.opennaas.core.resources.profile.IProfile;
import org.opennaas.core.resources.profile.IProfileManager;
import org.opennaas.core.resources.profile.ProfileDescriptor;
import org.opennaas.core.resources.protocol.IProtocolManager;
import org.opennaas.core.resources.protocol.IProtocolSession;
import org.opennaas.core.resources.protocol.IProtocolSessionManager;
import org.opennaas.core.resources.protocol.ProtocolException;
import org.opennaas.core.resources.protocol.ProtocolSessionContext;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class CoreTest
{
	static Log						log	= LogFactory.getLog(CoreTest.class);

	@Inject
	private IResourceManager		resourceManager;

	@Inject
	private IProfileManager			profileManager;

	@Inject
	private BundleContext			bundleContext;

	@Inject
	IProtocolManager				protocolManager;

	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.opennaas.extensions.router.repository)")
	private BlueprintContainer		routerRepositoryService;

	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.opennaas.extensions.router.capability.chassis)")
	private BlueprintContainer		chasisService;

	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.opennaas.extensions.router.capability.ip)")
	private BlueprintContainer		ipService;

	@Inject
	@Filter("(osgi.blueprint.container.symbolicname=org.opennaas.core.tests-mockprofile)")
	private BlueprintContainer		mockProfileService;

	private ResourceDescriptor		resourceDescriptor;
	private ProtocolSessionContext	sessionContext;

	@Configuration
	public static Option[] configuration() {
		return options(opennaasDistributionConfiguration(),
				includeFeatures("opennaas-router", "opennaas-junos", "nexus-testprofile"),
				noConsole(),
				keepRuntimeFolder());
	}

	// from protocolsessionmanagertest
	@Before
	public void init() {

		List<String> capabilities = new ArrayList<String>();
		capabilities.add("chassis");
		capabilities.add("queue");
		resourceDescriptor = ResourceDescriptorFactory.newResourceDescriptor("TestResource", "router", capabilities);

		sessionContext = newSessionContextNetconf();
	}

	@After
	public void clearRepo() {

		resourceManager.destroyAllResources();

		log.info("Clearing resource repo");

		IResource[] toRemove = new IResource[resourceManager.listResources().size()];
		toRemove = resourceManager.listResources().toArray(toRemove);
		int count = 0;
		for (IResource resource : toRemove) {
			if (resource.getState().equals(State.ACTIVE)) {
				try {
					resourceManager.stopResource(resource.getResourceIdentifier());
				} catch (ResourceException e) {
					log.error("Failed to remove resource " + resource.getResourceIdentifier().getId() + " from repository.");
				}
			}
			try {
				resourceManager.removeResource(resource.getResourceIdentifier());
				count++;
			} catch (ResourceException e) {
				log.error("Failed to remove resource " + resource.getResourceIdentifier().getId() + " from repository.");
			}
		}

		log.info("Resource repo cleared! Removed " + count + " resources.");
	}

	/**
	 * Creates a resource indicating a profileId in its descriptor. Checks profile is loaded correctly and profile actions are called instead of
	 * original ones.
	 */
	@Test
	public void createResourceWithProfileManager() {

		try {

			List<String> capabilities = new ArrayList<String>();
			capabilities.add("chassis");
			capabilities.add("queue");

			ProfileDescriptor profileDescriptor = ResourceDescriptorFactory.newProfileDescriptor("profile", "router");
			ResourceDescriptor resourceDescriptor = ResourceDescriptorFactory.newResourceDescriptor("TestResource", "router", capabilities);
			/* specify profiles */
			Map<String, IActionSet> actionSets = new HashMap<String, IActionSet>();
			ActionSet actionSet = new ActionSet();
			actionSet.putAction("setIPv4", MockAction.class);

			actionSets.put("chassis", actionSet);

			IProfile profile = MockProfileFactory.newMockProfilefactory(profileDescriptor, actionSets);

			profileManager.addProfile(profile);

			resourceDescriptor.setProfileId(profile.getProfileName());

			// call createResource(resourceDescriptor)
			IResource resource = resourceManager.createResource(resourceDescriptor);
			createProtocolForResource(resource.getResourceIdentifier().getId());
			resourceManager.startResource(resource.getResourceIdentifier());

			// assert profile loading has been correct
			Assert.assertNotNull(resource.getProfile());
			Assert.assertTrue(resource.getProfile().equals(profile));

			// // TODO launch setInterface Action and assert DummyAction is executed instead of original one

		} catch (ResourceException e) {
			log.error("Error ocurred!!!", e);
			Assert.fail(e.getMessage());
		} catch (ProtocolException e) {
			log.error("Error ocurred!!!", e);
			Assert.fail(e.getMessage());
		} finally {
			clearRepo();
		}

	}

	/**
	 * Creates a resource indicating a profileId in its descriptor. Checks profile is loaded correctly and profile actions are called instead of
	 * original ones.
	 */
	@Test
	public void createResourceWithProfileEmtpy() {

		try {

			List<String> capabilities = new ArrayList<String>();
			capabilities.add("chassis");
			capabilities.add("queue");

			// put profile in profileManager
			List<ProfileDescriptor> profileDescriptors = profileManager.listProfiles();
			Assert.assertFalse(profileDescriptors.isEmpty());
			Assert.assertTrue("TestProfile".equals(profileDescriptors.get(0).getProfileName()));

			log.info("Found profile with name: " + profileDescriptors.get(0).getProfileName());
			IProfile profile = profileManager.getProfile(profileDescriptors.get(0).getProfileName());

			// create resourceDescriptor with profile id

			ResourceDescriptor resourceDescriptor = ResourceDescriptorFactory.newResourceDescriptor("TestResource", "router", capabilities);
			resourceDescriptor.setProfileId(profile.getProfileName());

			// call createResource(resourceDescriptor)
			IResource resource = resourceManager.createResource(resourceDescriptor);
			createProtocolForResource(resourceDescriptor.getId());
			// log.info("UseProfileBundleTest: resource. getResourceIdentifier.getId gives us: " + resource.getResourceIdentifier().getId());
			resourceManager.startResource(resource.getResourceIdentifier());

			// assert profile loading has been correct
			Assert.assertNotNull(resource.getProfile());
			Assert.assertNotNull(profile);
			// FIXME there are problems with CGLIB used by hibernate and equals()
			// Assert.assertTrue(resource.getProfile().equals(profile));
			Assert.assertTrue(resource.getProfile().getProfileName().equals(profile.getProfileName()));
			Assert.assertTrue(resource.getProfile().getProfileDescriptor().equals(profile.getProfileDescriptor()));

			// TODO launch setInterface Action and assert DummyAction is executed instead of original one

		} catch (ResourceException e) {
			log.error("Error ocurred!!!", e);
			Assert.fail(e.getMessage());
		} catch (ProtocolException e) {
			log.error("Error ocurred!!!", e);
			Assert.fail(e.getMessage());
		} finally {
			clearRepo();
		}
		// catch (ProtocolException e) {
		// log.error("Error ocurred!!!", e);
		// Assert.fail(e.getMessage());
		// }
	}

	/*
	 * PROTOCOL SESSION MANAGER TESTS
	 */
	@Test
	public void testSessionManagerLifecycleIsSyncWithResourceLifecycle() throws Exception {

		IResource resource = resourceManager.createResource(resourceDescriptor);
		String resourceId = resource.getResourceIdentifier().getId();

		assertTrue("New resource should be registered in ProtocolManager",
				protocolManager.getAllResourceIds().contains(resourceId));

		// FIXME this will create a protocolSessionManager if it does not exists
		// use a true getter method (not modifying state) to test!
		IProtocolSessionManager sessionManager = protocolManager.getProtocolSessionManager(resourceId);
		assertEquals(resourceId, sessionManager.getResourceID());
		assertTrue("No context registered yet", sessionManager.getRegisteredContexts().isEmpty());
		assertTrue("No sessions are created", sessionManager.getAllProtocolSessionIds().isEmpty());

		protocolManager.getProtocolSessionManager(resourceId).registerContext(sessionContext);
		assertTrue("Context is registered", sessionManager.getRegisteredContexts().contains(sessionContext));
		assertTrue("No sessions are created", sessionManager.getAllProtocolSessionIds().isEmpty());

		resourceManager.startResource(resource.getResourceIdentifier());

		assertEquals(1, sessionManager.getAllProtocolSessionIds().size());
		for (String sessionId : sessionManager.getAllProtocolSessionIds()) {
			IProtocolSession session = sessionManager.getSessionById(sessionId, false);
			assertEquals(sessionContext, session.getSessionContext());
			assertEquals(IProtocolSession.Status.CONNECTED, session.getStatus());
		}

		resourceManager.stopResource(resource.getResourceIdentifier());
		assertFalse("Context should be still registered ", sessionManager.getRegisteredContexts().isEmpty());
		assertTrue("No active sessions", sessionManager.getAllProtocolSessionIds().isEmpty());

		resourceManager.removeResource(resource.getResourceIdentifier());
		assertTrue("Contexts should be unregistered", sessionManager.getRegisteredContexts().isEmpty());
		assertTrue("Sessions should be destroyed", sessionManager.getAllProtocolSessionIds().isEmpty());

		assertFalse("Removed resource should not be registered in ProtocolManager",
				protocolManager.getAllResourceIds().contains(resourceId));
	}

	/*
	 * HELPERS
	 */

	private void createProtocolForResource(String resourceId) throws ProtocolException {
		protocolManager.getProtocolSessionManagerWithContext(resourceId, newSessionContextNetconf());
	}

	/**
	 * Configure the protocol to connect
	 */
	private ProtocolSessionContext newSessionContextNetconf() {
		String uri = System.getProperty("protocol.uri");
		if (uri == null || uri.equals("${protocol.uri}") || uri.isEmpty()) {
			uri = "mock://user:pass@host.net:2212/mocksubsystem";
		}

		ProtocolSessionContext protocolSessionContext = new ProtocolSessionContext();

		protocolSessionContext.addParameter(ProtocolSessionContext.PROTOCOL_URI, uri);
		protocolSessionContext.addParameter(ProtocolSessionContext.PROTOCOL, "netconf");

		return protocolSessionContext;
	}

}