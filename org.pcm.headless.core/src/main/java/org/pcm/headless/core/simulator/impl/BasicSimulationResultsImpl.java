package org.pcm.headless.core.simulator.impl;

import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.edp2.dao.exception.DataNotAccessibleException;
import org.palladiosimulator.edp2.impl.RepositoryManager;
import org.palladiosimulator.edp2.models.Repository.LocalMemoryRepository;
import org.pcm.headless.core.data.InMemoryRepositoryReader;
import org.pcm.headless.core.simulator.ISimulationResults;
import org.pcm.headless.shared.data.results.InMemoryResultRepository;

import lombok.extern.java.Log;

@Log
public class BasicSimulationResultsImpl implements ISimulationResults {

	private List<LocalMemoryRepository> repositories;

	public BasicSimulationResultsImpl() {
		this.repositories = new ArrayList<>();
	}

	public void addRepository(LocalMemoryRepository nRepository) {
		this.repositories.add(nRepository);
	}

	@Override
	public List<LocalMemoryRepository> getRaw() {
		return repositories;
	}

	@Override
	public InMemoryResultRepository getConvertedRepository() {
		InMemoryRepositoryReader reader = new InMemoryRepositoryReader();
		if (repositories.size() == 0) {
			return null;
		} else {
			InMemoryResultRepository currRepo = reader.convertRepository(repositories.get(0));
			for (int i = 1; i < repositories.size(); i++) {
				currRepo.merge(reader.convertRepository(repositories.get(i)));
			}
			return currRepo;
		}
	}

	@Override
	public void close() {
		repositories.forEach(r -> cleanUp(r));
	}

	private void cleanUp(LocalMemoryRepository results) {
		try {
			results.close();
		} catch (DataNotAccessibleException e) {
			log.warning("The repository could not be closed. This may lead to a memory leak.");
		} finally {
			RepositoryManager.removeRepository(RepositoryManager.getCentralRepository(), results);
		}
	}

}
