package org.pcm.headless.core.proxy;

import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;

import de.uka.ipd.sdq.simucomframework.resources.IResourceContainerFactory;
import de.uka.ipd.sdq.simucomframework.resources.SchedulingStrategy;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedLinkingResourceContainer;
import de.uka.ipd.sdq.simucomframework.resources.SimulatedResourceContainer;

public class ResourceContainerFactoryProxy implements IResourceContainerFactory {

	private ResourceEnvironment origin;

	public ResourceContainerFactoryProxy(ResourceEnvironment origin) {
		this.origin = origin;
	}

	@Override
	public void fillLinkingResourceContainer(SimulatedLinkingResourceContainer arg0) {
		// TODO i think this is wrong atm
		origin.getLinkingResources__ResourceEnvironment().forEach(lr -> {
			if (lr.getId().equals(arg0.getResourceContainerID())) {
				arg0.addActiveResource(lr, arg0.getResourceContainerID());
			}
		});
	}

	@Override
	public void fillResourceContainerWithNestedResourceContainers(SimulatedResourceContainer arg0) {
		origin.getResourceContainer_ResourceEnvironment().forEach(rc -> {
			resourceContainerCaseResourceContainers(rc, arg0);
		});
	}

	private void resourceContainerCaseResourceContainers(ResourceContainer rc, SimulatedResourceContainer arg0) {
		if (rc.getId().equals(arg0.getResourceContainerID())) {
			rc.getNestedResourceContainers__ResourceContainer().forEach(nr -> {
				arg0.addNestedResourceContainer(nr.getId());
			});
			if (rc.getParentResourceContainer__ResourceContainer() != null) {
				arg0.setParentResourceContainer(rc.getParentResourceContainer__ResourceContainer().getId());
			}
		}

		rc.getNestedResourceContainers__ResourceContainer().forEach(rci -> {
			resourceContainerCaseResourceContainers(rci, arg0);
		});
	}

	@Override
	public void fillResourceContainerWithResources(SimulatedResourceContainer arg0) {
		origin.getResourceContainer_ResourceEnvironment().stream().forEach(rc -> {
			resourceContainerCaseResources(arg0, rc);
		});
	}

	@Override
	public String[] getLinkingResourceContainerIDList() {
		return origin.getLinkingResources__ResourceEnvironment().stream().map(l -> l.getId()).toArray(String[]::new);
	}

	@Override
	public String[] getResourceContainerIDList() {
		return origin.getResourceContainer_ResourceEnvironment().stream().map(c -> c.getId()).toArray(String[]::new);
	}

	private void resourceContainerCaseResources(SimulatedResourceContainer src, ResourceContainer rc) {
		if (rc.getId().equals(src.getResourceContainerID())) {
			// active
			rc.getActiveResourceSpecifications_ResourceContainer().forEach(ar -> {
				src.addActiveResource(ar,
						ar.getActiveResourceType_ActiveResourceSpecification()
								.getResourceProvidedRoles__ResourceInterfaceProvidingEntity().stream()
								.map(pr -> pr.getEntityName()).toArray(String[]::new),
						rc.getId(), mapSchedulingStrategy(ar.getSchedulingPolicy().getId()));
			});
		} else {
			for (ResourceContainer rcc : rc.getNestedResourceContainers__ResourceContainer()) {
				resourceContainerCaseResources(src, rcc);
			}
		}
	}

	private String mapSchedulingStrategy(String strat) {
		if (strat.equals("FCFS")) {
			return SchedulingStrategy.FCFS;
		} else if (strat.equals("ProcessorSharing")) {
			return SchedulingStrategy.PROCESSOR_SHARING;
		} else if (strat.equals("Delay")) {
			return SchedulingStrategy.DELAY;
		}
		return strat;
	}

}
