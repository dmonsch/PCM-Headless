package org.pcm.headless.api;

import java.io.File;

import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.parameter.ParameterFactory;
import org.palladiosimulator.pcm.parameter.VariableCharacterisation;
import org.palladiosimulator.pcm.parameter.VariableCharacterisationType;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.OpenWorkload;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelFactory;
import org.pcm.headless.api.client.PCMHeadlessClient;
import org.pcm.headless.api.client.SimulationClient;
import org.pcm.headless.api.client.measure.MonitorRepositoryBuilderUtil;
import org.pcm.headless.api.util.ModelUtil;
import org.pcm.headless.api.util.PCMUtil;
import org.pcm.headless.shared.data.ESimulationType;
import org.pcm.headless.shared.data.config.HeadlessSimulationConfig;

import de.uka.ipd.sdq.stoex.StoexFactory;
import de.uka.ipd.sdq.stoex.VariableReference;

public class HeadlessClientTestInMemoryCoCoME {

	public static void main(String[] args) {
		PCMHeadlessClient client = new PCMHeadlessClient("http://127.0.0.1:8080/");
		if (client.isReachable(3000)) {
			System.out.println("Backend erreichbar.");

			File allocationFile = new File("examples/cocome/cocome.allocation");
			File monitorRepositoryFile = new File("examples/cocome/cocome_gen.monitorrepository");
			File repositoryFile = new File("examples/cocome/cocome.repository");
			File resourceEnvironmentFile = new File("examples/cocome/cocome.resourceenvironment");
			File systemFile = new File("examples/cocome/cocome.system");
			File usageFile = new File("examples/cocome/cocome.usagemodel");

			MonitorRepositoryBuilderUtil monitorBuilder = new MonitorRepositoryBuilderUtil(repositoryFile, systemFile,
					usageFile);
			monitorBuilder.monitorExternalCalls().monitorUsageScenarios();
			monitorBuilder.saveToFile(monitorRepositoryFile, new File("examples/cocome/cocome_gen.measuringpoint"));

			org.palladiosimulator.pcm.system.System system = ModelUtil.readFromFile(systemFile.getAbsolutePath(),
					org.palladiosimulator.pcm.system.System.class);

			SimulationClient sim = client.prepareSimulation();

			Allocation alloc = ModelUtil.readFromFile(allocationFile.getAbsolutePath(), Allocation.class);
			Repository repo = ModelUtil.readFromFile(repositoryFile.getAbsolutePath(), Repository.class);
			org.palladiosimulator.pcm.system.System sys = ModelUtil.readFromFile(systemFile.getAbsolutePath(),
					org.palladiosimulator.pcm.system.System.class);
			UsageModel usage = ModelUtil.readFromFile(usageFile.getAbsolutePath(), UsageModel.class);
			ResourceEnvironment env = ModelUtil.readFromFile(resourceEnvironmentFile.getAbsolutePath(),
					ResourceEnvironment.class);

			sim.setAllocation(alloc);
			sim.setRepository(repo);
			sim.setSystem(sys);
			sim.setUsageModel(usage);
			sim.setResourceEnvironment(env);
			sim.setMonitorRepository(monitorBuilder.getModel());

			sim.setSimulationConfig(HeadlessSimulationConfig.builder().type(ESimulationType.SIMUCOM).repetitions(1)
					.simulationTime(500000).maximumMeasurementCount(30000).experimentName("CoCoME Simulation").build());

			sim.createTransitiveClosure();
			sim.sync();

			long simStart = System.currentTimeMillis();
			boolean success = sim.executeSimulation(res -> {
				System.out.println(res.getValues().size());
				System.out.println("Simulation needed " + (System.currentTimeMillis() - simStart) + "ms");
			});

			ModelUtil.saveToFile(system, systemFile);
			ModelUtil.saveToFile(alloc, allocationFile);
			ModelUtil.saveToFile(repo, repositoryFile);
			ModelUtil.saveToFile(usage, usageFile);
			ModelUtil.saveToFile(env, resourceEnvironmentFile);
		}
	}

	private static UsageModel buildUsageModelProgrammatically(Repository repo,
			org.palladiosimulator.pcm.system.System system) {
		UsageModel model = UsagemodelFactory.eINSTANCE.createUsageModel();
		UsageScenario scenario = UsagemodelFactory.eINSTANCE.createUsageScenario();

		// ids
		OperationSignature sig = PCMUtil.getElementById(repo, OperationSignature.class, "_2NKrM1cvEeGI_ZRmqWdpYA");
		OperationProvidedRole role = PCMUtil.getElementById(system, OperationProvidedRole.class,
				"_MSQMUFcnEd23wcZsd06DZ");

		// create fixed elements
		Start start = UsagemodelFactory.eINSTANCE.createStart();
		Stop stop = UsagemodelFactory.eINSTANCE.createStop();

		EntryLevelSystemCall entryCall = UsagemodelFactory.eINSTANCE.createEntryLevelSystemCall();
		entryCall.setOperationSignature__EntryLevelSystemCall(sig);
		entryCall.setProvidedRole_EntryLevelSystemCall(role);

		start.setSuccessor(entryCall);
		entryCall.setSuccessor(stop);

		VariableUsage usage = ParameterFactory.eINSTANCE.createVariableUsage();
		VariableCharacterisation character = ParameterFactory.eINSTANCE.createVariableCharacterisation();
		VariableReference reference = StoexFactory.eINSTANCE.createVariableReference();

		reference.setReferenceName("saleTO");
		character.setType(VariableCharacterisationType.get("NUMBER_OF_ELEMENTS"));
		character.setSpecification_VariableCharacterisation(buildPCMVariable(
				"IntPMF[(1;0.0470692718)(2;0.0488454707)(3;0.0537300178)(4;0.0572824156)(5;0.0479573712)(6;0.0381882771)(7;0.0488454707)(8;0.3530195382)(9;0.0248667851)(10;0.0213143872)(11;0.026642984)(12;0.0239786856)(13;0.0195381883)(14;0.0288632327)(15;0.0257548845)(16;0.0328596803)(17;0.0261989343)(18;0.0275310835)(19;0.026642984)(20;0.0208703375)]"));
		usage.setNamedReference__VariableUsage(reference);
		usage.getVariableCharacterisation_VariableUsage().add(character);

		entryCall.getInputParameterUsages_EntryLevelSystemCall().add(usage);

		OpenWorkload workload = UsagemodelFactory.eINSTANCE.createOpenWorkload();
		workload.setInterArrivalTime_OpenWorkload(buildPCMVariable("1500"));

		ScenarioBehaviour back = UsagemodelFactory.eINSTANCE.createScenarioBehaviour();
		back.getActions_ScenarioBehaviour().add(start);
		back.getActions_ScenarioBehaviour().add(entryCall);
		back.getActions_ScenarioBehaviour().add(stop);

		scenario.setWorkload_UsageScenario(workload);
		scenario.setScenarioBehaviour_UsageScenario(back);

		model.getUsageScenario_UsageModel().add(scenario);

		return model;
	}

	private static PCMRandomVariable buildPCMVariable(String value) {
		PCMRandomVariable ret = CoreFactory.eINSTANCE.createPCMRandomVariable();
		ret.setSpecification(value);
		return ret;
	}

}