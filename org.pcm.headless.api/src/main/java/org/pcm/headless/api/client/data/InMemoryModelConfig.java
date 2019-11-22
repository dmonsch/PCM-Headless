package org.pcm.headless.api.client.data;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class InMemoryModelConfig {

	private List<EObject> repositorys = new ArrayList<>();
	private EObject system;
	private EObject allocation;
	private EObject usage;
	private EObject resourceEnvironment;
	private EObject monitorRepository;

	private List<EObject> additionals = new ArrayList<>();

	public List<EObject> getAllModels() {
		List<EObject> res = Lists.newArrayList(system, allocation, usage, resourceEnvironment, monitorRepository);
		res.addAll(repositorys);
		res.addAll(additionals);
		return res;
	}

	public void clear() {
		this.repositorys.clear();
		system = null;
		allocation = null;
		usage = null;
		resourceEnvironment = null;
		monitorRepository = null;
		additionals.clear();
	}

}
